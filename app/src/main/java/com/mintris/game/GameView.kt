package com.mintris.game

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.WindowManager
import android.view.Display
import android.hardware.display.DisplayManager
import com.mintris.model.GameBoard
import com.mintris.model.Tetromino
import com.mintris.model.TetrominoType
import kotlin.math.abs
import kotlin.math.min

/**
 * GameView that renders the Tetris game and handles touch input
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Game board model
    val gameBoard = GameBoard()
    
    // Game state
    private var isRunning = false
    private var isPaused = false
    
    // Rendering
    private val blockPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }
    
    private val ghostBlockPaint = Paint().apply {
        color = Color.WHITE
        alpha = 80  // 30% opacity
        isAntiAlias = true
    }
    
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#222222")  // Very dark gray
        alpha = 40
        isAntiAlias = true
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    
    private val glowPaint = Paint().apply {
        color = Color.WHITE
        alpha = 80
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private val borderGlowPaint = Paint().apply {
        color = Color.CYAN
        alpha = 120
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        setShadowLayer(10f, 0f, 0f, Color.CYAN)
    }
    
    private val lineClearPaint = Paint().apply {
        color = Color.WHITE
        alpha = 255
        isAntiAlias = true
    }
    
    // Animation
    private var lineClearAnimator: ValueAnimator? = null
    private var lineClearProgress = 0f
    private val lineClearDuration = 150L // milliseconds
    
    // Calculate block size based on view dimensions and board size
    private var blockSize = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    
    // Game loop handler and runnable
    private val handler = Handler(Looper.getMainLooper())
    private val gameLoopRunnable = object : Runnable {
        override fun run() {
            if (isRunning && !isPaused) {
                update()
                invalidate()
                handler.postDelayed(this, gameBoard.dropInterval)
            }
        }
    }
    
    // Touch parameters
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var startX = 0f
    private var startY = 0f
    private var lastTapTime = 0L
    private var lastRotationTime = 0L
    private var minSwipeVelocity = 800  // Minimum velocity for swipe to be considered a hard drop
    private val maxTapMovement = 20f    // Maximum movement allowed for a tap (in pixels)
    private val minTapTime = 100L       // Minimum time for a tap (in milliseconds)
    private val rotationCooldown = 150L // Minimum time between rotations (in milliseconds)
    
    // Callback for game events
    var onGameStateChanged: ((score: Int, level: Int, lines: Int) -> Unit)? = null
    var onGameOver: ((score: Int) -> Unit)? = null
    var onLineClear: ((Int) -> Unit)? = null // New callback for line clear events
    var onPieceMove: (() -> Unit)? = null // New callback for piece movement
    var onPieceLock: (() -> Unit)? = null // New callback for piece locking
    
    init {
        // Start with paused state
        pause()
        
        // Connect our callbacks to the GameBoard
        gameBoard.onPieceMove = { onPieceMove?.invoke() }
        gameBoard.onPieceLock = { onPieceLock?.invoke() }
        
        // Enable hardware acceleration
        setLayerType(LAYER_TYPE_HARDWARE, null)
        
        // Set better frame rate using modern APIs
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        display?.let { disp ->
            val refreshRate = disp.refreshRate
            // Set game loop interval based on refresh rate, but don't go faster than the base interval
            val targetFps = refreshRate.toInt()
            if (targetFps > 0) {
                gameBoard.dropInterval = gameBoard.dropInterval.coerceAtMost(1000L / targetFps)
            }
        }
        
        // Enable edge-to-edge rendering
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setSystemGestureExclusionRects(listOf(Rect(0, 0, width, height)))
        }
    }
    
    /**
     * Start the game
     */
    fun start() {
        if (isPaused || !isRunning) {
            isPaused = false
            if (!isRunning) {
                isRunning = true
                gameBoard.reset()
            }
            handler.post(gameLoopRunnable)
            invalidate()
        }
    }
    
    /**
     * Pause the game
     */
    fun pause() {
        isPaused = true
        handler.removeCallbacks(gameLoopRunnable)
        lineClearAnimator?.cancel()
        invalidate()
    }
    
    /**
     * Reset the game
     */
    fun reset() {
        isRunning = false
        isPaused = true
        gameBoard.reset()
        handler.removeCallbacks(gameLoopRunnable)
        lineClearAnimator?.cancel()
        invalidate()
    }
    
    /**
     * Update game state (called on game loop)
     */
    private fun update() {
        if (gameBoard.isGameOver) {
            isRunning = false
            isPaused = true
            onGameOver?.invoke(gameBoard.score)
            return
        }
        
        // Move the current tetromino down automatically
        gameBoard.moveDown()
        
        // Check if lines need to be cleared and start animation if needed
        if (gameBoard.linesToClear.isNotEmpty() && gameBoard.isLineClearAnimationInProgress) {
            // Trigger line clear callback for vibration
            onLineClear?.invoke(gameBoard.linesToClear.size)
            
            // Start line clearing animation if not already running
            if (lineClearAnimator == null || !lineClearAnimator!!.isRunning) {
                startLineClearAnimation()
            }
        }
        
        // Update UI with current game state
        onGameStateChanged?.invoke(gameBoard.score, gameBoard.level, gameBoard.lines)
    }
    
    /**
     * Start the line clearing animation
     */
    private fun startLineClearAnimation() {
        lineClearAnimator?.cancel()
        
        lineClearAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = lineClearDuration
            interpolator = LinearInterpolator()
            
            addUpdateListener { animator ->
                lineClearProgress = animator.animatedValue as Float
                invalidate()
            }
            
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // When animation completes, actually clear the lines
                    gameBoard.clearLinesFromGrid()
                    invalidate()
                }
            })
            
            start()
        }
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Enable hardware acceleration
        setLayerType(LAYER_TYPE_HARDWARE, null)
        
        // Update gesture exclusion rect for edge-to-edge rendering
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setSystemGestureExclusionRects(listOf(Rect(0, 0, w, h)))
        }
        
        calculateDimensions(w, h)
    }
    
    /**
     * Calculate dimensions for the board and blocks based on view size
     */
    private fun calculateDimensions(width: Int, height: Int) {
        // Calculate block size based on available space
        val horizontalBlocks = gameBoard.width
        val verticalBlocks = gameBoard.height
        
        // Calculate block size to fit within the view
        blockSize = min(
            width.toFloat() / horizontalBlocks,
            height.toFloat() / verticalBlocks
        )
        
        // Center the board within the view
        boardLeft = (width - (blockSize * horizontalBlocks)) / 2
        boardTop = (height - (blockSize * verticalBlocks)) / 2
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw background (already black from theme)
        
        // Draw board border glow
        drawBoardBorder(canvas)
        
        // Draw grid (very subtle)
        drawGrid(canvas)
        
        // Check if line clear animation is in progress
        if (gameBoard.isLineClearAnimationInProgress) {
            // Draw the line clearing animation
            drawLineClearAnimation(canvas)
        } else {
            // Draw locked pieces
            drawLockedBlocks(canvas)
        }
        
        if (!gameBoard.isGameOver && isRunning) {
            // Draw ghost piece (landing preview)
            drawGhostPiece(canvas)
            
            // Draw active piece
            drawActivePiece(canvas)
        }
    }
    
    /**
     * Draw the line clearing animation
     */
    private fun drawLineClearAnimation(canvas: Canvas) {
        // Draw non-clearing blocks
        for (y in 0 until gameBoard.height) {
            // Skip lines that are being cleared
            if (gameBoard.linesToClear.contains(y)) continue
            
            for (x in 0 until gameBoard.width) {
                if (gameBoard.isOccupied(x, y)) {
                    drawBlock(canvas, x, y, blockPaint)
                }
            }
        }
        
        // Draw all clearing lines with a single animation effect
        for (lineY in gameBoard.linesToClear) {
            for (x in 0 until gameBoard.width) {
                // Animation effects for all lines simultaneously
                val brightness = 255 - (lineClearProgress * 200).toInt()
                val scale = 1.0f - lineClearProgress * 0.5f
                
                // Set the paint for the clear animation
                lineClearPaint.color = Color.WHITE
                lineClearPaint.alpha = brightness.coerceIn(0, 255)
                
                // Calculate block position with scaling
                val left = boardLeft + x * blockSize + (blockSize * (1 - scale) / 2)
                val top = boardTop + lineY * blockSize + (blockSize * (1 - scale) / 2)
                val right = left + blockSize * scale
                val bottom = top + blockSize * scale
                
                // Draw the shrinking, fading block
                val rect = RectF(left, top, right, bottom)
                canvas.drawRect(rect, lineClearPaint)
                
                // Add a glow effect
                lineClearPaint.setShadowLayer(10f * (1f - lineClearProgress), 0f, 0f, Color.WHITE)
                canvas.drawRect(rect, lineClearPaint)
            }
        }
    }
    
    /**
     * Draw glowing border around the playable area
     */
    private fun drawBoardBorder(canvas: Canvas) {
        val left = boardLeft
        val top = boardTop
        val right = boardLeft + gameBoard.width * blockSize
        val bottom = boardTop + gameBoard.height * blockSize
        
        val rect = RectF(left, top, right, bottom)
        canvas.drawRect(rect, borderGlowPaint)
    }
    
    /**
     * Draw the grid lines (very subtle)
     */
    private fun drawGrid(canvas: Canvas) {
        // Draw vertical grid lines
        for (x in 0..gameBoard.width) {
            val xPos = boardLeft + x * blockSize
            canvas.drawLine(
                xPos, boardTop,
                xPos, boardTop + gameBoard.height * blockSize,
                gridPaint
            )
        }
        
        // Draw horizontal grid lines
        for (y in 0..gameBoard.height) {
            val yPos = boardTop + y * blockSize
            canvas.drawLine(
                boardLeft, yPos,
                boardLeft + gameBoard.width * blockSize, yPos,
                gridPaint
            )
        }
    }
    
    /**
     * Draw the locked blocks on the board
     */
    private fun drawLockedBlocks(canvas: Canvas) {
        for (y in 0 until gameBoard.height) {
            for (x in 0 until gameBoard.width) {
                if (gameBoard.isOccupied(x, y)) {
                    drawBlock(canvas, x, y, blockPaint)
                }
            }
        }
    }
    
    /**
     * Draw the currently active tetromino
     */
    private fun drawActivePiece(canvas: Canvas) {
        val piece = gameBoard.getCurrentPiece() ?: return
        
        for (y in 0 until piece.getHeight()) {
            for (x in 0 until piece.getWidth()) {
                if (piece.isBlockAt(x, y)) {
                    val boardX = piece.x + x
                    val boardY = piece.y + y
                    
                    // Only draw if within bounds and visible on screen
                    if (boardY >= 0 && boardY < gameBoard.height && 
                        boardX >= 0 && boardX < gameBoard.width) {
                        drawBlock(canvas, boardX, boardY, blockPaint)
                    }
                }
            }
        }
    }
    
    /**
     * Draw the ghost piece (landing preview)
     */
    private fun drawGhostPiece(canvas: Canvas) {
        val piece = gameBoard.getCurrentPiece() ?: return
        val ghostY = gameBoard.getGhostY()
        
        for (y in 0 until piece.getHeight()) {
            for (x in 0 until piece.getWidth()) {
                if (piece.isBlockAt(x, y)) {
                    val boardX = piece.x + x
                    val boardY = ghostY + y
                    
                    // Only draw if within bounds and visible on screen
                    if (boardY >= 0 && boardY < gameBoard.height && 
                        boardX >= 0 && boardX < gameBoard.width) {
                        drawBlock(canvas, boardX, boardY, ghostBlockPaint)
                    }
                }
            }
        }
    }
    
    /**
     * Draw a single tetris block at the given grid position
     */
    private fun drawBlock(canvas: Canvas, x: Int, y: Int, paint: Paint) {
        val left = boardLeft + x * blockSize
        val top = boardTop + y * blockSize
        val right = left + blockSize
        val bottom = top + blockSize
        
        // Draw block with a slight inset to create separation
        val rect = RectF(left + 1, top + 1, right - 1, bottom - 1)
        canvas.drawRect(rect, paint)
        
        // Draw enhanced glow effect
        val glowRect = RectF(left, top, right, bottom)
        val blockGlowPaint = Paint(glowPaint)
        if (paint == blockPaint) {
            val piece = gameBoard.getCurrentPiece()
            if (piece != null && isPositionInPiece(x, y, piece)) {
                // Set glow color based on piece type
                blockGlowPaint.color = getTetrominoColor(piece.type)
                blockGlowPaint.alpha = 150
                blockGlowPaint.setShadowLayer(3f, 0f, 0f, blockGlowPaint.color)
            }
        }
        canvas.drawRect(glowRect, blockGlowPaint)
    }
    
    /**
     * Check if the given board position is part of the current piece
     */
    private fun isPositionInPiece(boardX: Int, boardY: Int, piece: Tetromino): Boolean {
        for (y in 0 until piece.getHeight()) {
            for (x in 0 until piece.getWidth()) {
                if (piece.isBlockAt(x, y)) {
                    val pieceX = piece.x + x
                    val pieceY = piece.y + y
                    if (pieceX == boardX && pieceY == boardY) {
                        return true
                    }
                }
            }
        }
        return false
    }
    
    /**
     * Get color for tetromino type
     */
    private fun getTetrominoColor(type: TetrominoType): Int {
        return when (type) {
            TetrominoType.I -> Color.CYAN
            TetrominoType.J -> Color.BLUE
            TetrominoType.L -> Color.rgb(255, 165, 0) // Orange
            TetrominoType.O -> Color.YELLOW
            TetrominoType.S -> Color.GREEN
            TetrominoType.T -> Color.MAGENTA
            TetrominoType.Z -> Color.RED
        }
    }
    
    // Custom touch event handling
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isRunning || isPaused || gameBoard.isGameOver) {
            return true
        }
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Record start of touch
                startX = event.x
                startY = event.y
                lastTouchX = event.x
                lastTouchY = event.y
                
                // Check for double tap (rotate)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < 250) {
                    // Double tap detected, rotate the piece
                    if (currentTime - lastRotationTime >= rotationCooldown) {
                        gameBoard.rotate()
                        lastRotationTime = currentTime
                        invalidate()
                    }
                }
                lastTapTime = currentTime
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - lastTouchX
                val deltaY = event.y - lastTouchY
                
                // Horizontal movement (left/right)
                if (abs(deltaX) > blockSize) {
                    if (deltaX > 0) {
                        gameBoard.moveRight()
                    } else {
                        gameBoard.moveLeft()
                    }
                    lastTouchX = event.x
                    invalidate()
                }
                
                // Vertical movement (soft drop)
                if (deltaY > blockSize / 2) {
                    gameBoard.moveDown()
                    lastTouchY = event.y
                    invalidate()
                }
            }
            
            MotionEvent.ACTION_UP -> {
                // Calculate movement speed for potential fling detection
                val moveTime = System.currentTimeMillis() - lastTapTime
                val deltaY = event.y - startY
                val deltaX = event.x - startX
                
                // If the movement was fast and downward, treat as hard drop
                if (moveTime > 0 && deltaY > blockSize && (deltaY / moveTime) * 1000 > minSwipeVelocity) {
                    gameBoard.hardDrop()
                    invalidate()
                } else if (moveTime < minTapTime && 
                         abs(deltaY) < maxTapMovement && 
                         abs(deltaX) < maxTapMovement) {
                    // Quick tap with minimal movement (rotation)
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastRotationTime >= rotationCooldown) {
                        gameBoard.rotate()
                        lastRotationTime = currentTime
                        invalidate()
                    }
                }
            }
        }
        
        return true
    }
    
    /**
     * Get the current score
     */
    fun getScore(): Int = gameBoard.score
    
    /**
     * Get the current level
     */
    fun getLevel(): Int = gameBoard.level
    
    /**
     * Get the number of lines cleared
     */
    fun getLines(): Int = gameBoard.lines
    
    /**
     * Check if the game is over
     */
    fun isGameOver(): Boolean = gameBoard.isGameOver
    
    /**
     * Get the next tetromino
     */
    fun getNextPiece() = gameBoard.getNextPiece()
    
    /**
     * Clean up resources when view is detached
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(gameLoopRunnable)
    }
} 