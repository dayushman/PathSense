#if DEBUG
import PathSenseCore
import UIKit

final class TouchOverlayView: UIView {
    private let tracker: PathTracker
    var overlayConfig: PathOverlayConfig = PathOverlayConfig() {
        didSet { applyHudConfig() }
    }

    private var startPoint: CGPoint?
    private var isTouchActive = false

    // Bounding box cache — recomputed only when tracker's pointsVersion changes
    private var cachedBoundingBox: CGRect?
    private var cachedBboxVersion: Int32 = -1

    private static let hudDefaultText = "x: \u{2013}  y: \u{2013}  dx: \u{2013}  dy: \u{2013}"
    private static let hudFont = UIFont.monospacedSystemFont(ofSize: 13, weight: .regular)
    private static let hudMaxSize: CGSize = "x: 0000  y: 0000  dx: -0000  dy: -0000"
        .size(withAttributes: [.font: hudFont])
    private static let hudCornerRadius: CGFloat = 8
    private static let crosshairColor = UIColor(red: 1, green: 0, blue: 1, alpha: 1)

    private var hudText = TouchOverlayView.hudDefaultText

    private let hudPadding: CGFloat = 12
    private let hudHPad: CGFloat = 12
    private let hudVPad: CGFloat = 6

    init(tracker: PathTracker) {
        self.tracker = tracker
        super.init(frame: .zero)
        isOpaque = false
        applyHudConfig()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func applyHudConfig() {
        setNeedsDisplay()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        setNeedsDisplay()
    }

    // MARK: - Touch lifecycle helpers

    func notifyTouchStart(at point: CGPoint) {
        isTouchActive = true
        startPoint = point
        cachedBoundingBox = nil
        cachedBboxVersion = -1
        updateHudText(current: point)
        requestImmediateDisplay()
    }

    func notifyTouchMove(to point: CGPoint) {
        isTouchActive = true
        updateHudText(current: point)
        requestImmediateDisplay()
    }

    func notifyTouchEnd(at _: CGPoint) {
        isTouchActive = false
        resetOverlayState(clearPoints: true)
    }

    func notifyTouchCancel() {
        resetOverlayState(clearPoints: true)
    }

    private func resetOverlayState(clearPoints: Bool) {
        if clearPoints {
            tracker.clearPoints()
        }
        isTouchActive = false
        startPoint = nil
        cachedBoundingBox = nil
        cachedBboxVersion = -1
        hudText = Self.hudDefaultText
        requestImmediateDisplay()
    }

    private func requestImmediateDisplay() {
        setNeedsDisplay()
    }

    private func updateHudText(current: CGPoint) {
        guard overlayConfig.showCoordinateHUD else { return }
        let sp = startPoint ?? current
        let dx = current.x - sp.x
        let dy = current.y - sp.y
        hudText = "x: \(Int(current.x))  y: \(Int(current.y))  dx: \(Int(dx))  dy: \(Int(dy))"
    }

    func clearCaches() {
        cachedBoundingBox = nil
        cachedBboxVersion = -1
    }

    func clearCanvas() {
        resetOverlayState(clearPoints: true)
    }

    // MARK: - Drawing

    override func draw(_ rect: CGRect) {
        if overlayConfig.debugOnly {
            #if !DEBUG
                return
            #endif
        }

        drawHud(in: rect)

        let points = tracker.currentPoints
        guard !points.isEmpty else { return }
        guard let ctx = UIGraphicsGetCurrentContext() else { return }

        // --- Tap dot (single-point or near-zero-distance gesture, matching Android's isTap logic) ---
        let first = points[0]
        let last = points[points.count - 1]
        let dx = Double(last.x - first.x)
        let dy = Double(last.y - first.y)
        let isTap = hypot(dx, dy) < 1.0

        if isTap {
            let radius = max(overlayConfig.style.strokeWidth, 4.0)
            ctx.setFillColor(overlayConfig.style.gradientStartUIColor.cgColor)
            ctx.fillEllipse(in: CGRect(
                x: CGFloat(last.x) - radius, y: CGFloat(last.y) - radius,
                width: radius * 2, height: radius * 2))
        } else {
            // --- Gradient trail ---
            let path = UIBezierPath()
            path.move(to: CGPoint(x: CGFloat(points[0].x), y: CGFloat(points[0].y)))
            for i in 1..<points.count {
                let prev = points[i - 1]
                let curr = points[i]
                let mid = CGPoint(
                    x: CGFloat((prev.x + curr.x) / 2.0), y: CGFloat((prev.y + curr.y) / 2.0))
                path.addQuadCurve(
                    to: mid, controlPoint: CGPoint(x: CGFloat(prev.x), y: CGFloat(prev.y)))
            }
            if let last = points.last {
                path.addLine(to: CGPoint(x: CGFloat(last.x), y: CGFloat(last.y)))
            }

            ctx.saveGState()
            ctx.addPath(path.cgPath)
            ctx.setLineWidth(overlayConfig.style.strokeWidth)
            ctx.setLineCap(overlayConfig.style.strokeLineCap)
            ctx.replacePathWithStrokedPath()
            ctx.clip()

            let colors =
                [
                    overlayConfig.style.gradientStartUIColor.cgColor,
                    overlayConfig.style.gradientEndUIColor.cgColor,
                ] as CFArray
            let gradient = CGGradient(
                colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0, 1])
            let start = CGPoint(x: CGFloat(points.first?.x ?? 0), y: CGFloat(points.first?.y ?? 0))
            let end = CGPoint(x: CGFloat(points.last?.x ?? 0), y: CGFloat(points.last?.y ?? 0))
            if let gradient = gradient {
                ctx.drawLinearGradient(
                    gradient, start: start, end: end,
                    options: [.drawsBeforeStartLocation, .drawsAfterEndLocation])
            }
            ctx.restoreGState()
        }

        // --- Crosshair (2pt stroke, #FF00FF) ---
        if overlayConfig.showCrosshair, isTouchActive, let last = points.last {
            let crosshairPath = UIBezierPath()
            crosshairPath.move(to: CGPoint(x: 0, y: CGFloat(last.y)))
            crosshairPath.addLine(to: CGPoint(x: bounds.width, y: CGFloat(last.y)))
            crosshairPath.move(to: CGPoint(x: CGFloat(last.x), y: 0))
            crosshairPath.addLine(to: CGPoint(x: CGFloat(last.x), y: bounds.height))
            Self.crosshairColor.setStroke()
            crosshairPath.lineWidth = 2.0
            crosshairPath.stroke()
        }

        // --- Touch circle (3pt stroke, ~78% alpha, matching Android) ---
        if overlayConfig.showTouchCircle, let last = points.last {
            let radius = max(16.0, overlayConfig.style.strokeWidth * 3.0)
            let circle = UIBezierPath(
                ovalIn: CGRect(
                    x: CGFloat(last.x) - radius, y: CGFloat(last.y) - radius, width: radius * 2,
                    height: radius * 2))
            overlayConfig.style.gradientStartUIColor.withAlphaComponent(0.78).setStroke()
            circle.lineWidth = 3.0
            circle.stroke()
        }

        // --- Bounding box (optional, off by default, matching Android) ---
        if overlayConfig.style.showBoundingBox {
            let currentVersion = tracker.pointsVersion
            if cachedBoundingBox == nil || cachedBboxVersion != currentVersion {
                var minX = CGFloat.greatestFiniteMagnitude
                var minY = CGFloat.greatestFiniteMagnitude
                var maxX = -CGFloat.greatestFiniteMagnitude
                var maxY = -CGFloat.greatestFiniteMagnitude
                for p in points {
                    minX = min(minX, CGFloat(p.x))
                    minY = min(minY, CGFloat(p.y))
                    maxX = max(maxX, CGFloat(p.x))
                    maxY = max(maxY, CGFloat(p.y))
                }
                cachedBoundingBox = CGRect(x: minX, y: minY, width: maxX - minX, height: maxY - minY)
                cachedBboxVersion = currentVersion
            }
            guard let bbox = cachedBoundingBox else { return }
            let boxPath = UIBezierPath(rect: bbox)
            overlayConfig.style.boundingBoxUIColor.setStroke()
            boxPath.lineWidth = max(2.0, overlayConfig.style.strokeWidth / 2.0)
            boxPath.stroke()
        }
    }

    private func drawHud(in _: CGRect) {
        guard overlayConfig.showCoordinateHUD else { return }

        let frame = hudFrame()
        let bgPath = UIBezierPath(roundedRect: frame, cornerRadius: Self.hudCornerRadius)
        overlayConfig.hudUIBackgroundColor.setFill()
        bgPath.fill()

        let attributes: [NSAttributedString.Key: Any] = [
            .font: Self.hudFont,
            .foregroundColor: overlayConfig.hudUITextColor,
        ]
        let textRect = frame.insetBy(dx: hudHPad, dy: hudVPad)
        let textSize = hudText.size(withAttributes: attributes)
        let textPoint = CGPoint(
            x: textRect.minX,
            y: textRect.minY + max(0, (textRect.height - textSize.height) / 2.0)
        )
        hudText.draw(at: textPoint, withAttributes: attributes)
    }

    private func hudFrame() -> CGRect {
        let width = Self.hudMaxSize.width + hudHPad * 2
        let height = Self.hudMaxSize.height + hudVPad * 2

        let safeTop = safeAreaInsets.top
        let safeBottom = safeAreaInsets.bottom
        let safeLeft = safeAreaInsets.left
        let safeRight = safeAreaInsets.right

        let origin: CGPoint
        let alignment = overlayConfig.hudAlignment
        if alignment == .topLeft {
            origin = CGPoint(x: hudPadding + safeLeft, y: hudPadding + safeTop)
        } else if alignment == .topRight {
            origin = CGPoint(
                x: bounds.width - width - hudPadding - safeRight,
                y: hudPadding + safeTop
            )
        } else if alignment == .bottomLeft {
            origin = CGPoint(
                x: hudPadding + safeLeft,
                y: bounds.height - height - hudPadding - safeBottom
            )
        } else if alignment == .bottomRight {
            origin = CGPoint(
                x: bounds.width - width - hudPadding - safeRight,
                y: bounds.height - height - hudPadding - safeBottom
            )
        } else if alignment == .centerLeft {
            origin = CGPoint(
                x: hudPadding + safeLeft,
                y: (bounds.height - height) / 2
            )
        } else {  // .centerRight
            origin = CGPoint(
                x: bounds.width - width - hudPadding - safeRight,
                y: (bounds.height - height) / 2
            )
        }

        return CGRect(origin: origin, size: CGSize(width: width, height: height))
    }
}
#endif
