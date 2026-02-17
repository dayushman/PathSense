import UIKit

public struct PathStyle {
    public var gradientStartColor: UIColor = UIColor(red: 1.0, green: 0.23, blue: 0.19, alpha: 1.0)
    public var gradientEndColor: UIColor = UIColor(red: 0.0, green: 0.48, blue: 1.0, alpha: 1.0)
    public var strokeWidth: CGFloat = 4.0
    public var strokeCap: CGLineCap = .round
    public var fadeOutMs: Int = 300
    public var showBoundingBox: Bool = false
    public var boundingBoxColor: UIColor = UIColor(red: 0.0, green: 1.0, blue: 0.0, alpha: 0.27)

    public init() {}
}

public enum HUDAlignment {
    case topLeft
    case topRight
    case bottomLeft
    case bottomRight
    case centerLeft
    case centerRight
}

public struct PathOverlayConfig {
    public var debugOnly: Bool = true
    public var style: PathStyle = PathStyle()
    public var showCrosshair: Bool = false
    public var showTouchCircle: Bool = true
    public var showCoordinateHUD: Bool = false
    public var hudAlignment: HUDAlignment = .topLeft

    public init() {}
}
