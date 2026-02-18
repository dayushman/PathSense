import UIKit
import PathSenseCore

// MARK: - Configuration

/// Top-level configuration for the PathSense auto-integration, mirroring
/// the Android `PathSenseConfig`.
public struct PathSenseConfig {
    /// Visual overlay configuration (style, crosshair, HUD, etc.)
    public var overlayConfig: PathOverlayConfig

    /// Optional global callback for all `PathEvent`s across every window.
    public var listener: ((PathEvent) -> Void)?

    public init(
        overlayConfig: PathOverlayConfig = PathOverlayConfig(),
        listener: ((PathEvent) -> Void)? = nil
    ) {
        self.overlayConfig = overlayConfig
        self.listener = listener
    }
}

// MARK: - PathSense (zero-config entry point)

/// Zero-config entry point for the PathSense SDK on iOS.
///
/// Call ``configure(_:)`` once — typically in your `App.init()` (SwiftUI)
/// or `application(_:didFinishLaunchingWithOptions:)` (UIKit) — and the
/// SDK automatically attaches to every window: intercepting touch events,
/// tracking paths, recognizing gestures, and rendering a visual overlay.
///
/// **No views need to be added. No code changes in ViewControllers or Views.**
///
/// ```swift
/// // SwiftUI
/// @main
/// struct MyApp: App {
///     init() {
///         PathSense.configure()
///     }
///     var body: some Scene { ... }
/// }
///
/// // UIKit
/// func application(_ application: UIApplication,
///                   didFinishLaunchingWithOptions ...) -> Bool {
///     PathSense.configure()
///     return true
/// }
/// ```
///
/// To receive gesture/metrics events globally:
/// ```swift
/// PathSense.configure(PathSenseConfig(
///     listener: { event in print("PathSense: \(event)") }
/// ))
/// ```
public enum PathSense {

    // MARK: Public

    /// Whether `configure()` has been called.
    public private(set) static var isConfigured = false

    /// The active configuration.
    public private(set) static var config = PathSenseConfig()

    /// Initialize the SDK. Safe to call multiple times — subsequent calls
    /// update the config but do not re-register observers or swizzle again.
    public static func configure(_ config: PathSenseConfig = PathSenseConfig()) {
        Self.config = config

        // Update existing overlays with new config
        if isConfigured {
            updateAttachedOverlays()
            return
        }
        isConfigured = true

        swizzleSendEvent()
        observeWindowLifecycle()

        // Attach to already-visible windows
        for scene in UIApplication.shared.connectedScenes {
            guard let windowScene = scene as? UIWindowScene else { continue }
            for window in windowScene.windows where !window.isHidden {
                attach(to: window)
            }
        }
    }

    /// Returns the `PathTracker` attached to the given window, or `nil`
    /// if the SDK has not yet attached to it.
    public static func tracker(for window: UIWindow) -> PathTracker? {
        return attachments.object(forKey: window)?.tracker
    }

    // MARK: - Internals

    /// Weak-key map from UIWindow → Attachment (auto-cleared when window deallocs).
    private static let attachments = NSMapTable<UIWindow, Attachment>.weakToStrongObjects()

    // MARK: Swizzle

    private static func swizzleSendEvent() {
        let original = #selector(UIWindow.sendEvent(_:))
        let swizzled = #selector(UIWindow._pathSense_sendEvent(_:))

        guard let origMethod = class_getInstanceMethod(UIWindow.self, original),
              let swizMethod = class_getInstanceMethod(UIWindow.self, swizzled) else { return }

        method_exchangeImplementations(origMethod, swizMethod)
    }

    // MARK: Window lifecycle observation

    private static func observeWindowLifecycle() {
        NotificationCenter.default.addObserver(
            forName: UIWindow.didBecomeVisibleNotification,
            object: nil,
            queue: .main
        ) { note in
            guard let window = note.object as? UIWindow else { return }
            attach(to: window)
        }
    }

    // MARK: Attach / Detach

    static func attach(to window: UIWindow) {
        guard attachments.object(forKey: window) == nil else { return }
        // PathTrackingWindow already has its own tracking — skip to avoid double-tracking.
        guard !(window is PathTrackingWindow) else { return }

        let tracker = PathTracker()
        if let listener = config.listener {
            tracker.listener = listener
        }

        let overlay = TouchOverlayView(tracker: tracker)
        overlay.overlayConfig = config.overlayConfig
        overlay.isUserInteractionEnabled = false
        overlay.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        overlay.frame = window.bounds
        window.addSubview(overlay)

        attachments.setObject(Attachment(tracker: tracker, overlay: overlay), forKey: window)
    }

    private static func updateAttachedOverlays() {
        let enumerator = attachments.objectEnumerator()
        while let attachment = enumerator?.nextObject() as? Attachment {
            attachment.overlay.overlayConfig = config.overlayConfig
            if let listener = config.listener {
                attachment.tracker.listener = listener
            }
        }
    }

    // MARK: Event handling (called from swizzled sendEvent)

    fileprivate static func handleSendEvent(_ event: UIEvent, in window: UIWindow) {
        guard PathSenseUI.isEnabled else { return }
        guard let attachment = attachments.object(forKey: window) else { return }

        // Keep overlay on top of all other subviews
        window.bringSubviewToFront(attachment.overlay)

        guard let touches = event.allTouches, let touch = touches.first else { return }
        let point = touch.location(in: window)
        let pathPoint = PathPoint(
            x: Float(point.x),
            y: Float(point.y),
            tMillis: Int64(Date().timeIntervalSince1970 * 1000)
        )

        switch touch.phase {
        case .began:
            attachment.tracker.onDown(p: pathPoint)
            attachment.overlay.notifyTouchStart(at: point)
        case .moved:
            attachment.tracker.onMove(p: pathPoint)
            attachment.overlay.notifyTouchMove(to: point)
        case .ended:
            attachment.tracker.onUp(p: pathPoint)
            attachment.overlay.notifyTouchEnd(at: point)
        case .cancelled:
            attachment.tracker.onCancel()
            attachment.overlay.notifyTouchCancel()
        default:
            break
        }
    }

    // MARK: - Attachment (per-window state)

    private final class Attachment {
        let tracker: PathTracker
        let overlay: TouchOverlayView

        init(tracker: PathTracker, overlay: TouchOverlayView) {
            self.tracker = tracker
            self.overlay = overlay
        }
    }
}

// MARK: - UIWindow swizzle

extension UIWindow {
    /// Swizzled implementation of `sendEvent(_:)`.
    /// After swizzling, calling `_pathSense_sendEvent` actually invokes the
    /// **original** `sendEvent` implementation (method_exchangeImplementations
    /// swaps the two IMP pointers).
    @objc func _pathSense_sendEvent(_ event: UIEvent) {
        // Call original implementation (IMP pointers are swapped)
        _pathSense_sendEvent(event)

        // Forward to PathSense — skip PathTrackingWindow (has its own tracking)
        guard PathSense.isConfigured, !(self is PathTrackingWindow) else { return }
        PathSense.handleSendEvent(event, in: self)
    }
}
