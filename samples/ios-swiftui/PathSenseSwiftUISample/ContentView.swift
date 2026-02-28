import PathSenseUI
import SwiftUI

struct ContentView: View {
    var body: some View {
        ZStack {
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
                        PathSense.clearCanvas()
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
