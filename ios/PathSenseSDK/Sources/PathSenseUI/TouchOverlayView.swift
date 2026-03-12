import PathSenseCore
import UIKit

final class TouchOverlayView: UIView {
    private let tracker: PathTracker
    var overlayConfig: PathOverlayConfig = PathOverlayConfig() {
        didSet { applyHudConfig() }
    }

    private var fadeStart: Date?
    private var startPoint: CGPoint?
    private var drawingOpacity: Float = 1.0
    private var displayLink: CADisplayLink?

    // Bounding box cache — recomputed only when tracker's pointsVersion changes
    private var cachedBoundingBox: CGRect?
    private var cachedBboxVersion: Int32 = -1

    private static let hudDefaultText = "x: \u{2013}  y: \u{2013}  dx: \u{2013}  dy: \u{2013}"

    // ---- HUD label (matches Android PathOverlayView's hudLabel) ----
    private let hudLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.monospacedSystemFont(ofSize: 13, weight: .regular)
        label.text = TouchOverlayView.hudDefaultText
        label.textAlignment = .left
        label.clipsToBounds = true
        label.layer.cornerRadius = 8
        label.isHidden = true
        return label
    }()

    private let hudPadding: CGFloat = 12
    private let hudHPad: CGFloat = 12
    private let hudVPad: CGFloat = 6

    init(tracker: PathTracker) {
        self.tracker = tracker
        super.init(frame: .zero)
        isOpaque = false
        addSubview(hudLabel)
        applyHudConfig()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    deinit {
        displayLink?.invalidate()
    }

    private func applyHudConfig() {
        hudLabel.textColor = overlayConfig.hudUITextColor
        hudLabel.backgroundColor = overlayConfig.hudUIBackgroundColor
        hudLabel.isHidden = !overlayConfig.showCoordinateHUD
        setNeedsLayout()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        guard overlayConfig.showCoordinateHUD else { return }

        let maxText = "x: 0000  y: 0000  dx: -0000  dy: -0000"
        let maxSize = maxText.size(withAttributes: [.font: hudLabel.font as Any])
        let labelWidth = maxSize.width + hudHPad * 2
        let labelHeight = maxSize.height + hudVPad * 2
        hudLabel.frame.size = CGSize(width: labelWidth, height: labelHeight)

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
                x: bounds.width - labelWidth - hudPadding - safeRight, y: hudPadding + safeTop)
        } else if alignment == .bottomLeft {
            origin = CGPoint(
                x: hudPadding + safeLeft, y: bounds.height - labelHeight - hudPadding - safeBottom)
        } else if alignment == .bottomRight {
            origin = CGPoint(
                x: bounds.width - labelWidth - hudPadding - safeRight,
                y: bounds.height - labelHeight - hudPadding - safeBottom)
        } else if alignment == .centerLeft {
            origin = CGPoint(x: hudPadding + safeLeft, y: (bounds.height - labelHeight) / 2)
        } else {  // .centerRight
            origin = CGPoint(
                x: bounds.width - labelWidth - hudPadding - safeRight,
                y: (bounds.height - labelHeight) / 2)
        }
        hudLabel.frame.origin = origin
    }

    // MARK: - Touch lifecycle helpers

    func notifyTouchStart(at point: CGPoint) {
        startPoint = point
        cachedBoundingBox = nil
        cachedBboxVersion = -1
        resetFade()
        updateHudText(current: point)
        setNeedsDisplay()
    }

    func notifyTouchMove(to point: CGPoint) {
        updateHudText(current: point)
        setNeedsDisplay()
    }

    func notifyTouchEnd(at point: CGPoint) {
        updateHudText(current: point)
        startFadeIfNeeded()
        setNeedsDisplay()
    }

    func notifyTouchCancel() {
        displayLink?.invalidate()
        displayLink = nil
        tracker.clearPoints()
        fadeStart = nil
        drawingOpacity = 1.0
        startPoint = nil
        cachedBoundingBox = nil
        cachedBboxVersion = -1
        hudLabel.text = Self.hudDefaultText
        setNeedsLayout()
        setNeedsDisplay()
    }

    private func resetFade() {
        displayLink?.invalidate()
        displayLink = nil
        drawingOpacity = 1.0
        fadeStart = nil
    }

    private func startFadeIfNeeded() {
        guard overlayConfig.style.fadeOutMs > 0 else { return }
        fadeStart = Date()
        displayLink?.invalidate()
        let link = CADisplayLink(target: self, selector: #selector(fadeStep))
        link.add(to: .main, forMode: .common)
        displayLink = link
    }

    @objc private func fadeStep() {
        guard let fadeStart = fadeStart else {
            displayLink?.invalidate()
            displayLink = nil
            return
        }
        let elapsed = Date().timeIntervalSince(fadeStart)
        let duration = TimeInterval(overlayConfig.style.fadeOutMs) / 1000.0
        let t = min(Float(elapsed / duration), 1.0)
        drawingOpacity = 1.0 - t
        setNeedsDisplay()
        if t >= 1.0 {
            displayLink?.invalidate()
            displayLink = nil
            tracker.clearPoints()
            startPoint = nil
            hudLabel.text = Self.hudDefaultText
            setNeedsLayout()
        }
    }

    private func updateHudText(current: CGPoint) {
        guard overlayConfig.showCoordinateHUD else { return }
        let sp = startPoint ?? current
        let dx = current.x - sp.x
        let dy = current.y - sp.y
        hudLabel.text = "x: \(Int(current.x))  y: \(Int(current.y))  dx: \(Int(dx))  dy: \(Int(dy))"
    }

    func clearCaches() {
        cachedBoundingBox = nil
        cachedBboxVersion = -1
    }

    func clearCanvas() {
        displayLink?.invalidate()
        displayLink = nil
        tracker.clearPoints()
        fadeStart = nil
        drawingOpacity = 1.0
        startPoint = nil
        cachedBoundingBox = nil
        cachedBboxVersion = -1
        hudLabel.text = Self.hudDefaultText
        setNeedsLayout()
        setNeedsDisplay()
    }

    // MARK: - Drawing

    override func draw(_ rect: CGRect) {
        if overlayConfig.debugOnly {
            #if !DEBUG
                return
            #endif
        }
        guard drawingOpacity > 0 else { return }
        let points = tracker.currentPoints
        guard !points.isEmpty else { return }
        guard let ctx = UIGraphicsGetCurrentContext() else { return }

        ctx.setAlpha(CGFloat(drawingOpacity))

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

        // --- Crosshair (2pt stroke, ~63% alpha, matching Android) ---
        if overlayConfig.showCrosshair, let last = points.last {
            let crosshairPath = UIBezierPath()
            crosshairPath.move(to: CGPoint(x: 0, y: CGFloat(last.y)))
            crosshairPath.addLine(to: CGPoint(x: bounds.width, y: CGFloat(last.y)))
            crosshairPath.move(to: CGPoint(x: CGFloat(last.x), y: 0))
            crosshairPath.addLine(to: CGPoint(x: CGFloat(last.x), y: bounds.height))
            overlayConfig.style.gradientEndUIColor.withAlphaComponent(0.63).setStroke()
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
}
