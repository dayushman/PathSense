# Architecture

## Module Overview

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
| -------------------- | ------ | ------------------------------------------------- |
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

## Key Design Decisions

- **Headless-first**: `PathTracker` has zero UI dependencies — usable in services, tests, or background processing without any View.
- **Memory bounded**: Point buffer capped at `maxPoints` (default 500) via FIFO ring buffer; long gestures never cause unbounded memory growth.
- **Zero touch-to-pixel latency**: Smoothing runs inline on the main thread; the renderer reads smoothed points directly. All heavy computation (resampling, metrics, recognition) runs on `Dispatchers.Default` and never blocks rendering.
- **Non-intrusive overlays**: Overlay views are transparent to touch events (`isUserInteractionEnabled = false` / `clickable = false`); the app functions normally.
- **Rendering is opt-in**: Consumers add `:pathsense-ui` only if they want visual overlays. The core module works standalone.

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
