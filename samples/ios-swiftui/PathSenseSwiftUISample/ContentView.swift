import SwiftUI
import PathSenseCore
import PathSenseUI

struct ContentView: View {
    @StateObject private var model = PathModel()

    var body: some View {
        ZStack {
            PathCaptureRepresentable(
                tracker: model.tracker,
                overlayConfig: model.config
            )
            .ignoresSafeArea()

            Text("Draw anywhere on screen")
                .font(.headline)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
        }
    }
}

private class PathModel: ObservableObject {
    let tracker = PathTracker()
    let config: PathOverlayConfig = {
        var c = PathOverlayConfig()
        c.debugOnly = false
        c.showCoordinateHUD = true
        return c
    }()
}
