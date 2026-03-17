package com.dayushmand.pathsense.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.inspector.WindowInspector
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

    private var initialized = false
    private var config = PathSenseConfig()

    private val activityAttachments = WeakHashMap<Activity, Attachment>()
    private val dialogAttachments = LinkedHashMap<ViewGroup, Attachment>()
    private val rootAttachments = LinkedHashMap<ViewGroup, Attachment>()

    private var activeAttachment: Attachment? = null
    private var gestureAttachment: Attachment? = null

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
        allAttachments().forEach { attachment ->
            attachment.tracker.captureEnabled = false
            attachment.overlay.clearCanvas()
            attachment.overlay.visibility = View.GONE
        }
        gestureAttachment = null
    }

    /**
     * Enable path capture globally.
     *
     * New touch events are processed normally from the next gesture.
     * Must be called on the main thread.
     */
    fun enable() {
        isEnabled = true
        allAttachments().forEach { attachment ->
            attachment.tracker.captureEnabled = true
        }
        syncAndRefreshOverlays()
    }

    /**
     * Initialize the SDK. Safe to call multiple times — subsequent calls
     * update the [config] but do not re-register lifecycle callbacks.
     */
    fun init(application: Application, config: PathSenseConfig = PathSenseConfig()) {
        this.config = config
        initDebugContext(application)
        if (initialized) {
            updateAttachedOverlays()
            syncAndRefreshOverlays()
            return
        }
        initialized = true
        application.registerActivityLifecycleCallbacks(Callbacks())
    }

    /**
     * Programmatically clear all rendered paths and overlays across all
     * attached windows. Does **not** disable capture — new gestures
     * will still be tracked.
     *
     * Must be called on the main thread.
     */
    fun clearCanvas() {
        allAttachments().forEach { attachment ->
            attachment.overlay.clearCanvas()
        }
    }

    /**
     * Returns the [PathTracker] attached to the given [activity], or
     * `null` if the SDK has not yet attached to it.
     */
    fun trackerFor(activity: Activity): PathTracker? =
        activityAttachments[activity]?.tracker

    // ---- internals ---------------------------------------------------------

    private fun attach(activity: Activity) {
        if (activityAttachments.containsKey(activity)) return

        val decor = activity.window.decorView as? FrameLayout ?: return
        val tracker = PathTracker(config.pathConfig)
        if (!isEnabled) tracker.captureEnabled = false
        config.listener?.let { listener -> tracker.listener = listener }

        val overlay = PathOverlayView(activity).apply {
            this.tracker = tracker
            overlayConfig = config.overlayConfig
            visibility = View.INVISIBLE
        }

        val focusListener = installWindowFocusListener(decor)
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        decor.addView(overlay, lp)

        val originalCallback = activity.window.callback
        val attachment = Attachment(
            tracker = tracker,
            overlay = overlay,
            hostView = decor,
            window = activity.window,
            originalCallback = originalCallback,
            focusListener = focusListener,
        )
        promoteOverlay(attachment)
        val interceptor = TouchInterceptWindowCallback(
            wrapped = originalCallback,
            tracker = tracker,
            overlayView = overlay,
            onGestureStart = { onGestureStarted(attachment) },
            onGestureFinish = { onGestureFinished(attachment) },
        )
        attachment.installedCallback = interceptor
        activity.window.callback = interceptor

        activityAttachments[activity] = attachment
        rootAttachments[decor] = attachment
    }

    private fun detach(activity: Activity) {
        val attachment = activityAttachments.remove(activity) ?: return
        rootAttachments.remove(attachment.hostView)
        detachAttachment(attachment)
        if (activeAttachment === attachment) activeAttachment = null
        if (gestureAttachment === attachment) gestureAttachment = null
    }

    private fun attachDialogWindow(root: ViewGroup) {
        if (rootAttachments.containsKey(root)) return

        val window = resolveWindowForRoot(root) ?: return
        val originalCallback = window.callback ?: return
        if (originalCallback is TouchInterceptWindowCallback) return

        val tracker = PathTracker(config.pathConfig)
        if (!isEnabled) tracker.captureEnabled = false
        config.listener?.let { listener -> tracker.listener = listener }

        val overlay = PathOverlayView(root.context).apply {
            this.tracker = tracker
            overlayConfig = config.overlayConfig
            visibility = View.INVISIBLE
        }

        val focusListener = installWindowFocusListener(root)
        val lp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        val addSucceeded = runCatching {
            root.addView(overlay, lp)
        }.isSuccess
        if (!addSucceeded) {
            removeWindowFocusListener(root, focusListener)
            tracker.destroy()
            return
        }

        val attachment = Attachment(
            tracker = tracker,
            overlay = overlay,
            hostView = root,
            window = window,
            originalCallback = originalCallback,
            focusListener = focusListener,
        )
        promoteOverlay(attachment)
        val interceptor = TouchInterceptWindowCallback(
            wrapped = originalCallback,
            tracker = tracker,
            overlayView = overlay,
            onGestureStart = { onGestureStarted(attachment) },
            onGestureFinish = { onGestureFinished(attachment) },
        )
        attachment.installedCallback = interceptor
        val wrapped = runCatching {
            window.callback = interceptor
        }.isSuccess
        if (!wrapped) {
            removeWindowFocusListener(root, focusListener)
            root.removeView(overlay)
            tracker.destroy()
            return
        }

        dialogAttachments[root] = attachment
        rootAttachments[root] = attachment
    }

    private fun detachAttachment(attachment: Attachment) {
        removeWindowFocusListener(attachment.hostView, attachment.focusListener)
        attachment.hostView.removeView(attachment.overlay)

        val window = attachment.window
        val original = attachment.originalCallback
        val installed = attachment.installedCallback
        if (window != null && original != null && installed != null && window.callback === installed) {
            window.callback = original
        }

        attachment.tracker.destroy()
    }

    private fun syncDialogAttachments() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val currentRoots = WindowInspector.getGlobalWindowViews()
            .mapNotNull { it as? ViewGroup }
            .filter { it.isAttachedToWindow }
            .toSet()

        for (root in currentRoots) {
            if (!rootAttachments.containsKey(root)) {
                attachDialogWindow(root)
            }
        }

        val staleRoots = dialogAttachments.keys
            .filter { root -> !root.isAttachedToWindow || root !in currentRoots }
            .toList()
        for (root in staleRoots) {
            val attachment = dialogAttachments.remove(root) ?: continue
            rootAttachments.remove(root)
            detachAttachment(attachment)
            if (activeAttachment === attachment) activeAttachment = null
            if (gestureAttachment === attachment) gestureAttachment = null
        }
    }

    private fun syncAndRefreshOverlays() {
        syncDialogAttachments()
        updateTopmostOverlay()
    }

    /**
     * Show overlay only on top-most attached window while no gesture is active.
     */
    private fun updateTopmostOverlay() {
        if (gestureAttachment != null) return

        var topAttachment: Attachment? = null
        for (root in orderedWindowRoots()) {
            if (!root.isAttachedToWindow || root.visibility != View.VISIBLE) continue
            topAttachment = rootAttachments[root] ?: topAttachment
        }

        allAttachments().forEach { attachment ->
            if (attachment === topAttachment) {
                showOverlay(attachment)
            } else {
                attachment.overlay.visibility = View.INVISIBLE
            }
        }
        activeAttachment = topAttachment
    }

    private fun orderedWindowRoots(): List<ViewGroup> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return WindowInspector.getGlobalWindowViews()
                .mapNotNull { it as? ViewGroup }
        }
        return activityAttachments.values.map { it.hostView }
    }

    private fun showOverlay(attachment: Attachment) {
        attachment.overlay.visibility = View.VISIBLE
        promoteOverlay(attachment)
    }

    private fun promoteOverlay(attachment: Attachment) {
        attachment.overlay.elevation = OVERLAY_PRIORITY_Z
        attachment.overlay.translationZ = OVERLAY_PRIORITY_Z
        attachment.hostView.bringChildToFront(attachment.overlay)
    }

    private fun onGestureStarted(attachment: Attachment) {
        gestureAttachment = attachment
        if (activeAttachment !== attachment || attachment.overlay.visibility != View.VISIBLE) {
            allAttachments().forEach { other ->
                if (other !== attachment) {
                    other.overlay.visibility = View.INVISIBLE
                }
            }
            showOverlay(attachment)
            activeAttachment = attachment
        } else {
            promoteOverlay(attachment)
        }
    }

    private fun onGestureFinished(attachment: Attachment) {
        if (gestureAttachment === attachment) {
            gestureAttachment = null
        }
        syncAndRefreshOverlays()
    }

    private fun updateAttachedOverlays() {
        allAttachments().forEach { attachment ->
            attachment.overlay.overlayConfig = config.overlayConfig
            config.listener?.let { listener ->
                attachment.tracker.listener = listener
            }
        }
    }

    private fun allAttachments(): List<Attachment> =
        rootAttachments.values.toSet().toList()

    private fun installWindowFocusListener(host: ViewGroup): ViewTreeObserver.OnWindowFocusChangeListener {
        val listener = ViewTreeObserver.OnWindowFocusChangeListener {
            syncAndRefreshOverlays()
        }
        host.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        return listener
    }

    private fun removeWindowFocusListener(host: ViewGroup, listener: ViewTreeObserver.OnWindowFocusChangeListener?) {
        val callback = listener ?: return
        val observer = host.viewTreeObserver
        if (observer.isAlive) {
            observer.removeOnWindowFocusChangeListener(callback)
        }
    }

    private fun resolveWindowForRoot(root: ViewGroup): Window? {
        readWindowFromObject(root)?.let { return it }
        return readWindowFromContext(root.context)
    }

    private fun readWindowFromContext(context: Context?): Window? {
        var current = context
        repeat(8) {
            readWindowFromObject(current)?.let { return it }
            current = (current as? ContextWrapper)?.baseContext
            if (current == null) return null
        }
        return null
    }

    private fun readWindowFromObject(instance: Any?): Window? {
        if (instance == null) return null
        if (instance is Window) return instance

        var type: Class<*>? = instance.javaClass
        while (type != null && type != Any::class.java) {
            // Fast path for framework internals used by DecorView/DecorContext.
            for (name in WINDOW_FIELD_NAMES) {
                val field = runCatching { type.getDeclaredField(name) }.getOrNull() ?: continue
                val value = runCatching {
                    field.isAccessible = true
                    field.get(instance)
                }.getOrNull()
                if (value is Window) return value
            }

            // Fallback: scan any field whose value is a Window.
            for (field in type.declaredFields) {
                val value = runCatching {
                    field.isAccessible = true
                    field.get(instance)
                }.getOrNull()
                if (value is Window) return value
            }
            type = type.superclass
        }
        return null
    }

    private class Attachment(
        val tracker: PathTracker,
        val overlay: PathOverlayView,
        val hostView: ViewGroup,
        val window: Window?,
        val originalCallback: Window.Callback?,
        val focusListener: ViewTreeObserver.OnWindowFocusChangeListener?,
    ) {
        var installedCallback: TouchInterceptWindowCallback? = null
    }

    private class Callbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            attach(activity)
            syncAndRefreshOverlays()
        }

        override fun onActivityDestroyed(activity: Activity) {
            detach(activity)
            syncAndRefreshOverlays()
        }

        override fun onActivityStarted(activity: Activity) {
            syncAndRefreshOverlays()
        }

        override fun onActivityResumed(activity: Activity) {
            syncAndRefreshOverlays()
        }

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) {
            syncAndRefreshOverlays()
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }

    private val WINDOW_FIELD_NAMES = arrayOf("mWindow", "mPhoneWindow")
    internal const val OVERLAY_PRIORITY_Z = 10_000f
}
