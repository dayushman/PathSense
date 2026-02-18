import UIKit
import PathSenseUI

class SceneDelegate: UIResponder, UIWindowSceneDelegate {

    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }

        // Use PathTrackingWindow instead of UIWindow for zero-integration gesture tracking.
        // It intercepts all touch events and renders the path overlay automatically.
        let window = PathTrackingWindow(frame: windowScene.coordinateSpace.bounds)
        window.windowScene = windowScene

        var config = PathOverlayConfig()
        config.debugOnly = false
        config.showCoordinateHUD = true
        window.overlayConfig = config

        window.rootViewController = ViewController()
        window.makeKeyAndVisible()
        self.window = window
    }
}
