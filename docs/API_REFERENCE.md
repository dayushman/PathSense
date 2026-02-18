# API Reference

## Core Models

| Type                                   | Description                                                                                                     |
| -------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| `PathPoint(x, y, tMillis)`             | A timestamped touch coordinate                                                                                  |
| `PathConfig`                           | Sampling rate, distance threshold, smoothing window, resample spacing, max points                               |
| `PathMetrics`                          | Computed path length, bounding box, start/end points, direction, speed, deltas                                  |
| `PathEvent`                            | Sealed class: `Started`, `Updated`, `MetricsUpdated`, `Ended`, `MetricsEnded`, `GestureRecognized`, `Cancelled` |
| `GestureMatch(type, score, algorithm)` | Recognition result with confidence score (0.0–1.0)                                                              |
| `GestureType`                          | `LINE`, `CIRCLE`, `RECTANGLE`, `ZIGZAG`, `UNKNOWN`                                                              |

---

## PathTracker

The headless core engine — no UI dependencies.

```kotlin
val tracker = PathTracker(config = PathConfig())

// Feed touch events
tracker.onDown(PathPoint(x, y, timeMillis))
tracker.onMove(PathPoint(x, y, timeMillis))
tracker.onUp(PathPoint(x, y, timeMillis))
tracker.onCancel()

// Listen for events
tracker.listener = { event: PathEvent -> /* ... */ }

// Read current smoothed points
val points: List<PathPoint> = tracker.currentPoints

// Manage recognizers
tracker.addRecognizer(customRecognizer)
tracker.removeRecognizer(customRecognizer)
tracker.clearPoints()
```

---

## Custom Gesture Recognizer

```kotlin
val customRecognizer = GestureRecognizer { points ->
    // Analyze points, return GestureMatch or null
    GestureMatch(GestureType.LINE, score = 0.9f, algorithm = "custom")
}
tracker.addRecognizer(customRecognizer)
```

---

## PathConfig

| Property            | Default | Description                             |
| ------------------- | ------- | --------------------------------------- |
| `samplingHz`        | `120`   | Max sampling rate (time-based throttle) |
| `minDistancePx`     | `2f`    | Min distance between accepted points    |
| `smoothingWindow`   | `3`     | 3-point moving average smoothing window |
| `resampleSpacingPx` | `6f`    | Resampling interval for recognition     |
| `maxPoints`         | `500`   | Ring buffer capacity (FIFO eviction)    |

---

## PathOverlayConfig

| Property             | Default                  | Description                                                                                         |
| -------------------- | ------------------------ | --------------------------------------------------------------------------------------------------- |
| `debugOnly`          | `true`                   | When true, overlay no-ops in release builds                                                         |
| `style`              | `PathStyle()`            | Visual style settings                                                                               |
| `showCrosshair`      | `false`                  | Full-screen crosshair lines at touch position                                                       |
| `showTouchCircle`    | `true`                   | Circle indicator at current touch point                                                             |
| `showCoordinateHUD`  | `false`                  | Built-in pill-shaped HUD showing `x: y: dx: dy:` with delta from start point (monospace 13sp)       |
| `hudAlignment`       | `TOP_LEFT`               | HUD position: `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`, `CENTER_LEFT`, `CENTER_RIGHT` |
| `hudTextColor`       | `0xFFFFFFFF` (white)     | Coordinate HUD text color (ARGB)                                                                    |
| `hudBackgroundColor` | `0xB3000000` (70% black) | Coordinate HUD background color (ARGB)                                                              |

> **Note:** All color values use standard ARGB hex format (e.g. `0xFFFF3B30`). The SDK handles conversion for both View and Compose renderers.
>
> The coordinate HUD automatically respects system bar insets (status bar, navigation bar) on edge-to-edge configurations — no extra layout work required.

---

## PathStyle

| Property             | Default             | Description                                                    |
| -------------------- | ------------------- | -------------------------------------------------------------- |
| `gradientStartColor` | `0xFFFF3B30` (red)  | Trail gradient start color                                     |
| `gradientEndColor`   | `0xFF007AFF` (blue) | Trail gradient end color                                       |
| `strokeWidthPx`      | `4f`                | Trail stroke width                                             |
| `strokeCap`          | `ROUND`             | Stroke cap style: `BUTT`, `ROUND`, `SQUARE`                    |
| `fadeOutMs`          | `300`               | Fade-out duration after finger lifts (0 = stays until cleared) |
| `showBoundingBox`    | `false`             | Draw bounding box around the path                              |
| `boundingBoxColor`   | `0x4400FF00`        | Bounding box color                                             |

---

## PathSenseConfig (Android)

```kotlin
PathSenseConfig(
    pathConfig: PathConfig = PathConfig(),
    overlayConfig: PathOverlayConfig = PathOverlayConfig(),
    listener: ((PathEvent) -> Unit)? = null
)
```

## PathSenseConfig (iOS)

```swift
var config = PathSenseConfig()
config.overlayConfig = PathOverlayConfig()
config.listener = { event in /* ... */ }
```

---

## Android Entry Points

| API                            | Description                                      |
| ------------------------------ | ------------------------------------------------ |
| `PathSense.init(app)`          | Zero-config auto-attach to all Activities         |
| `PathSense.init(app, config)`  | Auto-attach with custom config                    |
| `PathSense.trackerFor(activity)` | Retrieve the `PathTracker` for a specific Activity |

## iOS Entry Points

| API                                | Description                                |
| ---------------------------------- | ------------------------------------------ |
| `PathSense.configure()`            | Zero-config auto-attach to all UIWindows    |
| `PathSense.configure(config)`      | Auto-attach with custom config              |
| `PathSense.tracker(for: window)`   | Retrieve the `PathTracker` for a UIWindow   |

---

## Advanced / Manual Integration

For cases where you need direct control over the overlay or capture view:

### Android

- `PathOverlayView` — transparent overlay `FrameLayout` (gradient trail, crosshair, touch circle, HUD)
- `PathCaptureView` — `FrameLayout` that captures touches and renders the overlay
- Compose `PathCapture` / `PathOverlay` — composable equivalents

### iOS

- `PathTrackingWindow` — `UIWindow` subclass that intercepts touches
- `TouchOverlayView` — transparent `UIView` overlay
- `PathCaptureView` / `PathCaptureRepresentable` — manual `UIView` / SwiftUI wrapper
