#if DEBUG
import PathSenseCore
import UIKit

/// A UIWindow subclass that intercepts touches and renders the path overlay.
///
/// Uses a single tracked finger (first direct touch) for parity with
/// GesturePathKit-style interaction.
public final class PathSenseTrackingWindow: UIWindow {
    /// Tracker for this window. Exposes the full KMM metrics/recognition pipeline.
    public let tracker: PathTracker

    /// Overlay configuration. Updating this value applies immediately.
    public var overlayConfig: PathOverlayConfig {
        didSet { overlayView.overlayConfig = overlayConfig }
    }

    /// When false, touch capture is paused and current overlay state is cleared.
    public var isCaptureEnabled: Bool = true {
        didSet {
            tracker.captureEnabled = isCaptureEnabled
            if !isCaptureEnabled {
                cancelTrackedTouchIfNeeded()
                overlayView.clearCanvas()
            }
        }
    }

    private let overlayView: TouchOverlayView
    private weak var trackedTouch: UITouch?

    /// Creates a tracking window attached to the given scene.
    public init(windowScene: UIWindowScene, config: PathSenseConfig = PathSenseConfig()) {
        self.tracker = PathTracker()
        self.overlayConfig = config.overlayConfig
        if let listener = config.listener {
            self.tracker.listener = listener
        }
        self.overlayView = TouchOverlayView(tracker: tracker)
        self.overlayView.overlayConfig = config.overlayConfig
        super.init(windowScene: windowScene)
        setupOverlay()
    }

    /// Creates a tracking window by adopting properties from an existing window.
    public init(window: UIWindow, config: PathSenseConfig = PathSenseConfig()) {
        self.tracker = PathTracker()
        self.overlayConfig = config.overlayConfig
        if let listener = config.listener {
            self.tracker.listener = listener
        }
        self.overlayView = TouchOverlayView(tracker: tracker)
        self.overlayView.overlayConfig = config.overlayConfig

        if let scene = window.windowScene {
            super.init(windowScene: scene)
        } else {
            super.init(frame: window.frame)
        }

        frame = window.frame
        rootViewController = window.rootViewController
        windowLevel = window.windowLevel
        backgroundColor = window.backgroundColor
        overrideUserInterfaceStyle = window.overrideUserInterfaceStyle

        setupOverlay()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    deinit {
        tracker.destroy()
    }

    /// Clear all rendered paths. Does not disable capture.
    public func clearCanvas() {
        overlayView.clearCanvas()
    }

    public override func addSubview(_ view: UIView) {
        super.addSubview(view)
        if view !== overlayView {
            bringSubviewToFront(overlayView)
        }
    }

    public override func sendEvent(_ event: UIEvent) {
        super.sendEvent(event)

        guard isCaptureEnabled, tracker.captureEnabled else { return }
        guard let touches = event.allTouches else { return }

        for touch in touches where touch.type == .direct {
            switch touch.phase {
            case .began:
                if trackedTouch == nil {
                    trackedTouch = touch
                    handleTouch(touch)
                }
            case .moved:
                if touch === trackedTouch {
                    handleTouch(touch)
                }
            case .ended:
                if touch === trackedTouch {
                    handleTouch(touch)
                    trackedTouch = nil
                }
            case .cancelled:
                if touch === trackedTouch {
                    tracker.onCancel()
                    overlayView.notifyTouchCancel()
                    trackedTouch = nil
                }
            default:
                break
            }
        }
    }

    private func setupOverlay() {
        overlayView.frame = bounds
        overlayView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        overlayView.isUserInteractionEnabled = false
        overlayView.backgroundColor = .clear
        addSubview(overlayView)
    }

    private func cancelTrackedTouchIfNeeded() {
        guard trackedTouch != nil else { return }
        tracker.onCancel()
        overlayView.notifyTouchCancel()
        trackedTouch = nil
    }

    private func handleTouch(_ touch: UITouch) {
        let point = touch.location(in: self)
        let pathPoint = PathPoint(
            x: Float(point.x),
            y: Float(point.y),
            tMillis: Int64(Date().timeIntervalSince1970 * 1000)
        )

        switch touch.phase {
        case .began:
            tracker.onDown(p: pathPoint)
            overlayView.notifyTouchStart(at: point)
        case .moved:
            tracker.onMove(p: pathPoint)
            overlayView.notifyTouchMove(to: point)
        case .ended:
            tracker.onUp(p: pathPoint)
            overlayView.notifyTouchEnd(at: point)
        case .cancelled:
            tracker.onCancel()
            overlayView.notifyTouchCancel()
        default:
            break
        }
    }
}
#endif
