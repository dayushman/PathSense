# PathSenseSDK

A **Kotlin Multiplatform** gesture SDK for **Android** (View + Compose) and **iOS** (UIKit + SwiftUI). Touch-path capture, real-time smoothing, metrics, gesture recognition, and visual overlays out of the box.

All configuration types (`PathOverlayConfig`, `PathStyle`, `HUDAlignment`, `StrokeCap`) are defined once in `pathsense-core` and shared across both platforms — Android uses them directly via Gradle, iOS consumes them through the `PathSenseCore.xcframework`.

---

## Features

- **Integration ready** — auto-attaches on Android, explicit tracking window on iOS
- **Gesture recognition** — Line, Circle, Rectangle, Zigzag ($1 Unistroke Recognizer)
- **Real-time metrics** — path length, bounding box, direction, speed, deltas
- **Visual overlays** — gradient trail, crosshair, touch circle, coordinate HUD
- **Edge-to-edge safe** — HUD respects system bar insets on Android 15+
- **Memory-bounded** — FIFO ring buffer (default 500 points)
- **Zero render latency** — smoothing inline on main thread; heavy math offloaded

---

## Installation

### Android (JitPack)

**1. Add the JitPack repository** to your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

**2. Add the dependency** to your module `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.dayushman.PathSense:pathsense-ui:<version>")
    // pathsense-core is included transitively — no need to add it separately
}
```

> Replace `<version>` with a release tag (e.g. `0.1.0`) or a commit hash. Use `master-SNAPSHOT` for the latest commit on `master`.

### iOS (Swift Package Manager)

**1.** In Xcode, go to **File → Add Package Dependencies…**

**2.** Enter the package URL:

```
https://github.com/dayushman/PathSense.git
```

**3.** Select the `PathSenseUI` library product — it includes `PathSenseCore` transitively.

---

## Quick Start — Android

**1. Initialize in your Application class**

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PathSense.init(this)
    }
}
```

Done. The SDK auto-attaches to every Activity, intercepts touches, tracks paths, recognizes gestures, and renders a gradient overlay. No layout changes needed.

**2. (Optional) Enable / Disable**

```kotlin
// Disable path capture
PathSense.disable()   // stops tracking + clears overlays

// Re-enable
PathSense.enable()    // resumes from next gesture
```

**3. (Optional) Customize**

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

---

## Quick Start — iOS

`PathSenseTrackingWindow` is a debug-only (`#if DEBUG`) `UIWindow` subclass.
Use it in your scene/window bootstrap and keep release builds on plain `UIWindow`.

### UIKit / SceneDelegate

```swift
var config = PathSenseConfig()
config.overlayConfig.debugOnly = false
config.overlayConfig.showCrosshair = true
config.overlayConfig.showCoordinateHUD = true
config.overlayConfig.style.strokeWidthPx = 6.0
config.listener = { event in print("PathSense: \(event)") }

#if DEBUG
let window = PathSenseTrackingWindow(windowScene: windowScene, config: config)
#else
let window = UIWindow(windowScene: windowScene)
#endif

window.rootViewController = ViewController()
window.makeKeyAndVisible()
```

> **Note:** On iOS, colour properties (e.g. `gradientStartColor`, `hudTextColor`) are `Int64` ARGB values from the shared Kotlin module. The `PathSenseUI` Swift package provides UIKit convenience extensions like `.gradientStartUIColor`, `.hudUITextColor` etc. for converting to `UIColor` when needed.

**Runtime controls (Debug window instance):**

```swift
window.isCaptureEnabled = false
window.clearCanvas()
let tracker = window.tracker
```

---

## Samples

| Sample                    | Platform | Key Code                                                         |
| ------------------------- | -------- | ---------------------------------------------------------------- |
| `samples/android-compose` | Android  | `PathSense.init(this, config)` in `Application.onCreate()`      |
| `samples/android-view`    | Android  | `PathSense.init(this, config)` in `Application.onCreate()`      |
| `samples/ios-swiftui`     | iOS      | `PathSenseTrackingWindow(...)` in `SceneDelegate`               |
| `samples/ios-uikit`       | iOS      | `PathSenseTrackingWindow(...)` in `SceneDelegate`               |

---

## Requirements

| Platform   | Minimum           |
| ---------- | ----------------- |
| Android    | API 21 (Lollipop) |
| iOS        | 13.0              |
| Kotlin     | 2.0.20            |
| JVM Target | 17                |

---

## Documentation

| Document                                | Description                                                                                        |
| --------------------------------------- | -------------------------------------------------------------------------------------------------- |
| [Architecture](docs/ARCHITECTURE.md)    | Module overview, threading model, event flow, recognition algorithm, project structure, build guide |
| [API Reference](docs/API_REFERENCE.md)  | Full API tables — models, PathTracker, configs, styles, entry points, advanced integration          |

---

## License

TBD

## Releases
[![](https://jitpack.io/v/dayushman/PathSense.svg)](https://jitpack.io/#dayushman/PathSense)
