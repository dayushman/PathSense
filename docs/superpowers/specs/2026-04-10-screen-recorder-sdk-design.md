# Screen Recorder SDK — Design Spec

## Context

A plug-and-play screen recording SDK with a floating bubble UI control, shipped as an external library for third-party developers. Single module, internally layered (engine + bubble), bubble-driven recording flow.

**Decisions locked in brainstorming:**

| Decision | Choice |
|---|---|
| Consumer | External SDK for third-party devs |
| Control flow | Bubble-driven (SDK owns the UI) |
| Customization | Minimal (tint color, position) |
| Output | Temp file delivered to host app via callback |
| Audio | Optional mic capture, SDK handles permission |
| Min targets | Android 29 (API 29) / iOS 14 |
| Architecture | Single module, internal engine/bubble layering (Approach C) |

---

## 1. Public API Surface

Everything below is `public`. All other types are `internal`.

### 1.1 Initialization

**Android (Kotlin):**
```kotlin
// Application.onCreate()
ScreenRecorder.init(
    application = this,
    config = ScreenRecorderConfig()
)
```

**iOS (Swift):**
```swift
// SceneDelegate
ScreenRecorder.start(in: windowScene, config: ScreenRecorderConfig())
```

One call. SDK creates the bubble, manages lifecycle, handles permissions on first record tap.

### 1.2 Configuration (commonMain)

```kotlin
data class ScreenRecorderConfig(
    var tintColor: Long = 0xFFFF3B30,       // ARGB — bubble accent color
    var bubblePosition: BubblePosition = BubblePosition.TRAILING_CENTER,
    var audioEnabled: Boolean = false,       // opt-in mic capture
    var videoQuality: VideoQuality = VideoQuality.HD_720,
    var maxDurationSec: Int = 300,           // 5 min default, 0 = unlimited
    var outputFormat: OutputFormat = OutputFormat.MP4,
    var listener: ((RecordingEvent) -> Unit)? = null,
)

// No-arg constructor for Swift interop (KMM doesn't export default params to ObjC)
constructor() : this(
    tintColor = 0xFFFF3B30,
    bubblePosition = BubblePosition.TRAILING_CENTER,
    audioEnabled = false,
    videoQuality = VideoQuality.HD_720,
    maxDurationSec = 300,
    outputFormat = OutputFormat.MP4,
    listener = null,
)

enum class BubblePosition {
    LEADING_CENTER, TRAILING_CENTER,
    LEADING_TOP, TRAILING_TOP,
    LEADING_BOTTOM, TRAILING_BOTTOM,
}

enum class VideoQuality(val width: Int, val height: Int, val bitrateMbps: Float) {
    SD_480(854, 480, 2f),
    HD_720(1280, 720, 5f),
    FHD_1080(1920, 1080, 8f),
}

enum class OutputFormat { MP4, MOV }
```

Mutable `var` properties for Swift interop. `VideoQuality` carries encoding parameters directly — no magic strings.

### 1.3 Events (commonMain)

```kotlin
sealed class RecordingEvent {
    data class PermissionRequired(val type: PermissionType) : RecordingEvent()
    data class PermissionGranted(val type: PermissionType) : RecordingEvent()
    data class PermissionDenied(val type: PermissionType) : RecordingEvent()
    data class RecordingStarted(val sessionId: String) : RecordingEvent()
    data class DurationUpdate(val sessionId: String, val elapsedMs: Long) : RecordingEvent()
    data class RecordingStopped(val sessionId: String, val file: RecordingFile) : RecordingEvent()
    data class RecordingFailed(val sessionId: String, val error: RecordingError) : RecordingEvent()
    object BubbleShown : RecordingEvent()
    object BubbleHidden : RecordingEvent()
}

enum class PermissionType { OVERLAY, SCREEN_CAPTURE, MICROPHONE }

data class RecordingFile(
    val path: String,          // absolute path to temp file
    val durationMs: Long,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int,
)

sealed class RecordingError(val message: String) {
    class PermissionDenied(message: String) : RecordingError(message)
    class EncoderFailed(message: String) : RecordingError(message)
    class DiskFull(message: String) : RecordingError(message)
    class MaxDurationReached : RecordingError("Max duration reached")  // file delivered via RecordingStopped, not here
    class SystemUnavailable(message: String) : RecordingError(message)
}
```

All events delivered on main thread. `RecordingFile` gives the host app everything it needs to decide what to do with the output.

### 1.4 Runtime Control

```kotlin
// commonMain
expect class ScreenRecorder {
    companion object {
        val state: RecordingState          // current state (read-only)
        fun show()                         // show bubble
        fun hide()                         // hide bubble
        fun destroy()                      // teardown — must call on app exit
    }
}

enum class RecordingState { IDLE, REQUESTING_PERMISSION, RECORDING, STOPPING }
```

Host app does **not** call start/stop recording — the bubble does. Host can show/hide the bubble and observe state.

**Lifecycle contract:**
- `destroy()` **must** be called by the host app when done (e.g., `Application.onTerminate()` on Android, or when the feature is no longer needed).
- On Android, the SDK also auto-cleans up if the foreground service is killed by the OS.
- On iOS, `BubbleWindow` releases resources on `deinit`.
- Calling any method after `destroy()` is a no-op (not a crash).

### 1.5 Minimal Integration Example

**Android:**
```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ScreenRecorder.init(this, ScreenRecorderConfig().apply {
            audioEnabled = true
            listener = { event ->
                when (event) {
                    is RecordingEvent.RecordingStopped -> uploadVideo(event.file.path)
                    is RecordingEvent.RecordingFailed -> log(event.error.message)
                    else -> {}
                }
            }
        })
    }
}
```

**iOS:**
```swift
func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options: UIScene.ConnectionOptions) {
    guard let windowScene = scene as? UIWindowScene else { return }
    
    let config = ScreenRecorderConfig()
    config.audioEnabled = true
    config.listener = { event in
        if let stopped = event as? RecordingEvent.RecordingStopped {
            self.uploadVideo(path: stopped.file.path)
        }
    }
    ScreenRecorder.start(in: windowScene, config: config)
}
```

---

## 2. Internal Architecture — State Machine & Engine

### 2.1 State Machine (commonMain)

All state transitions go through one deterministic state machine. No scattered `if (isRecording)` checks anywhere.

```
                    ┌──────────────────────────────────────┐
                    │                                      │
                    ▼                                      │
   IDLE ──→ REQUESTING_PERMISSION ──→ PERMISSION_DENIED ──┘
                    │
                    ▼
              PREPARING ──→ RECORDING ──→ STOPPING ──→ FINALIZING ──→ IDLE
                    │            │                           │
                    ▼            ▼                           ▼
                  ERROR       ERROR                       ERROR
                    │            │                           │
                    └────────────┴───────────────────────────┘
                                 │
                                 ▼
                               IDLE (after error delivered)
```

**Transition rules:**
- Every state transition emits a `RecordingEvent` to the host listener
- `ERROR` is transient — auto-resets to `IDLE` after delivering the error event
- `PERMISSION_DENIED` returns to `IDLE` — user can tap bubble again to retry
- `FINALIZING` covers: stop encoder → write file trailer → compute file metadata → deliver `RecordingFile`
- `PREPARING` covers: acquire MediaProjection/RPScreenRecorder → configure encoder → create output file

```kotlin
// commonMain — internal
internal class RecordingStateMachine {
    private val _state = MutableStateFlow(InternalState.IDLE)
    val state: StateFlow<InternalState> = _state.asStateFlow()

    fun transition(action: Action): InternalState {
        val current = _state.value
        val next = resolve(current, action)
        if (next == current) return current  // no-op for invalid transitions
        _state.value = next
        return next
    }

    private fun resolve(state: InternalState, action: Action): InternalState {
        return when (state to action) {
            IDLE to Action.TapRecord                          -> REQUESTING_PERMISSION
            REQUESTING_PERMISSION to Action.PermissionGranted -> PREPARING
            REQUESTING_PERMISSION to Action.PermissionDenied  -> IDLE
            PREPARING to Action.EncoderReady                  -> RECORDING
            PREPARING to Action.Failed                        -> IDLE
            RECORDING to Action.TapStop                       -> STOPPING
            RECORDING to Action.MaxDuration                   -> STOPPING
            RECORDING to Action.Failed                        -> IDLE
            STOPPING to Action.EncoderStopped                 -> FINALIZING
            FINALIZING to Action.FileReady                    -> IDLE
            FINALIZING to Action.Failed                       -> IDLE
            else -> state
        }
    }
}

internal enum class InternalState {
    IDLE, REQUESTING_PERMISSION, PREPARING, RECORDING, STOPPING, FINALIZING
}

internal sealed class Action {
    object TapRecord : Action()
    object TapStop : Action()
    object PermissionGranted : Action()
    object PermissionDenied : Action()
    object EncoderReady : Action()
    object EncoderStopped : Action()
    data class FileReady(val file: RecordingFile) : Action()
    data class Failed(val error: RecordingError) : Action()
    object MaxDuration : Action()
}
```

### 2.2 RecordingController (expect/actual)

Platform-specific recording implementation. Knows how to capture frames and encode — nothing else.

```kotlin
// commonMain — internal
internal expect class RecordingController {
    fun requestPermissions()
    fun prepare(config: ScreenRecorderConfig)
    fun startCapture()
    fun stopCapture()
    fun release()

    // Platform calls these to feed actions into state machine
    var onAction: (Action) -> Unit
}
```

- **Android actual:** wraps `MediaProjection` + `MediaRecorder` + `VirtualDisplay`
- **iOS actual:** thin bridge to Swift `ReplayKitRecorder` via `RPScreenRecorder.startCapture()`

### 2.3 Orchestrator (commonMain)

Central coordinator wiring the state machine, controller, bubble events, and host listener.

```kotlin
// commonMain — internal
internal class RecordingOrchestrator(
    private val config: ScreenRecorderConfig,
    private val stateMachine: RecordingStateMachine,
    private val controller: RecordingController,
    private val durationTimer: DurationTimer,
) {
    private var sessionId: String = ""

    fun onBubbleTapRecord() {
        sessionId = generateSessionId()
        val newState = stateMachine.transition(Action.TapRecord)
        if (newState == REQUESTING_PERMISSION) {
            controller.requestPermissions()
        }
    }

    fun onBubbleTapStop() {
        val newState = stateMachine.transition(Action.TapStop)
        if (newState == STOPPING) {
            durationTimer.stop()
            controller.stopCapture()
        }
    }

    // Called by controller via onAction callback
    internal fun handleAction(action: Action) {
        val newState = stateMachine.transition(action)
        when (newState) {
            PREPARING -> {
                emitEvent(RecordingEvent.PermissionGranted(PermissionType.SCREEN_CAPTURE))
                controller.prepare(config)
            }
            RECORDING -> {
                controller.startCapture()
                emitEvent(RecordingEvent.RecordingStarted(sessionId))
                durationTimer.start { elapsed ->
                    emitEvent(RecordingEvent.DurationUpdate(sessionId, elapsed))
                    if (config.maxDurationSec > 0 && elapsed >= config.maxDurationSec * 1000L) {
                        // MaxDuration triggers normal stop flow, not an error.
                        // The file is delivered via RecordingStopped.
                        // Host app can check durationMs to know it hit the limit.
                        handleAction(Action.MaxDuration)
                    }
                }
            }
            IDLE -> {
                when (action) {
                    is Action.FileReady -> emitEvent(RecordingEvent.RecordingStopped(sessionId, action.file))
                    is Action.Failed -> emitEvent(RecordingEvent.RecordingFailed(sessionId, action.error))
                    is Action.PermissionDenied -> emitEvent(RecordingEvent.PermissionDenied(PermissionType.SCREEN_CAPTURE))
                    else -> {}
                }
            }
            else -> {}
        }
    }

    private fun emitEvent(event: RecordingEvent) {
        // Always deliver on main thread
        scope.launch(MainDispatcher) {
            config.listener?.invoke(event)
        }
    }

    private fun generateSessionId(): String = "rec_${currentTimeMillis()}"
}
```

### 2.4 Duration Timer (commonMain)

```kotlin
internal class DurationTimer(private val scope: CoroutineScope) {
    private var job: Job? = null

    fun start(onTick: (elapsedMs: Long) -> Unit) {
        val startTime = currentTimeMillis()
        job = scope.launch(MainDispatcher) {
            while (isActive) {
                delay(1000)
                onTick(currentTimeMillis() - startTime)
            }
        }
    }

    fun stop() { job?.cancel() }
}
```

---

## 3. Platform Layer — Android

### 3.1 Foreground Service

`ScreenRecorderService` is the backbone. It hosts the MediaProjection, bubble overlay, and recording engine. Runs as a foreground service so recording continues when the host app is backgrounded.

```kotlin
// androidMain — internal
internal class ScreenRecorderService : Service() {
    private lateinit var orchestrator: RecordingOrchestrator
    private lateinit var bubbleManager: BubbleManager
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        startForeground(NOTIFICATION_ID, notificationHelper.buildIdleNotification())
        bubbleManager.attach(windowManager)
    }

    override fun onDestroy() {
        orchestrator.controller.release()
        bubbleManager.detach()
    }
}
```

**Manifest declarations required by host app:**
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />  <!-- if audioEnabled -->

<service
    android:name="com.screenrecorder.ScreenRecorderService"
    android:foregroundServiceType="mediaProjection"
    android:exported="false" />
```

The SDK will ship a **manifest merger snippet** so host apps don't manually add these. Permissions are declared in the SDK's own `AndroidManifest.xml`.

### 3.2 MediaProjection Recorder (Android actual)

```kotlin
// androidMain — internal
internal actual class RecordingController {
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputFile: File? = null
    actual var onAction: (Action) -> Unit = {}

    actual fun requestPermissions() {
        // 1. Check SYSTEM_ALERT_WINDOW → redirect to Settings if needed
        // 2. Launch MediaProjectionManager.createScreenCaptureIntent()
        // 3. On result: onAction(PermissionGranted) or onAction(PermissionDenied)
    }

    actual fun prepare(config: ScreenRecorderConfig) {
        outputFile = createTempFile(config.outputFormat)
        mediaRecorder = MediaRecorder(context).apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            if (config.audioEnabled) setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(config.outputFormat.toMediaRecorderFormat())
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (config.audioEnabled) setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(config.videoQuality.width, config.videoQuality.height)
            setVideoEncodingBitRate((config.videoQuality.bitrateMbps * 1_000_000).toInt())
            setVideoFrameRate(30)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
        }
        onAction(Action.EncoderReady)
    }

    actual fun startCapture() {
        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "ScreenRecorder",
            config.videoQuality.width,
            config.videoQuality.height,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder!!.surface,
            null, null
        )
        mediaRecorder!!.start()
    }

    actual fun stopCapture() {
        try {
            mediaRecorder?.stop()
        } catch (e: RuntimeException) {
            // MediaRecorder.stop() throws if no frames were recorded
            onAction(Action.Failed(RecordingError.EncoderFailed("No frames captured")))
            return
        }
        virtualDisplay?.release()
        mediaProjection?.stop()

        val file = outputFile!!
        onAction(Action.FileReady(RecordingFile(
            path = file.absolutePath,
            durationMs = extractDuration(file),
            fileSizeBytes = file.length(),
            width = config.videoQuality.width,
            height = config.videoQuality.height,
        )))
    }

    actual fun release() {
        mediaRecorder?.release()
        virtualDisplay?.release()
        mediaProjection?.stop()
        mediaRecorder = null
        virtualDisplay = null
        mediaProjection = null
    }
}
```

**Android 14+ re-consent handling:** MediaProjection consent cannot persist across app restarts on API 34+. The SDK handles this transparently — if the stored projection is stale, it re-triggers the consent dialog on next record tap. No special handling needed from the host app.

### 3.3 Overlay Permission Flow (Android)

`SYSTEM_ALERT_WINDOW` is a special grant — cannot be requested via the standard permission dialog.

```
User taps bubble record button
  → SDK checks Settings.canDrawOverlays(context)
  → If false:
      1. Emit RecordingEvent.PermissionRequired(OVERLAY)
      2. Launch Settings.ACTION_MANAGE_OVERLAY_PERMISSION intent
      3. On return to app: re-check canDrawOverlays()
      4. If granted: proceed to MediaProjection consent
      5. If denied: emit RecordingEvent.PermissionDenied(OVERLAY), return to IDLE
  → If true:
      Proceed to MediaProjection consent dialog
```

### 3.4 Notification Management (Android)

Foreground service requires a persistent notification. The SDK manages two notification states:

| State | Notification |
|---|---|
| Bubble visible, not recording | "Screen Recorder ready" (low priority, minimal) |
| Recording | "Recording in progress — 0:42" (ongoing, with stop action) |

The notification includes a **stop action** so the user can stop recording from the notification shade without returning to the app.

### 3.5 Screen Rotation Handling (Android)

When the device rotates during recording:
1. `VirtualDisplay` receives a configuration change callback
2. SDK does **not** restart the recording — this would lose frames
3. Instead: the `VirtualDisplay` continues capturing at the original resolution
4. The output file will have black bars on the rotated axis
5. This matches the behavior of the system screen recorder on Android

---

## 4. Platform Layer — iOS

### 4.1 BubbleWindow (Swift)

Following the existing `PathSenseTrackingWindow` pattern — a dedicated `UIWindow` with elevated `windowLevel`.

```swift
// iosSwift — internal
internal final class BubbleWindow: UIWindow {
    private let bubbleVC: BubbleViewController
    private let orchestrator: RecordingOrchestrator  // bridged from KMM

    init(windowScene: UIWindowScene, config: ScreenRecorderConfig) {
        bubbleVC = BubbleViewController(config: config)
        super.init(windowScene: windowScene)
        windowLevel = .alert + 1
        rootViewController = bubbleVC
        isHidden = false

        // Pass-through touches outside the bubble
        // (identical to PathSenseTrackingWindow's hitTest approach)
    }

    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        let hit = super.hitTest(point, with: event)
        // Return nil if touch is outside bubble/popup — lets touch pass to app
        return (hit === rootViewController?.view) ? nil : hit
    }
}
```

**Key behaviors:**
- Visible on top of all app content (nav stacks, modals, sheets)
- Draggable via `UIPanGestureRecognizer` on the bubble view
- Disappears when app is backgrounded (iOS limitation — documented, not a bug)
- `hitTest` returns `nil` for touches outside bubble, so app interaction is unaffected

### 4.2 ReplayKit Recorder (iOS actual → Swift)

```swift
// iosSwift — internal
internal final class ReplayKitRecorder {
    private let recorder = RPScreenRecorder.shared()
    private var assetWriter: AVAssetWriter?
    private var videoInput: AVAssetWriterInput?
    private var audioInput: AVAssetWriterInput?
    private var outputURL: URL?
    private var startTime: CMTime?

    func prepare(config: ScreenRecorderConfig) throws {
        guard recorder.isAvailable else {
            throw RecorderError.systemUnavailable
        }
        outputURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("rec_\(Date().timeIntervalSince1970)")
            .appendingPathExtension(config.outputFormat.fileExtension)

        assetWriter = try AVAssetWriter(outputURL: outputURL!, fileType: config.outputFormat.avFileType)

        let videoSettings: [String: Any] = [
            AVVideoCodecKey: AVVideoCodecType.h264,
            AVVideoWidthKey: config.videoQuality.width,
            AVVideoHeightKey: config.videoQuality.height,
            AVVideoCompressionPropertiesKey: [
                AVVideoAverageBitRateKey: config.videoQuality.bitrateMbps * 1_000_000,
            ]
        ]
        videoInput = AVAssetWriterInput(mediaType: .video, outputSettings: videoSettings)
        videoInput!.expectsMediaDataInRealTime = true
        assetWriter!.add(videoInput!)

        if config.audioEnabled {
            let audioSettings: [String: Any] = [
                AVFormatIDKey: kAudioFormatMPEG4AAC,
                AVSampleRateKey: 44100,
                AVNumberOfChannelsKey: 2,
            ]
            audioInput = AVAssetWriterInput(mediaType: .audio, outputSettings: audioSettings)
            audioInput!.expectsMediaDataInRealTime = true
            assetWriter!.add(audioInput!)
        }
    }

    func startCapture() {
        assetWriter?.startWriting()

        recorder.startCapture { [weak self] sampleBuffer, bufferType, error in
            guard let self = self, error == nil else { return }

            if self.startTime == nil {
                self.startTime = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)
                self.assetWriter?.startSession(atSourceTime: self.startTime!)
            }

            switch bufferType {
            case .video:
                if self.videoInput?.isReadyForMoreMediaData == true {
                    self.videoInput?.append(sampleBuffer)
                }
            case .audioMic:
                if self.audioInput?.isReadyForMoreMediaData == true {
                    self.audioInput?.append(sampleBuffer)
                }
            default:
                break
            }
        }
    }

    func stopCapture(completion: @escaping (Result<RecordingFile, RecorderError>) -> Void) {
        recorder.stopCapture { [weak self] error in
            guard let self = self else { return }
            if let error = error {
                completion(.failure(.encoderFailed(error.localizedDescription)))
                return
            }

            self.videoInput?.markAsFinished()
            self.audioInput?.markAsFinished()
            self.assetWriter?.finishWriting {
                guard self.assetWriter?.status == .completed,
                      let url = self.outputURL else {
                    completion(.failure(.encoderFailed("Asset writer failed")))
                    return
                }
                let attrs = try? FileManager.default.attributesOfItem(atPath: url.path)
                let size = (attrs?[.size] as? Int64) ?? 0
                completion(.success(RecordingFile(
                    path: url.path,
                    durationMs: self.computeDuration(),
                    fileSizeBytes: size,
                    width: Int32(self.config.videoQuality.width),
                    height: Int32(self.config.videoQuality.height)
                )))
            }
        }
    }
}
```

### 4.3 iOS Permission Flow

Simpler than Android — ReplayKit in-app recording requires **no user consent dialog** for video. Only microphone needs a permission prompt.

```
User taps record on bubble
  → Check RPScreenRecorder.shared().isAvailable
  → If false: emit RecordingFailed(SystemUnavailable), return to IDLE
  → If config.audioEnabled:
      Check AVAudioSession.recordPermission
      If .undetermined: requestRecordPermission()
      If .denied: emit PermissionDenied(MICROPHONE), record video-only (graceful degradation)
      If .granted: proceed
  → Proceed to PREPARING
```

**No overlay permission needed on iOS** — the bubble is an in-app `UIWindow`, not a system overlay.

### 4.4 iOS Limitations (Documented in SDK)

These are hard platform constraints. The SDK documents them clearly so host developers don't file bugs:

| Limitation | Detail |
|---|---|
| In-app recording only | `RPScreenRecorder` captures only the host app's window, not other apps or system UI |
| Bubble disappears on background | iOS does not allow third-party system-wide overlays |
| Simulator not supported | `RPScreenRecorder.isAvailable` returns `false` on simulators |
| AirPlay / screen mirroring | Recording may be unavailable during AirPlay sessions |

---

## 5. Bubble UI Architecture

### 5.1 Visual Design

The bubble is a **44×44pt circle** (meets Apple's minimum touch target) with a recording icon inside.

**States:**

| Bubble State | Appearance |
|---|---|
| Idle | Solid circle with `tintColor`, camera icon (white) |
| Recording | Pulsing red ring animation, stop-square icon, elapsed time label below |
| Permission pending | Spinner replacing the icon |

### 5.2 Bubble Interaction

```
Tap (idle)       → expand popup with "Record" button
Tap (recording)  → stop recording (direct, no popup)
Long press       → enter drag mode
Drag             → reposition bubble, snaps to nearest edge on release
Tap outside popup → collapse popup
```

### 5.3 Popup Layout

Minimal popup that appears adjacent to the bubble:

```
┌─────────────────────┐
│  ● Record  [00:00]  │    ← single row when idle
│  ■ Stop    [02:34]  │    ← single row when recording
└─────────────────────┘
```

No settings, no extra options. One button, one timer.

### 5.4 Edge Snapping

When the user finishes dragging, the bubble animates to the nearest screen edge (left or right). Y position is preserved. This prevents the bubble from floating in the middle of the screen.

```
On drag end:
  if bubble.centerX < screenWidth / 2:
    animate to x = margin
  else:
    animate to x = screenWidth - bubbleWidth - margin
```

### 5.5 Bubble Z-Order

| Platform | Strategy |
|---|---|
| Android | `TYPE_APPLICATION_OVERLAY` via `WindowManager` in foreground service. Visible over all apps. |
| iOS | `UIWindow.windowLevel = .alert + 1`. Visible over all app content. |

### 5.6 Avoiding Touch Conflicts

The bubble must not intercept touches meant for the app.

- **Android:** `FLAG_NOT_FOCUSABLE` on the overlay `LayoutParams`. Only the bubble and popup views consume touches; everything else passes through.
- **iOS:** `hitTest(_:with:)` override on `BubbleWindow` returns `nil` for touches outside the bubble/popup views.

---

## 6. Permission Orchestration

### 6.1 Permission Sequence (Android)

Android requires up to 3 permissions, acquired lazily on first record tap:

```
Step 1: SYSTEM_ALERT_WINDOW (overlay)
  → Special grant: redirect to Settings
  → Cached: once granted, persists across app restarts

Step 2: MediaProjection consent (screen capture)
  → OS dialog: "Start recording?"
  → Android 14+: must re-acquire every app session (cannot persist)

Step 3: RECORD_AUDIO (microphone) — only if audioEnabled
  → Standard runtime permission dialog
  → If denied: record video-only (graceful degradation, emit PermissionDenied event)
```

**The SDK chains these automatically.** The host app sees `PermissionRequired` / `PermissionGranted` / `PermissionDenied` events but doesn't manage the flow.

### 6.2 Permission Sequence (iOS)

```
Step 1: RPScreenRecorder.isAvailable check
  → If false: emit RecordingFailed(SystemUnavailable)

Step 2: Microphone — only if audioEnabled
  → Standard AVAudioSession permission dialog
  → If denied: record video-only (graceful degradation)
```

No overlay permission needed. No screen capture consent dialog for in-app recording.

### 6.3 Graceful Degradation

| Permission denied | Behavior |
|---|---|
| Overlay (Android) | Cannot show bubble — emit error, SDK non-functional |
| Screen capture | Cannot record — emit error, return to idle |
| Microphone | Record video-only silently — emit `PermissionDenied(MICROPHONE)` so host app can inform user |

Microphone denial is **not fatal** — the SDK degrades to video-only. Overlay and screen capture denial are fatal.

---

## 7. Error Recovery & Edge Cases

### 7.1 Crash During Recording

**Android:**
- `MediaRecorder` writes to a temp file via `setOutputFile()`. If the app crashes, the file is incomplete but may be partially playable (MP4 moov atom will be missing).
- On next SDK init, the SDK scans the temp directory for orphaned `.mp4` files. If found:
  - Attempt to read duration via `MediaMetadataRetriever`
  - If readable: emit `RecordingEvent.RecordingStopped` with the recovered file
  - If corrupt: delete silently

**iOS:**
- `AVAssetWriter` may not finalize if the app crashes. The partial file is unusable.
- On next SDK init: delete any orphaned temp files in the SDK's temp directory.

### 7.2 MediaRecorder Method-Order Crashes (Android)

`MediaRecorder` throws `IllegalStateException` if methods are called in wrong order. The state machine prevents this:
- `prepare()` is only called in `PREPARING` state
- `start()` is only called after `PREPARING → RECORDING` transition
- `stop()` is only called after `RECORDING → STOPPING` transition
- Double-stop is impossible (state machine rejects `TapStop` in non-`RECORDING` states)

### 7.3 RPScreenRecorder Becomes Unavailable Mid-Recording (iOS)

`RPScreenRecorder` can become unavailable if:
- AirPlay starts during recording
- The device enters certain managed configurations

The SDK registers for `RPScreenRecorder`'s availability KVO. If it becomes `false` during recording:
1. Force-stop the capture
2. Finalize whatever frames were written
3. Emit `RecordingFailed(SystemUnavailable)` with partial file if available

### 7.4 App Backgrounding

| Platform | Behavior |
|---|---|
| Android | Recording continues — foreground service keeps it alive. Bubble remains visible (system overlay). |
| iOS | Recording continues (ReplayKit works in background briefly). Bubble disappears (UIWindow is hidden). When user returns, bubble reappears showing recording state. |

### 7.5 FLAG_SECURE Windows (Android)

Apps with `FLAG_SECURE` (banking, DRM) render as black rectangles in MediaProjection captures. This is an Android system behavior — the SDK does not attempt to circumvent it. Document in SDK README.

### 7.6 Orientation Changes During Recording

| Platform | Strategy |
|---|---|
| Android | Continue recording at original resolution. VirtualDisplay handles rotation internally. Output may have black bars if orientation changes — documented behavior. |
| iOS | `RPScreenRecorder.startCapture()` delivers frames in the current orientation. `AVAssetWriter` receives frames with varying dimensions. SDK applies an affine transform to maintain consistent output. |

---

## 8. File Management & Disk Space

### 8.1 Temp File Strategy

All recordings are written to the app's **cache directory** (not documents, not external storage):
- Android: `context.cacheDir/screen-recorder/`
- iOS: `FileManager.default.temporaryDirectory/screen-recorder/`

Benefits:
- No storage permissions needed
- OS can reclaim space under pressure
- Clear ownership — SDK manages its own directory

### 8.2 Disk Space Checks

**Before recording starts (in PREPARING state):**
1. Check available disk space via `StatFs` (Android) / `FileManager.attributesOfFileSystem` (iOS)
2. Estimate required space: `bitrateMbps * maxDurationSec / 8` bytes
3. Require 2x estimated space as safety margin
4. If insufficient: emit `RecordingFailed(DiskFull)`, return to IDLE

**During recording (every 10 seconds):**
1. Check remaining disk space
2. If below 50MB threshold: auto-stop recording, finalize file, emit `RecordingFailed(DiskFull)` with the partial file

### 8.3 Cleanup Policy

- After `RecordingEvent.RecordingStopped` is delivered, the SDK **does not delete the file**. The host app owns it.
- On SDK `init()`: delete any temp files older than 24 hours (orphaned from crashes or host apps that forgot to handle the file).
- On `destroy()`: do not delete files — host app may still be processing them.

### 8.4 File Size Estimates

| Quality | Bitrate | 1 min | 5 min |
|---|---|---|---|
| SD 480p | 2 Mbps | ~15 MB | ~75 MB |
| HD 720p | 5 Mbps | ~37 MB | ~187 MB |
| FHD 1080p | 8 Mbps | ~60 MB | ~300 MB |

These estimates should be documented in the SDK's public API docs so host developers can set appropriate `maxDurationSec` values.

---

## 9. Threading Model

### 9.1 Thread Assignment

| Component | Thread | Reason |
|---|---|---|
| State machine | Main | All transitions are synchronous, sub-µs. No contention. |
| Bubble UI | Main | UIKit/Android View requirement |
| Event listener callbacks | Main | Host app expects main-thread delivery (matches PathSense pattern) |
| MediaRecorder encoding | MediaRecorder internal thread | Android manages this |
| RPScreenRecorder capture | ReplayKit callback thread | iOS manages this |
| AVAssetWriter append | ReplayKit callback thread | Append frames on the thread they arrive |
| Duration timer | Main (via coroutine) | 1-second ticks, negligible cost |
| Disk space checks | Background (Dispatchers.Default) | File I/O, don't block main |
| File finalization | Background | Writing moov atom, computing metadata |

### 9.2 Thread Safety Guarantee

All public API methods (`init`, `show`, `hide`, `destroy`, `state`) are safe to call from any thread. Internally, they dispatch to main if needed.

```kotlin
fun show() {
    scope.launch(MainDispatcher) {
        bubbleManager.show()
    }
}
```

---

## 10. Testing Strategy

### 10.1 What's Testable in commonTest (No Device Required)

| Component | Test approach |
|---|---|
| `RecordingStateMachine` | Feed sequences of `Action`s, assert state transitions. Test every valid path and every invalid transition (should no-op). |
| `RecordingOrchestrator` | Mock `RecordingController` and `DurationTimer`. Verify correct actions dispatched for each bubble event. |
| `DurationTimer` | Use `TestCoroutineDispatcher` to advance time. Verify tick intervals and max duration cutoff. |
| `ScreenRecorderConfig` | Verify defaults. Verify no-arg constructor matches parameterized defaults. |
| Event mapping | Verify each state transition emits the correct `RecordingEvent`. |

This covers ~100% of the shared logic.

### 10.2 Platform Testing

**Android:**
- `RecordingController` (actual): Integration test on real device. Cannot test MediaProjection on emulator without a virtual display hack.
- `BubbleManager`: UI test — verify overlay appears, dragging works, tap triggers callback.
- Permission flows: Manual test matrix (fresh install, previously granted, previously denied).
- Foreground service: Verify notification appears, service survives backgrounding.

**iOS:**
- `ReplayKitRecorder`: Integration test on real device only. `RPScreenRecorder.isAvailable` is `false` on simulators.
- `BubbleWindow`: UI test on simulator — verify window appears, hitTest passes through, dragging works.
- Permission flows: Manual test (microphone only).

### 10.3 Testing Pyramid

```
         ┌──────────────┐
         │  Manual E2E  │  ← permission flows, full recording, crash recovery
         ├──────────────┤
         │ Device Tests │  ← MediaProjection/ReplayKit integration, bubble UI
         ├──────────────┤
         │ Common Tests │  ← state machine, orchestrator, timer, config, events
         └──────────────┘
              (bulk)
```

### 10.4 Simulator Limitations

| Platform | What works on simulator | What doesn't |
|---|---|---|
| Android emulator | Overlay UI, permission intent launching | MediaProjection (no virtual display support by default) |
| iOS simulator | BubbleWindow, hitTest, drag gesture | RPScreenRecorder (isAvailable = false) |

Document this in CONTRIBUTING.md — developers must use physical devices for recording tests.

---

## 11. Performance & Resource Impact

### 11.1 Memory

| Component | Memory | Notes |
|---|---|---|
| Bubble UI (idle) | ~2-3 MB | Single UIWindow/View hierarchy |
| MediaRecorder (recording) | ~15-30 MB | Encoder buffers, depends on resolution |
| RPScreenRecorder (recording) | ~10-20 MB | Frame buffers managed by ReplayKit |
| AVAssetWriter (recording) | ~5-10 MB | Write buffers |
| **Total during recording** | **~30-60 MB** | Acceptable for a foreground feature |

### 11.2 Battery

Recording is inherently expensive — hardware encoder + continuous frame capture.

| Quality | Estimated drain | Notes |
|---|---|---|
| SD 480p | ~5-8% per hour | Hardware encoder, low resolution |
| HD 720p | ~8-12% per hour | Default quality |
| FHD 1080p | ~12-18% per hour | Consider documenting as "high drain" |

The SDK does not attempt to optimize battery beyond using hardware encoders (which both platforms use by default).

### 11.3 Thermal Throttling

On sustained recording (>5 minutes), devices may thermal throttle:
- Frame rate drops from 30 to 20-24 fps
- Encoder may reduce quality automatically

The SDK does **not** fight this — it's the correct system behavior. If thermal state becomes critical (Android `THERMAL_STATUS_SEVERE`), the SDK auto-stops recording and emits `RecordingFailed(EncoderFailed("Thermal throttle"))`.

### 11.4 Frame Drops

During recording, if the encoder cannot keep up:
- **Android:** `MediaRecorder` drops frames silently. No API to detect this.
- **iOS:** `startCapture` callback delivers frames as fast as possible. If `AVAssetWriterInput.isReadyForMoreMediaData` is `false`, the SDK drops the frame. No stalling.

Frame drops are transparent to the host app. The output video may have slight stutters under extreme load — this is expected behavior documented in the SDK.

---

## 12. Module Structure

```
screen-recorder/
├── build.gradle.kts
├── src/
│   ├── commonMain/kotlin/com/screenrecorder/
│   │   ├── api/
│   │   │   ├── ScreenRecorder.kt          // expect class — public entry point
│   │   │   ├── ScreenRecorderConfig.kt     // public config
│   │   │   ├── RecordingEvent.kt           // public events
│   │   │   ├── RecordingFile.kt            // public output model
│   │   │   ├── RecordingError.kt           // public error types
│   │   │   └── RecordingState.kt           // public state enum
│   │   └── engine/
│   │       ├── RecordingStateMachine.kt    // internal state machine
│   │       ├── RecordingOrchestrator.kt    // internal coordinator
│   │       ├── RecordingController.kt      // internal expect — platform recorder
│   │       ├── DurationTimer.kt            // internal timer
│   │       └── Action.kt                   // internal state machine actions
│   ├── commonTest/kotlin/com/screenrecorder/
│   │   ├── StateMachineTest.kt
│   │   ├── OrchestratorTest.kt
│   │   ├── DurationTimerTest.kt
│   │   └── ConfigTest.kt
│   ├── androidMain/kotlin/com/screenrecorder/
│   │   ├── api/
│   │   │   └── ScreenRecorder.android.kt   // actual — init(Application), auto lifecycle
│   │   ├── engine/
│   │   │   └── MediaProjectionRecorder.kt  // actual RecordingController
│   │   ├── bubble/
│   │   │   ├── BubbleManager.kt            // overlay lifecycle
│   │   │   ├── BubbleView.kt               // circular draggable view
│   │   │   └── PopupView.kt                // record/stop popup
│   │   ├── service/
│   │   │   ├── ScreenRecorderService.kt    // foreground service
│   │   │   └── NotificationHelper.kt       // notification management
│   │   └── permission/
│   │       ├── OverlayPermissionHelper.kt
│   │       └── MediaProjectionPermissionHelper.kt
│   └── iosMain/kotlin/com/screenrecorder/
│       ├── api/
│       │   └── ScreenRecorder.ios.kt        // actual — bridge to Swift
│       └── engine/
│           └── ReplayKitBridge.kt           // actual RecordingController → Swift calls
├── ios/
│   └── Sources/ScreenRecorderUI/
│       ├── BubbleWindow.swift
│       ├── BubbleViewController.swift
│       ├── RecordingPopupView.swift
│       ├── ReplayKitRecorder.swift
│       └── ScreenRecorder+iOS.swift         // Swift-side public init
└── ios/
    └── Package.swift                        // SPM manifest
```

---

## 13. Distribution

### 13.1 Android

- Published as a Maven artifact: `com.screenrecorder:screen-recorder:x.y.z`
- Manifest merger handles permissions and service declaration
- Host app adds one Gradle dependency, one `init()` call

### 13.2 iOS

- Distributed as Swift Package (SPM)
- KMM compiled to `ScreenRecorderCore.xcframework` (arm64 device, arm64+x86_64 simulator)
- Swift sources in `Sources/ScreenRecorderUI/`
- Host app adds SPM dependency, one `start(in:config:)` call

### 13.3 Versioning

Single version number across both platforms. Follows semver:
- Major: breaking API changes
- Minor: new features, backward compatible
- Patch: bug fixes

---

## 14. What This Spec Intentionally Excludes

| Excluded | Reason |
|---|---|
| Programmatic start/stop API | Bubble-driven only for v1. Internal seams allow adding this later. |
| Gallery save | Host app handles output. SDK could ship a `GallerySaver` utility later. |
| Internal audio capture | Adds complexity (API 29+ only, app opt-out flags). Mic is sufficient for v1. |
| Broadcast Extension (iOS) | Cannot be triggered programmatically. Fundamentally different UX. |
| Custom bubble UI / theming | Minimal customization (tint + position) keeps the SDK opinionated and consistent. |
| Video editing / trimming | Out of scope — the SDK captures, the host app processes. |
| Cloud upload | Out of scope — the SDK delivers a local file. |
| Analytics / telemetry | External SDK must not phone home without explicit opt-in. |

---

## 15. Platform API Reference

### Android

| API | Min API | Purpose |
|---|---|---|
| `WindowManager` + `TYPE_APPLICATION_OVERLAY` | 26 | Floating bubble |
| `Settings.canDrawOverlays()` | 23 | Overlay permission check |
| `MediaProjectionManager` | 21 | Screen capture consent |
| `MediaProjection` + `VirtualDisplay` | 21 | Screen capture |
| `MediaRecorder` | 21 | Video encoding to MP4 |
| `Foreground Service (mediaProjection)` | 29 | Required for MediaProjection |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | 34 | Required on Android 14+ |
| `StatFs` | 18 | Disk space checks |

### iOS

| API | Min iOS | Purpose |
|---|---|---|
| `UIWindow` (elevated windowLevel) | 13 | Floating bubble |
| `RPScreenRecorder.startCapture()` | 11 | Raw frame capture |
| `AVAssetWriter` | 11 | Writing frames to MP4/MOV |
| `FileManager.attributesOfFileSystem` | 2 | Disk space checks |
| `AVAudioSession.recordPermission` | 7 | Microphone permission |

---

## 16. Open Questions

None — all decisions resolved during brainstorming. Ready for implementation planning.
