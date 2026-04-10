import UIKit
import ScreenRecorderCore

public extension ScreenRecorder {

    static func start(in windowScene: UIWindowScene, config: ScreenRecorderConfig) {
        // Initialize the KMM side
        ScreenRecorder.companion.start(config: config)

        // Create bubble window
        let bubbleWindow = BubbleWindow(windowScene: windowScene, config: config)

        // Store reference to prevent dealloc
        BubbleWindowHolder.shared.window = bubbleWindow
    }
}

internal class BubbleWindowHolder {
    static let shared = BubbleWindowHolder()
    var window: BubbleWindow?
}
