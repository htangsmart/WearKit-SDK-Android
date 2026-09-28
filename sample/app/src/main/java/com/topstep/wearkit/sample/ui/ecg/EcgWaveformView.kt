package com.topstep.wearkit.sample.ui.ecg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

/**
 * 心电实时波形：深色网格 + 红色走纸式绘制（对齐交互原型擦除滚动）。
 * 采样值单位 µV；增益默认 10 mm/mV，走纸 25 mm/s。
 */
class EcgWaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4D4F")
        strokeWidth = 3.5f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(14, 255, 255, 255)
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val gridBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(33, 255, 255, 255)
        strokeWidth = 1.8f
        style = Paint.Style.STROKE
    }
    private val bgPaint = Paint().apply { color = Color.parseColor("#0A0A0A") }

    private var samplingRate = 250
    private var flatLine = false
    private var frozen = false

    /** 画布尚未就绪时暂存，补画后清空，不保留全量实时采样。 */
    private val pendingSamples = ArrayList<Int>(256)
    private val frozenSamples = ArrayList<Int>()
    private var drawCursorX = 0f
    private var lastY = Float.NaN
    private var gridBitmap: Bitmap? = null
    private var contentBitmap: Bitmap? = null
    private var contentCanvas: Canvas? = null

    private var scrollOffsetPx = 0f
    private var lastTouchX = 0f
    private var dragging = false

    private val path = Path()

    fun setSamplingRate(hz: Int) {
        if (hz > 0) samplingRate = hz
    }

    fun setFlatLine(flat: Boolean) {
        flatLine = flat
    }

    fun isFrozen(): Boolean = frozen

    fun resetLive() {
        frozen = false
        pendingSamples.clear()
        frozenSamples.clear()
        drawCursorX = 0f
        lastY = Float.NaN
        scrollOffsetPx = 0f
        recreateBuffers()
        invalidate()
    }

    fun appendSamples(samples: IntArray) {
        if (frozen || samples.isEmpty()) return
        if (contentCanvas == null) {
            for (uv in samples) {
                pendingSamples.add(uv)
            }
            return
        }
        drawNewSamples(samples)
        invalidate()
    }

    fun freeze(allSamples: List<Int>) {
        frozen = true
        pendingSamples.clear()
        frozenSamples.clear()
        frozenSamples.addAll(allSamples)
        scrollOffsetPx = 0f
        invalidate()
    }

    private fun pxPerSample(): Float {
        val mmPerSec = 25f
        val pxPerMm = resources.displayMetrics.xdpi / 25.4f
        return (mmPerSec * pxPerMm) / samplingRate
    }

    private fun uvToY(uv: Int, height: Int): Float {
        val mmPerMv = 10f
        val pxPerMm = resources.displayMetrics.ydpi / 25.4f
        val pxPerUv = (mmPerMv * pxPerMm) / 1000f
        val cy = height * 0.52f
        return cy - uv * pxPerUv
    }

    private fun recreateBuffers() {
        if (width <= 0 || height <= 0) return
        gridBitmap?.recycle()
        contentBitmap?.recycle()
        gridBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bmp ->
            val c = Canvas(bmp)
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            val small = 5.4f * resources.displayMetrics.density
            val big = small * 5f
            var x = 0f
            while (x < width) {
                c.drawLine(x, 0f, x, height.toFloat(), gridPaint)
                x += small
            }
            var y = 0f
            while (y < height) {
                c.drawLine(0f, y, width.toFloat(), y, gridPaint)
                y += small
            }
            x = 0f
            while (x < width) {
                c.drawLine(x, 0f, x, height.toFloat(), gridBoldPaint)
                x += big
            }
            y = 0f
            while (y < height) {
                c.drawLine(0f, y, width.toFloat(), y, gridBoldPaint)
                y += big
            }
        }
        contentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bmp ->
            contentCanvas = Canvas(bmp)
            gridBitmap?.let { contentCanvas?.drawBitmap(it, 0f, 0f, null) }
            val cy = height * 0.52f
            contentCanvas?.drawLine(0f, cy, width.toFloat(), cy, linePaint)
        }
        drawCursorX = 0f
        lastY = height * 0.52f
    }

    private fun drawNewSamples(samples: IntArray) {
        val canvas = contentCanvas ?: return
        val grid = gridBitmap ?: return
        val step = pxPerSample().coerceAtLeast(0.2f)
        val eraseW = 16f * resources.displayMetrics.density
        for (uv in samples) {
            val y = if (flatLine) height * 0.52f else uvToY(uv, height)
            val x0 = drawCursorX
            val x1 = drawCursorX + step
            // 擦除前方一小段网格
            canvas.drawBitmap(grid, null, android.graphics.RectF(x1, 0f, x1 + eraseW, height.toFloat()), null)
            if (!lastY.isNaN()) {
                canvas.drawLine(x0, lastY, x1, y, linePaint)
            }
            lastY = y
            drawCursorX = x1
            if (drawCursorX >= width) {
                drawCursorX = 0f
                lastY = Float.NaN
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recreateBuffers()
        if (!frozen) {
            flushPending()
        }
        invalidate()
    }

    private fun flushPending() {
        if (pendingSamples.isEmpty() || contentCanvas == null) return
        val samples = IntArray(pendingSamples.size) { pendingSamples[it] }
        pendingSamples.clear()
        drawNewSamples(samples)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (frozen) {
            drawFrozen(canvas)
        } else {
            val bmp = contentBitmap
            if (bmp != null) {
                canvas.drawBitmap(bmp, 0f, 0f, null)
            } else {
                canvas.drawColor(Color.parseColor("#0A0A0A"))
            }
        }
    }

    private fun drawFrozen(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#0A0A0A"))
        val small = 5.4f * resources.displayMetrics.density
        val big = small * 5f
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += small
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += small
        }
        x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridBoldPaint)
            x += big
        }
        y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridBoldPaint)
            y += big
        }

        if (frozenSamples.isEmpty()) return
        val step = pxPerSample().coerceAtLeast(0.2f)
        val totalWidth = frozenSamples.size * step
        val maxScroll = max(0f, totalWidth - width)
        scrollOffsetPx = scrollOffsetPx.coerceIn(0f, maxScroll)

        path.reset()
        var started = false
        for (i in frozenSamples.indices) {
            val px = i * step - scrollOffsetPx
            if (px < -step || px > width + step) continue
            val py = uvToY(frozenSamples[i], height)
            if (!started) {
                path.moveTo(px, py)
                started = true
            } else {
                path.lineTo(px, py)
            }
        }
        canvas.drawPath(path, linePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!frozen) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                dragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    val dx = event.x - lastTouchX
                    lastTouchX = event.x
                    scrollOffsetPx = max(0f, scrollOffsetPx - dx)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
