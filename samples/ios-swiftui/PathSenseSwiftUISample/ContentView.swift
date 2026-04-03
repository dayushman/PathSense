import PhotosUI
import SwiftUI
import UIKit
#if DEBUG
import PathSenseUI
#endif

final class PhotoPickerCoordinator: NSObject, PHPickerViewControllerDelegate, ObservableObject {
    @Published var selectedImage: UIImage?
    private var retainSelf: PhotoPickerCoordinator?

    func presentPicker() {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController
        else { return }

        // Walk to the topmost presented VC to avoid "already presenting" errors
        var topVC = rootVC
        while let presented = topVC.presentedViewController {
            topVC = presented
        }

        var config = PHPickerConfiguration()
        config.filter = .images
        config.selectionLimit = 1
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = self
        // Retain self until picker is dismissed
        retainSelf = self
        topVC.present(picker, animated: true)
    }

    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true) { [weak self] in
            self?.retainSelf = nil
        }
        guard let provider = results.first?.itemProvider,
              provider.canLoadObject(ofClass: UIImage.self) else { return }
        provider.loadObject(ofClass: UIImage.self) { [weak self] image, _ in
            DispatchQueue.main.async {
                self?.selectedImage = image as? UIImage
            }
        }
    }
}

struct ContentView: View {
    @StateObject private var pickerCoordinator = PhotoPickerCoordinator()

    var body: some View {
        ZStack {
            Color(.systemBackground)
                .ignoresSafeArea()

            VStack(spacing: 20) {
                if let selectedImage = pickerCoordinator.selectedImage {
                    Image(uiImage: selectedImage)
                        .resizable()
                        .scaledToFit()
                        .frame(maxHeight: 300)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .padding(.horizontal, 20)
                }

                Text("Draw anywhere on screen")
                    .font(.headline)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
            }

            VStack {
                Spacer()
                HStack {
                    Button(action: { pickerCoordinator.presentPicker() }) {
                        Image(systemName: "photo.on.rectangle.angled")
                            .font(.system(size: 24, weight: .medium))
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(Color.green)
                            .clipShape(Circle())
                    }
                    .padding(.leading, 24)
                    .padding(.bottom, 24)

                    Button(action: presentDummyModal) {
                        Image(systemName: "rectangle.portrait.on.rectangle.portrait")
                            .font(.system(size: 24, weight: .medium))
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(Color.orange)
                            .clipShape(Circle())
                    }
                    .padding(.bottom, 24)

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

    private func presentDummyModal() {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController
        else { return }

        var topVC = rootVC
        while let presented = topVC.presentedViewController {
            topVC = presented
        }

        let dummyVC = UIViewController()
        dummyVC.view.backgroundColor = .systemGroupedBackground
        let label = UILabel()
        label.text = "Presented VC — draw here to test overlay"
        label.font = .preferredFont(forTextStyle: .headline)
        label.textColor = .label
        label.translatesAutoresizingMaskIntoConstraints = false
        dummyVC.view.addSubview(label)
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: dummyVC.view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: dummyVC.view.centerYAnchor),
        ])
        dummyVC.title = "Modal Test"

        let nav = UINavigationController(rootViewController: dummyVC)
        dummyVC.navigationItem.rightBarButtonItem = UIBarButtonItem(
            barButtonSystemItem: .done,
            target: nav,
            action: #selector(UIViewController.dismissSelf)
        )
        topVC.present(nav, animated: true)
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

private extension UIViewController {
    @objc func dismissSelf() {
        dismiss(animated: true)
    }
}
