import UIKit
import PathSenseCore

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
