# PathSenseSDK

A **Kotlin Multiplatform** SDK for touch-path capture, real-time smoothing, metrics computation, and gesture recognition — with an opt-in rendering module for visual overlays. Ships for **Android** (View + Compose) and **iOS** (UIKit + SwiftUI).

---

## Features

- **Headless path tracking** — capture, smooth, and analyze touch paths with zero UI dependency
- **Gesture recognition** — built-in $1 Unistroke Recognizer for Line, Circle, Rectangle, and Zigzag detection
- **Real-time metrics** — path length, bounding box, direction, speed, and deltas computed on a background thread
- **Visual overlays** (opt-in) — gradient trail, crosshair lines, touch circle, and built-in coordinate HUD
- **Live coordinate HUD** — pill-shaped label showing `x: y: dx: dy:` with delta from start point, monospace font, fully customizable colors and position
- **Edge-to-edge safe** — HUD automatically insets away from status bar and navigation bar on Android 15+ and `enableEdgeToEdge()` apps
- **Zero-config integration** — one-line setup auto-attaches to all Activities (Android) and all Windows (iOS)
- **Memory-bounded** — FIFO ring buffer caps point storage (default 500), preventing unbounded growth
- **Zero touch-to-pixel latency** — smoothing runs inline on the main thread; heavy math is offloaded to background

---

## Architecture

```
┌───────────────────────────────────────────────┐
│                 pathsense-core                │   ← Headless, no UI deps
│  PathTracker · PathEvent · PathMetrics        │
│  PointBuffer · Resampler · $1 Recognizer      │
│  commonMain / androidMain / iosMain           │
└───────────────────┬───────────────────────────┘
                    │ depends on
┌───────────────────▼───────────────────────────┐
│                 pathsense-ui                  │   ← Opt-in rendering
│  PathSense.init() / PathSense.configure()     │
│  Android: PathOverlayView · PathCaptureView   │
│  iOS: PathTrackingWindow · TouchOverlayView   │
│  commonMain / androidMain / iosMain (Swift)   │
└───────────────────────────────────────────────┘
```

| Module            | Artifact                                  | Description                                                                                      |
| ----------------- | ----------------------------------------- | ------------------------------------------------------------------------------------------------ |
| `:pathsense-core` | `com.dayushmand.pathsense:pathsense-core` | Path tracking, smoothing, metrics, gesture recognition. No UI.                                   |
| `:pathsense-ui`   | `com.dayushmand.pathsense:pathsense-ui`   | Rendering overlays, zero-config auto-attach: `PathSense.init()` (Android) / `.configure()` (iOS) |

---

## Quick Start

### Android — Zero-Config (Recommended)

**1. Add dependencies**

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.dayushmand.pathsense:pathsense-ui:<version>")
    // pathsense-core is pulled in transitively
}
```

**2. Initialize in your Application class**

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PathSense.init(this)
    }
}
```

That's it — the SDK automatically intercepts touches, tracks paths, recognizes gestures, and renders a gradient overlay on every Activity. No layout changes needed.

**3. (Optional) Customize and listen for events**

```kotlin
PathSense.init(this, PathSenseConfig(
    pathConfig = PathConfig(samplingHz = 120, maxPoints = 500),
    overlayConfig = PathOverlayConfig(
        showCrosshair = true,
        showCoordinateHUD = true,
        style = PathStyle(strokeWidthPx = 6f),
    ),
    listener = { event ->
        when (event) {
            is PathEvent.GestureRecognized -> Log.d("PathSense", "Gesture: ${event.match}")
            is PathEvent.MetricsEnded -> Log.d("PathSense", "Metrics: ${event.metrics}")
            else -> {}
        }
    }
))
```

### iOS — Zero-Config (Recommended)

**1. Add the Swift Package**

Add the `PathSenseSDK` package (from `ios/PathSenseSDK`) and depend on the `PathSenseUI` product.

**2. Initialize — SwiftUI**

```swift
import PathSenseUI

@main
struct MyApp: App {
    init() {
        PathSense.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

**2. Initialize — UIKit**

```swift
import PathSenseUI

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        PathSense.configure()
        return true
    }
}
```

That's it — the SDK automatically intercepts touches on every `UIWindow`, tracks paths, recognizes gestures, and renders a gradient overlay. No view hierarchy changes needed.

**3. (Optional) Customize and listen for events**

```swift
var config = PathSenseConfig()
config.overlayConfig.showCrosshair = true
config.overlayConfig.showCoordinateHUD = true
config.overlayConfig.style.strokeWidth = 6.0
config.listener = { event in print("PathSense: \(event)") }
PathSense.configure(config)
```

---

## Samples

| Sample                    | Platform | Integration             | Key Code                                                   |
| ------------------------- | -------- | ----------------------- | ---------------------------------------------------------- |
| `samples/android-compose` | Android  | Zero-config (auto-attach) | `PathSense.init(this, config)` in `Application.onCreate()` |
| `samples/android-view`    | Android  | Zero-config (auto-attach) | `PathSense.init(this, config)` in `Application.onCreate()` |
| `samples/ios-swiftui`     | iOS      | Zero-config (auto-attach) | `PathSense.configure(config)` in `App.init()`              |
| `samples/ios-uikit`       | iOS      | Zero-config (auto-attach) | `PathSense.configure(config)` in `didFinishLaunchingWithOptions` |

All samples show "Draw anywhere on screen" — touch and drag to see the gradient trail, touch circle, and coordinate HUD.

---

## Core API

### Models

| Type                                   | Description                                                                                                     |
| -------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| `PathPoint(x, y, tMillis)`             | A timestamped touch coordinate                                                                                  |
| `PathConfig`                           | Sampling rate, distance threshold, smoothing window, resample spacing, max points                               |
| `PathMetrics`                          | Computed path length, bounding box, start/end points, direction, speed, deltas                                  |
| `PathEvent`                            | Sealed class: `Started`, `Updated`, `MetricsUpdated`, `Ended`, `MetricsEnded`, `GestureRecognized`, `Cancelled` |
| `GestureMatch(type, score, algorithm)` | Recognition result with confidence score (0.0–1.0)                                                              |
| `GestureType`                          | `LINE`, `CIRCLE`, `RECTANGLE`, `ZIGZAG`, `UNKNOWN`                                                              |

### PathTracker

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

### Custom Gesture Recognizer

```kotlin
val customRecognizer = GestureRecognizer { points ->
    // Analyze points, return GestureMatch or null
    GestureMatch(GestureType.LINE, score = 0.9f, algorithm = "custom")
}
tracker.addRecognizer(customRecognizer)
```

---

## UI Configuration

### PathOverlayConfig

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

### PathStyle

| Property             | Default             | Description                                                    |
| -------------------- | ------------------- | -------------------------------------------------------------- |
| `gradientStartColor` | `0xFFFF3B30` (red)  | Trail gradient start color                                     |
| `gradientEndColor`   | `0xFF007AFF` (blue) | Trail gradient end color                                       |
| `strokeWidthPx`      | `4f`                | Trail stroke width                                             |
| `strokeCap`          | `ROUND`             | Stroke cap style: `BUTT`, `ROUND`, `SQUARE`                    |
| `fadeOutMs`          | `300`               | Fade-out duration after finger lifts (0 = stays until cleared) |
| `showBoundingBox`    | `false`             | Draw bounding box around the path                              |
| `boundingBoxColor`   | `0x4400FF00`        | Bounding box color                                             |

### PathConfig

| Property            | Default | Description                             |
| ------------------- | ------- | --------------------------------------- |
| `samplingHz`        | `120`   | Max sampling rate (time-based throttle) |
| `minDistancePx`     | `2f`    | Min distance between accepted points    |
| `smoothingWindow`   | `3`     | 3-point moving average smoothing window |
| `resampleSpacingPx` | `6f`    | Resampling interval for recognition     |
| `maxPoints`         | `500`   | Ring buffer capacity (FIFO eviction)    |

---

## Threading Model

```
Main Thread (synchronous, inline)
├── Touch input → onDown/onMove/onUp
├── 3-point moving average smoothing (~sub-µs)
├── Ring buffer write
├── Emit Started / Updated / Ended / Cancelled
└── Renderer reads smoothed points directly

Background Thread (Dispatchers.Default)
├── Resampling (64 points)
├── Metrics computation
├── $1 Unistroke gesture recognition
└── Post MetricsUpdated / MetricsEnded / GestureRecognized → Main
```

All `PathEvent` callbacks are delivered on the **main thread**. The renderer draws from the main-thread point buffer with zero latency — background work never blocks rendering.

---

## Event Flow

```
Started → Updated* → Ended → MetricsEnded → GestureRecognized
                  ↑
          MetricsUpdated (async, ~1 frame delay)
```

| Event               | Thread | Timing                                            |
| ------------------- | ------ | ------------------------------------------------- |
| `Started`           | Main   | Synchronous on `onDown`                           |
| `Updated`           | Main   | Synchronous on `onMove` (points only, no metrics) |
| `MetricsUpdated`    | Main   | Async, ~1 frame after `Updated`                   |
| `Ended`             | Main   | Synchronous on `onUp`                             |
| `MetricsEnded`      | Main   | Async, after `Ended`                              |
| `GestureRecognized` | Main   | Async, after `MetricsEnded`                       |
| `Cancelled`         | Main   | Synchronous on `onCancel`                         |

---

## Recognition Algorithm

Built-in gesture recognition uses the **$1 Unistroke Recognizer**:

1. **Resample** candidate to 64 equidistant points
2. **Rotate** to indicative angle (angle from centroid to first point)
3. **Scale** to unit square
4. **Translate** centroid to origin
5. **Match** against templates via cosine distance
6. **Score** = `1 - (distance / (π/2))`, clamped to 0.0–1.0

Default recognition threshold: **0.75** — below this, the gesture is classified as `UNKNOWN`.

Built-in templates: **Line**, **Circle**, **Rectangle**, **Zigzag**.

---

## Project Structure

```
PathSenseSDK/
├── pathsense-core/                    # Headless KMM core
│   └── src/
│       ├── commonMain/                # Models, PathTracker, Resampler, $1 Recognizer
│       ├── androidMain/               # Android dispatcher + time utils
│       ├── iosMain/                   # iOS dispatcher + time utils
│       └── commonTest/                # Unit tests
├── pathsense-ui/                      # Opt-in rendering module
│   └── src/
│       ├── commonMain/                # PathOverlayConfig, PathStyle, HUDAlignment
│       ├── androidMain/               # PathOverlayView, PathCaptureView, Compose, PathSense
│       ├── iosMain/                   # (iOS rendering via Swift sources)
│       ├── commonTest/                # Common UI tests
│       └── androidTest/               # Android instrumentation tests
├── samples/
│   ├── android-compose/               # Compose sample app
│   ├── android-view/                  # View-based sample app
│   ├── ios-swiftui/                   # SwiftUI sample app
│   └── ios-uikit/                     # UIKit sample app
├── ios/PathSenseSDK/                  # Swift Package
│   ├── Package.swift
│   ├── Sources/
│   │   ├── PathSenseCore/             # XCFramework wrapper
│   │   └── PathSenseUI/               # Swift UIKit/SwiftUI adapters
│   └── Tests/
└── build.gradle.kts                   # Root build config
```

---

## Distribution

| Platform | Channel               | Artifacts                                                    |
| -------- | --------------------- | ------------------------------------------------------------ |
| Android  | MavenCentral          | `pathsense-core`, `pathsense-ui`                             |
| iOS      | Swift Package Manager | `PathSenseCore` (XCFramework), `PathSenseUI` (Swift sources) |

iOS consumers add only the targets they need — `PathSenseUI` depends on `PathSenseCore`.

---

## Building

### Prerequisites

- **JDK 17+**
- **Android SDK** (compileSdk 34, minSdk 21)
- **Kotlin 2.0.20**
- **Xcode 14.3+** (for iOS targets)

### Build Android

```bash
./gradlew :pathsense-core:assembleDebug :pathsense-ui:assembleDebug
```

### Build Sample Apps

**Android:**

```bash
./gradlew :samples:android-compose:assembleDebug :samples:android-view:assembleDebug
```

**iOS (requires [xcodegen](https://github.com/yonaskolb/XcodeGen)):**

```bash
# Generate Xcode projects
cd samples/ios-swiftui && xcodegen generate && cd ../..
cd samples/ios-uikit && xcodegen generate && cd ../..

# Build
xcodebuild -project samples/ios-swiftui/PathSenseSwiftUISample.xcodeproj \
  -scheme PathSenseSwiftUISample -destination 'generic/platform=iOS Simulator' build

xcodebuild -project samples/ios-uikit/PathSenseUIKitSample.xcodeproj \
  -scheme PathSenseUIKitSample -destination 'generic/platform=iOS Simulator' build
```

### Run Tests

```bash
# Common + JVM tests
./gradlew :pathsense-core:allTests :pathsense-ui:allTests
```

### Build iOS XCFramework

```bash
./gradlew :pathsense-core:assemblePathSenseCoreXCFramework
```

Then copy the output into the SPM package:

```bash
cp -R pathsense-core/build/XCFrameworks/debug/PathSenseCore.xcframework \
  ios/PathSenseSDK/PathSenseCore.xcframework
```

---

## Requirements

| Platform   | Minimum           |
| ---------- | ----------------- |
| Android    | API 21 (Lollipop) |
| iOS        | 13.0              |
| Kotlin     | 2.0.20            |
| JVM Target | 17                |

---

## License

TBD
