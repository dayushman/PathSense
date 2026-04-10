package com.screenrecorder.api

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.screenrecorder.bubble.BubbleManager
import com.screenrecorder.engine.*
import com.screenrecorder.permission.MediaProjectionPermissionHelper
import com.screenrecorder.permission.OverlayPermissionHelper
import com.screenrecorder.service.ScreenRecorderService
import com.screenrecorder.service.ScreenRecorderServiceBridge
import com.screenrecorder.share.ShareBottomSheet
import kotlinx.coroutines.*

actual class ScreenRecorder {
    actual companion object {
        private var orchestrator: RecordingOrchestrator? = null
        private var controller: RecordingController? = null
        private var bubbleManager: BubbleManager? = null
        private var config: ScreenRecorderConfig? = null
        private var scope: CoroutineScope? = null
        private var application: Application? = null
        private var currentActivity: Activity? = null
        private var projectionLauncher: ActivityResultLauncher<Intent>? = null
        private var overlayLauncher: ActivityResultLauncher<Intent>? = null
        private var pendingProjectionResultCode: Int = 0
        private var pendingProjectionData: Intent? = null

        actual val state: RecordingState
            get() {
                val internal = orchestrator?.let {
                    when (it.currentInternalState) {
                        InternalState.IDLE -> RecordingState.IDLE
                        InternalState.REQUESTING_PERMISSION -> RecordingState.REQUESTING_PERMISSION
                        InternalState.PREPARING -> RecordingState.REQUESTING_PERMISSION
                        InternalState.RECORDING -> RecordingState.RECORDING
                        InternalState.STOPPING -> RecordingState.STOPPING
                        InternalState.FINALIZING -> RecordingState.STOPPING
                    }
                }
                return internal ?: RecordingState.IDLE
            }

        fun init(application: Application, config: ScreenRecorderConfig = ScreenRecorderConfig()) {
            this.application = application
            this.config = config
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

            // Wrap the user's listener to intercept RecordingStopped and show share sheet
            val userListener = config.listener
            val wrappedConfig = config.apply {
                listener = { event ->
                    userListener?.invoke(event)
                    if (event is RecordingEvent.RecordingStopped) {
                        val activity = currentActivity
                        if (activity != null) {
                            ShareBottomSheet.show(activity, event.file)
                        }
                    }
                }
            }

            val ctrl = RecordingController()
            ctrl.setContext(application)
            controller = ctrl

            val stateMachine = RecordingStateMachine()
            val timer = DurationTimer(scope!!)

            orchestrator = RecordingOrchestrator(wrappedConfig, stateMachine, ctrl, timer, scope!!)

            // Set up service bridge — stop recording from notification
            ScreenRecorderServiceBridge.onStopRequested = {
                orchestrator?.onBubbleTapStop()
            }

            // Register activity lifecycle for permission flows
            application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
                    currentActivity = activity
                    registerPermissionLaunchers(activity)
                }
                override fun onActivityResumed(activity: Activity) { currentActivity = activity }
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    if (currentActivity == activity) currentActivity = null
                }
            })

            // Do NOT start the foreground service here.
            // Android 14+ requires MediaProjection consent BEFORE starting
            // a foreground service with mediaProjection type.
            // The service starts in onProjectionGranted() after user consent.
        }

        actual fun show() {
            val ctx = currentActivity ?: return
            if (!OverlayPermissionHelper.hasOverlayPermission(ctx)) {
                config?.listener?.invoke(RecordingEvent.PermissionRequired(PermissionType.OVERLAY))
                overlayLauncher?.launch(OverlayPermissionHelper.createOverlayPermissionIntent(ctx))
                return
            }
            if (bubbleManager == null) {
                bubbleManager = BubbleManager(
                    context = ctx.applicationContext,
                    tintColor = config?.tintColor ?: 0xFFFF3B30,
                    onRecordTap = { startRecordingFlow() },
                    onStopTap = {
                        orchestrator?.onBubbleTapStop()
                        bubbleManager?.setRecording(false)
                        // Stop the foreground service when recording stops
                        application?.let { ScreenRecorderService.stop(it) }
                    },
                )
            }
            bubbleManager?.attach()
            config?.listener?.invoke(RecordingEvent.BubbleShown)
        }

        actual fun hide() {
            bubbleManager?.detach()
            config?.listener?.invoke(RecordingEvent.BubbleHidden)
        }

        actual fun destroy() {
            bubbleManager?.detach()
            bubbleManager = null
            controller?.release()
            scope?.cancel()
            scope = null
            orchestrator = null
            controller = null
            config = null
            application?.let { ScreenRecorderService.stop(it) }
            application = null
        }

        private fun startRecordingFlow() {
            orchestrator?.onBubbleTapRecord()
            // Launch MediaProjection consent dialog.
            // The foreground service starts AFTER consent is granted.
            val activity = currentActivity ?: return
            val intent = MediaProjectionPermissionHelper.createScreenCaptureIntent(activity)
            projectionLauncher?.launch(intent)
        }

        private fun onProjectionGranted(resultCode: Int, data: Intent) {
            val app = application ?: return
            // Android 14+: start foreground service AFTER consent, BEFORE getMediaProjection
            ScreenRecorderService.start(app)

            // Small delay to let the service start before getting the projection
            scope?.launch {
                delay(200)
                val activity = currentActivity ?: return@launch
                val projectionManager = MediaProjectionPermissionHelper.getProjectionManager(activity)
                val projection = projectionManager.getMediaProjection(resultCode, data)
                controller?.setMediaProjection(projection)
                controller?.onAction?.invoke(Action.PermissionGranted)
                bubbleManager?.setRecording(true)
            }
        }

        private fun registerPermissionLaunchers(activity: Activity) {
            if (activity !is ComponentActivity) return

            projectionLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    onProjectionGranted(result.resultCode, result.data!!)
                } else {
                    controller?.onAction?.invoke(Action.PermissionDenied)
                }
            }

            overlayLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { _ ->
                if (OverlayPermissionHelper.hasOverlayPermission(activity)) {
                    config?.listener?.invoke(RecordingEvent.PermissionGranted(PermissionType.OVERLAY))
                    show()
                } else {
                    config?.listener?.invoke(RecordingEvent.PermissionDenied(PermissionType.OVERLAY))
                }
            }
        }
    }
}
