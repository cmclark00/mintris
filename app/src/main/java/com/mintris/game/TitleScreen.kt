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

class TitleScreen @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private val glowPaint = Paint()
    private val titlePaint = Paint()
    private val promptPaint = Paint()
    private val cellSize = 30f
    private val random = Random()
    private var width = 0
    private var height = 0
    private val tetrominosToAdd = mutableListOf<Tetromino>()
    
    // Callback for when the user touches the screen
    var onStartGame: (() -> Unit)? = null
    
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
            color = Color.WHITE
            textSize = 120f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        
        // "Touch to start" text settings
        promptPaint.apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
            alpha = 180
        }
        
        // General paint settings for tetrominos (white)
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Glow paint settings for tetrominos
        glowPaint.apply {
            color = Color.WHITE
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
            
            // Draw background
            canvas.drawColor(Color.BLACK)
            
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
                        // Save canvas state before rotation
                        canvas.save()
                        
                        // Translate to the tetromino's position
                        canvas.translate(tetromino.x, tetromino.y)
                        
                        // Scale according to the tetromino's scale factor
                        canvas.scale(tetromino.scale, tetromino.scale)
                        
                        // Rotate around the center of the tetromino
                        val centerX = tetromino.shape.size * cellSize / 2
                        val centerY = tetromino.shape.size * cellSize / 2
                        canvas.rotate(tetromino.rotation.toFloat(), centerX, centerY)
                        
                        // Draw the tetromino
                        for (row in tetromino.shape.indices) {
                            for (col in 0 until tetromino.shape[row].size) {
                                if (tetromino.shape[row][col] == 1) {
                                    // Draw larger glow effect
                                    glowPaint.alpha = 30
                                    canvas.drawRect(
                                        col * cellSize - 8,
                                        row * cellSize - 8,
                                        (col + 1) * cellSize + 8,
                                        (row + 1) * cellSize + 8,
                                        glowPaint
                                    )
                                    
                                    // Draw medium glow
                                    glowPaint.alpha = 60
                                    canvas.drawRect(
                                        col * cellSize - 4,
                                        row * cellSize - 4,
                                        (col + 1) * cellSize + 4,
                                        (row + 1) * cellSize + 4,
                                        glowPaint
                                    )
                                    
                                    // Draw main block
                                    canvas.drawRect(
                                        col * cellSize,
                                        row * cellSize,
                                        (col + 1) * cellSize,
                                        (row + 1) * cellSize,
                                        paint
                                    )
                                }
                            }
                        }
                        
                        // Restore canvas state
                        canvas.restore()
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
            
            // Draw "touch to start" prompt
            canvas.drawText("touch to start", width / 2f, height * 0.6f, promptPaint)
            
            // Request another frame
            invalidate()
        } catch (e: Exception) {
            Log.e("TitleScreen", "Error in onDraw", e)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            onStartGame?.invoke()
            return true
        }
        return super.onTouchEvent(event)
    }
} 