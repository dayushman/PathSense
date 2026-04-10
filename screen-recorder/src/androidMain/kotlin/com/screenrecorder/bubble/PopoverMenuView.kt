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
