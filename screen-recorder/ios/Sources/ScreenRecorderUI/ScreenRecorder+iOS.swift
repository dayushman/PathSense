import UIKit
import ScreenRecorderCore
import PathSenseCore
import PathSenseUI

public extension ScreenRecorder {

    static func start(in windowScene: UIWindowScene, config: ScreenRecorderConfig) {
        // Initialize the KMM side
        ScreenRecorder.companion.start(config: config)

        // Create bubble window
        let bubbleWindow = BubbleWindow(windowScene: windowScene, config: config)

        // Store reference to prevent dealloc
        BubbleWindowHolder.shared.window = bubbleWindow

        // Create PathSense tracking window (starts disabled, enabled on recording)
        let pathSenseConfig = PathSenseConfig(
            overlayConfig: {
                let overlay = PathOverlayConfig()
                overlay.debugOnly = false
                overlay.showCoordinateHUD = true
                overlay.showCrosshair = true
                overlay.showTouchCircle = true
                return overlay
            }()
        )
        let trackingWindow = PathSenseTrackingWindow(windowScene: windowScene, config: pathSenseConfig)
        trackingWindow.isCaptureEnabled = false
        trackingWindow.makeKeyAndVisible()
        BubbleWindowHolder.shared.trackingWindow = trackingWindow
    }
}

internal class BubbleWindowHolder {
    static let shared = BubbleWindowHolder()
    var window: BubbleWindow?
    var trackingWindow: PathSenseTrackingWindow?
}
