# PathSenseSDK

A **Kotlin Multiplatform** gesture SDK for **Android** (View + Compose) and **iOS** (UIKit + SwiftUI). One-line setup — touch-path capture, real-time smoothing, metrics, gesture recognition, and visual overlays out of the box.

---

## Features

- **One-line integration** — auto-attaches to all Activities / UIWindows
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

> Replace `<version>` with a release tag (e.g. `0.1.0`) or a commit hash. Use `main-SNAPSHOT` for the latest commit on `main`.

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

**2. (Optional) Customize**

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

### SwiftUI

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

### UIKit

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

Done. The SDK intercepts touches on every `UIWindow`, tracks paths, recognizes gestures, and renders a gradient overlay. No view hierarchy changes needed.

**Customize:**

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

| Sample                    | Platform | Key Code                                                         |
| ------------------------- | -------- | ---------------------------------------------------------------- |
| `samples/android-compose` | Android  | `PathSense.init(this, config)` in `Application.onCreate()`      |
| `samples/android-view`    | Android  | `PathSense.init(this, config)` in `Application.onCreate()`      |
| `samples/ios-swiftui`     | iOS      | `PathSense.configure(config)` in `App.init()`                   |
| `samples/ios-uikit`       | iOS      | `PathSense.configure(config)` in `didFinishLaunchingWithOptions` |

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
