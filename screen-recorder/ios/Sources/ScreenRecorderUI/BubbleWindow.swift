import UIKit
import ScreenRecorderCore

public final class BubbleWindow: UIWindow {
    private let bubbleVC: BubbleViewController

    public init(windowScene: UIWindowScene, config: ScreenRecorderConfig) {
        bubbleVC = BubbleViewController(config: config)
        super.init(windowScene: windowScene)
        windowLevel = .alert + 1
        rootViewController = bubbleVC
        backgroundColor = .clear
        isHidden = false
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) not supported")
    }

    public override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        let hit = super.hitTest(point, with: event)
        // Pass touches through if not on the bubble or popup
        if hit === rootViewController?.view {
            return nil
        }
        return hit
    }
}
