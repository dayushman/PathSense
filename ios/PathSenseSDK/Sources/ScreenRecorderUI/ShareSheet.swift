import UIKit
import ScreenRecorderCore

public final class RecordingShareSheet {

    public static func show(from viewController: UIViewController, file: RecordingFile) {
        let fileURL = URL(fileURLWithPath: file.path)

        guard FileManager.default.fileExists(atPath: file.path) else {
            print("[ScreenRecorder] File not found: \(file.path)")
            return
        }

        let durationSec = file.durationMs / 1000
        let minutes = durationSec / 60
        let seconds = durationSec % 60
        let sizeMB = Double(file.fileSizeBytes) / (1024.0 * 1024.0)

        let alert = UIAlertController(
            title: "Recording Complete",
            message: String(format: "Duration: %02d:%02d  •  Size: %.1f MB", minutes, seconds, sizeMB),
            preferredStyle: .actionSheet
        )

        alert.addAction(UIAlertAction(title: "Share Recording", style: .default) { _ in
            let activityVC = UIActivityViewController(
                activityItems: [fileURL],
                applicationActivities: nil
            )
            activityVC.popoverPresentationController?.sourceView = viewController.view
            viewController.present(activityVC, animated: true)
        })

        alert.addAction(UIAlertAction(title: "Discard", style: .destructive) { _ in
            try? FileManager.default.removeItem(at: fileURL)
        })

        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))

        viewController.present(alert, animated: true)
    }
}
