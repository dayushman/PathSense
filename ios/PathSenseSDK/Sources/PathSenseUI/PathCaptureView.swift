import UIKit
import PathSenseCore

public final class PathCaptureView: UIView {
    public let tracker: PathTracker
    public var overlayConfig: PathOverlayConfig = PathOverlayConfig() {
        didSet { overlayView.overlayConfig = overlayConfig }
    }

    private let overlayView: TouchOverlayView

    public init(tracker: PathTracker) {
        self.tracker = tracker
        self.overlayView = TouchOverlayView(tracker: tracker)
        super.init(frame: .zero)
        isMultipleTouchEnabled = false
        overlayView.isUserInteractionEnabled = false
        addSubview(overlayView)
    }

    public required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        overlayView.frame = bounds
    }

    public func clearCanvas() {
        tracker.clearPoints()
        overlayView.resetFade()
        overlayView.setNeedsDisplay()
    }

    public override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first else { return }
        let point = touch.location(in: self)
        tracker.onDown(p: PathPoint(x: Float(point.x), y: Float(point.y), tMillis: Int64(Date().timeIntervalSince1970 * 1000)))
        overlayView.resetFade()
        overlayView.setNeedsDisplay()
    }

    public override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first else { return }
        let point = touch.location(in: self)
        tracker.onMove(p: PathPoint(x: Float(point.x), y: Float(point.y), tMillis: Int64(Date().timeIntervalSince1970 * 1000)))
        overlayView.setNeedsDisplay()
    }

    public override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first else { return }
        let point = touch.location(in: self)
        tracker.onUp(p: PathPoint(x: Float(point.x), y: Float(point.y), tMillis: Int64(Date().timeIntervalSince1970 * 1000)))
        overlayView.startFadeIfNeeded()
        overlayView.setNeedsDisplay()
    }

    public override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        tracker.onCancel()
        overlayView.startFadeIfNeeded()
        overlayView.setNeedsDisplay()
    }
}

final class TouchOverlayView: UIView {
    private let tracker: PathTracker
    var overlayConfig: PathOverlayConfig = PathOverlayConfig()

    private var fadeStart: Date?

    init(tracker: PathTracker) {
        self.tracker = tracker
        super.init(frame: .zero)
        isOpaque = false
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func resetFade() {
        layer.opacity = 1.0
        fadeStart = nil
    }

    func startFadeIfNeeded() {
        guard overlayConfig.style.fadeOutMs > 0 else { return }
        fadeStart = Date()
        UIView.animate(withDuration: TimeInterval(overlayConfig.style.fadeOutMs) / 1000.0) {
            self.layer.opacity = 0.0
        }
    }

    override func draw(_ rect: CGRect) {
        if overlayConfig.debugOnly {
            #if !DEBUG
            return
            #endif
        }
        if !PathSenseUI.isEnabled { return }
        guard let points = tracker.currentPoints as? [PathPoint], points.count > 1 else { return }
        guard let ctx = UIGraphicsGetCurrentContext() else { return }
        if !overlayConfig.showCoordinateHUD && !overlayConfig.showCrosshair && !overlayConfig.showTouchCircle && points.isEmpty {
            return
        }

        let path = UIBezierPath()
        path.move(to: CGPoint(x: CGFloat(points[0].x), y: CGFloat(points[0].y)))
        for i in 1..<points.count {
            let prev = points[i - 1]
            let curr = points[i]
            let mid = CGPoint(x: CGFloat((prev.x + curr.x) / 2.0), y: CGFloat((prev.y + curr.y) / 2.0))
            path.addQuadCurve(to: mid, controlPoint: CGPoint(x: CGFloat(prev.x), y: CGFloat(prev.y)))
        }
        if let last = points.last {
            path.addLine(to: CGPoint(x: CGFloat(last.x), y: CGFloat(last.y)))
        }

        ctx.saveGState()
        ctx.addPath(path.cgPath)
        ctx.setLineWidth(overlayConfig.style.strokeWidth)
        ctx.setLineCap(overlayConfig.style.strokeCap)
        ctx.replacePathWithStrokedPath()
        ctx.clip()

        let colors = [overlayConfig.style.gradientStartColor.cgColor, overlayConfig.style.gradientEndColor.cgColor] as CFArray
        let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0, 1])
        let start = CGPoint(x: CGFloat(points.first?.x ?? 0), y: CGFloat(points.first?.y ?? 0))
        let end = CGPoint(x: CGFloat(points.last?.x ?? 0), y: CGFloat(points.last?.y ?? 0))
        if let gradient = gradient {
            ctx.drawLinearGradient(gradient, start: start, end: end, options: [])
        }
        ctx.restoreGState()

        if overlayConfig.showCrosshair, let last = points.last {
            let crosshairPath = UIBezierPath()
            crosshairPath.move(to: CGPoint(x: 0, y: CGFloat(last.y)))
            crosshairPath.addLine(to: CGPoint(x: bounds.width, y: CGFloat(last.y)))
            crosshairPath.move(to: CGPoint(x: CGFloat(last.x), y: 0))
            crosshairPath.addLine(to: CGPoint(x: CGFloat(last.x), y: bounds.height))
            overlayConfig.style.gradientEndColor.setStroke()
            crosshairPath.lineWidth = 1.5
            crosshairPath.stroke()
        }

        if overlayConfig.showTouchCircle, let last = points.last {
            let radius = max(16.0, overlayConfig.style.strokeWidth * 3.0)
            let circle = UIBezierPath(ovalIn: CGRect(x: CGFloat(last.x) - radius, y: CGFloat(last.y) - radius, width: radius * 2, height: radius * 2))
            overlayConfig.style.gradientStartColor.setStroke()
            circle.lineWidth = 2.5
            circle.stroke()
        }

        if overlayConfig.showCoordinateHUD, let last = points.last {
            let prev = points.dropLast().last
            let dx = prev != nil ? Float(last.x - prev!.x) : 0
            let dy = prev != nil ? Float(last.y - prev!.y) : 0
            let text = "x=\(Int(last.x))  y=\(Int(last.y))  dx=\(Int(dx))  dy=\(Int(dy))"
            let attributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 14),
                .foregroundColor: overlayConfig.style.gradientEndColor,
            ]

            let padding: CGFloat = 12
            let textSize = text.size(withAttributes: attributes)
            let origin: CGPoint
            switch overlayConfig.hudAlignment {
            case .topLeft:
                origin = CGPoint(x: padding, y: padding)
            case .topRight:
                origin = CGPoint(x: bounds.width - textSize.width - padding, y: padding)
            case .bottomLeft:
                origin = CGPoint(x: padding, y: bounds.height - textSize.height - padding)
            case .bottomRight:
                origin = CGPoint(x: bounds.width - textSize.width - padding, y: bounds.height - textSize.height - padding)
            case .centerLeft:
                origin = CGPoint(x: padding, y: bounds.height / 2 - textSize.height / 2)
            case .centerRight:
                origin = CGPoint(x: bounds.width - textSize.width - padding, y: bounds.height / 2 - textSize.height / 2)
            }
            text.draw(at: origin, withAttributes: attributes)
        }
    }
}
