package com.topstep.wearkit.sample.ui.dial.style

import android.graphics.*

internal object DanMuTextRenderer {
    const val DEFAULT_FONT_SIZE_PX = 32
    const val MIN_FONT_SIZE_PX = 12
    const val MAX_FONT_SIZE_PX = 96
    const val DEFAULT_WALK_SPEED_PX_PER_SEC = 60
    const val MIN_WALK_SPEED_PX_PER_SEC = 1
    const val MAX_WALK_SPEED_PX_PER_SEC = 200
    const val TEXT_PADDING = 4

    fun resolveTypeface(styleIndex: Int): Typeface {
        return when (styleIndex) {
            1 -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            2 -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            3 -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            else -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    }

    fun render(
        text: String,
        fontSizePx: Float,
        typeface: Typeface,
        textColor: Int = Color.WHITE,
    ): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = fontSizePx
            this.typeface = typeface
            this.color = textColor
        }
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val measuredWidth = (bounds.width() + TEXT_PADDING * 2).coerceAtLeast(1)
        val width = if (measuredWidth % 2 == 0) measuredWidth else measuredWidth + 1
        val height = (bounds.height() + TEXT_PADDING * 2).coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val x = -bounds.left.toFloat() + TEXT_PADDING
        val y = -bounds.top.toFloat() + TEXT_PADDING
        canvas.drawText(text, x, y, paint)
        return bitmap
    }
}
