import PathSenseCore
import UIKit

/// A container view that wraps client content and adds a transparent path
/// drawing overlay on top. Touch events are observed via a custom gesture
/// recognizer without intercepting them — the client's views receive all
/// touches normally.
///
/// Create via `PathSense.wrap(view:config:)` or use `PathSenseOverlay` in SwiftUI.
public final class PathSenseContainerView: UIView {
    /// The tracker instance for this container.
    public let tracker: PathTracker

    /// The overlay configuration. Updating this applies changes immediately.
    public var overlayConfig: PathOverlayConfig {
        didSet { overlayView.overlayConfig = overlayConfig }
    }

    /// Enable or disable path capture on this container.
    /// When disabled, touches are still passed through but not tracked or drawn.
    public var isCaptureEnabled: Bool = true {
        didSet {
            touchRecognizer.isCaptureEnabled = isCaptureEnabled
            tracker.captureEnabled = isCaptureEnabled
            if !isCaptureEnabled {
                overlayView.clearCanvas()
            }
        }
    }

    private let overlayView: TouchOverlayView
    private let touchRecognizer: PathSenseTouchRecognizer
    private let clientView: UIView

    init(clientView: UIView, config: PathSenseConfig) {
        self.clientView = clientView
        self.tracker = PathTracker()
        self.overlayConfig = config.overlayConfig

        if let listener = config.listener {
            tracker.listener = listener
        }

        self.overlayView = TouchOverlayView(tracker: tracker)
        overlayView.overlayConfig = config.overlayConfig
        overlayView.isUserInteractionEnabled = false
        overlayView.backgroundColor = .clear

        self.touchRecognizer = PathSenseTouchRecognizer(tracker: tracker, overlay: overlayView)

        super.init(frame: .zero)

        addSubview(clientView)
        addSubview(overlayView)
        addGestureRecognizer(touchRecognizer)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    deinit {
        tracker.destroy()
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        clientView.frame = bounds
        overlayView.frame = bounds
    }

    /// Clear all rendered paths. Does not disable capture.
    public func clearCanvas() {
        overlayView.clearCanvas()
    }
}
