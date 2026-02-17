import SwiftUI
import PathSenseCore

public struct PathCaptureRepresentable: UIViewRepresentable {
    private let tracker: PathTracker
    private let overlayConfig: PathOverlayConfig

    public init(tracker: PathTracker, overlayConfig: PathOverlayConfig = PathOverlayConfig()) {
        self.tracker = tracker
        self.overlayConfig = overlayConfig
    }

    public func makeUIView(context: Context) -> PathCaptureView {
        let view = PathCaptureView(tracker: tracker)
        view.overlayConfig = overlayConfig
        return view
    }

    public func updateUIView(_ uiView: PathCaptureView, context: Context) {
        uiView.overlayConfig = overlayConfig
    }
}
