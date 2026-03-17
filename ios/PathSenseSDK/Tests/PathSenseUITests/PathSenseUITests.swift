import XCTest
import UIKit
@testable import PathSenseUI

@MainActor
final class PathSenseUITests: XCTestCase {
    func testTouchEndClearsPointsImmediately() {
        let tracker = PathTracker()
        let overlay = TouchOverlayView(tracker: tracker)

        let start = CGPoint(x: 10, y: 20)
        let end = CGPoint(x: 40, y: 50)

        tracker.onDown(p: pathPoint(start, tMillis: 1))
        overlay.notifyTouchStart(at: start)
        tracker.onMove(p: pathPoint(end, tMillis: 2))
        overlay.notifyTouchMove(to: end)
        XCTAssertFalse(tracker.currentPoints.isEmpty)

        tracker.onUp(p: pathPoint(end, tMillis: 3))
        overlay.notifyTouchEnd(at: end)

        XCTAssertTrue(tracker.currentPoints.isEmpty)
    }

    func testTouchCancelClearsPointsImmediately() {
        let tracker = PathTracker()
        let overlay = TouchOverlayView(tracker: tracker)

        let start = CGPoint(x: 5, y: 6)
        let move = CGPoint(x: 8, y: 12)

        tracker.onDown(p: pathPoint(start, tMillis: 1))
        overlay.notifyTouchStart(at: start)
        tracker.onMove(p: pathPoint(move, tMillis: 2))
        overlay.notifyTouchMove(to: move)
        XCTAssertFalse(tracker.currentPoints.isEmpty)

        overlay.notifyTouchCancel()

        XCTAssertTrue(tracker.currentPoints.isEmpty)
    }

    private func pathPoint(_ point: CGPoint, tMillis: Int64) -> PathPoint {
        PathPoint(x: Float(point.x), y: Float(point.y), tMillis: tMillis)
    }
}
