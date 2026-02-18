import SwiftUI
import PathSenseUI

@main
struct PathSenseSwiftUISampleApp: App {
    init() {
        var config = PathSenseConfig()
        config.overlayConfig.debugOnly = false
        config.overlayConfig.showCoordinateHUD = true
        PathSense.configure(config)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
