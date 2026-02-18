import UIKit
import PathSenseCore

public final class PathTrackingWindow: UIWindow {
    public let tracker: PathTracker
    public var overlayConfig: PathOverlayConfig = PathOverlayConfig() {
        didSet { overlayView.overlayConfig = overlayConfig }
    }

    private let overlayView: TouchOverlayView

    public override init(frame: CGRect) {
        self.tracker = PathTracker()
        self.overlayView = TouchOverlayView(tracker: tracker)
        super.init(frame: frame)
        overlayView.isUserInteractionEnabled = false
        addSubview(overlayView)
    }

    public required init?(coder: NSCoder) {
        self.tracker = PathTracker()
        self.overlayView = TouchOverlayView(tracker: tracker)
        super.init(coder: coder)
        overlayView.isUserInteractionEnabled = false
        addSubview(overlayView)
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        overlayView.frame = bounds
    }

    public override func sendEvent(_ event: UIEvent) {
        super.sendEvent(event)
        guard let touches = event.allTouches, let touch = touches.first else { return }
        let point = touch.location(in: self)
        let pathPoint = PathPoint(x: Float(point.x), y: Float(point.y), tMillis: Int64(Date().timeIntervalSince1970 * 1000))

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
