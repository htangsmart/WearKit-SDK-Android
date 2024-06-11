package com.topstep.wearkit.sample.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import com.github.kilnn.tool.ui.DisplayUtil
import com.topstep.wearkit.sample.R

class MeasureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val primaryColor: Int
    private val alpha1Color: Int
    private val alpha2Color: Int
    var isMeasuring = false
        set(value) {
            field = value
            progress = 0f
            invalidate()
        }
    var progress: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val outCirclePadding: Int
    private val outCircleWidth: Int
    private val outerCircleRect: RectF
    private val outerCirclePath: Path
    private val dashPathEffect: DashPathEffect

    init {
        setWillNotDraw(false)
        val a = context.obtainStyledAttributes(attrs, R.styleable.MeasureView, defStyleAttr, 0)
        primaryColor = a.getColor(R.styleable.MeasureView_primaryColor, Color.RED)
        val alpha = Color.alpha(primaryColor)
        alpha1Color = ColorUtils.setAlphaComponent(primaryColor, (alpha * 0.7).toInt())
        alpha2Color = ColorUtils.setAlphaComponent(primaryColor, (alpha * 0.2).toInt())
        a.recycle()
        outCirclePadding = DisplayUtil.dip2px(context, 6f)
        outCircleWidth = DisplayUtil.dip2px(context, 6f)
        paint.strokeWidth = outCircleWidth.toFloat()
        outerCircleRect = RectF()
        outerCirclePath = Path()
        dashPathEffect = DashPathEffect(
            floatArrayOf(
                DisplayUtil.dip2px(getContext(), 2f).toFloat(),
                DisplayUtil.dip2px(getContext(), 1f)
                    .toFloat()
            ), 0f
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.style = Paint.Style.FILL
        paint.color = primaryColor
        paint.shader = LinearGradient(
            0f, paddingTop.toFloat(), 0f, height.toFloat() - paddingBottom,
            alpha1Color, primaryColor, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(width / 2.0f, height / 2.0f, width / 2.0f - outCircleWidth - outCirclePadding, paint)

        if (isMeasuring) {
            //绘制进度条底部颜色
            paint.color = alpha2Color
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND

            canvas.drawArc(outerCircleRect, -90f, 360f, false, paint)

            //绘制进度条颜色
            paint.color = primaryColor
            val progressAngle = (progress * 360).coerceAtMost(360f)
            if (progressAngle >= 360) { //画整个进度条。
                canvas.drawArc(outerCircleRect, -90f, progressAngle, false, paint)
            } else {
                //因为画笔宽度比较大，导致在绘制圆弧的时候，会超出角度，所以通过计算画笔宽度和圆周长的比率，绘制时偏移一部分角度
                val strokeAngle: Float = (outCircleWidth.toFloat() / (2 * Math.PI * outerCircleRect.width() / 2) * 360).toFloat()
                if (progressAngle > strokeAngle) { //当达到一定进度的时候才绘制，否则可能进度先从负数开始，效果不好
                    canvas.drawArc(outerCircleRect, -90 + strokeAngle / 2 + progressAngle, 360 - progressAngle, false, paint)
                }
            }
        } else {
            //绘制外部虚线
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.BUTT
            paint.pathEffect = dashPathEffect
            outerCirclePath.reset()
            outerCirclePath.addOval(outerCircleRect, Path.Direction.CW)
            canvas.drawPath(outerCirclePath, paint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var size = measuredWidth.coerceAtLeast(measuredHeight)
        size += outCirclePadding * 2 + outCircleWidth * 2
        setMeasuredDimension(size, size)
        val halfWidth = outCircleWidth / 2.0f
        outerCircleRect.set(halfWidth, halfWidth, size - halfWidth, size - halfWidth)
    }
}