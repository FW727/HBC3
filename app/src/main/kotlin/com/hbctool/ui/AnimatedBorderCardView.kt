package com.hbctool.ui

import com.hbctool.R
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import kotlin.math.sin

class AnimatedBorderCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val dp = context.resources.displayMetrics.density
    private val corner = 20f * dp
    private val strokeW = 4f * dp
    private var isLoading = false

    private val borderRect = RectF()
    private val borderPath = Path()
    private val wigglePath = Path()
    private var pathLen = 0f
    private val samplePts = mutableListOf<Pair<FloatArray, FloatArray>>()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val tracePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = strokeW * 0.45f; color = 0x2200C8FF.toInt()
    }
    private val wormPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }

    private val gradColors = intArrayOf(
        0xFF00C8FF.toInt(), 0xFF7B3FFF.toInt(), 0xFF00E57A.toInt(), 0xFF00C8FF.toInt()
    )

    private var phase = 0f
    private var wiggle = 0f

    private val crawlAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2600L; repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { phase = it.animatedValue as Float; invalidate() }
    }
    private val wiggleAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 850L; repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { wiggle = it.animatedValue as Float; rebuildWiggle(); invalidate() }
    }

    init {
        setWillNotDraw(false); isClickable = true; isFocusable = true
        val p = (strokeW * 2f).toInt()
        setPadding(p, p, p, p)
    }

    fun setLoading(loading: Boolean) {
        if (isLoading == loading) return
        isLoading = loading
        if (loading) { crawlAnim.cancel(); wiggleAnim.start() }
        else { wiggleAnim.cancel(); crawlAnim.start() }
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isLoading) wiggleAnim.start() else crawlAnim.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        crawlAnim.cancel(); wiggleAnim.cancel()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val half = strokeW / 2f + 1f
        borderRect.set(half, half, w - half, h - half)
        borderPath.reset()
        borderPath.addRoundRect(borderRect, corner, corner, Path.Direction.CW)
        val pm = PathMeasure(borderPath, false)
        pathLen = pm.length
        samplePts.clear()
        val n = 160
        repeat(n + 1) { i ->
            val pos = FloatArray(2); val tan = FloatArray(2)
            pm.getPosTan(i * pathLen / n, pos, tan)
            samplePts.add(pos to tan)
        }
        rebuildWiggle()
    }

    private fun rebuildWiggle() {
        if (samplePts.isEmpty()) return
        val amp = strokeW * 2.5f
        val freq = 5f
        wigglePath.reset()
        samplePts.forEachIndexed { i, (pos, tan) ->
            val t = i.toFloat() / samplePts.size
            val off = amp * sin(((t * freq - wiggle) * 2.0 * Math.PI).toFloat())
            val px = pos[0] + (-tan[1]) * off
            val py = pos[1] + tan[0] * off
            if (i == 0) wigglePath.moveTo(px, py) else wigglePath.lineTo(px, py)
        }
        wigglePath.close()
    }

    override fun onDraw(canvas: Canvas) {
        // Card bg: use the color resource that auto-switches light/dark
        bgPaint.color = context.resources.getColor(R.color.card_bg, context.theme)
        canvas.drawRoundRect(borderRect, corner, corner, bgPaint)
        if (pathLen <= 0f) return

        canvas.drawPath(borderPath, tracePaint)

        wormPaint.shader = SweepGradient(width / 2f, height / 2f, gradColors, null)
        if (isLoading) {
            wormPaint.strokeWidth = strokeW * 1.4f
            wormPaint.pathEffect = null
            canvas.drawPath(wigglePath, wormPaint)
        } else {
            wormPaint.strokeWidth = strokeW
            val wlen = pathLen * 0.35f
            wormPaint.pathEffect = DashPathEffect(floatArrayOf(wlen, pathLen - wlen), -(phase * pathLen))
            canvas.drawPath(borderPath, wormPaint)
        }
    }
}
