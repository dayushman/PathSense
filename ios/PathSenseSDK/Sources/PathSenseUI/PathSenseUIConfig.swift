@_exported import PathSenseCore
import UIKit

// Types PathOverlayConfig, PathStyle, HUDAlignment, and StrokeCap are now
// defined in the shared Kotlin module (pathsense-core) and exported through
// PathSenseCore.xcframework. No manual re-declaration is needed here.
//
// This file provides UIKit convenience extensions so that the rest of the
// Swift UI layer can work with UIColor / CGLineCap / CGFloat seamlessly.

// MARK: - UIColor ← Long ARGB

extension UIColor {
    /// Creates a UIColor from an ARGB `Int64` matching the Kotlin `Long` colour format.
    convenience init(argb value: Int64) {
        let a = CGFloat((value >> 24) & 0xFF) / 255.0
        let r = CGFloat((value >> 16) & 0xFF) / 255.0
        let g = CGFloat((value >> 8) & 0xFF) / 255.0
        let b = CGFloat(value & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b, alpha: a)
    }
}

// MARK: - PathStyle UIKit helpers

extension PathStyle {
    /// Gradient start colour as `UIColor`.
    public var gradientStartUIColor: UIColor { UIColor(argb: gradientStartColor) }
    /// Gradient end colour as `UIColor`.
    public var gradientEndUIColor: UIColor { UIColor(argb: gradientEndColor) }
    /// Stroke width as `CGFloat`.
    public var strokeWidth: CGFloat { CGFloat(strokeWidthPx) }
    /// Stroke cap mapped to Core Graphics.
    public var strokeLineCap: CGLineCap {
        switch strokeCap {
        case .butt: return .butt
        case .round: return .round
        case .square: return .square
        default: return .round
        }
    }
    /// Bounding box colour as `UIColor`.
    public var boundingBoxUIColor: UIColor { UIColor(argb: boundingBoxColor) }
}

// MARK: - PathOverlayConfig UIKit helpers

extension PathOverlayConfig {
    /// HUD text colour as `UIColor`.
    public var hudUITextColor: UIColor { UIColor(argb: hudTextColor) }
    /// HUD background colour as `UIColor`.
    public var hudUIBackgroundColor: UIColor { UIColor(argb: hudBackgroundColor) }
}
