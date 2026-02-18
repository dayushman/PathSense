# PathSense SDK Plan

## Summary

Build a Kotlin Multiplatform SDK split into two layers: a **headless core** (`:pathsdk-core`) for path capture, smoothing, metrics, and gesture recognition — usable without any UI dependency — and an **opt-in rendering module** (`:pathsdk-ui`) that provides real-time visualization with gradient trails, crosshair overlays, touch circles, and a coordinate HUD. Provide Android View + Compose adapters and iOS UIKit + SwiftUI adapters. Distribute via MavenCentral (Android) and Swift Package Manager (iOS). Path analysis and recognition are **production-ready by default**; rendering overlays default to debug-only.

## Public APIs / Interfaces

### Core Module (`:pathsdk-core`)

- `PathPoint(x: Float, y: Float, tMillis: Long)`
- `PathMetrics(length: Float, bbox: RectF, start: PathPoint, end: PathPoint, avgDirectionDeg: Float, avgSpeed: Float, deltaX: Float, deltaY: Float)`
- `PathConfig(samplingHz: Int = 120, minDistancePx: Float = 2f, smoothingWindow: Int = 3, resampleSpacingPx: Float = 6f, maxPoints: Int = 500)`
  - `maxPoints`: memory-bounded point cap — oldest points evicted when exceeded (FIFO ring buffer)
- `PathEvent`
  - `Started(sessionId: String, point: PathPoint)` — main thread, synchronous
  - `Updated(sessionId: String, points: List<PathPoint>)` — main thread, synchronous; carries smoothed points only (no metrics) so the renderer can draw immediately
  - `MetricsUpdated(sessionId: String, metrics: PathMetrics)` — posted to main thread from background; arrives ~1 frame after the corresponding `Updated`
  - `Ended(sessionId: String, points: List<PathPoint>)` — main thread, synchronous
  - `MetricsEnded(sessionId: String, metrics: PathMetrics)` — final metrics, posted to main from background after `Ended`
  - `GestureRecognized(sessionId: String, match: GestureMatch)` — posted to main from background after `MetricsEnded`; always asynchronous
  - `Cancelled(sessionId: String)` — main thread, synchronous
- `GestureMatch(type: GestureType, score: Float, algorithm: String)`
  - `score`: 0.0–1.0 normalized confidence; computed via **$1 Unistroke Recognizer** for built-ins (cosine distance between resampled candidate and template)
- `GestureType` built-ins: `LINE`, `CIRCLE`, `RECTANGLE`, `ZIGZAG`, `UNKNOWN`
- `GestureRecognizer` interface
  - `fun recognize(points: List<PathPoint>): GestureMatch?`
- `PathTracker(config: PathConfig)` — **headless, no UI dependency**
  - `fun onDown(p: PathPoint)`
  - `fun onMove(p: PathPoint)`
  - `fun onUp(p: PathPoint)`
  - `fun onCancel()`
  - `fun addRecognizer(r: GestureRecognizer)`
  - `fun removeRecognizer(r: GestureRecognizer)`
  - `fun clearPoints()` — reset internal point buffer
  - `var listener: (PathEvent) -> Unit`
  - `val currentPoints: List<PathPoint>` — read-only access to the current (smoothed) point buffer

### Rendering Module (`:pathsdk-ui`) — opt-in

- `PathOverlayConfig(debugOnly: Boolean = true, style: PathStyle = PathStyle(), showCrosshair: Boolean = false, showTouchCircle: Boolean = true, showCoordinateHUD: Boolean = false, hudAlignment: HUDAlignment = TOP_LEFT)`
  - `debugOnly`: when `true`, overlay is compiled out / no-ops in release builds
- `PathStyle`
  - `gradientStartColor: Long = 0xFFFF3B30` (red)
  - `gradientEndColor: Long = 0xFF007AFF` (blue)
  - `strokeWidthPx: Float = 4f`
  - `strokeCap: StrokeCap = ROUND`
  - `fadeOutMs: Long = 300` — path fades out over this duration after finger lifts (0 = stays until cleared)
  - `showBoundingBox: Boolean = false`
  - `boundingBoxColor: Long = 0x4400FF00`
- `HUDAlignment`: `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`, `CENTER_LEFT`, `CENTER_RIGHT`
- Coordinate HUD displays: live `(x, y)` position and `(dx, dy)` delta from last point
- `fun clearCanvas()` — programmatically clear all rendered paths and overlays

## Architecture / Modules

### `:pathsdk-core` (KMM — no UI dependencies)

- `commonMain`: models, smoothing/resampling, metrics, $1 Unistroke recognizer, `PathTracker`
  - `PointBuffer`: ring-buffer capped at `maxPoints` with FIFO eviction
  - `AnalysisPipeline`: background coroutine on `Dispatchers.Default` that receives snapshots of smoothed points and produces resampled points → metrics → recognition results, then posts events back to main
  - **Threading model**:
    - **Main thread (synchronous, inline):** touch ingestion (`onDown`/`onMove`/`onUp`) → smoothing (3-point moving average, sub-µs) → ring buffer write → emit `Updated`/`Ended` (points only) → notify renderer
    - **Background (`Dispatchers.Default`):** resampling → metrics computation → gesture recognition → post `MetricsUpdated` / `MetricsEnded` / `GestureRecognized` back to main thread
    - This keeps touch-to-pixel latency at zero while moving all heavier math off the main thread
- `androidMain`: Android-specific helpers (main dispatcher binding, etc.)
- `iosMain`: K/N bindings (main-thread dispatch via `DispatchQueue.main`; no UIKit/SwiftUI here)

### `:pathsdk-ui` (depends on `:pathsdk-core`)

- **Android UI**
  - `PathSense.init(app, config)` — zero-config entry point; auto-attaches to every Activity via `ActivityLifecycleCallbacks`, intercepts touches via `Window.Callback`, renders overlay on `decor` `FrameLayout`
    - `PathSenseConfig(pathConfig, overlayConfig, listener)` — configuration data class
    - `PathSense.trackerFor(activity)` — retrieve the `PathTracker` for a given Activity
  - `TouchInterceptWindowCallback` — wraps original `Window.Callback` to transparently observe touch events
  - `PathOverlayView : FrameLayout` — transparent overlay (gradient trail, crosshair, touch circle, bounding box, coordinate HUD) used internally by auto-attach
  - Compose `@Composable PathCapture / PathOverlay` — available for advanced manual integration if needed
  - `PathCaptureView : FrameLayout` — available for advanced manual integration if needed
- **iOS UI (Swift sources in SPM)**
  - `PathSense.configure(config)` — zero-config entry point mirroring Android; swizzles `UIWindow.sendEvent(_:)` to intercept touches, auto-attaches `TouchOverlayView` to every window, and auto-creates a `PathTracker` per window. Works with both UIKit and SwiftUI apps — no view hierarchy changes needed.
    - `PathSenseConfig(overlayConfig, listener)` — configuration struct (iOS equivalent of Android `PathSenseConfig`)
    - `PathSense.tracker(for: window)` — retrieve the `PathTracker` for a given window
  - `TouchOverlayView : UIView` — transparent overlay (gradient trail, crosshair, touch circle, bounding box, coordinate HUD) used internally by auto-attach and `PathTrackingWindow`
  - `PathTrackingWindow : UIWindow` — available for advanced manual integration (UIKit window subclass)
  - `PathCaptureView : UIView` / `PathCaptureRepresentable` — available for advanced manual integration
- **Rendering features (both platforms)**
  - **Gradient trail**: smooth color gradient from `gradientStartColor` → `gradientEndColor` following the path
  - **Crosshair lines**: full-screen horizontal + vertical lines tracking current touch position
  - **Touch circle**: circle indicator at current contact point
  - **Coordinate HUD**: live `(x, y)` + `(dx, dy)` label, configurable position (6 alignment options)
  - Drawing uses the smoothed (not raw) points for a visually clean stroke
  - Quadratic Bézier interpolation between consecutive points for smooth curves
  - Fade-out animation after `onUp` when `fadeOutMs > 0` (default 300ms)
  - `clearCanvas()` removes all drawn paths / overlays and resets internal buffers
  - Memory bounded: trail rendering capped at `maxPoints` (inherited from core config)

## Distribution

- **Android**: MavenCentral — publish `pathsdk-core` and `pathsdk-ui` as separate artifacts from KMM targets.
- **iOS**: Swift Package Manager — publish a `PathSenseSDK` Swift package containing:
  - `PathSenseCore` library target wrapping the KMM XCFramework
  - `PathSenseUI` library target with Swift sources for UIKit/SwiftUI adapters
  - Consumers add only the targets they need; `PathSenseUI` depends on `PathSenseCore`
- **Debug-only scope** — applies only to the rendering module (`:pathsdk-ui`):
  - Android: `if (!BuildConfig.DEBUG && overlayConfig.debugOnly) { no-op }`
  - iOS: `#if DEBUG` wrappers around window/overlay code + runtime `PathSenseUI.isEnabled`
- Core module (`:pathsdk-core`) is **always active** — metrics and recognition are production features, not debug tools.

## Key Behaviors

- Single-finger tracking only.
- Time-based throttle plus distance threshold (configurable).
- **Two-thread processing pipeline**:
  - **Main thread (synchronous):** touch ingestion → smoothing (3-point moving average) → ring buffer write → emit `Started` / `Updated` / `Ended` / `Cancelled` → notify renderer. Smoothing is sub-microsecond for a 3-point window — no frame impact.
  - **Background thread (`Dispatchers.Default`):** resampling → metrics computation → gesture recognition → post `MetricsUpdated` / `MetricsEnded` / `GestureRecognized` back to main. Metrics arrive ~1 frame after the corresponding `Updated` event; this latency is imperceptible to consumers.
- **Memory bounded**: point buffer capped at `maxPoints` (default 500); oldest points evicted via ring buffer so long gestures never cause unbounded growth.
- **Headless-first**: `PathTracker` has zero UI dependencies; it can be used in services, tests, or background processing without any View.
- **Rendering is opt-in**: consumers add `:pathsdk-ui` only if they want visual overlays. Primary integration on both platforms is **auto-attach**:
  - **Android**: `PathSense.init(app)` — one-liner in `Application.onCreate()`; auto-attaches to every Activity via `ActivityLifecycleCallbacks`
  - **iOS**: `PathSense.configure()` — one-liner in `App.init()` or `didFinishLaunchingWithOptions:`; swizzles `UIWindow.sendEvent(_:)` to automatically intercept touches and add an overlay to every window
  - Manual integration APIs (`PathCaptureView`, Compose `PathCapture`, `PathTrackingWindow`) remain available for advanced use cases
- **Zero touch-to-pixel latency**: the renderer reads smoothed points directly from the main-thread buffer — no waiting for background work. Resampling, metrics, and recognition never block drawing.
- **Recognition algorithm**: built-in recognizers use the **$1 Unistroke Recognizer** (resampling to 64 points, rotating to indicative angle, scaling to unit square, matching against templates via cosine distance). `GestureMatch.score` = 1 − (distance / half-diagonal of unit square), range 0.0–1.0. Default threshold: 0.75.
- Non-intrusive: overlay views are transparent to touch events (`isUserInteractionEnabled = false` / `clickable = false`); app functions normally.
- All `PathEvent` callbacks delivered on main thread. Background pipeline posts results via platform main dispatcher (`Dispatchers.Main` / `DispatchQueue.main`).

## Tests

- `commonTest`
  - Smoothing preserves endpoints and reduces jitter.
  - Resampling produces expected count for known input length/spacing.
  - Metrics for straight line and circle inputs (including `deltaX`/`deltaY`).
  - Built-in $1 recognizers: line/circle/rectangle/zigzag correct within tolerance.
  - Threshold behavior returns `UNKNOWN` when score < 0.75.
  - **Ring buffer**: verify FIFO eviction at `maxPoints` boundary; oldest points dropped, newest retained.
  - **Threading pipeline**: verify `Updated` emitted synchronously on calling thread; verify `MetricsUpdated` and `GestureRecognized` posted back to main after background processing.
  - **Event ordering**: `Started` → `Updated`\* → `Ended` → `MetricsEnded` → `GestureRecognized` — verify strict sequence per session.
- `androidTest`
  - `PathCaptureView` emits Started/Updated/Ended for synthesized touch sequence.
  - `PathOverlayView` renders non-empty bitmap with gradient trail after a synthesized draw gesture.
  - `clearCanvas()` resets the drawn bitmap to blank.
  - Compose `PathCapture` emits same event sequence and renders correctly.
  - Compose `PathOverlay` renders gradient, crosshair, touch circle, HUD independently of capture.
  - Headless `PathTracker` (no UI) produces correct events and metrics.
- iOS
  - Unit tests for Swift wrappers' delegate wiring.
  - `PathTrackingWindow` intercepts touch via `sendEvent(_:)` and emits correct events without modifying view hierarchy.
  - Snapshot test: `PathCaptureView` layer renders a visible gradient stroke after simulated touches.
  - Snapshot test: crosshair lines and touch circle visible at correct coordinates.
  - Coordinate HUD displays correct `(x, y)` and `(dx, dy)` values.

## Assumptions / Defaults

- Kotlin 2.x + KMM plugin; can pin versions based on your existing toolchain.
- Package namespace defaults to `com.yourorg.pathsdk` (adjustable).
- Built-in gesture set: line, circle, rectangle, zigzag — implemented via $1 Unistroke Recognizer.
- Recognition score threshold default: 0.75.
- Point buffer default cap: 500 points (configurable via `maxPoints`).
- Rendering is a separate opt-in module; core SDK has zero UI framework dependencies.
- iOS distribution via SPM (not CocoaPods); XCFramework for the KMM binary, pure Swift for UI adapters.
- Both Android and iOS use auto-attach as the primary integration; manual APIs available for advanced use cases.
- All GesturePathKit visualization features included: gradient trail, crosshair lines, touch circle, coordinate HUD with 6 alignment options.
- Repo is currently empty; we will scaffold from scratch.
