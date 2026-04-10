import UIKit
#if DEBUG
import PathSenseUI
#endif
import ScreenRecorderCore

class SceneDelegate: UIResponder, UIWindowSceneDelegate {

    var window: UIWindow?
    var bubbleWindow: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }

        #if DEBUG
            var config = PathSenseConfig()
            config.overlayConfig.debugOnly = false
            config.overlayConfig.showCoordinateHUD = true
            let window = PathSenseTrackingWindow(windowScene: windowScene, config: config)
        #else
            let window = UIWindow(windowScene: windowScene)
        #endif
        window.rootViewController = ViewController()
        window.makeKeyAndVisible()
        self.window = window

        // Initialize screen recorder
        let recorderConfig = ScreenRecorderConfig()
        recorderConfig.audioEnabled = false
        recorderConfig.listener = { event in
            if let started = event as? RecordingEvent.RecordingStarted {
                print("[ScreenRecorder] Recording started: \(started.sessionId)")
            } else if let stopped = event as? RecordingEvent.RecordingStopped {
                print("[ScreenRecorder] Saved to: \(stopped.file.path)")
            } else if let failed = event as? RecordingEvent.RecordingFailed {
                print("[ScreenRecorder] Error: \(failed.error.message)")
            }
        }
        ScreenRecorder.companion.start(config: recorderConfig)
        ScreenRecorder.companion.show()
    }
}
