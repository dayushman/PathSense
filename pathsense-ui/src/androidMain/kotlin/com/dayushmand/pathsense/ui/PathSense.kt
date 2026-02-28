package com.dayushmand.pathsense.ui

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import com.dayushmand.pathsense.core.PathTracker
import java.util.WeakHashMap

/**
 * Zero-config entry point for the PathSense SDK.
 *
 * Call [init] once — typically in [Application.onCreate] — and the SDK
 * automatically attaches to every Activity: intercepting touch events,
 * tracking paths, recognizing gestures, and rendering a visual overlay.
 *
 * **No views need to be added to layouts. No code changes are needed in
 * Activities.**
 *
 * ```kotlin
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         PathSense.init(this)
 *     }
 * }
 * ```
 *
 * To receive gesture/metrics events globally:
 * ```kotlin
 * PathSense.init(this, PathSenseConfig(
 *     listener = { event -> Log.d("PathSense", "$event") }
 * ))
 * ```
 */
object PathSense {

    /**
     * Whether path capture is currently enabled.
     * When false, all touch tracking, event emission, and overlay rendering are paused.
     */
    var isEnabled: Boolean = true
        private set

    /**
     * Disable path capture globally.
     *
     * - All active gesture sessions are cancelled
     * - All overlays are cleared immediately
     * - New touch events are ignored
     *
     * Must be called on the main thread.
     * Call [enable] to resume.
     */
    fun disable() {
        isEnabled = false
        attachments.values.forEach { attachment ->
            attachment.tracker.captureEnabled = false
            attachment.overlay.clearCanvas()
        }
    }

    /**
     * Enable path capture globally.
     *
     * New touch events are processed normally from the next gesture.
     * Must be called on the main thread.
     */
    fun enable() {
        isEnabled = true
        attachments.values.forEach { attachment ->
            attachment.tracker.captureEnabled = true
        }
    }

    private var initialized = false
    private var config = PathSenseConfig()
    private val attachments = WeakHashMap<Activity, Attachment>()

    /**
     * Initialize the SDK. Safe to call multiple times — subsequent calls
     * update the [config] but do not re-register lifecycle callbacks.
     */
    fun init(application: Application, config: PathSenseConfig = PathSenseConfig()) {
        this.config = config
        initDebugContext(application)
        if (initialized) return
        initialized = true
        application.registerActivityLifecycleCallbacks(Callbacks())
    }

    /**
     * Programmatically clear all rendered paths and overlays across all
     * attached Activities. Does **not** disable capture — new gestures
     * will still be tracked.
     *
     * Must be called on the main thread.
     */
    fun clearCanvas() {
        attachments.values.forEach { attachment ->
            attachment.overlay.clearCanvas()
        }
    }

    /**
     * Returns the [PathTracker] attached to the given [activity], or
     * `null` if the SDK has not yet attached to it.
     */
    fun trackerFor(activity: Activity): PathTracker? =
        attachments[activity]?.tracker

    // ---- internals ---------------------------------------------------------

    private fun attach(activity: Activity) {
        if (attachments.containsKey(activity)) return

        val tracker = PathTracker(config.pathConfig)
        if (!isEnabled) tracker.captureEnabled = false
        config.listener?.let { l -> tracker.listener = l }

        val overlay = PathOverlayView(activity).apply {
            this.tracker = tracker
            overlayConfig = config.overlayConfig
        }

        val decor = activity.window.decorView as? FrameLayout ?: return
        decor.addView(
            overlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        val originalCallback = activity.window.callback
        activity.window.callback = TouchInterceptWindowCallback(
            wrapped = originalCallback,
            tracker = tracker,
            overlayView = overlay,
        )

        attachments[activity] = Attachment(tracker, overlay)
    }

    private fun detach(activity: Activity) {
        val attachment = attachments.remove(activity) ?: return
        val decor = activity.window.decorView as? FrameLayout
        decor?.removeView(attachment.overlay)
        attachment.tracker.destroy()
    }

    private class Attachment(
        val tracker: PathTracker,
        val overlay: PathOverlayView,
    )

    private class Callbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            attach(activity)
        }

        override fun onActivityDestroyed(activity: Activity) {
            detach(activity)
        }

        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }
}
