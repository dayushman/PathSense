import UIKit
import ScreenRecorderCore

public final class BubbleViewController: UIViewController {
    private let config: ScreenRecorderConfig
    private let bubbleButton = UIButton(type: .custom)
    private let durationLabel = UILabel()
    private var isRecording = false

    private let bubbleSize: CGFloat = 44
    private var popoverView: PopoverMenuView?

    init(config: ScreenRecorderConfig) {
        self.config = config
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) not supported")
    }

    public override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
        setupBubble()
        setupDurationLabel()
        setupDragGesture()
    }

    private func setupBubble() {
        bubbleButton.frame = CGRect(x: 0, y: 0, width: bubbleSize, height: bubbleSize)
        bubbleButton.layer.cornerRadius = bubbleSize / 2
        bubbleButton.clipsToBounds = true
        bubbleButton.backgroundColor = UIColor(argb: config.tintColor)

        // Record icon (white circle)
        let iconSize: CGFloat = 14
        let iconView = UIView(frame: CGRect(
            x: (bubbleSize - iconSize) / 2,
            y: (bubbleSize - iconSize) / 2,
            width: iconSize, height: iconSize
        ))
        iconView.backgroundColor = .white
        iconView.layer.cornerRadius = iconSize / 2
        iconView.isUserInteractionEnabled = false
        iconView.tag = 100
        bubbleButton.addSubview(iconView)

        bubbleButton.addTarget(self, action: #selector(bubbleTapped), for: .touchUpInside)
        view.addSubview(bubbleButton)

        // Position at trailing center
        let screen = UIScreen.main.bounds
        bubbleButton.center = CGPoint(x: screen.width - bubbleSize / 2 - 16, y: screen.height / 2)
    }

    private func setupDurationLabel() {
        durationLabel.font = .monospacedDigitSystemFont(ofSize: 10, weight: .medium)
        durationLabel.textColor = .white
        durationLabel.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        durationLabel.textAlignment = .center
        durationLabel.layer.cornerRadius = 4
        durationLabel.clipsToBounds = true
        durationLabel.isHidden = true
        view.addSubview(durationLabel)
    }

    private func setupDragGesture() {
        let pan = UIPanGestureRecognizer(target: self, action: #selector(handleDrag(_:)))
        bubbleButton.addGestureRecognizer(pan)
    }

    @objc private func bubbleTapped() {
        if isRecording {
            ScreenRecorder.companion.onBubbleTapStop()
            setRecording(false)
        } else {
            if popoverView != nil {
                dismissPopover()
            } else {
                showPopover()
            }
        }
    }

    private func showPopover() {
        guard popoverView == nil else { return }

        let screen = UIScreen.main.bounds
        let isBubbleOnRight = bubbleButton.center.x > screen.width / 2

        let popover = PopoverMenuView(
            bubbleCenter: bubbleButton.center,
            isBubbleOnRight: isBubbleOnRight,
            audioEnabled: config.audioEnabled,
            onStartRecording: { [weak self] in
                self?.dismissPopoverImmediate()
                ScreenRecorder.companion.onBubbleTapRecord()
                self?.setRecording(true)
            },
            onGetMoreInfo: { [weak self] in
                self?.dismissPopover()
                // No-op for now
            },
            onAudioToggle: { [weak self] enabled in
                self?.config.audioEnabled = enabled
            },
            onDismiss: { [weak self] in
                self?.dismissPopover()
            }
        )

        view.addSubview(popover)
        popoverView = popover
    }

    private func dismissPopover() {
        popoverView?.animateOut { [weak self] in
            self?.popoverView?.removeFromSuperview()
            self?.popoverView = nil
        }
    }

    private func dismissPopoverImmediate() {
        popoverView?.removeFromSuperview()
        popoverView = nil
    }

    @objc private func handleDrag(_ gesture: UIPanGestureRecognizer) {
        let translation = gesture.translation(in: view)

        switch gesture.state {
        case .changed:
            if popoverView != nil {
                dismissPopoverImmediate()
            }
            bubbleButton.center = CGPoint(
                x: bubbleButton.center.x + translation.x,
                y: bubbleButton.center.y + translation.y
            )
            gesture.setTranslation(.zero, in: view)
        case .ended:
            snapToEdge()
        default:
            break
        }
    }

    private func snapToEdge() {
        let screen = UIScreen.main.bounds
        let margin: CGFloat = 16
        let targetX: CGFloat

        if bubbleButton.center.x < screen.width / 2 {
            targetX = bubbleSize / 2 + margin
        } else {
            targetX = screen.width - bubbleSize / 2 - margin
        }

        UIView.animate(withDuration: 0.2) {
            self.bubbleButton.center.x = targetX
        }
    }

    func setRecording(_ recording: Bool) {
        dismissPopoverImmediate()
        isRecording = recording
        durationLabel.isHidden = !recording

        if let iconView = bubbleButton.viewWithTag(100) {
            UIView.animate(withDuration: 0.2) {
                if recording {
                    iconView.layer.cornerRadius = 3
                    iconView.backgroundColor = .white
                    self.bubbleButton.backgroundColor = .red
                } else {
                    iconView.layer.cornerRadius = 7
                    iconView.backgroundColor = .white
                    self.bubbleButton.backgroundColor = UIColor(argb: self.config.tintColor)
                }
            }
        }

        if recording {
            startPulseAnimation()
        } else {
            bubbleButton.layer.removeAnimation(forKey: "pulse")
        }
    }

    func updateDuration(_ durationMs: Int64) {
        let totalSeconds = durationMs / 1000
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        durationLabel.text = String(format: " %02d:%02d ", minutes, seconds)
        durationLabel.sizeToFit()
        durationLabel.center = CGPoint(
            x: bubbleButton.center.x,
            y: bubbleButton.frame.maxY + 12
        )
    }

    private func startPulseAnimation() {
        let pulse = CABasicAnimation(keyPath: "opacity")
        pulse.fromValue = 1.0
        pulse.toValue = 0.6
        pulse.duration = 0.75
        pulse.autoreverses = true
        pulse.repeatCount = .infinity
        bubbleButton.layer.add(pulse, forKey: "pulse")
    }
}

extension UIColor {
    convenience init(argb value: Int64) {
        let a = CGFloat((value >> 24) & 0xFF) / 255.0
        let r = CGFloat((value >> 16) & 0xFF) / 255.0
        let g = CGFloat((value >> 8) & 0xFF) / 255.0
        let b = CGFloat(value & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b, alpha: a)
    }
}
