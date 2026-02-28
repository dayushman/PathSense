import UIKit
import PathSenseUI

class ViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground

        let label = UILabel()
        label.text = "Draw anywhere on screen"
        label.font = .preferredFont(forTextStyle: .headline)
        label.textColor = .secondaryLabel
        label.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(label)

        let clearButton = UIButton(type: .system)
        clearButton.translatesAutoresizingMaskIntoConstraints = false
        let config = UIImage.SymbolConfiguration(pointSize: 24, weight: .medium)
        clearButton.setImage(UIImage(systemName: "xmark.circle.fill", withConfiguration: config), for: .normal)
        clearButton.tintColor = .white
        clearButton.backgroundColor = .systemBlue
        clearButton.layer.cornerRadius = 28
        clearButton.clipsToBounds = true
        clearButton.addTarget(self, action: #selector(clearTapped), for: .touchUpInside)
        view.addSubview(clearButton)

        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor),

            clearButton.widthAnchor.constraint(equalToConstant: 56),
            clearButton.heightAnchor.constraint(equalToConstant: 56),
            clearButton.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -24),
            clearButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -24),
        ])
    }

    @objc private func clearTapped() {
        PathSense.clearCanvas()
    }
}
