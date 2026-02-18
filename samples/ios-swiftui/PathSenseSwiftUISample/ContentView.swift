import SwiftUI

struct ContentView: View {
    var body: some View {
        Text("Draw anywhere on screen")
            .font(.headline)
            .foregroundStyle(.secondary)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
    }
}
