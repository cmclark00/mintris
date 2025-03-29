package com.mintris.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Random
import android.util.Log
import com.mintris.model.HighScoreManager
import com.mintris.model.HighScore
import com.mintris.model.PlayerProgressionManager
import kotlin.math.abs
import androidx.core.graphics.withTranslation
import androidx.core.graphics.withScale
import androidx.core.graphics.withRotation

class TitleScreen @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private val glowPaint = Paint()
    private val titlePaint = Paint()
    private val promptPaint = Paint()
    private val highScorePaint = Paint()
    private val cellSize = 30f
    private val random = Random()
    private var width = 0
    private var height = 0
    private val tetrominosToAdd = mutableListOf<Tetromino>()
    private val highScoreManager = HighScoreManager(context)  // Pre-allocate HighScoreManager
    
    // Touch handling variables
    private var startX = 0f
    private var startY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private val maxTapMovement = 20f  // Maximum movement allowed for a tap (in pixels)
    
    // Callback for when the user touches the screen
    var onStartGame: (() -> Unit)? = null

    // Theme color and background color
    private var themeColor = Color.WHITE
    private var backgroundColor = Color.BLACK
    
    // Define tetromino shapes (I, O, T, S, Z, J, L)
    private val tetrominoShapes = arrayOf(
        // I
        arrayOf(
            intArrayOf(0, 0, 0, 0),
            intArrayOf(1, 1, 1, 1),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        // O
        arrayOf(
            intArrayOf(1, 1),
            intArrayOf(1, 1)
        ),
        // T
        arrayOf(
            intArrayOf(0, 1, 0),
            intArrayOf(1, 1, 1),
            intArrayOf(0, 0, 0)
        ),
        // S
        arrayOf(
            intArrayOf(0, 1, 1),
            intArrayOf(1, 1, 0),
            intArrayOf(0, 0, 0)
        ),
        // Z
        arrayOf(
            intArrayOf(1, 1, 0),
            intArrayOf(0, 1, 1),
            intArrayOf(0, 0, 0)
        ),
        // J
        arrayOf(
            intArrayOf(1, 0, 0),
            intArrayOf(1, 1, 1),
            intArrayOf(0, 0, 0)
        ),
        // L
        arrayOf(
            intArrayOf(0, 0, 1),
            intArrayOf(1, 1, 1),
            intArrayOf(0, 0, 0)
        )
    )
    
    // Tetromino class to represent falling pieces
    private class Tetromino(
        var x: Float,
        var y: Float,
        val shape: Array<IntArray>,
        val speed: Float,
        val scale: Float,
        val rotation: Int = 0
    )
    
    private val tetrominos = mutableListOf<Tetromino>()
    
    init {
        // Title text settings
        titlePaint.apply {
            color = themeColor
            textSize = 120f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        
        // "Touch to start" text settings
        promptPaint.apply {
            color = themeColor
            textSize = 50f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
            alpha = 180
        }
        
        // High scores text settings
        highScorePaint.apply {
            color = themeColor
            textSize = 70f
            textAlign = Paint.Align.LEFT  // Changed to LEFT alignment
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)  // Changed to monospace
            isAntiAlias = true
            alpha = 200
        }
        
        // General paint settings for tetrominos
        paint.apply {
            color = themeColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Glow paint settings for tetrominos
        glowPaint.apply {
            color = themeColor
            style = Paint.Style.FILL
            isAntiAlias = true
            alpha = 60
        }
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        width = w
        height = h
        
        // Clear existing tetrominos
        tetrominos.clear()
        
        // Initialize some tetrominos
        repeat(20) {
            val tetromino = createRandomTetromino()
            tetrominos.add(tetromino)
        }
    }
    
    private fun createRandomTetromino(): Tetromino {
        val x = random.nextFloat() * (width - 150) + 50 // Keep away from edges
        val y = -cellSize * 4 - (random.nextFloat() * height / 2)
        val shapeIndex = random.nextInt(tetrominoShapes.size)
        val shape = tetrominoShapes[shapeIndex]
        val speed = 1f + random.nextFloat() * 2f
        val scale = 0.8f + random.nextFloat() * 0.4f
        val rotation = random.nextInt(4) * 90
        
        return Tetromino(x, y, shape, speed, scale, rotation)
    }
    
    override fun onDraw(canvas: Canvas) {
        try {
            super.onDraw(canvas)
            
            // Draw background using the current background color
            canvas.drawColor(backgroundColor)
            
            // Add any pending tetrominos
            tetrominos.addAll(tetrominosToAdd)
            tetrominosToAdd.clear()
            
            // Update and draw falling tetrominos
            val tetrominosToRemove = mutableListOf<Tetromino>()
            
            for (tetromino in tetrominos) {
                tetromino.y += tetromino.speed
                
                // Remove tetrominos that have fallen off the screen
                if (tetromino.y > height) {
                    tetrominosToRemove.add(tetromino)
                    tetrominosToAdd.add(createRandomTetromino())
                } else {
                    try {
                        // Draw the tetromino
                        for (y in 0 until tetromino.shape.size) {
                            for (x in 0 until tetromino.shape.size) {
                                if (tetromino.shape[y][x] == 1) {
                                    val left = x * cellSize
                                    val top = y * cellSize
                                    val right = left + cellSize
                                    val bottom = top + cellSize
                                    
                                    // Draw block with glow effect
                                    canvas.withTranslation(tetromino.x, tetromino.y) {
                                        withScale(tetromino.scale, tetromino.scale) {
                                            withRotation(tetromino.rotation.toFloat(), 
                                                tetromino.shape.size * cellSize / 2,
                                                tetromino.shape.size * cellSize / 2) {
                                                // Draw glow
                                                canvas.drawRect(left - 8f, top - 8f, right + 8f, bottom + 8f, glowPaint)
                                                // Draw block
                                                canvas.drawRect(left, top, right, bottom, paint)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TitleScreen", "Error drawing tetromino", e)
                    }
                }
            }
            
            // Remove tetrominos that fell off the screen
            tetrominos.removeAll(tetrominosToRemove)
            
            // Draw title
            val titleY = height * 0.4f
            canvas.drawText("mintris", width / 2f, titleY, titlePaint)
            
            // Draw high scores using pre-allocated manager
            val highScores: List<HighScore> = highScoreManager.getHighScores()
            val highScoreY = height * 0.5f
            if (highScores.isNotEmpty()) {
                // Calculate the starting X position to center the entire block of scores
                val maxScoreWidth = highScorePaint.measureText("99. PLAYER: 999999")
                val startX = (width - maxScoreWidth) / 2
                
                highScores.forEachIndexed { index: Int, score: HighScore ->
                    val y = highScoreY + (index * 80f)
                    // Pad the rank number to ensure alignment
                    val rank = (index + 1).toString().padStart(2, ' ')
                    // Pad the name to ensure score alignment
                    val paddedName = score.name.padEnd(8, ' ')
                    canvas.drawText("$rank. $paddedName ${score.score}", startX, y, highScorePaint)
                }
            }
            
            // Draw "touch to start" prompt
            canvas.drawText("touch to start", width / 2f, height * 0.7f, promptPaint)
            
            // Request another frame
            invalidate()
        } catch (e: Exception) {
            Log.e("TitleScreen", "Error in onDraw", e)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - lastTouchX
                val deltaY = event.y - lastTouchY
                
                // Update tetromino positions
                for (tetromino in tetrominos) {
                    tetromino.x += deltaX * 0.5f
                    tetromino.y += deltaY * 0.5f
                }
                
                lastTouchX = event.x
                lastTouchY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val deltaX = event.x - startX
                val deltaY = event.y - startY
                
                // If the movement was minimal, treat as a tap
                if (abs(deltaX) < maxTapMovement && abs(deltaY) < maxTapMovement) {
                    performClick()
                }
                
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        // Call the superclass's performClick
        super.performClick()
        
        // Handle the click event
        onStartGame?.invoke()
        return true
    }

    /**
     * Apply a theme to the title screen
     */
    fun applyTheme(themeId: String) {
        // Get theme color based on theme ID
        themeColor = when (themeId) {
            PlayerProgressionManager.THEME_CLASSIC -> Color.WHITE
            PlayerProgressionManager.THEME_NEON -> Color.parseColor("#FF00FF")
            PlayerProgressionManager.THEME_MONOCHROME -> Color.LTGRAY
            PlayerProgressionManager.THEME_RETRO -> Color.parseColor("#FF5A5F")
            PlayerProgressionManager.THEME_MINIMALIST -> Color.BLACK
            PlayerProgressionManager.THEME_GALAXY -> Color.parseColor("#66FCF1")
            else -> Color.WHITE
        }

        // Update paint colors
        titlePaint.color = themeColor
        promptPaint.color = themeColor
        highScorePaint.color = themeColor
        paint.color = themeColor
        glowPaint.color = themeColor

        // Update background color
        backgroundColor = when (themeId) {
            PlayerProgressionManager.THEME_CLASSIC -> Color.BLACK
            PlayerProgressionManager.THEME_NEON -> Color.parseColor("#0D0221")
            PlayerProgressionManager.THEME_MONOCHROME -> Color.parseColor("#1A1A1A")
            PlayerProgressionManager.THEME_RETRO -> Color.parseColor("#3F2832")
            PlayerProgressionManager.THEME_MINIMALIST -> Color.WHITE
            PlayerProgressionManager.THEME_GALAXY -> Color.parseColor("#0B0C10")
            else -> Color.BLACK
        }

        invalidate()
    }

    /**
     * Set the theme color for the title screen
     */
    fun setThemeColor(color: Int) {
        themeColor = color
        titlePaint.color = color
        promptPaint.color = color
        highScorePaint.color = color
        paint.color = color
        glowPaint.color = color
        invalidate()
    }
    
    /**
     * Set the background color for the title screen
     */
    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
        invalidate()
    }
} 