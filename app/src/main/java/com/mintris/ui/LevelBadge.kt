package com.mintris.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Custom view for displaying the player's level in a fancy badge style
 */
class LevelBadge @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val badgePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 48f
        isFakeBoldText = true
    }

    private var level = 1
    private var themeColor = Color.WHITE

    fun setLevel(newLevel: Int) {
        level = newLevel
        invalidate()
    }

    fun setThemeColor(color: Int) {
        themeColor = color
        badgePaint.color = color
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Adjust text size based on view size
        textPaint.textSize = h * 0.6f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw badge circle
        val radius = (width.coerceAtMost(height) / 2f) * 0.9f
        canvas.drawCircle(width / 2f, height / 2f, radius, badgePaint)

        // Draw level text
        canvas.drawText(
            level.toString(),
            width / 2f,
            height / 2f + (textPaint.textSize / 3),
            textPaint
        )
    }
} 