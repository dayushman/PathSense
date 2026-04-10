# PathSense + Screen Recorder Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate PathSense as a compile-time dependency of Screen Recorder, auto-enable overlay during recording with a popover toggle, and publish all modules on JitPack.

**Architecture:** Screen Recorder gains `api()` dependencies on `pathsense-core` and `pathsense-ui`. On Android, `ScreenRecorder.init()` calls `PathSense.init()` with recording-appropriate overlay config and starts disabled; on iOS, a `PathSenseTrackingWindow` is created alongside the bubble window. The popover menu gets a 4th "PathSense" toggle row on both platforms.

**Tech Stack:** Kotlin Multiplatform (KMM), Android SDK, iOS UIKit/Swift, Gradle, Swift Package Manager, JitPack.

**Spec:** `docs/superpowers/specs/2026-04-10-pathsense-recorder-integration-design.md`

---

## File Map

| Action | File | Responsibility |
|--------|------|---------------|
| Modify | `screen-recorder/build.gradle.kts` | Add `api(project(":pathsense-core"))` and `api(project(":pathsense-ui"))` |
| Modify | `jitpack.yml` | Add `:screen-recorder:publishToMavenLocal` |
| Modify | `screen-recorder/src/commonMain/kotlin/com/screenrecorder/api/ScreenRecorderConfig.kt` | Add `pathSenseEnabled` field |
| Modify | `screen-recorder/src/commonTest/kotlin/com/screenrecorder/ConfigTest.kt` | Test new field default |
| Modify | `screen-recorder/src/androidMain/kotlin/com/screenrecorder/api/ScreenRecorder.android.kt` | Init PathSense, enable/disable on record |
| Modify | `screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/BubbleManager.kt` | Add PathSense callback + state |
| Modify | `screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/PopoverMenuView.kt` | Add 4th PathSense toggle row |
| Modify | `ios/PathSenseSDK/Sources/PathSenseUI/TouchOverlayView.swift` | Remove `#if DEBUG` wrapper |
| Modify | `ios/PathSenseSDK/Sources/PathSenseUI/PathSenseTrackingWindow.swift` | Remove `#if DEBUG` wrapper |
| Modify | `screen-recorder/ios/Sources/ScreenRecorderUI/ScreenRecorder+iOS.swift` | Create PathSenseTrackingWindow, store in holder |
| Modify | `screen-recorder/ios/Sources/ScreenRecorderUI/BubbleViewController.swift` | Toggle PathSense on record start/stop, wire popover |
| Modify | `screen-recorder/ios/Sources/ScreenRecorderUI/PopoverMenuView.swift` | Add 4th PathSense toggle row |
| Modify | `ios/PathSenseSDK/Package.swift` | Add PathSense deps to ScreenRecorderUI target |
| Modify | `screen-recorder/ios/Package.swift` | Add PathSense deps to ScreenRecorderUI target |
| Modify | `samples/android-view/src/main/java/com/dayushmand/pathsense/sample/view/SampleApp.kt` | Remove standalone PathSense.init() |

---

### Task 1: Add PathSense dependencies to screen-recorder Gradle module

**Files:**
- Modify: `screen-recorder/build.gradle.kts`

- [ ] **Step 1: Add pathsense-core and pathsense-ui as api dependencies**

In `screen-recorder/build.gradle.kts`, inside the `sourceSets` block, change the `commonMain` dependencies:

```kotlin
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                api(project(":pathsense-core"))
                api(project(":pathsense-ui"))
            }
        }
```

This adds `api()` (not `implementation()`) so consumers transitively get PathSense.

- [ ] **Step 2: Verify Gradle sync succeeds**

Run: `./gradlew :screen-recorder:dependencies --configuration commonMainImplementationDependenciesMetadata`
Expected: Output includes `:pathsense-core` and `:pathsense-ui` in the dependency tree, no errors.

- [ ] **Step 3: Commit**

```bash
git add screen-recorder/build.gradle.kts
git commit -m "build: add pathsense-core and pathsense-ui as api deps to screen-recorder"
```

---

### Task 2: Update JitPack config to publish screen-recorder

**Files:**
- Modify: `jitpack.yml`

- [ ] **Step 1: Add screen-recorder to the install command**

Replace the `install` section in `jitpack.yml`:

```yaml
install:
  - ./gradlew :pathsense-core:publishToMavenLocal :pathsense-ui:publishToMavenLocal :screen-recorder:publishToMavenLocal
```

- [ ] **Step 2: Commit**

```bash
git add jitpack.yml
git commit -m "build: add screen-recorder to JitPack publish"
```

---

### Task 3: Add pathSenseEnabled to ScreenRecorderConfig + test

**Files:**
- Modify: `screen-recorder/src/commonMain/kotlin/com/screenrecorder/api/ScreenRecorderConfig.kt`
- Modify: `screen-recorder/src/commonTest/kotlin/com/screenrecorder/ConfigTest.kt`

- [ ] **Step 1: Write the failing test**

Add these two tests to `ConfigTest.kt`:

```kotlin
    @Test
    fun defaultConfig_hasPathSenseEnabled() {
        val config = ScreenRecorderConfig()
        assertTrue(config.pathSenseEnabled)
    }
```

Add the `assertTrue` import at the top:

```kotlin
import kotlin.test.assertTrue
```

Also update the `noArgConstructor_matchesParameterizedDefaults` test — add `pathSenseEnabled = true` to the parameterized constructor call and add the assertion:

```kotlin
        val parameterized = ScreenRecorderConfig(
            tintColor = 0xFFFF3B30,
            bubblePosition = BubblePosition.TRAILING_CENTER,
            audioEnabled = false,
            videoQuality = VideoQuality.DEVICE_NATIVE,
            maxDurationSec = 300,
            outputFormat = OutputFormat.MP4,
            listener = null,
            pathSenseEnabled = true,
        )
```

And add:
```kotlin
        assertEquals(parameterized.pathSenseEnabled, noArg.pathSenseEnabled)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :screen-recorder:jvmTest --tests "com.screenrecorder.ConfigTest" 2>&1 | tail -20`
Expected: Compilation failure — `pathSenseEnabled` doesn't exist yet.

- [ ] **Step 3: Add pathSenseEnabled to ScreenRecorderConfig**

In `ScreenRecorderConfig.kt`, add the new field to both the data class and the no-arg constructor:

```kotlin
data class ScreenRecorderConfig(
    var tintColor: Long = 0xFFFF3B30,
    var bubblePosition: BubblePosition = BubblePosition.TRAILING_CENTER,
    var audioEnabled: Boolean = false,
    var videoQuality: VideoQuality = VideoQuality.DEVICE_NATIVE,
    var maxDurationSec: Int = 300,
    var outputFormat: OutputFormat = OutputFormat.MP4,
    var listener: ((RecordingEvent) -> Unit)? = null,
    var pathSenseEnabled: Boolean = true,
) {
    constructor() : this(
        tintColor = 0xFFFF3B30,
        bubblePosition = BubblePosition.TRAILING_CENTER,
        audioEnabled = false,
        videoQuality = VideoQuality.DEVICE_NATIVE,
        maxDurationSec = 300,
        outputFormat = OutputFormat.MP4,
        listener = null,
        pathSenseEnabled = true,
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :screen-recorder:jvmTest --tests "com.screenrecorder.ConfigTest" 2>&1 | tail -20`
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add screen-recorder/src/commonMain/kotlin/com/screenrecorder/api/ScreenRecorderConfig.kt \
       screen-recorder/src/commonTest/kotlin/com/screenrecorder/ConfigTest.kt
git commit -m "feat: add pathSenseEnabled config field to ScreenRecorderConfig"
```

---

### Task 4: Android — Wire PathSense init + enable/disable on recording

**Files:**
- Modify: `screen-recorder/src/androidMain/kotlin/com/screenrecorder/api/ScreenRecorder.android.kt`
- Modify: `screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/BubbleManager.kt`

- [ ] **Step 1: Add PathSense import and init in ScreenRecorder.android.kt**

Add imports at the top of `ScreenRecorder.android.kt`:

```kotlin
import com.dayushmand.pathsense.core.PathOverlayConfig
import com.dayushmand.pathsense.ui.PathSense
import com.dayushmand.pathsense.ui.PathSenseConfig
```

At the end of the `init()` function (after the activity lifecycle callbacks block, before the closing comment about foreground service), add:

```kotlin
            // Initialize PathSense with recording overlay config (disabled until recording starts)
            PathSense.init(application, PathSenseConfig(
                overlayConfig = PathOverlayConfig(
                    debugOnly = false,
                    showCoordinateHUD = true,
                    showCrosshair = true,
                    showTouchCircle = true,
                )
            ))
            PathSense.disable()
```

- [ ] **Step 2: Enable PathSense on recording start**

In `onProjectionGranted()`, after `bubbleManager?.setRecording(true)` (the last line of the `scope?.launch` block), add:

```kotlin
                if (config?.pathSenseEnabled == true) {
                    PathSense.enable()
                }
```

- [ ] **Step 3: Disable PathSense on recording stop**

In the `show()` function, inside the `onStopTap` callback of `BubbleManager`, after `application?.let { ScreenRecorderService.stop(it) }`, add:

```kotlin
                        PathSense.disable()
```

- [ ] **Step 4: Add PathSense toggle callback + initial state to BubbleManager**

In `BubbleManager.kt`, add two new constructor parameters after `onAudioToggle`:

```kotlin
internal class BubbleManager(
    private val context: Context,
    private val tintColor: Long,
    initialAudioEnabled: Boolean,
    private val onStartRecording: () -> Unit,
    private val onStopTap: () -> Unit,
    private val onGetMoreInfo: () -> Unit,
    private val onAudioToggle: (Boolean) -> Unit,
    initialPathSenseEnabled: Boolean,
    private val onPathSenseToggle: (Boolean) -> Unit,
) {
```

Add state tracking field after `currentAudioEnabled`:

```kotlin
    private var currentPathSenseEnabled = initialPathSenseEnabled
```

- [ ] **Step 5: Pass PathSense state to PopoverMenuView in BubbleManager.showPopover()**

In `showPopover()`, update the `PopoverMenuView` constructor call to include the new parameters. Add after `onAudioToggle = { enabled -> ... }`:

```kotlin
            pathSenseEnabled = currentPathSenseEnabled,
            onPathSenseToggle = { enabled ->
                currentPathSenseEnabled = enabled
                onPathSenseToggle(enabled)
            },
```

- [ ] **Step 6: Wire BubbleManager creation in ScreenRecorder.android.kt**

In `show()`, update the `BubbleManager` constructor call to pass the new parameters. Add after the `onAudioToggle` callback:

```kotlin
                    initialPathSenseEnabled = config?.pathSenseEnabled ?: true,
                    onPathSenseToggle = { enabled ->
                        config?.pathSenseEnabled = enabled
                        if (state == RecordingState.RECORDING) {
                            if (enabled) PathSense.enable() else PathSense.disable()
                        }
                    },
```

- [ ] **Step 7: Verify Android compiles**

Run: `./gradlew :screen-recorder:compileReleaseKotlinAndroid 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (will fail until Task 5 adds PopoverMenuView params — that's OK, we commit this task's changes and continue).

- [ ] **Step 8: Commit**

```bash
git add screen-recorder/src/androidMain/kotlin/com/screenrecorder/api/ScreenRecorder.android.kt \
       screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/BubbleManager.kt
git commit -m "feat(android): wire PathSense init, enable/disable on recording, and toggle callback"
```

---

### Task 5: Android — Add PathSense toggle row to PopoverMenuView

**Files:**
- Modify: `screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/PopoverMenuView.kt`

- [ ] **Step 1: Add constructor parameters**

Add two new parameters to `PopoverMenuView` after `onAudioToggle`:

```kotlin
    pathSenseEnabled: Boolean,
    private val onPathSenseToggle: (Boolean) -> Unit,
```

Add state field after `isAudioEnabled`:

```kotlin
    private var isPathSenseEnabled = pathSenseEnabled
```

- [ ] **Step 2: Update card height calculation**

In `buildCard()`, the card height is used for positioning. Update the calculation in the `init` block. Change:

```kotlin
        val cardHeight = rowHeight * 3 + (1 * 2) // 3 rows + 2 dividers (1px each)
```

to:

```kotlin
        val cardHeight = rowHeight * 4 + (1 * 3) // 4 rows + 3 dividers (1px each)
```

- [ ] **Step 3: Add PathSense row to buildCard()**

In `buildCard()`, after the `addView(buildAudioRow())` line, add:

```kotlin
            addView(buildDivider())
            addView(buildPathSenseRow())
```

- [ ] **Step 4: Implement buildPathSenseRow()**

Add this method after `buildAudioRow()`:

```kotlin
    private fun buildPathSenseRow(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), 0, (16 * dp).toInt(), 0)
            minimumHeight = rowHeight

            val iconSize = (20 * dp).toInt()
            addView(CrosshairIconView(context), LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = (12 * dp).toInt()
            })

            addView(TextView(context).apply {
                text = "PathSense"
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val toggle = PillToggleView(context, isPathSenseEnabled) { enabled ->
                isPathSenseEnabled = enabled
                onPathSenseToggle(enabled)
            }
            addView(toggle, LinearLayout.LayoutParams(
                (42 * dp).toInt(), (24 * dp).toInt(),
            ))

            setOnClickListener {
                toggle.toggle()
            }
        }
    }
```

- [ ] **Step 5: Add CrosshairIconView**

Add this inner class after `MicIconView`:

```kotlin
    private class CrosshairIconView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF00FF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val r = width * 0.35f
            // Circle
            canvas.drawCircle(cx, cy, r, paint)
            // Crosshair lines
            canvas.drawLine(cx, cy - r - 3f, cx, cy + r + 3f, paint)
            canvas.drawLine(cx - r - 3f, cy, cx + r + 3f, cy, paint)
        }
    }
```

- [ ] **Step 6: Verify Android compiles**

Run: `./gradlew :screen-recorder:compileReleaseKotlinAndroid 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/PopoverMenuView.kt
git commit -m "feat(android): add PathSense toggle row to popover menu"
```

---

### Task 6: iOS — Remove #if DEBUG guards from PathSense overlay

**Files:**
- Modify: `ios/PathSenseSDK/Sources/PathSenseUI/TouchOverlayView.swift`
- Modify: `ios/PathSenseSDK/Sources/PathSenseUI/PathSenseTrackingWindow.swift`

- [ ] **Step 1: Remove #if DEBUG from TouchOverlayView.swift**

Remove the first line `#if DEBUG` and the last line `#endif` from `ios/PathSenseSDK/Sources/PathSenseUI/TouchOverlayView.swift`.

The file should start with:
```swift
import PathSenseCore
import UIKit
```

And end with the closing brace of the class (no `#endif`).

The runtime `debugOnly` check already exists inside `draw(_:)` at line 112-116:
```swift
if overlayConfig.debugOnly {
    #if !DEBUG
        return
    #endif
}
```
This runtime check stays — it correctly guards release-build rendering unless `debugOnly = false` is set.

- [ ] **Step 2: Remove #if DEBUG from PathSenseTrackingWindow.swift**

Remove the first line `#if DEBUG` and the last line `#endif` from `ios/PathSenseSDK/Sources/PathSenseUI/PathSenseTrackingWindow.swift`.

The file should start with:
```swift
import PathSenseCore
import UIKit
```

And end with the closing brace of the class (no `#endif`).

- [ ] **Step 3: Commit**

```bash
git add ios/PathSenseSDK/Sources/PathSenseUI/TouchOverlayView.swift \
       ios/PathSenseSDK/Sources/PathSenseUI/PathSenseTrackingWindow.swift
git commit -m "feat(ios): remove #if DEBUG guards from PathSense overlay for release recording support"
```

---

### Task 7: iOS — Create PathSenseTrackingWindow on recorder start

**Files:**
- Modify: `screen-recorder/ios/Sources/ScreenRecorderUI/ScreenRecorder+iOS.swift`
- Modify: `screen-recorder/ios/Package.swift`
- Modify: `ios/PathSenseSDK/Package.swift`

- [ ] **Step 1: Update unified Package.swift (ios/PathSenseSDK/Package.swift)**

Update the `ScreenRecorderUI` target to depend on `PathSenseCore` and `PathSenseUI`:

```swift
        .target(
            name: "ScreenRecorderUI",
            dependencies: ["ScreenRecorderCore", "PathSenseCore", "PathSenseUI"],
            path: "Sources/ScreenRecorderUI"
        ),
```

- [ ] **Step 1b: Update standalone Package.swift (screen-recorder/ios/Package.swift)**

Add PathSenseCore and PathSenseUI targets (with relative paths to the unified package), and update ScreenRecorderUI deps:

```swift
let package = Package(
    name: "ScreenRecorderSDK",
    platforms: [
        .iOS(.v14),
    ],
    products: [
        .library(
            name: "ScreenRecorderSDK",
            targets: ["ScreenRecorderUI", "ScreenRecorderCore"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "ScreenRecorderCore",
            path: "ScreenRecorderCore.xcframework"
        ),
        .binaryTarget(
            name: "PathSenseCore",
            path: "../../ios/PathSenseSDK/PathSenseCore.xcframework"
        ),
        .target(
            name: "PathSenseUI",
            dependencies: ["PathSenseCore"],
            path: "../../ios/PathSenseSDK/Sources/PathSenseUI"
        ),
        .target(
            name: "ScreenRecorderUI",
            dependencies: ["ScreenRecorderCore", "PathSenseCore", "PathSenseUI"],
            path: "Sources/ScreenRecorderUI"
        ),
    ]
)
```

- [ ] **Step 2: Add trackingWindow to BubbleWindowHolder**

In `screen-recorder/ios/Sources/ScreenRecorderUI/ScreenRecorder+iOS.swift`, add the import and tracking window property:

```swift
import UIKit
import ScreenRecorderCore
import PathSenseCore
import PathSenseUI
```

Update `BubbleWindowHolder` to store the tracking window:

```swift
internal class BubbleWindowHolder {
    static let shared = BubbleWindowHolder()
    var window: BubbleWindow?
    var trackingWindow: PathSenseTrackingWindow?
}
```

- [ ] **Step 3: Create PathSenseTrackingWindow in start()**

In the `start(in:config:)` method, after creating the bubble window and before the closing brace, add:

```swift
        // Create PathSense tracking window (starts disabled, enabled on recording)
        let pathSenseConfig = PathSenseConfig(
            overlayConfig: {
                let overlay = PathOverlayConfig()
                overlay.debugOnly = false
                overlay.showCoordinateHUD = true
                overlay.showCrosshair = true
                overlay.showTouchCircle = true
                return overlay
            }()
        )
        let trackingWindow = PathSenseTrackingWindow(windowScene: windowScene, config: pathSenseConfig)
        trackingWindow.isCaptureEnabled = false
        trackingWindow.makeKeyAndVisible()
        BubbleWindowHolder.shared.trackingWindow = trackingWindow
```

- [ ] **Step 4: Commit**

```bash
git add screen-recorder/ios/Sources/ScreenRecorderUI/ScreenRecorder+iOS.swift \
       ios/PathSenseSDK/Package.swift \
       screen-recorder/ios/Package.swift
git commit -m "feat(ios): create PathSenseTrackingWindow on recorder start"
```

---

### Task 8: iOS — Wire PathSense toggle in BubbleViewController + popover

**Files:**
- Modify: `screen-recorder/ios/Sources/ScreenRecorderUI/BubbleViewController.swift`
- Modify: `screen-recorder/ios/Sources/ScreenRecorderUI/PopoverMenuView.swift`

- [ ] **Step 1: Add PathSense toggle to PopoverMenuView constructor**

In `PopoverMenuView.swift`, add two new parameters after `onAudioToggle`:

```swift
    private var isPathSenseEnabled: Bool
    private let onPathSenseToggle: (Bool) -> Void
```

Update the `init` signature to include:

```swift
    init(
        bubbleCenter: CGPoint,
        isBubbleOnRight: Bool,
        audioEnabled: Bool,
        pathSenseEnabled: Bool,
        onStartRecording: @escaping () -> Void,
        onGetMoreInfo: @escaping () -> Void,
        onAudioToggle: @escaping (Bool) -> Void,
        onPathSenseToggle: @escaping (Bool) -> Void,
        onDismiss: @escaping () -> Void
    ) {
```

Add to the init body before `super.init`:

```swift
        self.isPathSenseEnabled = pathSenseEnabled
        self.onPathSenseToggle = onPathSenseToggle
```

- [ ] **Step 2: Update card height**

In `layoutCardAndNib`, change:

```swift
        let cardHeight = rowHeight * 3 + 2 // 3 rows + 2 dividers
```

to:

```swift
        let cardHeight = rowHeight * 4 + 3 // 4 rows + 3 dividers
```

- [ ] **Step 3: Add PathSense row to card stack**

In `setupCard()`, after `stack.addArrangedSubview(buildAudioRow())`, add:

```swift
        stack.addArrangedSubview(buildDivider())
        stack.addArrangedSubview(buildPathSenseRow())
```

- [ ] **Step 4: Implement buildPathSenseRow()**

Add this method after `buildAudioRow()`:

```swift
    private let pathSenseSwitch = UISwitch()

    private func buildPathSenseRow() -> UIView {
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false
        container.heightAnchor.constraint(equalToConstant: rowHeight).isActive = true

        let hStack = UIStackView()
        hStack.axis = .horizontal
        hStack.alignment = .center
        hStack.spacing = 12
        hStack.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(hStack)
        NSLayoutConstraint.activate([
            hStack.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 16),
            hStack.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -16),
            hStack.centerYAnchor.constraint(equalTo: container.centerYAnchor),
        ])

        let icon = buildCrosshairIcon()
        icon.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            icon.widthAnchor.constraint(equalToConstant: 20),
            icon.heightAnchor.constraint(equalToConstant: 20),
        ])
        hStack.addArrangedSubview(icon)

        let label = UILabel()
        label.text = "PathSense"
        label.textColor = .white
        label.font = .systemFont(ofSize: 15, weight: .semibold)
        hStack.addArrangedSubview(label)

        pathSenseSwitch.isOn = isPathSenseEnabled
        pathSenseSwitch.onTintColor = UIColor(red: 0.2, green: 0.78, blue: 0.35, alpha: 1)
        pathSenseSwitch.transform = CGAffineTransform(scaleX: 0.75, y: 0.75)
        pathSenseSwitch.addTarget(self, action: #selector(pathSenseToggled), for: .valueChanged)
        pathSenseSwitch.setContentHuggingPriority(.required, for: .horizontal)
        hStack.addArrangedSubview(pathSenseSwitch)

        let rowTap = UITapGestureRecognizer(target: self, action: #selector(pathSenseRowTapped))
        container.addGestureRecognizer(rowTap)

        return container
    }
```

- [ ] **Step 5: Add crosshair icon builder**

Add after `buildMicIcon()`:

```swift
    private func buildCrosshairIcon() -> UIView {
        let config = UIImage.SymbolConfiguration(pointSize: 14, weight: .bold)
        let imageView = UIImageView(image: UIImage(systemName: "scope", withConfiguration: config))
        imageView.tintColor = UIColor(red: 1, green: 0, blue: 1, alpha: 1) // magenta
        return imageView
    }
```

- [ ] **Step 6: Add PathSense toggle action methods**

Add after `audioRowTapped()`:

```swift
    @objc private func pathSenseToggled() {
        isPathSenseEnabled = pathSenseSwitch.isOn
        onPathSenseToggle(isPathSenseEnabled)
    }

    @objc private func pathSenseRowTapped() {
        pathSenseSwitch.setOn(!pathSenseSwitch.isOn, animated: true)
        pathSenseToggled()
    }
```

- [ ] **Step 7: Wire PathSense in BubbleViewController**

In `BubbleViewController.swift`, add import:

```swift
import PathSenseUI
```

Update `showPopover()` to pass PathSense params to PopoverMenuView:

```swift
        let popover = PopoverMenuView(
            bubbleCenter: bubbleButton.center,
            isBubbleOnRight: isBubbleOnRight,
            audioEnabled: config.audioEnabled,
            pathSenseEnabled: config.pathSenseEnabled,
            onStartRecording: { [weak self] in
                self?.dismissPopoverImmediate()
                ScreenRecorder.companion.onBubbleTapRecord()
                self?.setRecording(true)
            },
            onGetMoreInfo: { [weak self] in
                self?.dismissPopover()
            },
            onAudioToggle: { [weak self] enabled in
                self?.config.audioEnabled = enabled
            },
            onPathSenseToggle: { [weak self] enabled in
                self?.config.pathSenseEnabled = enabled
                if self?.isRecording == true {
                    BubbleWindowHolder.shared.trackingWindow?.isCaptureEnabled = enabled
                }
            },
            onDismiss: { [weak self] in
                self?.dismissPopover()
            }
        )
```

- [ ] **Step 8: Enable PathSense on recording start**

In `BubbleViewController.swift`, in `showPopover()`'s `onStartRecording` callback, after `self?.setRecording(true)`, add:

```swift
                if self?.config.pathSenseEnabled == true {
                    BubbleWindowHolder.shared.trackingWindow?.isCaptureEnabled = true
                }
```

- [ ] **Step 9: Disable PathSense on recording stop**

In `bubbleTapped()`, in the `if isRecording` branch, after `setRecording(false)`, add:

```swift
            BubbleWindowHolder.shared.trackingWindow?.isCaptureEnabled = false
```

So the full `bubbleTapped()` becomes:

```swift
    @objc private func bubbleTapped() {
        if isRecording {
            ScreenRecorder.companion.onBubbleTapStop()
            setRecording(false)
            BubbleWindowHolder.shared.trackingWindow?.isCaptureEnabled = false
        } else {
            if popoverView != nil {
                dismissPopover()
            } else {
                showPopover()
            }
        }
    }
```

- [ ] **Step 10: Commit**

```bash
git add screen-recorder/ios/Sources/ScreenRecorderUI/PopoverMenuView.swift \
       screen-recorder/ios/Sources/ScreenRecorderUI/BubbleViewController.swift
git commit -m "feat(ios): add PathSense toggle to popover and wire enable/disable on recording"
```

---

### Task 9: Update sample app — remove standalone PathSense.init()

**Files:**
- Modify: `samples/android-view/src/main/java/com/dayushmand/pathsense/sample/view/SampleApp.kt`

- [ ] **Step 1: Remove PathSense.init() from SampleApp**

In `SampleApp.kt`, remove the standalone `PathSense.init(this)` call (line 13) and the import `import com.dayushmand.pathsense.ui.PathSense` (line 5), since ScreenRecorder.init() now handles PathSense initialization.

The file becomes:

```kotlin
package com.dayushmand.pathsense.sample.view

import android.app.Application
import android.util.Log
import com.screenrecorder.api.RecordingEvent
import com.screenrecorder.api.ScreenRecorder
import com.screenrecorder.api.ScreenRecorderConfig

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()

        ScreenRecorder.init(this, ScreenRecorderConfig().apply {
            audioEnabled = false
            listener = { event ->
                when (event) {
                    is RecordingEvent.RecordingStarted -> Log.d("ScreenRecorder", "Recording started: ${event.sessionId}")
                    is RecordingEvent.DurationUpdate -> Log.d("ScreenRecorder", "Duration: ${event.elapsedMs}ms")
                    is RecordingEvent.RecordingStopped -> Log.d("ScreenRecorder", "Saved to: ${event.file.path}")
                    is RecordingEvent.RecordingFailed -> Log.e("ScreenRecorder", "Error: ${event.error.message}")
                    is RecordingEvent.PermissionRequired -> Log.d("ScreenRecorder", "Permission needed: ${event.type}")
                    is RecordingEvent.PermissionGranted -> Log.d("ScreenRecorder", "Permission granted: ${event.type}")
                    is RecordingEvent.PermissionDenied -> Log.e("ScreenRecorder", "Permission denied: ${event.type}")
                    is RecordingEvent.BubbleShown -> Log.d("ScreenRecorder", "Bubble shown")
                    is RecordingEvent.BubbleHidden -> Log.d("ScreenRecorder", "Bubble hidden")
                }
            }
        })
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add samples/android-view/src/main/java/com/dayushmand/pathsense/sample/view/SampleApp.kt
git commit -m "refactor: remove standalone PathSense.init from sample app (now handled by ScreenRecorder)"
```

---

### Task 10: Build verification

- [ ] **Step 1: Run full Android build**

Run: `./gradlew :screen-recorder:assembleRelease 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run common tests**

Run: `./gradlew :screen-recorder:jvmTest 2>&1 | tail -15`
Expected: All tests pass

- [ ] **Step 3: Run sample app build**

Run: `./gradlew :samples:android-view:assembleDebug 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Verify publishToMavenLocal works for all 3 modules**

Run: `./gradlew :pathsense-core:publishToMavenLocal :pathsense-ui:publishToMavenLocal :screen-recorder:publishToMavenLocal 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL — confirms JitPack install command will work.

- [ ] **Step 5: Commit any remaining fixes (if needed)**
