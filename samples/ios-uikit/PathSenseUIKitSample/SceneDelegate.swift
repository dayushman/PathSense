import UIKit
import ScreenRecorderCore
import ScreenRecorderUI

class SceneDelegate: UIResponder, UIWindowSceneDelegate {

    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }

        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = ViewController()
        window.makeKeyAndVisible()
        self.window = window

        // Initialize screen recorder (handles PathSense + bubble + tracking window)
        let recorderConfig = ScreenRecorderConfig()
        recorderConfig.audioEnabled = false
        recorderConfig.listener = { [weak self] event in
            if let started = event as? RecordingEvent.RecordingStarted {
                print("[ScreenRecorder] Recording started: \(started.sessionId)")
            } else if let stopped = event as? RecordingEvent.RecordingStopped {
                print("[ScreenRecorder] Saved to: \(stopped.file.path)")
                DispatchQueue.main.async {
                    if let vc = self?.window?.rootViewController {
                        RecordingShareSheet.show(from: vc, file: stopped.file)
                    }
                }
            } else if let failed = event as? RecordingEvent.RecordingFailed {
                print("[ScreenRecorder] Error: \(failed.error.message)")
            }
        }
        ScreenRecorder.start(in: windowScene, config: recorderConfig)
    }
}
