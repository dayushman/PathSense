# Floating Bubble + Screen Recording SDK — Feasibility Study

## Context

Evaluate the feasibility of building a general-purpose screen recording SDK with a floating bubble UI control, targeting both Android and iOS via KMM. The bubble should allow users to start/stop screen recording and the output should be saved as MP4/MOV to the device photo library.

This study is scoped to **technical feasibility and API requirements** — no implementation yet.

---

## 1. Feature Summary

| Feature | Android | iOS |
|---|---|---|
| **Floating bubble** | System-wide overlay (over all apps) | In-app only (within host app's window) |
| **Popup controls** | Overlay popup with play/stop | In-app popup with play/stop |
| **Screen recording** | Full screen via MediaProjection | App content only via RPScreenRecorder |
| **Save to gallery** | MediaStore API | PHPhotoLibrary |
| **Programmatic start** | Yes (from bubble tap) | Yes (from bubble tap, app-only) |

**Key asymmetry:** Android can do system-wide overlay + system-wide recording. iOS is limited to in-app overlay + in-app recording. This is a hard platform limitation, not an engineering gap.

---

## 2. Android — APIs & Architecture

### 2.1 Floating Bubble

**Permission:** `SYSTEM_ALERT_WINDOW`
- Declared in `AndroidManifest.xml`
- Granted via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` (API 23+) — not a standard runtime permission
- Check with `Settings.canDrawOverlays(context)`

**Implementation:**
- `WindowManager.addView(bubbleView, params)` with `TYPE_APPLICATION_OVERLAY` (API 26+)
- `LayoutParams` flags: `FLAG_NOT_FOCUSABLE` (lets touches pass through to apps beneath)
- Draggable via `OnTouchListener` updating `layoutParams.x/y` + `windowManager.updateViewLayout()`
- Popup is another overlay view added/removed on bubble tap

**Hosting:** Runs from a Foreground Service — keeps the bubble alive when the host app is backgrounded.

### 2.2 Screen Recording

**API:** `MediaProjection` (API 21+)

**Flow:**
1. `MediaProjectionManager.createScreenCaptureIntent()` → OS consent dialog
2. User grants consent → `MediaProjection` object obtained
3. `MediaProjection.createVirtualDisplay()` → mirrors screen to a `Surface`
4. `MediaRecorder` encodes the Surface content to MP4 (H.264, configurable bitrate/resolution)
5. On stop: `MediaRecorder.stop()` → file written → save to gallery

**Foreground Service (mandatory):**
- Type: `mediaProjection` (declared in manifest)
- Shows persistent notification during recording
- Android 14+: also requires `FOREGROUND_SERVICE_MEDIA_PROJECTION` permission
- Android 14+: MediaProjection consent must be re-obtained each app session (cannot persist)

**Saving to gallery:**
- API 29+: `MediaStore.Video.Media` via `ContentResolver.insert()` — no storage permission needed
- API 21-28: `WRITE_EXTERNAL_STORAGE` permission, save to `Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES)`

### 2.3 Required Permissions

| Permission | Purpose | API Level |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Floating bubble overlay | 23+ (special grant) |
| `FOREGROUND_SERVICE` | Keep service alive | 28+ |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | MediaProjection from service | 34+ |
| `RECORD_AUDIO` (optional) | Microphone capture | All (runtime) |
| `WRITE_EXTERNAL_STORAGE` (optional) | Gallery save on old APIs | 21-28 (runtime) |

### 2.4 Audio Capture

- **Microphone:** Available on all API levels via `MediaRecorder.setAudioSource(MIC)`. Requires `RECORD_AUDIO` permission.
- **Internal/system audio:** Available on API 29+ via `AudioPlaybackCapture`. Apps with `allowAudioPlaybackCapture="false"` are excluded.

---

## 3. iOS — APIs & Architecture

### 3.1 Floating Bubble (In-App Only)

**Approach:** Dedicated `UIWindow` with elevated `windowLevel`

```swift
let bubbleWindow = UIWindow(windowScene: scene)
bubbleWindow.windowLevel = .alert + 1
bubbleWindow.rootViewController = BubbleViewController()
bubbleWindow.isHidden = false
```

**Behavior:**
- Visible on top of all app content (navigation stacks, modals, sheets)
- Draggable via `UIPanGestureRecognizer`
- Disappears when app is backgrounded (iOS limitation — no system-wide overlay for third-party apps)
- Popup is shown/hidden within the same window or a sibling window

**Existing pattern in this repo:** `PathSenseTrackingWindow` already uses this exact approach.

### 3.2 Screen Recording (In-App Only)

**API:** `RPScreenRecorder` (ReplayKit, iOS 9+)

**Flow:**
1. Check `RPScreenRecorder.shared().isAvailable`
2. Call `startRecording(handler:)` — no OS consent dialog for in-app recording
3. Red status bar indicator appears automatically
4. On stop: `stopRecording { previewController in ... }`
5. `RPPreviewViewController` allows user to save/share

**Programmatic save (without preview UI):**
1. Use `startCapture(handler:completionHandler:)` (iOS 11+) to get raw `CMSampleBuffer` frames
2. Write frames to file via `AVAssetWriter` with H.264 encoding
3. Save file to Photos via `PHPhotoLibrary.shared().performChanges { ... }`

**Required Info.plist keys:**
- `NSPhotoLibraryAddUsageDescription` — for saving to Photos
- `NSMicrophoneUsageDescription` — if capturing microphone audio

### 3.3 What iOS Cannot Do

- No system-wide floating bubble (no `SYSTEM_ALERT_WINDOW` equivalent)
- No system-wide recording from within the app (Broadcast Extension exists but requires Control Center initiation)
- Bubble is not visible when app is backgrounded
- Recording captures only the host app's screen content, not other apps

### 3.4 Audio Capture

- **App audio:** Captured by default with `RPScreenRecorder`
- **Microphone:** Available via `RPScreenRecorder`, requires `NSMicrophoneUsageDescription` + user permission

---

## 4. KMM Architecture — What's Shared vs Platform-Specific

### 4.1 Shared (commonMain) — ~10-15% of codebase

```
RecordingState      — sealed class: Idle, Preparing, Recording, Stopping, Saving, Saved, Error
RecordingConfig     — data class: quality, framerate, bitrate, audioEnabled, outputFormat
RecordingEvent      — sealed class: Started, Stopped, Saved(uri), Error(msg), DurationUpdate(ms)
RecordingController — expect class: start(), stop(), state flow, event flow
Timer/Duration      — coroutine-based elapsed time emitter
PermissionState     — enum: NotDetermined, Granted, Denied, Restricted
```

### 4.2 Platform-Specific — ~85-90% of codebase

**Android (Kotlin):**
- `ScreenRecorderService` — Foreground Service hosting MediaProjection + bubble overlay
- `BubbleView` / `BubblePopupView` — overlay views managed via WindowManager
- `MediaProjectionRecorder` — actual RecordingController: VirtualDisplay + MediaRecorder
- `OverlayPermissionHelper` — SYSTEM_ALERT_WINDOW grant flow
- `MediaProjectionPermissionHelper` — consent dialog flow
- `GallerySaver` — MediaStore integration
- `NotificationHelper` — recording notification channel + builder

**iOS (Swift — following existing repo pattern):**
- `BubbleWindow` — UIWindow subclass with BubbleViewController
- `BubbleViewController` — draggable circular view + popup toggle
- `RecordingPopupView` — play/stop/timer UI
- `ReplayKitRecorder` — actual RecordingController: RPScreenRecorder + AVAssetWriter
- `PhotoLibrarySaver` — PHPhotoLibrary integration

### 4.3 Expect/Actual Boundary

Following the existing `PathTracker` pattern where the core state machine is in Kotlin common code:

```kotlin
// commonMain
expect class RecordingController {
    val state: StateFlow<RecordingState>
    val events: SharedFlow<RecordingEvent>
    fun requestPermissions()
    fun startRecording(config: RecordingConfig)
    fun stopRecording()
}

// androidMain — wraps MediaProjection + MediaRecorder
// iosMain — wraps RPScreenRecorder (or thin bridge to Swift impl)
```

---

## 5. Constraints & Gotchas

### 5.1 Critical

| Constraint | Impact |
|---|---|
| iOS has no system-wide overlay | Bubble is in-app only; disappears on background |
| iOS RPScreenRecorder captures app only | Cannot record other apps or system UI |
| Android 14+ re-consent | MediaProjection token cannot persist across app restarts |
| `FLAG_SECURE` windows | Banking/DRM apps appear as black rectangles in recording |

### 5.2 Important

| Constraint | Impact |
|---|---|
| Android overlay permission is special-grant | User must navigate to Settings; cannot be a standard dialog |
| iOS RPScreenRecorder availability | Returns false on simulators, some managed devices, during AirPlay |
| iOS Broadcast Extension 50MB memory limit | If Broadcast path is ever added, buffer management is critical |
| Screen rotation during recording | VirtualDisplay must handle rotation or recording will be cropped |
| Internal audio only on Android 10+ | API 21-28 can only capture microphone, not system audio |

### 5.3 Store Review

- **Play Store:** Screen recording apps allowed; must show persistent notification; must respect FLAG_SECURE
- **App Store:** RPScreenRecorder in-app recording is approved; must declare usage in description; privacy policy must cover recording

---

## 6. Feasibility Verdict

### Can it be built?

| | Android | iOS |
|---|---|---|
| Floating bubble | Yes (system-wide) | Yes (in-app only) |
| Screen recording | Yes (full screen) | Yes (app content only) |
| Save to gallery | Yes | Yes |
| Overall | Fully feasible as described | Feasible with reduced scope |

### Is KMM the right tool?

**Marginal benefit.** Only 10-15% of the code is shareable (state machine, config, events). The recording engine, UI, permissions, and file saving are 100% platform-specific with zero API overlap.

**However:** If this becomes a module in the PathSense ecosystem, KMM provides:
- Consistent API surface across platforms (same state/event/config types)
- Single source of truth for the state machine
- Build system and CI integration with existing modules

**Recommendation:** KMM is acceptable if this lives in the PathSense repo. For a standalone SDK, native-only (Kotlin + Swift) would be more practical.

### Effort Estimate (Rough)

| Component | Android | iOS |
|---|---|---|
| Floating bubble + popup | Medium | Medium |
| Recording engine | Medium-High | Medium |
| Permission flows | Medium | Low |
| Gallery saving | Low | Low |
| Foreground Service + notification | Medium | N/A |
| Shared KMM layer | Low | Low |
| **Total complexity** | **Medium-High** | **Medium** |

---

## 7. Required Platform APIs Summary

### Android

| API | Min API | Purpose |
|---|---|---|
| `WindowManager` + `TYPE_APPLICATION_OVERLAY` | 26 | Floating bubble |
| `Settings.canDrawOverlays()` | 23 | Overlay permission check |
| `MediaProjectionManager` | 21 | Screen capture consent |
| `MediaProjection` + `VirtualDisplay` | 21 | Screen capture |
| `MediaRecorder` | 21 | Video encoding to MP4 |
| `MediaStore.Video.Media` | 29 | Gallery save (scoped storage) |
| `Foreground Service (mediaProjection)` | 29 | Required for MediaProjection |
| `AudioPlaybackCapture` | 29 | Internal audio capture |

### iOS

| API | Min iOS | Purpose |
|---|---|---|
| `UIWindow` (elevated windowLevel) | 13 | Floating bubble |
| `RPScreenRecorder` | 9 | In-app screen recording |
| `RPScreenRecorder.startCapture()` | 11 | Raw frame capture for custom save |
| `AVAssetWriter` | 13 | Writing frames to MP4 |
| `PHPhotoLibrary` | 8 | Saving video to Photos |

---

## 8. Alternative iOS Paths (Not Recommended, Documented for Completeness)

| Approach | Pros | Cons |
|---|---|---|
| **Broadcast Extension** | System-wide recording | Cannot be triggered programmatically; requires Control Center; complex integration (separate Xcode target, App Groups) |
| **PiP hack** | Persists when backgrounded | Fragile; violates App Store guidelines; no interactive controls |
| **Live Activity** | Visible on Lock Screen | Non-interactive; 12hr max; cannot trigger recording |
