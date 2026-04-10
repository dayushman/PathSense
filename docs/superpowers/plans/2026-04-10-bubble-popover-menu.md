# Bubble Popover Menu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the bubble's direct tap-to-record with a dark glassmorphic popover menu showing Start Recording, Get More Info, and Audio toggle.

**Architecture:** Platform-native popover views (Android: WindowManager overlay, iOS: UIView subview) managed by existing bubble managers. No shared KMM code changes — purely UI layer on each platform.

**Tech Stack:** Android Canvas/Views + WindowManager, iOS UIKit + UIVisualEffectView, SF Symbols

---

## File Map

| Action | File | Purpose |
|--------|------|---------|
| Create | `screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/PopoverMenuView.kt` | Android popover: full-screen scrim + dark card with 3 menu rows, pill toggle, arrow nib, entry/exit animations |
| Modify | `screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/BubbleView.kt` | Add `onDragStart` callback, invoked when drag begins |
| Modify | `screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/BubbleManager.kt` | Add `showPopover()`/`hidePopover()`, track audio state, new constructor params |
| Modify | `screen-recorder/src/androidMain/kotlin/com/screenrecorder/api/ScreenRecorder.android.kt` | Wire popover: `onRecordTap` → show popover, popover callbacks → `startRecordingFlow()` + config update |
| Create | `screen-recorder/ios/Sources/ScreenRecorderUI/PopoverMenuView.swift` | iOS popover: blur card + SF Symbol icons + UISwitch + arrow nib, entry/exit animations |
| Modify | `screen-recorder/ios/Sources/ScreenRecorderUI/BubbleViewController.swift` | Show popover on idle tap, dismiss on drag/outside tap, update `config.audioEnabled` |

---

### Task 1: Create Android PopoverMenuView

**Files:**
- Create: `screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/PopoverMenuView.kt`

- [ ] **Step 1: Create PopoverMenuView.kt**

```kotlin
package com.screenrecorder.bubble

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

@SuppressLint("ViewConstructor")
internal class PopoverMenuView(
    context: Context,
    private val bubbleLeft: Int,
    private val bubbleTop: Int,
    private val bubbleSize: Int,
    private val isBubbleOnRight: Boolean,
    audioEnabled: Boolean,
    private val onStartRecording: () -> Unit,
    private val onGetMoreInfo: () -> Unit,
    private val onAudioToggle: (Boolean) -> Unit,
    private val onDismiss: () -> Unit,
) : FrameLayout(context) {

    private val dp = context.resources.displayMetrics.density
    private val cardWidth = (200 * dp).toInt()
    private val rowHeight = (52 * dp).toInt()
    private val cornerRadius = 16 * dp
    private val nibWidth = (8 * dp).toInt()
    private val nibHeight = (12 * dp).toInt()
    private val gap = (4 * dp).toInt()

    private var isAudioEnabled = audioEnabled
    private val card: LinearLayout
    private val nibView: View

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

        // Dismiss on scrim tap
        setOnClickListener { onDismiss() }

        card = buildCard()
        nibView = ArrowNibView(context, pointsRight = isBubbleOnRight)

        val bubbleCenterY = bubbleTop + bubbleSize / 2
        val cardHeight = rowHeight * 3 + (1 * 2) // 3 rows + 2 dividers (1px each)
        val cardTop = (bubbleCenterY - cardHeight / 2).coerceIn((24 * dp).toInt(), context.resources.displayMetrics.heightPixels - cardHeight - (24 * dp).toInt())

        val cardLeft: Int
        val nibLeft: Int
        if (isBubbleOnRight) {
            cardLeft = bubbleLeft - gap - nibWidth - cardWidth
            nibLeft = cardLeft + cardWidth
        } else {
            val bubbleRight = bubbleLeft + bubbleSize
            nibLeft = bubbleRight + gap
            cardLeft = nibLeft + nibWidth
        }

        addView(card, LayoutParams(cardWidth, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            leftMargin = cardLeft
            topMargin = cardTop
        })

        val nibTop = bubbleCenterY - nibHeight / 2
        addView(nibView, LayoutParams(nibWidth, nibHeight).apply {
            gravity = Gravity.TOP or Gravity.START
            leftMargin = nibLeft
            topMargin = nibTop
        })

        // Entry animation
        card.pivotX = if (isBubbleOnRight) cardWidth.toFloat() else 0f
        card.pivotY = cardHeight / 2f
        card.scaleX = 0.8f
        card.scaleY = 0.8f
        card.alpha = 0f
        nibView.alpha = 0f

        post {
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(card, "scaleX", 0.8f, 1f),
                    ObjectAnimator.ofFloat(card, "scaleY", 0.8f, 1f),
                    ObjectAnimator.ofFloat(card, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(nibView, "alpha", 0f, 1f),
                )
                duration = 200
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }

    fun animateOut(onEnd: () -> Unit) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(card, "scaleX", 1f, 0.9f),
                ObjectAnimator.ofFloat(card, "scaleY", 1f, 0.9f),
                ObjectAnimator.ofFloat(card, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(nibView, "alpha", 1f, 0f),
            )
            duration = 150
            interpolator = DecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd()
                }
            })
            start()
        }
    }

    // ── Card builder ──

    private fun buildCard(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xE61A1A1A.toInt())
                cornerRadius = this@PopoverMenuView.cornerRadius
            }
            elevation = 8 * dp
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }

            addView(buildRow(
                icon = RecordIconView(context),
                label = "Start Recording",
                showChevron = true,
                onClick = { onStartRecording() },
            ))
            addView(buildDivider())
            addView(buildRow(
                icon = InfoIconView(context),
                label = "Get More Info",
                showChevron = true,
                onClick = { onGetMoreInfo() },
            ))
            addView(buildDivider())
            addView(buildAudioRow())
        }
    }

    private fun buildRow(icon: View, label: String, showChevron: Boolean, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), 0, (16 * dp).toInt(), 0)
            minimumHeight = rowHeight
            isClickable = true
            isFocusable = true

            // Ripple-like touch feedback
            val attrs = intArrayOf(android.R.attr.selectableItemBackground)
            val ta = context.obtainStyledAttributes(attrs)
            foreground = ta.getDrawable(0)
            ta.recycle()

            val iconSize = (20 * dp).toInt()
            addView(icon, LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = (12 * dp).toInt()
            })

            addView(TextView(context).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            if (showChevron) {
                addView(TextView(context).apply {
                    text = "›"
                    setTextColor(0x80FFFFFF.toInt())
                    textSize = 18f
                })
            }

            setOnClickListener { onClick() }
        }
    }

    private fun buildAudioRow(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), 0, (16 * dp).toInt(), 0)
            minimumHeight = rowHeight

            val iconSize = (20 * dp).toInt()
            addView(MicIconView(context), LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = (12 * dp).toInt()
            })

            addView(TextView(context).apply {
                text = "Audio"
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val toggle = PillToggleView(context, isAudioEnabled) { enabled ->
                isAudioEnabled = enabled
                onAudioToggle(enabled)
            }
            addView(toggle, LinearLayout.LayoutParams(
                (42 * dp).toInt(), (24 * dp).toInt(),
            ))

            setOnClickListener {
                toggle.toggle()
            }
        }
    }

    private fun buildDivider(): View {
        return View(context).apply {
            setBackgroundColor(0x1AFFFFFF.toInt())
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1,
            ).apply {
                marginStart = (16 * dp).toInt()
                marginEnd = (16 * dp).toInt()
            }
        }
    }

    // ── Icon views ──

    private class RecordIconView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF3B30.toInt()
            style = Paint.Style.FILL
        }
        private var pulseAlpha = 255

        init {
            ValueAnimator.ofInt(255, 140, 255).apply {
                duration = 1200
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    pulseAlpha = it.animatedValue as Int
                    invalidate()
                }
                start()
            }
        }

        override fun onDraw(canvas: Canvas) {
            paint.alpha = pulseAlpha
            canvas.drawCircle(width / 2f, height / 2f, width * 0.4f, paint)
        }
    }

    private class InfoIconView(context: Context) : View(context) {
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0A84FF.toInt()
            style = Paint.Style.FILL
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawCircle(width / 2f, height / 2f, width * 0.45f, bgPaint)
            textPaint.textSize = height * 0.55f
            val yOffset = (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText("i", width / 2f, height / 2f - yOffset, textPaint)
        }
    }

    private class MicIconView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0A84FF.toInt()
        }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            // Mic body
            paint.style = Paint.Style.FILL
            val bodyW = width * 0.22f
            val bodyTop = height * 0.1f
            val bodyBottom = height * 0.5f
            canvas.drawRoundRect(cx - bodyW, bodyTop, cx + bodyW, bodyBottom, bodyW, bodyW, paint)
            // Arc
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = width * 0.08f
            val arcRect = RectF(cx - bodyW * 1.8f, bodyTop + height * 0.05f, cx + bodyW * 1.8f, bodyBottom + height * 0.12f)
            canvas.drawArc(arcRect, 0f, 180f, false, paint)
            // Stand
            paint.style = Paint.Style.FILL
            val standW = width * 0.04f
            canvas.drawRect(cx - standW, bodyBottom + height * 0.12f, cx + standW, height * 0.78f, paint)
            // Base
            canvas.drawRoundRect(cx - bodyW * 0.8f, height * 0.78f, cx + bodyW * 0.8f, height * 0.84f, 2f, 2f, paint)
        }
    }

    // ── Arrow nib ──

    private class ArrowNibView(context: Context, private val pointsRight: Boolean) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xE61A1A1A.toInt()
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            val path = Path()
            if (pointsRight) {
                path.moveTo(0f, 0f)
                path.lineTo(width.toFloat(), height / 2f)
                path.lineTo(0f, height.toFloat())
            } else {
                path.moveTo(width.toFloat(), 0f)
                path.lineTo(0f, height / 2f)
                path.lineTo(width.toFloat(), height.toFloat())
            }
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    // ── Pill toggle ──

    @SuppressLint("ViewConstructor")
    private class PillToggleView(
        context: Context,
        isChecked: Boolean,
        private val onToggle: (Boolean) -> Unit,
    ) : View(context) {

        var checked = isChecked
            private set

        private val dp = context.resources.displayMetrics.density
        private val trackW = 42 * dp
        private val trackH = 24 * dp
        private val thumbRadius = 9 * dp
        private val thumbPad = 3 * dp
        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        private var fraction = if (isChecked) 1f else 0f

        init {
            setOnClickListener { toggle() }
        }

        fun toggle() {
            checked = !checked
            onToggle(checked)
            val target = if (checked) 1f else 0f
            ValueAnimator.ofFloat(fraction, target).apply {
                duration = 200
                addUpdateListener { fraction = it.animatedValue as Float; invalidate() }
                start()
            }
        }

        override fun onMeasure(wSpec: Int, hSpec: Int) {
            setMeasuredDimension(trackW.toInt(), trackH.toInt())
        }

        override fun onDraw(canvas: Canvas) {
            trackPaint.color = lerpColor(0xFF636366.toInt(), 0xFF34C759.toInt(), fraction)
            canvas.drawRoundRect(RectF(0f, 0f, trackW, trackH), trackH / 2, trackH / 2, trackPaint)
            val minCx = thumbPad + thumbRadius
            val maxCx = trackW - thumbPad - thumbRadius
            canvas.drawCircle(minCx + (maxCx - minCx) * fraction, trackH / 2, thumbRadius, thumbPaint)
        }

        private fun lerpColor(a: Int, b: Int, f: Float): Int {
            fun ch(c: Int, shift: Int) = ((c shr shift and 0xFF) * (1 - f) + (b shr shift and 0xFF) * f).toInt()
            return (ch(a, 24) shl 24) or (ch(a, 16) shl 16) or (ch(a, 8) shl 8) or ch(a, 0)
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/PopoverMenuView.kt
git commit -m "feat(android): add PopoverMenuView with dark glassmorphic card, icons, pill toggle"
```

---

### Task 2: Update Android BubbleView with drag callback

**Files:**
- Modify: `screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/BubbleView.kt`

- [ ] **Step 1: Add `onDragStart` parameter**

Add a fourth constructor parameter `onDragStart: () -> Unit` and invoke it when dragging begins.

In the class header, add the parameter:
```kotlin
internal class BubbleView(
    context: Context,
    private val tintColor: Long,
    private val onRecordTap: () -> Unit,
    private val onStopTap: () -> Unit,
    private val onDragStart: () -> Unit,       // ← NEW
) : FrameLayout(context) {
```

In `onTouchEvent`, inside `ACTION_MOVE`, after setting `isDragging = true`:
```kotlin
if (!isDragging && (dx * dx + dy * dy) > 100) {
    isDragging = true
    onDragStart()                               // ← NEW
}
```

- [ ] **Step 2: Commit**

```bash
git add screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/BubbleView.kt
git commit -m "feat(android): add onDragStart callback to BubbleView"
```

---

### Task 3: Update Android BubbleManager for popover lifecycle

**Files:**
- Modify: `screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/BubbleManager.kt`

- [ ] **Step 1: Add new constructor params, popover state, and show/hide methods**

Replace the entire `BubbleManager` class with:

```kotlin
package com.screenrecorder.bubble

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager

internal class BubbleManager(
    private val context: Context,
    private val tintColor: Long,
    initialAudioEnabled: Boolean,
    private val onStartRecording: () -> Unit,
    private val onStopTap: () -> Unit,
    private val onGetMoreInfo: () -> Unit,
    private val onAudioToggle: (Boolean) -> Unit,
) {
    private var windowManager: WindowManager? = null
    private var bubbleView: BubbleView? = null
    private var popoverView: PopoverMenuView? = null
    private var isAttached = false
    private var isPopoverShown = false
    private var currentAudioEnabled = initialAudioEnabled

    private val dp = context.resources.displayMetrics.density
    private val bubbleSize = (44 * dp).toInt()

    fun attach() {
        if (isAttached) return
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        bubbleView = BubbleView(
            context, tintColor,
            onRecordTap = { showPopover() },
            onStopTap = onStopTap,
            onDragStart = { hidePopover() },
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 200
        }

        windowManager?.addView(bubbleView, params)
        isAttached = true
    }

    fun detach() {
        hidePopoverImmediate()
        if (!isAttached) return
        try { windowManager?.removeView(bubbleView) } catch (_: Exception) {}
        bubbleView = null
        isAttached = false
    }

    fun setRecording(isRecording: Boolean) {
        hidePopoverImmediate()
        bubbleView?.setRecording(isRecording)
    }

    fun updateDuration(durationMs: Long) {
        bubbleView?.updateDuration(durationMs)
    }

    fun showPopover() {
        if (isPopoverShown) return
        val bv = bubbleView ?: return
        val params = bv.layoutParams as? WindowManager.LayoutParams ?: return
        val screenWidth = context.resources.displayMetrics.widthPixels
        val isBubbleOnRight = params.x < screenWidth / 2
        val bubbleLeft = screenWidth - params.x - bubbleSize

        popoverView = PopoverMenuView(
            context = context,
            bubbleLeft = bubbleLeft,
            bubbleTop = params.y,
            bubbleSize = bubbleSize,
            isBubbleOnRight = isBubbleOnRight,
            audioEnabled = currentAudioEnabled,
            onStartRecording = {
                hidePopoverImmediate()
                onStartRecording()
            },
            onGetMoreInfo = {
                hidePopoverImmediate()
                onGetMoreInfo()
            },
            onAudioToggle = { enabled ->
                currentAudioEnabled = enabled
                onAudioToggle(enabled)
            },
            onDismiss = { hidePopover() },
        )

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )

        windowManager?.addView(popoverView, overlayParams)
        isPopoverShown = true
    }

    fun hidePopover() {
        if (!isPopoverShown) return
        popoverView?.animateOut {
            removePopoverView()
        }
    }

    private fun hidePopoverImmediate() {
        if (!isPopoverShown) return
        removePopoverView()
    }

    private fun removePopoverView() {
        try { windowManager?.removeView(popoverView) } catch (_: Exception) {}
        popoverView = null
        isPopoverShown = false
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add screen-recorder/src/androidMain/kotlin/com/screenrecorder/bubble/BubbleManager.kt
git commit -m "feat(android): add popover show/hide lifecycle to BubbleManager"
```

---

### Task 4: Wire Android ScreenRecorder to use popover

**Files:**
- Modify: `screen-recorder/src/androidMain/kotlin/com/screenrecorder/api/ScreenRecorder.android.kt`

- [ ] **Step 1: Update BubbleManager construction in `show()`**

Replace the `bubbleManager` construction block (lines 111-126) with:

```kotlin
if (bubbleManager == null) {
    bubbleManager = BubbleManager(
        context = ctx.applicationContext,
        tintColor = config?.tintColor ?: 0xFFFF3B30,
        initialAudioEnabled = config?.audioEnabled ?: false,
        onStartRecording = {
            Log.d("ScreenRecorderSDK", "Record tap via popover, state=${orchestrator?.currentInternalState}")
            startRecordingFlow()
        },
        onStopTap = {
            Log.d("ScreenRecorderSDK", "Stop tap, state=${orchestrator?.currentInternalState}")
            orchestrator?.onBubbleTapStop()
            bubbleManager?.setRecording(false)
            application?.let { ScreenRecorderService.stop(it) }
        },
        onGetMoreInfo = {
            // No-op for now — placeholder for future
        },
        onAudioToggle = { enabled ->
            config?.audioEnabled = enabled
        },
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add screen-recorder/src/androidMain/kotlin/com/screenrecorder/api/ScreenRecorder.android.kt
git commit -m "feat(android): wire ScreenRecorder to popover callbacks"
```

---

### Task 5: Create iOS PopoverMenuView

**Files:**
- Create: `screen-recorder/ios/Sources/ScreenRecorderUI/PopoverMenuView.swift`

- [ ] **Step 1: Create PopoverMenuView.swift**

```swift
import UIKit

internal final class PopoverMenuView: UIView {

    private let cardView: UIVisualEffectView
    private let nibView: NibView
    private let audioSwitch = UISwitch()
    private var isAudioEnabled: Bool

    private let onStartRecording: () -> Void
    private let onGetMoreInfo: () -> Void
    private let onAudioToggle: (Bool) -> Void
    private let onDismiss: () -> Void

    private let cardWidth: CGFloat = 200
    private let rowHeight: CGFloat = 52

    init(
        bubbleCenter: CGPoint,
        isBubbleOnRight: Bool,
        audioEnabled: Bool,
        onStartRecording: @escaping () -> Void,
        onGetMoreInfo: @escaping () -> Void,
        onAudioToggle: @escaping (Bool) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.isAudioEnabled = audioEnabled
        self.onStartRecording = onStartRecording
        self.onGetMoreInfo = onGetMoreInfo
        self.onAudioToggle = onAudioToggle
        self.onDismiss = onDismiss

        let blurEffect = UIBlurEffect(style: .systemMaterialDark)
        cardView = UIVisualEffectView(effect: blurEffect)
        nibView = NibView(pointsRight: isBubbleOnRight)

        super.init(frame: UIScreen.main.bounds)
        backgroundColor = .clear

        // Dismiss tap on scrim
        let tap = UITapGestureRecognizer(target: self, action: #selector(scrimTapped(_:)))
        tap.cancelsTouchesInView = false
        addGestureRecognizer(tap)

        setupCard()
        layoutCardAndNib(bubbleCenter: bubbleCenter, isBubbleOnRight: isBubbleOnRight)
        animateIn()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) not supported") }

    // MARK: - Card setup

    private func setupCard() {
        cardView.layer.cornerRadius = 16
        cardView.clipsToBounds = true

        let stack = UIStackView()
        stack.axis = .vertical
        stack.translatesAutoresizingMaskIntoConstraints = false
        cardView.contentView.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: cardView.contentView.topAnchor),
            stack.leadingAnchor.constraint(equalTo: cardView.contentView.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: cardView.contentView.trailingAnchor),
            stack.bottomAnchor.constraint(equalTo: cardView.contentView.bottomAnchor),
        ])

        stack.addArrangedSubview(buildRow(
            icon: buildRecordIcon(),
            label: "Start Recording",
            showChevron: true,
            action: #selector(startRecordingTapped)
        ))
        stack.addArrangedSubview(buildDivider())
        stack.addArrangedSubview(buildRow(
            icon: buildInfoIcon(),
            label: "Get More Info",
            showChevron: true,
            action: #selector(getMoreInfoTapped)
        ))
        stack.addArrangedSubview(buildDivider())
        stack.addArrangedSubview(buildAudioRow())
    }

    private func buildRow(icon: UIView, label: String, showChevron: Bool, action: Selector) -> UIView {
        let row = UIButton(type: .system)
        row.addTarget(self, action: action, for: .touchUpInside)
        row.translatesAutoresizingMaskIntoConstraints = false
        row.heightAnchor.constraint(equalToConstant: rowHeight).isActive = true

        let hStack = UIStackView()
        hStack.axis = .horizontal
        hStack.alignment = .center
        hStack.spacing = 12
        hStack.isUserInteractionEnabled = false
        hStack.translatesAutoresizingMaskIntoConstraints = false
        row.addSubview(hStack)
        NSLayoutConstraint.activate([
            hStack.leadingAnchor.constraint(equalTo: row.leadingAnchor, constant: 16),
            hStack.trailingAnchor.constraint(equalTo: row.trailingAnchor, constant: -16),
            hStack.centerYAnchor.constraint(equalTo: row.centerYAnchor),
        ])

        icon.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            icon.widthAnchor.constraint(equalToConstant: 20),
            icon.heightAnchor.constraint(equalToConstant: 20),
        ])
        hStack.addArrangedSubview(icon)

        let labelView = UILabel()
        labelView.text = label
        labelView.textColor = .white
        labelView.font = .systemFont(ofSize: 15, weight: .semibold)
        hStack.addArrangedSubview(labelView)

        if showChevron {
            let chevron = UILabel()
            chevron.text = "›"
            chevron.textColor = UIColor.white.withAlphaComponent(0.5)
            chevron.font = .systemFont(ofSize: 18)
            chevron.setContentHuggingPriority(.required, for: .horizontal)
            hStack.addArrangedSubview(chevron)
        }

        return row
    }

    private func buildAudioRow() -> UIView {
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

        let micIcon = buildMicIcon()
        micIcon.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            micIcon.widthAnchor.constraint(equalToConstant: 20),
            micIcon.heightAnchor.constraint(equalToConstant: 20),
        ])
        hStack.addArrangedSubview(micIcon)

        let label = UILabel()
        label.text = "Audio"
        label.textColor = .white
        label.font = .systemFont(ofSize: 15, weight: .semibold)
        hStack.addArrangedSubview(label)

        audioSwitch.isOn = isAudioEnabled
        audioSwitch.onTintColor = UIColor(red: 0.2, green: 0.78, blue: 0.35, alpha: 1)
        audioSwitch.transform = CGAffineTransform(scaleX: 0.75, y: 0.75)
        audioSwitch.addTarget(self, action: #selector(audioToggled), for: .valueChanged)
        audioSwitch.setContentHuggingPriority(.required, for: .horizontal)
        hStack.addArrangedSubview(audioSwitch)

        // Tap entire row to toggle
        let rowTap = UITapGestureRecognizer(target: self, action: #selector(audioRowTapped))
        container.addGestureRecognizer(rowTap)

        return container
    }

    private func buildDivider() -> UIView {
        let wrapper = UIView()
        wrapper.translatesAutoresizingMaskIntoConstraints = false
        wrapper.heightAnchor.constraint(equalToConstant: 1).isActive = true

        let line = UIView()
        line.backgroundColor = UIColor.white.withAlphaComponent(0.1)
        line.translatesAutoresizingMaskIntoConstraints = false
        wrapper.addSubview(line)
        NSLayoutConstraint.activate([
            line.leadingAnchor.constraint(equalTo: wrapper.leadingAnchor, constant: 16),
            line.trailingAnchor.constraint(equalTo: wrapper.trailingAnchor, constant: -16),
            line.topAnchor.constraint(equalTo: wrapper.topAnchor),
            line.bottomAnchor.constraint(equalTo: wrapper.bottomAnchor),
        ])
        return wrapper
    }

    // MARK: - Icons (SF Symbols)

    private func buildRecordIcon() -> UIView {
        let config = UIImage.SymbolConfiguration(pointSize: 14, weight: .bold)
        let imageView = UIImageView(image: UIImage(systemName: "circle.fill", withConfiguration: config))
        imageView.tintColor = UIColor(red: 1, green: 0.23, blue: 0.19, alpha: 1)

        // Pulse animation
        let pulse = CABasicAnimation(keyPath: "opacity")
        pulse.fromValue = 1.0
        pulse.toValue = 0.4
        pulse.duration = 0.8
        pulse.autoreverses = true
        pulse.repeatCount = .infinity
        imageView.layer.add(pulse, forKey: "pulse")

        return imageView
    }

    private func buildInfoIcon() -> UIView {
        let config = UIImage.SymbolConfiguration(pointSize: 14, weight: .bold)
        let imageView = UIImageView(image: UIImage(systemName: "info.circle.fill", withConfiguration: config))
        imageView.tintColor = UIColor(red: 0.04, green: 0.52, blue: 1, alpha: 1)
        return imageView
    }

    private func buildMicIcon() -> UIView {
        let config = UIImage.SymbolConfiguration(pointSize: 14, weight: .bold)
        let imageView = UIImageView(image: UIImage(systemName: "mic.fill", withConfiguration: config))
        imageView.tintColor = UIColor(red: 0.04, green: 0.52, blue: 1, alpha: 1)
        return imageView
    }

    // MARK: - Layout

    private func layoutCardAndNib(bubbleCenter: CGPoint, isBubbleOnRight: Bool) {
        let screen = UIScreen.main.bounds
        let cardHeight = rowHeight * 3 + 2 // 3 rows + 2 dividers
        let bubbleRadius: CGFloat = 22 // bubbleSize / 2
        let nibSize: CGFloat = 8
        let gap: CGFloat = 4

        let cardY = max(24, min(bubbleCenter.y - cardHeight / 2, screen.height - cardHeight - 24))

        let cardX: CGFloat
        let nibX: CGFloat
        if isBubbleOnRight {
            cardX = bubbleCenter.x - bubbleRadius - gap - nibSize - cardWidth
            nibX = cardX + cardWidth
        } else {
            nibX = bubbleCenter.x + bubbleRadius + gap
            cardX = nibX + nibSize
        }

        cardView.frame = CGRect(x: cardX, y: cardY, width: cardWidth, height: cardHeight)
        addSubview(cardView)

        let nibY = bubbleCenter.y - 6
        nibView.frame = CGRect(x: nibX, y: nibY, width: nibSize, height: 12)
        nibView.backgroundColor = .clear
        addSubview(nibView)
    }

    // MARK: - Animations

    private func animateIn() {
        cardView.transform = CGAffineTransform(scaleX: 0.8, y: 0.8)
        cardView.alpha = 0
        nibView.alpha = 0

        UIView.animate(withDuration: 0.2, delay: 0, options: .curveEaseOut) {
            self.cardView.transform = .identity
            self.cardView.alpha = 1
            self.nibView.alpha = 1
        }
    }

    func animateOut(completion: @escaping () -> Void) {
        UIView.animate(withDuration: 0.15, delay: 0, options: .curveEaseIn, animations: {
            self.cardView.transform = CGAffineTransform(scaleX: 0.9, y: 0.9)
            self.cardView.alpha = 0
            self.nibView.alpha = 0
        }) { _ in
            completion()
        }
    }

    // MARK: - Actions

    @objc private func scrimTapped(_ gesture: UITapGestureRecognizer) {
        let location = gesture.location(in: self)
        if !cardView.frame.contains(location) {
            onDismiss()
        }
    }

    @objc private func startRecordingTapped() { onStartRecording() }
    @objc private func getMoreInfoTapped() { onGetMoreInfo() }

    @objc private func audioToggled() {
        isAudioEnabled = audioSwitch.isOn
        onAudioToggle(isAudioEnabled)
    }

    @objc private func audioRowTapped() {
        audioSwitch.setOn(!audioSwitch.isOn, animated: true)
        audioToggled()
    }

    // MARK: - Arrow nib

    private class NibView: UIView {
        let pointsRight: Bool

        init(pointsRight: Bool) {
            self.pointsRight = pointsRight
            super.init(frame: .zero)
            isOpaque = false
        }

        required init?(coder: NSCoder) { fatalError() }

        override func draw(_ rect: CGRect) {
            guard let ctx = UIGraphicsGetCurrentContext() else { return }
            // Match the blur card's dark tint approximately
            ctx.setFillColor(UIColor(white: 0.12, alpha: 0.9).cgColor)

            let path = UIBezierPath()
            if pointsRight {
                path.move(to: CGPoint(x: 0, y: 0))
                path.addLine(to: CGPoint(x: rect.maxX, y: rect.midY))
                path.addLine(to: CGPoint(x: 0, y: rect.maxY))
            } else {
                path.move(to: CGPoint(x: rect.maxX, y: 0))
                path.addLine(to: CGPoint(x: 0, y: rect.midY))
                path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
            }
            path.close()
            ctx.addPath(path.cgPath)
            ctx.fillPath()
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add screen-recorder/ios/Sources/ScreenRecorderUI/PopoverMenuView.swift
git commit -m "feat(ios): add PopoverMenuView with blur card, SF Symbol icons, and UISwitch toggle"
```

---

### Task 6: Update iOS BubbleViewController for popover flow

**Files:**
- Modify: `screen-recorder/ios/Sources/ScreenRecorderUI/BubbleViewController.swift`

- [ ] **Step 1: Add popover state property**

Add after the existing properties (after `private let bubbleSize: CGFloat = 44`):
```swift
private var popoverView: PopoverMenuView?
```

- [ ] **Step 2: Replace `bubbleTapped()` method**

Replace the existing `bubbleTapped()` (lines 72-79) with:
```swift
@objc private func bubbleTapped() {
    if isRecording {
        ScreenRecorder.companion.onBubbleTapStop()
        setRecording(false)
    } else {
        if popoverView != nil {
            dismissPopover()
        } else {
            showPopover()
        }
    }
}
```

- [ ] **Step 3: Add popover show/dismiss methods**

Add these methods after `bubbleTapped()`:
```swift
private func showPopover() {
    guard popoverView == nil else { return }

    let screen = UIScreen.main.bounds
    let isBubbleOnRight = bubbleButton.center.x > screen.width / 2

    let popover = PopoverMenuView(
        bubbleCenter: bubbleButton.center,
        isBubbleOnRight: isBubbleOnRight,
        audioEnabled: config.audioEnabled,
        onStartRecording: { [weak self] in
            self?.dismissPopoverImmediate()
            ScreenRecorder.companion.onBubbleTapRecord()
            self?.setRecording(true)
        },
        onGetMoreInfo: { [weak self] in
            self?.dismissPopover()
            // No-op for now
        },
        onAudioToggle: { [weak self] enabled in
            self?.config.audioEnabled = enabled
        },
        onDismiss: { [weak self] in
            self?.dismissPopover()
        }
    )

    view.addSubview(popover)
    popoverView = popover
}

private func dismissPopover() {
    popoverView?.animateOut { [weak self] in
        self?.popoverView?.removeFromSuperview()
        self?.popoverView = nil
    }
}

private func dismissPopoverImmediate() {
    popoverView?.removeFromSuperview()
    popoverView = nil
}
```

- [ ] **Step 4: Dismiss popover on drag**

In `handleDrag(_:)`, add at the top of the `.changed` case (before the center update):
```swift
case .changed:
    if popoverView != nil {
        dismissPopoverImmediate()
    }
    bubbleButton.center = CGPoint(
```

- [ ] **Step 5: Dismiss popover when recording state changes**

In `setRecording(_:)`, add at the top:
```swift
func setRecording(_ recording: Bool) {
    dismissPopoverImmediate()
    isRecording = recording
```

- [ ] **Step 6: Commit**

```bash
git add screen-recorder/ios/Sources/ScreenRecorderUI/BubbleViewController.swift
git commit -m "feat(ios): wire BubbleViewController to show popover on idle tap"
```

---

### Task 7: Build verification and final commit

- [ ] **Step 1: Verify Android builds**

```bash
cd screen-recorder && ../gradlew :screen-recorder:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify iOS builds**

```bash
cd screen-recorder/ios && swift build
```

Expected: Build complete

- [ ] **Step 3: Final commit if any fixes needed**

```bash
git add -A && git commit -m "fix: resolve build issues for popover menu"
```
