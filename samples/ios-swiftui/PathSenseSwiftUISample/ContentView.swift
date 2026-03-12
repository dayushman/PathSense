import PathSenseUI
import SwiftUI

struct ContentView: View {
    @State private var pathSenseAction = PathSenseAction()

    var body: some View {
        PathSenseOverlay(config: makeConfig(), action: $pathSenseAction) {
            ZStack {
                Color(.systemBackground)

                Text("Draw anywhere on screen")
                    .font(.headline)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))

                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Button(action: {
                            pathSenseAction.clearCanvas()
                        }) {
                            Image(systemName: "xmark.circle.fill")
                                .font(.system(size: 24, weight: .medium))
                                .foregroundColor(.white)
                                .frame(width: 56, height: 56)
                                .background(Color.blue)
                                .clipShape(Circle())
                        }
                        .padding(.trailing, 24)
                        .padding(.bottom, 24)
                    }
                }
            }
        }
    }

    private func makeConfig() -> PathSenseConfig {
        var config = PathSenseConfig()
        config.overlayConfig.debugOnly = false
        config.overlayConfig.showCoordinateHUD = true
        return config
    }
}
