import UIKit
#if DEBUG
import PathSenseUI
#endif

class ViewController: UIViewController {
    private let clearButton = UIButton(type: .system)
    private let modalButton = UIButton(type: .system)

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground

        let label = UILabel()
        label.text = "Draw anywhere on screen"
        label.font = .preferredFont(forTextStyle: .headline)
        label.textColor = .secondaryLabel
        label.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(label)

        let symbolConfig = UIImage.SymbolConfiguration(pointSize: 24, weight: .medium)

        modalButton.translatesAutoresizingMaskIntoConstraints = false
        modalButton.setImage(
            UIImage(systemName: "rectangle.portrait.on.rectangle.portrait", withConfiguration: symbolConfig),
            for: .normal
        )
        modalButton.tintColor = .white
        modalButton.backgroundColor = .systemOrange
        modalButton.layer.cornerRadius = 28
        modalButton.clipsToBounds = true
        modalButton.addTarget(self, action: #selector(presentModal), for: .touchUpInside)
        view.addSubview(modalButton)

        clearButton.translatesAutoresizingMaskIntoConstraints = false
        clearButton.setImage(
            UIImage(systemName: "xmark.circle.fill", withConfiguration: symbolConfig),
            for: .normal
        )
        clearButton.tintColor = .white
        clearButton.backgroundColor = .systemBlue
        clearButton.layer.cornerRadius = 28
        clearButton.clipsToBounds = true
        clearButton.addTarget(self, action: #selector(clearTapped), for: .touchUpInside)
        view.addSubview(clearButton)

        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor),

            modalButton.widthAnchor.constraint(equalToConstant: 56),
            modalButton.heightAnchor.constraint(equalToConstant: 56),
            modalButton.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 24),
            modalButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -24),

            clearButton.widthAnchor.constraint(equalToConstant: 56),
            clearButton.heightAnchor.constraint(equalToConstant: 56),
            clearButton.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -24),
            clearButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -24),
        ])
    }

    @objc private func presentModal() {
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
            target: self,
            action: #selector(dismissModal)
        )
        present(nav, animated: true)
    }

    @objc private func dismissModal() {
        dismiss(animated: true)
    }

    @objc private func clearTapped() {
        #if DEBUG
            (view.window as? PathSenseTrackingWindow)?.clearCanvas()
        #endif
    }
}
