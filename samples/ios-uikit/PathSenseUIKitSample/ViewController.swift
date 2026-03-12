import UIKit
import PathSenseUI

class ViewController: UIViewController {

    private var container: PathSenseContainerView!

    override func viewDidLoad() {
        super.viewDidLoad()

        let contentView = UIView()
        contentView.backgroundColor = .systemBackground

        let label = UILabel()
        label.text = "Draw anywhere on screen"
        label.font = .preferredFont(forTextStyle: .headline)
        label.textColor = .secondaryLabel
        label.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(label)

        let clearButton = UIButton(type: .system)
        clearButton.translatesAutoresizingMaskIntoConstraints = false
        let config = UIImage.SymbolConfiguration(pointSize: 24, weight: .medium)
        clearButton.setImage(UIImage(systemName: "xmark.circle.fill", withConfiguration: config), for: .normal)
        clearButton.tintColor = .white
        clearButton.backgroundColor = .systemBlue
        clearButton.layer.cornerRadius = 28
        clearButton.clipsToBounds = true
        clearButton.addTarget(self, action: #selector(clearTapped), for: .touchUpInside)
        contentView.addSubview(clearButton)

        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),

            clearButton.widthAnchor.constraint(equalToConstant: 56),
            clearButton.heightAnchor.constraint(equalToConstant: 56),
            clearButton.trailingAnchor.constraint(equalTo: contentView.safeAreaLayoutGuide.trailingAnchor, constant: -24),
            clearButton.bottomAnchor.constraint(equalTo: contentView.safeAreaLayoutGuide.bottomAnchor, constant: -24),
        ])

        var psConfig = PathSenseConfig()
        psConfig.overlayConfig.debugOnly = false
        psConfig.overlayConfig.showCoordinateHUD = true

        container = PathSense.wrap(view: contentView, config: psConfig)
        container.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(container)

        NSLayoutConstraint.activate([
            container.topAnchor.constraint(equalTo: view.topAnchor),
            container.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            container.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            container.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }

    @objc private func clearTapped() {
        container.clearCanvas()
    }
}
