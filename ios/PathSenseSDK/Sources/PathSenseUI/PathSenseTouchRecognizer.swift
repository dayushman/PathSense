import PathSenseCore
import UIKit

/// A gesture recognizer that observes all touches without stealing them from
/// the underlying view hierarchy. It feeds touch data to a `PathTracker` and
/// `TouchOverlayView` for path drawing.
///
/// Configured with `cancelsTouchesInView = false` so client content receives
/// all touches normally.
final class PathSenseTouchRecognizer: UIGestureRecognizer {
    private weak var tracker: PathTracker?
    private weak var overlay: TouchOverlayView?

    /// When false, touch events are ignored (but still passed through).
    var isCaptureEnabled: Bool = true

    init(tracker: PathTracker, overlay: TouchOverlayView) {
        self.tracker = tracker
        self.overlay = overlay
        super.init(target: nil, action: nil)
        cancelsTouchesInView = false
        delaysTouchesBegan = false
        delaysTouchesEnded = false
    }

    // MARK: - Touch handling

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent) {
        guard isCaptureEnabled, let tracker = tracker, tracker.captureEnabled else { return }
        guard let touch = touches.first else { return }
        let point = touch.location(in: view)
        let pathPoint = PathPoint(
            x: Float(point.x), y: Float(point.y),
            tMillis: Int64(Date().timeIntervalSince1970 * 1000))
        tracker.onDown(p: pathPoint)
        overlay?.notifyTouchStart(at: point)
        state = .began
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent) {
        guard isCaptureEnabled, let tracker = tracker, tracker.captureEnabled else { return }
        guard let touch = touches.first else { return }
        let point = touch.location(in: view)
        let pathPoint = PathPoint(
            x: Float(point.x), y: Float(point.y),
            tMillis: Int64(Date().timeIntervalSince1970 * 1000))
        tracker.onMove(p: pathPoint)
        overlay?.notifyTouchMove(to: point)
        state = .changed
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent) {
        guard isCaptureEnabled, let tracker = tracker, tracker.captureEnabled else {
            state = .ended
            return
        }
        guard let touch = touches.first else {
            state = .ended
            return
        }
        let point = touch.location(in: view)
        let pathPoint = PathPoint(
            x: Float(point.x), y: Float(point.y),
            tMillis: Int64(Date().timeIntervalSince1970 * 1000))
        tracker.onUp(p: pathPoint)
        overlay?.notifyTouchEnd(at: point)
        state = .ended
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent) {
        tracker?.onCancel()
        overlay?.notifyTouchCancel()
        state = .cancelled
    }
}
