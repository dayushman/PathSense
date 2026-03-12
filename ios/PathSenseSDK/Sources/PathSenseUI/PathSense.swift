import UIKit
import PathSenseCore

// MARK: - Configuration

/// Top-level configuration for the PathSense SDK, mirroring
/// the Android `PathSenseConfig`.
public struct PathSenseConfig {
    /// Visual overlay configuration (style, crosshair, HUD, etc.)
    public var overlayConfig: PathOverlayConfig

    /// Optional callback for all `PathEvent`s on the container.
    public var listener: ((PathEvent) -> Void)?

    public init(
        overlayConfig: PathOverlayConfig = PathOverlayConfig(),
        listener: ((PathEvent) -> Void)? = nil
    ) {
        self.overlayConfig = overlayConfig
        self.listener = listener
    }
}

// MARK: - PathSense (view-wrapping entry point)

/// Entry point for the PathSense SDK on iOS.
///
/// Wrap your content view to get a container with path drawing overlay:
///
/// ```swift
/// // UIKit
/// let container = PathSense.wrap(view: myView, config: PathSenseConfig())
/// parentView.addSubview(container)
///
/// // SwiftUI
/// PathSenseOverlay(config: .init()) {
///     YourContentView()
/// }
/// ```
public enum PathSense {
    /// Wrap a UIView with a PathSense drawing overlay.
    ///
    /// Returns a `PathSenseContainerView` that contains your view with a
    /// transparent overlay on top for drawing touch paths. The client view
    /// receives all touch events normally.
    ///
    /// - Parameters:
    ///   - view: The client's content view to wrap.
    ///   - config: Configuration for overlay style, HUD, and event listener.
    /// - Returns: A container view to add to your view hierarchy.
    public static func wrap(
        view: UIView,
        config: PathSenseConfig = PathSenseConfig()
    ) -> PathSenseContainerView {
        return PathSenseContainerView(clientView: view, config: config)
    }
}
