import SwiftUI

public struct PathTrackingWindowRepresentable: UIViewRepresentable {
    private let overlayConfig: PathOverlayConfig

    public init(overlayConfig: PathOverlayConfig = PathOverlayConfig()) {
        self.overlayConfig = overlayConfig
    }

    public func makeUIView(context: Context) -> UIView {
        return UIView(frame: .zero)
    }

    public func updateUIView(_ uiView: UIView, context: Context) {
        guard let scene = uiView.window?.windowScene else { return }
        if let window = scene.windows.first(where: { $0 is PathTrackingWindow }) as? PathTrackingWindow {
            window.overlayConfig = overlayConfig
        }
    }
}
