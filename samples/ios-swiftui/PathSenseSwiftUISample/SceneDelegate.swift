import SwiftUI
import UIKit
#if DEBUG
import PathSenseUI
#endif

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

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

        window.rootViewController = UIHostingController(rootView: ContentView())
        window.makeKeyAndVisible()
        self.window = window
    }
}
