package com.cute.battery

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a little animal-shaped battery that fills up with the battery level.
 *  - dot eyes normally, ^ ^ happy eyes while charging, sleepy eyes when low
 *  - blush cheeks and a "w" mouth
 *  - optional percentage text and background pill
 *
 * All shapes are drawn in a square of side `s`, using fractions of `s`,
 * so the icon looks the same at any size.
 */
class CuteBatteryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var settings: OverlaySettings = OverlaySettings()
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var level: Int = 75
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    var charging: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE6FFFFFF.toInt() }
    private val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK }
    private val faceStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99FF8FAB.toInt() }
    private val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFD84D.toInt() }
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val irisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE9E1F2.toInt() }
    private val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFC9D6.toInt() }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.LEFT
    }

    private val tmpRect = RectF()

    // ---------- sizing ----------

    private val hasPill get() = settings.pill != OverlaySettings.PILL_NONE

    private fun contentSize(h: Float) = if (hasPill) h * 0.8f else h
    private fun padX(h: Float) = if (hasPill) h * 0.35f else 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val size = MeasureSpec.getSize(heightMeasureSpec)
        val default = (24 * density).toInt()
        val h = when (mode) {
            MeasureSpec.EXACTLY -> size
            MeasureSpec.AT_MOST -> min(default, size)
            else -> default
        }
        val hf = h.toFloat()
        val c = contentSize(hf)
        var w = padX(hf) * 2 + c
        if (settings.showPercent) {
            textPaint.textSize = c * 0.62f
            w += c * 0.15f + textPaint.measureText("100%")
        }
        setMeasuredDimension(resolveSize(w.toInt() + 1, widthMeasureSpec), h)
    }

    // ---------- drawing ----------

    override fun onDraw(canvas: Canvas) {
        val h = height.toFloat()
        if (h <= 0f) return
        val c = contentSize(h)
        val px = padX(h)
        val top = (h - c) / 2f
        val ink = if (settings.lightOutline) Color.WHITE else INK

        if (hasPill) {
            pillPaint.color =
                if (settings.pill == OverlaySettings.PILL_BLACK) Color.BLACK else Color.WHITE
            tmpRect.set(0f, 0f, width.toFloat(), h)
            canvas.drawRoundRect(tmpRect, h / 2f, h / 2f, pillPaint)
        }

        canvas.save()
        canvas.translate(px, top)
        drawCritter(canvas, c, ink)
        canvas.restore()

        if (settings.showPercent) {
            textPaint.textSize = c * 0.62f
            textPaint.color = ink
            val fm = textPaint.fontMetrics
            val baseline = top + c / 2f - (fm.ascent + fm.descent) / 2f
            canvas.drawText("$level%", px + c + c * 0.15f, baseline, textPaint)
        }
    }

    private fun drawCritter(canvas: Canvas, s: Float, ink: Int) {
        if (settings.style == OverlaySettings.STYLE_EYE) {
            drawEye(canvas, s, ink)
            return
        }
        val shape = buildShape(settings.style, s)
        shape.computeBounds(tmpRect, true)
        val bounds = RectF(tmpRect)

        // empty part
        canvas.drawPath(shape, bgPaint)

        // filled part, rising from the bottom
        canvas.save()
        canvas.clipPath(shape)
        levelPaint.color = levelColor()
        val fillTop = bounds.bottom - bounds.height() * level / 100f
        canvas.drawRect(bounds.left, fillTop, bounds.right, bounds.bottom, levelPaint)
        canvas.restore()

        // outline
        outlinePaint.color = ink
        outlinePaint.strokeWidth = max(1.5f, s * 0.07f)
        canvas.drawPath(shape, outlinePaint)

        drawFace(canvas, s, faceY(settings.style))

        if (charging) drawBolt(canvas, s)
    }

    /**
     * Original anime-style eye: the iris fills up with the battery level,
     * the pupil turns into a star while charging, and the lid droops when low.
     */
    private fun drawEye(canvas: Canvas, s: Float, ink: Int) {
        val eye = Path().apply {
            moveTo(0.04f * s, 0.56f * s)
            quadTo(0.5f * s, 0.06f * s, 0.96f * s, 0.56f * s)
            quadTo(0.5f * s, 1.0f * s, 0.04f * s, 0.56f * s)
            close()
        }
        val cx = 0.5f * s
        val cy = 0.55f * s
        val ir = 0.215f * s

        // white of the eye
        canvas.drawPath(eye, bgPaint)

        canvas.save()
        canvas.clipPath(eye)

        // iris, filled from the bottom by the battery level
        canvas.drawCircle(cx, cy, ir, irisPaint)
        canvas.save()
        canvas.clipPath(Path().apply { addCircle(cx, cy, ir, Path.Direction.CW) })
        levelPaint.color = levelColor()
        canvas.drawRect(cx - ir, cy + ir - 2f * ir * level / 100f, cx + ir, cy + ir, levelPaint)
        canvas.restore()
        faceStroke.strokeWidth = max(1f, s * 0.03f)
        canvas.drawCircle(cx, cy, ir, faceStroke)

        // pupil
        if (charging) {
            val star = starPath(cx, cy, 0.12f * s, 0.045f * s)
            canvas.drawPath(star, boltPaint)
            faceStroke.strokeWidth = max(1f, s * 0.025f)
            canvas.drawPath(star, faceStroke)
        } else {
            canvas.drawCircle(cx, cy, 0.085f * s, facePaint)
        }

        // anime sparkles
        canvas.drawCircle(cx - 0.08f * s, cy - 0.08f * s, 0.05f * s, sparklePaint)
        canvas.drawCircle(cx + 0.075f * s, cy + 0.08f * s, 0.025f * s, sparklePaint)

        // sleepy lid when the battery is low
        if (!charging && level <= 15) {
            canvas.drawRect(0f, 0f, s, 0.52f * s, lidPaint)
            faceStroke.strokeWidth = max(1f, s * 0.05f)
            canvas.drawLine(0f, 0.52f * s, s, 0.52f * s, faceStroke)
        }
        canvas.restore()

        // outline, with a bolder upper lid and lashes
        outlinePaint.color = ink
        outlinePaint.strokeWidth = max(1.5f, s * 0.06f)
        canvas.drawPath(eye, outlinePaint)
        val upperLid = Path().apply {
            moveTo(0.04f * s, 0.56f * s)
            quadTo(0.5f * s, 0.06f * s, 0.96f * s, 0.56f * s)
        }
        outlinePaint.strokeWidth = max(2f, s * 0.1f)
        canvas.drawPath(upperLid, outlinePaint)
        outlinePaint.strokeWidth = max(1.5f, s * 0.06f)
        canvas.drawLine(0.73f * s, 0.37f * s, 0.78f * s, 0.24f * s, outlinePaint)
        canvas.drawLine(0.85f * s, 0.45f * s, 0.93f * s, 0.35f * s, outlinePaint)
        canvas.drawLine(0.93f * s, 0.53f * s, 0.99f * s, 0.47f * s, outlinePaint)
    }

    private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): Path {
        val p = Path()
        for (i in 0 until 8) {
            val r = if (i % 2 == 0) outer else inner
            val a = Math.PI / 4 * i - Math.PI / 2
            val x = cx + (r * Math.cos(a)).toFloat()
            val y = cy + (r * Math.sin(a)).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    private fun levelColor(): Int {
        if (level <= 15) return LOW_RED
        return when (settings.color) {
            OverlaySettings.COLOR_PINK -> 0xFFFF9EC7.toInt()
            OverlaySettings.COLOR_LAVENDER -> 0xFFC3A6FF.toInt()
            OverlaySettings.COLOR_MINT -> 0xFF8EE3C8.toInt()
            else -> when {               // auto: green / yellow / red
                level <= 30 -> 0xFFFFC75F.toInt()
                else -> 0xFF7BE495.toInt()
            }
        }
    }

    private fun faceY(style: String) = when (style) {
        OverlaySettings.STYLE_BUNNY -> 0.70f
        OverlaySettings.STYLE_BEAR -> 0.62f
        OverlaySettings.STYLE_HEART -> 0.50f
        else -> 0.66f
    }

    private fun buildShape(style: String, s: Float): Path {
        val p = Path()
        when (style) {
            OverlaySettings.STYLE_BUNNY -> {
                p.addOval(RectF(0.12f * s, 0.40f * s, 0.88f * s, 0.97f * s), Path.Direction.CW)
                val ears = Path().apply {
                    addRoundRect(RectF(0.20f * s, 0.03f * s, 0.42f * s, 0.58f * s), 0.11f * s, 0.11f * s, Path.Direction.CW)
                    addRoundRect(RectF(0.58f * s, 0.03f * s, 0.80f * s, 0.58f * s), 0.11f * s, 0.11f * s, Path.Direction.CW)
                }
                p.op(ears, Path.Op.UNION)
            }
            OverlaySettings.STYLE_BEAR -> {
                p.addCircle(0.5f * s, 0.60f * s, 0.36f * s, Path.Direction.CW)
                val ears = Path().apply {
                    addCircle(0.21f * s, 0.28f * s, 0.14f * s, Path.Direction.CW)
                    addCircle(0.79f * s, 0.28f * s, 0.14f * s, Path.Direction.CW)
                }
                p.op(ears, Path.Op.UNION)
            }
            OverlaySettings.STYLE_HEART -> {
                p.moveTo(0.5f * s, 0.92f * s)
                p.cubicTo(-0.05f * s, 0.60f * s, 0.10f * s, 0.05f * s, 0.5f * s, 0.30f * s)
                p.cubicTo(0.90f * s, 0.05f * s, 1.05f * s, 0.60f * s, 0.5f * s, 0.92f * s)
                p.close()
            }
            else -> { // cat
                p.addRoundRect(RectF(0.10f * s, 0.30f * s, 0.90f * s, 0.95f * s), 0.28f * s, 0.28f * s, Path.Direction.CW)
                val ears = Path().apply {
                    moveTo(0.14f * s, 0.48f * s); lineTo(0.20f * s, 0.06f * s); lineTo(0.48f * s, 0.32f * s); close()
                    moveTo(0.86f * s, 0.48f * s); lineTo(0.80f * s, 0.06f * s); lineTo(0.52f * s, 0.32f * s); close()
                }
                p.op(ears, Path.Op.UNION)
            }
        }
        return p
    }

    private fun drawFace(canvas: Canvas, s: Float, fy: Float) {
        val r = 0.05f * s
        val cx = 0.5f * s
        val ey = fy * s
        val lx = cx - 0.13f * s
        val rx = cx + 0.13f * s
        faceStroke.strokeWidth = max(1f, s * 0.045f)

        // blush
        tmpRect.set(lx - r * 2.3f, ey + r * 1.3f, lx + r * 0.1f, ey + r * 2.5f)
        canvas.drawOval(tmpRect, blushPaint)
        tmpRect.set(rx - r * 0.1f, ey + r * 1.3f, rx + r * 2.3f, ey + r * 2.5f)
        canvas.drawOval(tmpRect, blushPaint)

        // eyes
        for (x in floatArrayOf(lx, rx)) {
            when {
                charging -> { // happy ^ ^
                    val eye = Path().apply {
                        moveTo(x - r * 1.2f, ey + r * 0.6f)
                        lineTo(x, ey - r * 0.6f)
                        lineTo(x + r * 1.2f, ey + r * 0.6f)
                    }
                    canvas.drawPath(eye, faceStroke)
                }
                level <= 15 -> canvas.drawLine(x - r * 1.2f, ey, x + r * 1.2f, ey, faceStroke) // sleepy
                else -> canvas.drawCircle(x, ey, r, facePaint)
            }
        }

        // "w" mouth
        val my = ey + r * 1.9f
        val mw = r * 1.1f
        val mouth = Path().apply {
            moveTo(cx - mw, my)
            quadTo(cx - mw / 2f, my + mw, cx, my)
            quadTo(cx + mw / 2f, my + mw, cx + mw, my)
        }
        faceStroke.strokeWidth = max(1f, s * 0.035f)
        canvas.drawPath(mouth, faceStroke)
    }

    private fun drawBolt(canvas: Canvas, s: Float) {
        val bx = 0.68f * s
        val by = 0.56f * s
        val w = 0.30f * s
        val h = 0.44f * s
        val bolt = Path().apply {
            moveTo(bx + 0.60f * w, by)
            lineTo(bx + 0.10f * w, by + 0.55f * h)
            lineTo(bx + 0.45f * w, by + 0.55f * h)
            lineTo(bx + 0.30f * w, by + h)
            lineTo(bx + 0.90f * w, by + 0.40f * h)
            lineTo(bx + 0.55f * w, by + 0.40f * h)
            close()
        }
        canvas.drawPath(bolt, boltPaint)
        faceStroke.strokeWidth = max(1f, s * 0.03f)
        canvas.drawPath(bolt, faceStroke)
    }

    companion object {
        private const val INK = 0xFF3D2C3A.toInt()
        private const val LOW_RED = 0xFFFF6B81.toInt()
    }
}
