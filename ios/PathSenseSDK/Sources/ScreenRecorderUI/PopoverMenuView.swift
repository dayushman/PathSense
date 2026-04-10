import UIKit

internal final class PopoverMenuView: UIView {

    private let cardView: UIVisualEffectView
    private let nibView: NibView
    private let audioSwitch = UISwitch()
    private var isAudioEnabled: Bool
    private let pathSenseSwitch = UISwitch()
    private var isPathSenseEnabled: Bool

    private let onStartRecording: () -> Void
    private let onGetMoreInfo: () -> Void
    private let onAudioToggle: (Bool) -> Void
    private let onPathSenseToggle: (Bool) -> Void
    private let onDismiss: () -> Void

    private let cardWidth: CGFloat = 200
    private let rowHeight: CGFloat = 52

    init(
        bubbleCenter: CGPoint,
        isBubbleOnRight: Bool,
        audioEnabled: Bool,
        pathSenseEnabled: Bool,
        onStartRecording: @escaping () -> Void,
        onGetMoreInfo: @escaping () -> Void,
        onAudioToggle: @escaping (Bool) -> Void,
        onPathSenseToggle: @escaping (Bool) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.isAudioEnabled = audioEnabled
        self.isPathSenseEnabled = pathSenseEnabled
        self.onStartRecording = onStartRecording
        self.onGetMoreInfo = onGetMoreInfo
        self.onAudioToggle = onAudioToggle
        self.onPathSenseToggle = onPathSenseToggle
        self.onDismiss = onDismiss

        let blurEffect = UIBlurEffect(style: .systemMaterialDark)
        cardView = UIVisualEffectView(effect: blurEffect)
        nibView = NibView(pointsRight: isBubbleOnRight)

        super.init(frame: UIScreen.main.bounds)
        backgroundColor = .clear

        // Dismiss tap on scrim
        let tap = UITapGestureRecognizer(target: self, action: #selector(scrimTapped(_:)))
        tap.cancelsTouchesInView = false
        addGestureRecognizer(tap)

        setupCard()
        layoutCardAndNib(bubbleCenter: bubbleCenter, isBubbleOnRight: isBubbleOnRight)
        animateIn()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) not supported") }

    // MARK: - Card setup

    private func setupCard() {
        cardView.layer.cornerRadius = 16
        cardView.clipsToBounds = true

        let stack = UIStackView()
        stack.axis = .vertical
        stack.translatesAutoresizingMaskIntoConstraints = false
        cardView.contentView.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: cardView.contentView.topAnchor),
            stack.leadingAnchor.constraint(equalTo: cardView.contentView.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: cardView.contentView.trailingAnchor),
            stack.bottomAnchor.constraint(equalTo: cardView.contentView.bottomAnchor),
        ])

        stack.addArrangedSubview(buildRow(
            icon: buildRecordIcon(),
            label: "Start Recording",
            showChevron: true,
            action: #selector(startRecordingTapped)
        ))
        stack.addArrangedSubview(buildDivider())
        stack.addArrangedSubview(buildRow(
            icon: buildInfoIcon(),
            label: "Get More Info",
            showChevron: true,
            action: #selector(getMoreInfoTapped)
        ))
        stack.addArrangedSubview(buildDivider())
        stack.addArrangedSubview(buildAudioRow())
        stack.addArrangedSubview(buildDivider())
        stack.addArrangedSubview(buildPathSenseRow())
    }

    private func buildRow(icon: UIView, label: String, showChevron: Bool, action: Selector) -> UIView {
        let row = UIButton(type: .system)
        row.addTarget(self, action: action, for: .touchUpInside)
        row.translatesAutoresizingMaskIntoConstraints = false
        row.heightAnchor.constraint(equalToConstant: rowHeight).isActive = true

        let hStack = UIStackView()
        hStack.axis = .horizontal
        hStack.alignment = .center
        hStack.spacing = 12
        hStack.isUserInteractionEnabled = false
        hStack.translatesAutoresizingMaskIntoConstraints = false
        row.addSubview(hStack)
        NSLayoutConstraint.activate([
            hStack.leadingAnchor.constraint(equalTo: row.leadingAnchor, constant: 16),
            hStack.trailingAnchor.constraint(equalTo: row.trailingAnchor, constant: -16),
            hStack.centerYAnchor.constraint(equalTo: row.centerYAnchor),
        ])

        icon.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            icon.widthAnchor.constraint(equalToConstant: 20),
            icon.heightAnchor.constraint(equalToConstant: 20),
        ])
        hStack.addArrangedSubview(icon)

        let labelView = UILabel()
        labelView.text = label
        labelView.textColor = .white
        labelView.font = .systemFont(ofSize: 15, weight: .semibold)
        hStack.addArrangedSubview(labelView)

        if showChevron {
            let chevron = UILabel()
            chevron.text = "\u{203A}"
            chevron.textColor = UIColor.white.withAlphaComponent(0.5)
            chevron.font = .systemFont(ofSize: 18)
            chevron.setContentHuggingPriority(.required, for: .horizontal)
            hStack.addArrangedSubview(chevron)
        }

        return row
    }

    private func buildAudioRow() -> UIView {
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false
        container.heightAnchor.constraint(equalToConstant: rowHeight).isActive = true

        let hStack = UIStackView()
        hStack.axis = .horizontal
        hStack.alignment = .center
        hStack.spacing = 12
        hStack.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(hStack)
        NSLayoutConstraint.activate([
            hStack.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 16),
            hStack.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -16),
            hStack.centerYAnchor.constraint(equalTo: container.centerYAnchor),
        ])

        let micIcon = buildMicIcon()
        micIcon.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            micIcon.widthAnchor.constraint(equalToConstant: 20),
            micIcon.heightAnchor.constraint(equalToConstant: 20),
        ])
        hStack.addArrangedSubview(micIcon)

        let label = UILabel()
        label.text = "Audio"
        label.textColor = .white
        label.font = .systemFont(ofSize: 15, weight: .semibold)
        hStack.addArrangedSubview(label)

        audioSwitch.isOn = isAudioEnabled
        audioSwitch.onTintColor = UIColor(red: 0.2, green: 0.78, blue: 0.35, alpha: 1)
        audioSwitch.transform = CGAffineTransform(scaleX: 0.75, y: 0.75)
        audioSwitch.addTarget(self, action: #selector(audioToggled), for: .valueChanged)
        audioSwitch.setContentHuggingPriority(.required, for: .horizontal)
        hStack.addArrangedSubview(audioSwitch)

        // Tap entire row to toggle
        let rowTap = UITapGestureRecognizer(target: self, action: #selector(audioRowTapped))
        container.addGestureRecognizer(rowTap)

        return container
    }

    private func buildPathSenseRow() -> UIView {
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false
        container.heightAnchor.constraint(equalToConstant: rowHeight).isActive = true

        let hStack = UIStackView()
        hStack.axis = .horizontal
        hStack.alignment = .center
        hStack.spacing = 12
        hStack.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(hStack)
        NSLayoutConstraint.activate([
            hStack.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 16),
            hStack.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -16),
            hStack.centerYAnchor.constraint(equalTo: container.centerYAnchor),
        ])

        let icon = buildCrosshairIcon()
        icon.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            icon.widthAnchor.constraint(equalToConstant: 20),
            icon.heightAnchor.constraint(equalToConstant: 20),
        ])
        hStack.addArrangedSubview(icon)

        let label = UILabel()
        label.text = "PathSense"
        label.textColor = .white
        label.font = .systemFont(ofSize: 15, weight: .semibold)
        hStack.addArrangedSubview(label)

        pathSenseSwitch.isOn = isPathSenseEnabled
        pathSenseSwitch.onTintColor = UIColor(red: 0.2, green: 0.78, blue: 0.35, alpha: 1)
        pathSenseSwitch.transform = CGAffineTransform(scaleX: 0.75, y: 0.75)
        pathSenseSwitch.addTarget(self, action: #selector(pathSenseToggled), for: .valueChanged)
        pathSenseSwitch.setContentHuggingPriority(.required, for: .horizontal)
        hStack.addArrangedSubview(pathSenseSwitch)

        let rowTap = UITapGestureRecognizer(target: self, action: #selector(pathSenseRowTapped))
        container.addGestureRecognizer(rowTap)

        return container
    }

    private func buildDivider() -> UIView {
        let wrapper = UIView()
        wrapper.translatesAutoresizingMaskIntoConstraints = false
        wrapper.heightAnchor.constraint(equalToConstant: 1).isActive = true

        let line = UIView()
        line.backgroundColor = UIColor.white.withAlphaComponent(0.1)
        line.translatesAutoresizingMaskIntoConstraints = false
        wrapper.addSubview(line)
        NSLayoutConstraint.activate([
            line.leadingAnchor.constraint(equalTo: wrapper.leadingAnchor, constant: 16),
            line.trailingAnchor.constraint(equalTo: wrapper.trailingAnchor, constant: -16),
            line.topAnchor.constraint(equalTo: wrapper.topAnchor),
            line.bottomAnchor.constraint(equalTo: wrapper.bottomAnchor),
        ])
        return wrapper
    }

    // MARK: - Icons (SF Symbols)

    private func buildRecordIcon() -> UIView {
        let config = UIImage.SymbolConfiguration(pointSize: 14, weight: .bold)
        let imageView = UIImageView(image: UIImage(systemName: "circle.fill", withConfiguration: config))
        imageView.tintColor = UIColor(red: 1, green: 0.23, blue: 0.19, alpha: 1)

        // Pulse animation
        let pulse = CABasicAnimation(keyPath: "opacity")
        pulse.fromValue = 1.0
        pulse.toValue = 0.4
        pulse.duration = 0.8
        pulse.autoreverses = true
        pulse.repeatCount = .infinity
        imageView.layer.add(pulse, forKey: "pulse")

        return imageView
    }

    private func buildInfoIcon() -> UIView {
        let config = UIImage.SymbolConfiguration(pointSize: 14, weight: .bold)
        let imageView = UIImageView(image: UIImage(systemName: "info.circle.fill", withConfiguration: config))
        imageView.tintColor = UIColor(red: 0.04, green: 0.52, blue: 1, alpha: 1)
        return imageView
    }

    private func buildMicIcon() -> UIView {
        let config = UIImage.SymbolConfiguration(pointSize: 14, weight: .bold)
        let imageView = UIImageView(image: UIImage(systemName: "mic.fill", withConfiguration: config))
        imageView.tintColor = UIColor(red: 0.04, green: 0.52, blue: 1, alpha: 1)
        return imageView
    }

    private func buildCrosshairIcon() -> UIView {
        let config = UIImage.SymbolConfiguration(pointSize: 14, weight: .bold)
        let imageView = UIImageView(image: UIImage(systemName: "scope", withConfiguration: config))
        imageView.tintColor = UIColor(red: 1, green: 0, blue: 1, alpha: 1) // magenta
        return imageView
    }

    // MARK: - Layout

    private func layoutCardAndNib(bubbleCenter: CGPoint, isBubbleOnRight: Bool) {
        let screen = UIScreen.main.bounds
        let cardHeight = rowHeight * 4 + 3 // 4 rows + 3 dividers
        let bubbleRadius: CGFloat = 22 // bubbleSize / 2
        let nibSize: CGFloat = 8
        let gap: CGFloat = 4

        let cardY = max(24, min(bubbleCenter.y - cardHeight / 2, screen.height - cardHeight - 24))

        let cardX: CGFloat
        let nibX: CGFloat
        if isBubbleOnRight {
            cardX = bubbleCenter.x - bubbleRadius - gap - nibSize - cardWidth
            nibX = cardX + cardWidth
        } else {
            nibX = bubbleCenter.x + bubbleRadius + gap
            cardX = nibX + nibSize
        }

        cardView.frame = CGRect(x: cardX, y: cardY, width: cardWidth, height: cardHeight)
        addSubview(cardView)

        let nibY = bubbleCenter.y - 6
        nibView.frame = CGRect(x: nibX, y: nibY, width: nibSize, height: 12)
        nibView.backgroundColor = .clear
        addSubview(nibView)
    }

    // MARK: - Animations

    private func animateIn() {
        cardView.transform = CGAffineTransform(scaleX: 0.8, y: 0.8)
        cardView.alpha = 0
        nibView.alpha = 0

        UIView.animate(withDuration: 0.2, delay: 0, options: .curveEaseOut) {
            self.cardView.transform = .identity
            self.cardView.alpha = 1
            self.nibView.alpha = 1
        }
    }

    func animateOut(completion: @escaping () -> Void) {
        UIView.animate(withDuration: 0.15, delay: 0, options: .curveEaseIn, animations: {
            self.cardView.transform = CGAffineTransform(scaleX: 0.9, y: 0.9)
            self.cardView.alpha = 0
            self.nibView.alpha = 0
        }) { _ in
            completion()
        }
    }

    // MARK: - Actions

    @objc private func scrimTapped(_ gesture: UITapGestureRecognizer) {
        let location = gesture.location(in: self)
        if !cardView.frame.contains(location) {
            onDismiss()
        }
    }

    @objc private func startRecordingTapped() { onStartRecording() }
    @objc private func getMoreInfoTapped() { onGetMoreInfo() }

    @objc private func audioToggled() {
        isAudioEnabled = audioSwitch.isOn
        onAudioToggle(isAudioEnabled)
    }

    @objc private func audioRowTapped() {
        audioSwitch.setOn(!audioSwitch.isOn, animated: true)
        audioToggled()
    }

    @objc private func pathSenseToggled() {
        isPathSenseEnabled = pathSenseSwitch.isOn
        onPathSenseToggle(isPathSenseEnabled)
    }

    @objc private func pathSenseRowTapped() {
        pathSenseSwitch.setOn(!pathSenseSwitch.isOn, animated: true)
        pathSenseToggled()
    }

    // MARK: - Arrow nib

    private class NibView: UIView {
        let pointsRight: Bool

        init(pointsRight: Bool) {
            self.pointsRight = pointsRight
            super.init(frame: .zero)
            isOpaque = false
        }

        required init?(coder: NSCoder) { fatalError() }

        override func draw(_ rect: CGRect) {
            guard let ctx = UIGraphicsGetCurrentContext() else { return }
            // Match the blur card's dark tint approximately
            ctx.setFillColor(UIColor(white: 0.12, alpha: 0.9).cgColor)

            let path = UIBezierPath()
            if pointsRight {
                path.move(to: CGPoint(x: 0, y: 0))
                path.addLine(to: CGPoint(x: rect.maxX, y: rect.midY))
                path.addLine(to: CGPoint(x: 0, y: rect.maxY))
            } else {
                path.move(to: CGPoint(x: rect.maxX, y: 0))
                path.addLine(to: CGPoint(x: 0, y: rect.midY))
                path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
            }
            path.close()
            ctx.addPath(path.cgPath)
            ctx.fillPath()
        }
    }
}
