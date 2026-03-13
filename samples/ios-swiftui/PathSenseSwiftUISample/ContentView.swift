import SwiftUI
import UIKit
#if DEBUG
import PathSenseUI
#endif

struct ContentView: View {
    var body: some View {
        ZStack {
            Color(.systemBackground)
                .ignoresSafeArea()

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
                    Button(action: clearPathIfPossible) {
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

    private func clearPathIfPossible() {
        #if DEBUG
            guard
                let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                let window = scene.windows.first(where: { $0.isKeyWindow }) as? PathSenseTrackingWindow
            else { return }
            window.clearCanvas()
        #endif
    }
}
