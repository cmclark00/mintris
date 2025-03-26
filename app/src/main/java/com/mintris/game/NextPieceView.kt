package com.mintris.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.BlurMaskFilter
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Custom view to display the next Tetromino piece
 */
class NextPieceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var gameView: GameView? = null
    
    // Rendering
    private val blockPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }
    
    private val glowPaint = Paint().apply {
        color = Color.WHITE
        alpha = 30
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    
    /**
     * Set the game view to get the next piece from
     */
    fun setGameView(gameView: GameView) {
        this.gameView = gameView
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Get the next piece from game view
        gameView?.let {
            it.getNextPiece()?.let { piece ->
                val width = piece.getWidth()
                val height = piece.getHeight()
                
                // Calculate block size for the preview (smaller than main board)
                val previewBlockSize = min(
                    canvas.width.toFloat() / (width + 2),
                    canvas.height.toFloat() / (height + 2)
                )
                
                // Center the piece in the preview area
                val previewLeft = (canvas.width - width * previewBlockSize) / 2
                val previewTop = (canvas.height - height * previewBlockSize) / 2
                
                // Draw subtle background glow
                val glowPaint = Paint().apply {
                    color = Color.WHITE
                    alpha = 10
                    maskFilter = BlurMaskFilter(previewBlockSize * 0.5f, BlurMaskFilter.Blur.OUTER)
                }
                canvas.drawRect(
                    previewLeft - previewBlockSize,
                    previewTop - previewBlockSize,
                    previewLeft + width * previewBlockSize + previewBlockSize,
                    previewTop + height * previewBlockSize + previewBlockSize,
                    glowPaint
                )
                
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        if (piece.isBlockAt(x, y)) {
                            val left = previewLeft + x * previewBlockSize
                            val top = previewTop + y * previewBlockSize
                            val right = left + previewBlockSize
                            val bottom = top + previewBlockSize
                            
                            // Draw block with subtle glow
                            val rect = RectF(left + 1, top + 1, right - 1, bottom - 1)
                            canvas.drawRect(rect, blockPaint)
                            
                            // Draw subtle border glow
                            val glowRect = RectF(left, top, right, bottom)
                            canvas.drawRect(glowRect, glowPaint)
                        }
                    }
                }
            }
        }
    }
} 