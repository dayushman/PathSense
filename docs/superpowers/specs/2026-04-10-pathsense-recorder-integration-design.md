# PathSense + Screen Recorder Integration & JitPack Publishing

**Date:** 2026-04-10
**Branch:** feature/screen-recorder

## Goal

1. Publish all three modules (`pathsense-core`, `pathsense-ui`, `screen-recorder`) on JitPack
2. Integrate PathSense into Screen Recorder as a compile-time dependency
3. Auto-enable PathSense overlay (crosshair, touch circle, coordinate HUD) when recording starts; auto-disable when recording stops
4. Add a 4th "PathSense" toggle row to the popover menu (both Android and iOS), ON by default

## Architecture

### Dependency Graph (after change)

```
screen-recorder
  ├── api(pathsense-core)
  ├── api(pathsense-ui)
  ├── kotlinx-coroutines
  └── (existing deps)

pathsense-ui
  └── api(pathsense-core)

pathsense-core
  └── kotlinx-coroutines
```

`screen-recorder` uses `api()` so consumers transitively get PathSense without declaring it themselves. Consumers who only want PathSense can still pull `pathsense-core` / `pathsense-ui` independently.

### Module Changes

**screen-recorder/build.gradle.kts** — add dependencies:
```kotlin
sourceSets {
    commonMain {
        dependencies {
            api(project(":pathsense-core"))
            api(project(":pathsense-ui"))
            // existing deps...
        }
    }
}
```

**jitpack.yml** — add screen-recorder to install:
```yaml
install:
  - ./gradlew :pathsense-core:publishToMavenLocal :pathsense-ui:publishToMavenLocal :screen-recorder:publishToMavenLocal
```

**iOS SPM** (`ios/PathSenseSDK/Package.swift`) — add dependencies to ScreenRecorderUI target:
```swift
.target(
    name: "ScreenRecorderUI",
    dependencies: ["ScreenRecorderCore", "PathSenseCore", "PathSenseUI"],
    path: "Sources/ScreenRecorderUI"
),
```

## Config Changes

### ScreenRecorderConfig (commonMain)

Add one field:
```kotlin
data class ScreenRecorderConfig(
    // ...existing fields...
    var pathSenseEnabled: Boolean = true,   // NEW — default ON
)
```

This is the source of truth for the toggle state. The popover reads/writes it; the orchestrator checks it when starting/stopping recording.

### PathSense Overlay Config for Recording

When PathSense is activated for recording, the following config is used:
```kotlin
PathOverlayConfig(
    debugOnly = false,          // must work in release builds
    showCoordinateHUD = true,
    showCrosshair = true,
    showTouchCircle = true,
    // all other defaults (gradient trail, colors, etc.)
)
```

## Android Integration

### ScreenRecorder.android.kt

**On recording start** (inside `onProjectionGranted`, after `bubbleManager?.setRecording(true)`):
```kotlin
if (config?.pathSenseEnabled == true) {
    PathSense.enable()
}
```

**On recording stop** (inside `onStopTap` callback):
```kotlin
PathSense.disable()
```

**Prerequisite:** PathSense must already be initialized. `ScreenRecorder.init()` will call `PathSense.init()` internally with the recording overlay config:
```kotlin
fun init(application: Application, config: ScreenRecorderConfig) {
    // ...existing init...
    PathSense.init(application, PathSenseConfig(
        overlayConfig = PathOverlayConfig(
            debugOnly = false,
            showCoordinateHUD = true,
            showCrosshair = true,
            showTouchCircle = true,
        )
    ))
    PathSense.disable()  // start disabled, enable on record
}
```

### PopoverMenuView.kt (Android)

Add a 4th row after the audio row:

- **Icon:** Crosshair icon (magenta circle with cross lines, matching PathSense's crosshair color `#FF00FF`)
- **Label:** "PathSense"
- **Toggle:** `PillToggleView`, ON by default (reads from `config.pathSenseEnabled`)
- **Callback:** `onPathSenseToggle: (Boolean) -> Unit`

The `buildCard()` method adds:
```kotlin
addView(buildDivider())
addView(buildPathSenseRow())
```

Card height calculation updates from `rowHeight * 3 + (1 * 2)` to `rowHeight * 4 + (1 * 3)`.

### BubbleManager.kt (Android)

Add constructor parameter and callback:
```kotlin
internal class BubbleManager(
    // ...existing params...
    private val onPathSenseToggle: (Boolean) -> Unit,
    initialPathSenseEnabled: Boolean,
)
```

Wire to `PopoverMenuView` and propagate to `ScreenRecorder.android.kt`.

## iOS Integration

### Critical: Remove `#if DEBUG` guard

Both `PathSenseTrackingWindow` and `TouchOverlayView` in `ios/PathSenseSDK/Sources/PathSenseUI/` are currently wrapped in `#if DEBUG`. For PathSense to work during recording in release builds, this guard must be removed.

Replace with runtime `debugOnly` check (matching the Android behavior where `PathOverlayConfig.debugOnly` controls visibility at runtime, not at compile time).

### ScreenRecorder+iOS.swift

The `start(in:config:)` method currently creates only `BubbleWindow`. It must also create a `PathSenseTrackingWindow` (with `debugOnly = false`, `showCoordinateHUD/showCrosshair/showTouchCircle = true`) and store it in `BubbleWindowHolder`. The tracking window starts with `isCaptureEnabled = false`.

### BubbleViewController.swift

**On recording start** (inside `bubbleTapped` when `!isRecording`):
```swift
if config.pathSenseEnabled {
    BubbleWindowHolder.shared.trackingWindow?.isCaptureEnabled = true
}
```

**On recording stop** (inside `bubbleTapped` when `isRecording`):
```swift
BubbleWindowHolder.shared.trackingWindow?.isCaptureEnabled = false
```

### PopoverMenuView.swift (iOS)

Add 4th row after audio:

- **Icon:** SF Symbol `"scope"` (crosshair) in magenta tint
- **Label:** "PathSense"
- **Toggle:** `UISwitch`, ON by default
- **Callback:** `onPathSenseToggle: (Bool) -> Void`

Constructor gains: `pathSenseEnabled: Bool` and `onPathSenseToggle: @escaping (Bool) -> Void`.

Card height calculation updates from `rowHeight * 3 + 2` to `rowHeight * 4 + 3`.

## Toggle Behavior

| State | PathSense Toggle ON | PathSense Toggle OFF |
|-------|--------------------|--------------------|
| Idle (not recording) | Overlay hidden | Overlay hidden |
| Recording starts | Overlay auto-enables | Overlay stays hidden |
| During recording, toggle flipped | Enable/disable live | Enable/disable live |
| Recording stops | Overlay auto-disables | Overlay stays hidden |

The toggle state persists across recording sessions (stored in `ScreenRecorderConfig.pathSenseEnabled`). It does NOT persist across app launches (config is in-memory).

## JitPack Publishing

### Artifact coordinates (after publishing)

| Module | Coordinate |
|--------|-----------|
| pathsense-core | `com.github.dayushman.PathSense:pathsense-core:<version>` |
| pathsense-ui | `com.github.dayushman.PathSense:pathsense-ui:<version>` |
| screen-recorder | `com.github.dayushman.PathSense:screen-recorder:<version>` |

### Consumer usage

```kotlin
// Full SDK (recording + PathSense bundled)
implementation("com.github.dayushman.PathSense:screen-recorder:0.0.7-alpha")

// PathSense only (no recording)
implementation("com.github.dayushman.PathSense:pathsense-ui:0.0.7-alpha")
```

## Files to Modify

### Gradle / Publishing
- `screen-recorder/build.gradle.kts` — add pathsense dependencies
- `jitpack.yml` — add screen-recorder publish

### Common (KMM)
- `screen-recorder/src/commonMain/.../ScreenRecorderConfig.kt` — add `pathSenseEnabled`

### Android
- `screen-recorder/src/androidMain/.../ScreenRecorder.android.kt` — init PathSense, enable/disable on record start/stop
- `screen-recorder/src/androidMain/.../bubble/BubbleManager.kt` — add PathSense callback + initial state
- `screen-recorder/src/androidMain/.../bubble/PopoverMenuView.kt` — add 4th PathSense row

### iOS
- `ios/PathSenseSDK/Sources/PathSenseUI/PathSenseTrackingWindow.swift` — remove `#if DEBUG` wrapper
- `ios/PathSenseSDK/Sources/PathSenseUI/TouchOverlayView.swift` — remove `#if DEBUG` wrapper
- `ios/PathSenseSDK/Sources/ScreenRecorderUI/ScreenRecorder+iOS.swift` — create PathSenseTrackingWindow on start, store in holder
- `ios/PathSenseSDK/Sources/ScreenRecorderUI/BubbleViewController.swift` — enable/disable PathSense on record start/stop, wire popover toggle
- `screen-recorder/ios/Sources/ScreenRecorderUI/PopoverMenuView.swift` — add 4th PathSense row
- `ios/PathSenseSDK/Package.swift` — add PathSense deps to ScreenRecorderUI target

### Sample App
- `samples/android-view/.../SampleApp.kt` — remove standalone `PathSense.init()` (now handled by ScreenRecorder)

## Out of Scope

- Persisting toggle state across app launches
- PathSense config customization from ScreenRecorderConfig (colors, HUD position, etc.)
- Independent PathSense toggle outside of recording context
