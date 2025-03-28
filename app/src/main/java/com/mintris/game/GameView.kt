package com.mintris.game

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.BlurMaskFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.hardware.display.DisplayManager
import android.view.Display
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

    companion object {
        private const val TAG = "GameView"
    }

    // Game board model
    private var gameBoard = GameBoard()
    private var gameHaptics: GameHaptics? = null
    
    // Game state
    private var isRunning = false
    var isPaused = false  // Changed from private to public to allow access from MainActivity
    private var score = 0
    
    // Callbacks
    var onNextPieceChanged: (() -> Unit)? = null
    
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
        alpha = 20  // Reduced from 40 to be more subtle
        isAntiAlias = true
        strokeWidth = 1f
        style = Paint.Style.STROKE
        maskFilter = null  // Ensure no blur effect on grid lines
    }
    
    private val glowPaint = Paint().apply {
        color = Color.WHITE
        alpha = 40  // Reduced from 80 for more subtlety
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.OUTER)
    }
    
    private val blockGlowPaint = Paint().apply {
        color = Color.WHITE
        alpha = 60
        isAntiAlias = true
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.OUTER)
    }
    
    private val borderGlowPaint = Paint().apply {
        color = Color.WHITE
        alpha = 60
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.OUTER)
    }
    
    // Add a new paint for the pulse effect
    private val pulsePaint = Paint().apply {
        color = Color.CYAN
        alpha = 255
        isAntiAlias = true
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(32f, BlurMaskFilter.Blur.OUTER)  // Increased from 16f to 32f
    }
    
    // Pre-allocate paint objects to avoid GC
    private val tmpPaint = Paint()
    
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
    private var lastMoveTime = 0L
    private var minSwipeVelocity = 1200  // Increased from 800 to require more deliberate swipes
    private val maxTapMovement = 20f    // Maximum movement allowed for a tap (in pixels)
    private val minTapTime = 100L       // Minimum time for a tap (in milliseconds)
    private val rotationCooldown = 150L // Minimum time between rotations (in milliseconds)
    private val moveCooldown = 50L      // Minimum time between move haptics (in milliseconds)
    private var lockedDirection: Direction? = null  // Track the locked movement direction
    private val minMovementThreshold = 0.75f  // Minimum movement threshold relative to block size
    private val directionLockThreshold = 2.5f  // Increased from 1.5f to make direction locking more aggressive
    private val isStrictDirectionLock = true   // Enable strict direction locking to prevent diagonal inputs
    private val minHardDropDistance = 1.5f     // Minimum distance (in blocks) for hard drop gesture

    private enum class Direction {
        HORIZONTAL, VERTICAL
    }
    
    // Callback for game events
    var onGameStateChanged: ((score: Int, level: Int, lines: Int) -> Unit)? = null
    var onGameOver: ((score: Int) -> Unit)? = null
    var onLineClear: ((Int) -> Unit)? = null // New callback for line clear events
    var onPieceMove: (() -> Unit)? = null // New callback for piece movement
    var onPieceLock: (() -> Unit)? = null // New callback for piece locking
    
    // Animation state
    private var pulseAnimator: ValueAnimator? = null
    private var pulseAlpha = 0f
    private var isPulsing = false
    private var linesToPulse = mutableListOf<Int>()  // Track which lines are being cleared
    
    init {
        // Start with paused state
        pause()
        
        // Connect our callbacks to the GameBoard
        gameBoard.onPieceMove = { onPieceMove?.invoke() }
        gameBoard.onPieceLock = { onPieceLock?.invoke() }
        gameBoard.onLineClear = { lineCount, clearedLines -> 
            Log.d(TAG, "Received line clear from GameBoard: $lineCount lines")
            try {
                onLineClear?.invoke(lineCount)
                // Use the lines that were cleared directly
                linesToPulse.clear()
                linesToPulse.addAll(clearedLines)
                Log.d(TAG, "Found ${linesToPulse.size} lines to pulse")
                startPulseAnimation(lineCount)
                Log.d(TAG, "Forwarded line clear callback")
            } catch (e: Exception) {
                Log.e(TAG, "Error forwarding line clear callback", e)
            }
        }
        
        // Force hardware acceleration - This is critical for performance
        setLayerType(LAYER_TYPE_HARDWARE, null)
        
        // Set better frame rate using modern APIs
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        } else {
            displayManager.displays.firstOrNull()
        }
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
        isPaused = false
        isRunning = true
        gameBoard.startGame()  // Add this line to ensure a new piece is spawned
        handler.post(gameLoopRunnable)
        invalidate()
    }
    
    /**
     * Pause the game
     */
    fun pause() {
        isPaused = true
        handler.removeCallbacks(gameLoopRunnable)
        invalidate()
    }
    
    /**
     * Reset the game
     */
    fun reset() {
        isRunning = false
        isPaused = true
        gameBoard.reset()
        gameBoard.startGame()  // Add this line to ensure a new piece is spawned
        handler.removeCallbacks(gameLoopRunnable)
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
        
        // Update UI with current game state
        onGameStateChanged?.invoke(gameBoard.score, gameBoard.level, gameBoard.lines)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Force hardware acceleration - Critical for performance
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
        
        // Account for all glow effects and borders
        val borderPadding = 16f  // Padding for border glow effects
        
        // Calculate block size to fit the height exactly, accounting for all padding
        blockSize = (height.toFloat() - (borderPadding * 2)) / verticalBlocks
        
        // Calculate total board width
        val totalBoardWidth = blockSize * horizontalBlocks
        
        // Center horizontally
        boardLeft = (width - totalBoardWidth) / 2
        boardTop = borderPadding  // Start with border padding from top
        
        // Calculate the total height needed for the board
        val totalHeight = blockSize * verticalBlocks
        
        // Log dimensions for debugging
        Log.d(TAG, "Board dimensions: width=$width, height=$height, blockSize=$blockSize, boardLeft=$boardLeft, boardTop=$boardTop, totalHeight=$totalHeight")
    }
    
    override fun onDraw(canvas: Canvas) {
        // Skip drawing if paused or game over - faster return
        if (isPaused || gameBoard.isGameOver) {
            super.onDraw(canvas)
            return
        }
        
        // Set hardware layer type during draw for better performance
        val wasHardwareAccelerated = isHardwareAccelerated
        if (!wasHardwareAccelerated) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }
        
        super.onDraw(canvas)
        
        // Draw background (already black from theme)
        
        // Draw board border glow
        drawBoardBorder(canvas)
        
        // Draw grid (very subtle)
        drawGrid(canvas)
        
        // Draw locked pieces
        drawLockedBlocks(canvas)
        
        if (!gameBoard.isGameOver && isRunning) {
            // Draw ghost piece (landing preview)
            drawGhostPiece(canvas)
            
            // Draw active piece
            drawActivePiece(canvas)
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
        
        // Draw base border with increased glow
        borderGlowPaint.apply {
            alpha = 80  // Increased from 60
            maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.OUTER)  // Increased from 8f
        }
        canvas.drawRect(rect, borderGlowPaint)
        
        // Draw pulsing border if animation is active
        if (isPulsing) {
            val pulseBorderPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 6f + (16f * pulseAlpha)  // Increased from 4f+12f to 6f+16f
                alpha = (255 * pulseAlpha).toInt()
                isAntiAlias = true
                maskFilter = BlurMaskFilter(32f * (1f + pulseAlpha), BlurMaskFilter.Blur.OUTER)  // Increased from 24f to 32f
            }
            // Draw the border with a slight inset to prevent edge artifacts
            val inset = 1f
            canvas.drawRect(
                left + inset,
                top + inset,
                right - inset,
                bottom - inset,
                pulseBorderPaint
            )
            
            // Add an additional outer glow for more dramatic effect
            val outerGlowPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 2f
                alpha = (128 * pulseAlpha).toInt()
                isAntiAlias = true
                maskFilter = BlurMaskFilter(48f * (1f + pulseAlpha), BlurMaskFilter.Blur.OUTER)
            }
            canvas.drawRect(
                left - 4f,
                top - 4f,
                right + 4f,
                bottom + 4f,
                outerGlowPaint
            )

            // Add extra bright glow for side borders during line clear
            val sideGlowPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 8f + (24f * pulseAlpha)  // Thicker stroke for side borders
                alpha = (255 * pulseAlpha).toInt()
                isAntiAlias = true
                maskFilter = BlurMaskFilter(64f * (1f + pulseAlpha), BlurMaskFilter.Blur.OUTER)  // Larger blur for side borders
            }
            
            // Draw left border with extra glow
            canvas.drawLine(
                left + inset,
                top + inset,
                left + inset,
                bottom - inset,
                sideGlowPaint
            )
            
            // Draw right border with extra glow
            canvas.drawLine(
                right - inset,
                top + inset,
                right - inset,
                bottom - inset,
                sideGlowPaint
            )
        }
    }
    
    /**
     * Draw the grid lines (very subtle)
     */
    private fun drawGrid(canvas: Canvas) {
        // Save the canvas state to prevent any effects from affecting the grid
        canvas.save()
        
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
        
        // Restore the canvas state
        canvas.restore()
    }
    
    /**
     * Draw the locked blocks on the board
     */
    private fun drawLockedBlocks(canvas: Canvas) {
        for (y in 0 until gameBoard.height) {
            for (x in 0 until gameBoard.width) {
                if (gameBoard.isOccupied(x, y)) {
                    drawBlock(canvas, x, y, false, y in linesToPulse)
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
                    
                    // Draw piece regardless of vertical position
                    if (boardX >= 0 && boardX < gameBoard.width) {
                        drawBlock(canvas, boardX, boardY, false, false)
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
                    
                    // Draw ghost piece regardless of vertical position
                    if (boardX >= 0 && boardX < gameBoard.width) {
                        drawBlock(canvas, boardX, boardY, true, false)
                    }
                }
            }
        }
    }
    
    /**
     * Draw a single tetris block at the given grid position
     */
    private fun drawBlock(canvas: Canvas, x: Int, y: Int, isGhost: Boolean, isPulsingLine: Boolean) {
        val left = boardLeft + x * blockSize
        val top = boardTop + y * blockSize
        val right = left + blockSize
        val bottom = top + blockSize
        
        // Save canvas state before drawing block effects
        canvas.save()
        
        // Draw outer glow
        blockGlowPaint.color = if (isGhost) Color.argb(30, 255, 255, 255) else Color.WHITE
        canvas.drawRect(left - 2f, top - 2f, right + 2f, bottom + 2f, blockGlowPaint)
        
        // Draw block
        blockPaint.apply {
            color = if (isGhost) Color.argb(30, 255, 255, 255) else Color.WHITE
            alpha = if (isGhost) 30 else 255
        }
        canvas.drawRect(left, top, right, bottom, blockPaint)
        
        // Draw inner glow
        glowPaint.color = if (isGhost) Color.argb(30, 255, 255, 255) else Color.WHITE
        canvas.drawRect(left + 1f, top + 1f, right - 1f, bottom - 1f, glowPaint)
        
        // Draw pulse effect if animation is active and this is a pulsing line
        if (isPulsing && isPulsingLine) {
            val pulseBlockPaint = Paint().apply {
                color = Color.WHITE
                alpha = (255 * pulseAlpha).toInt()
                isAntiAlias = true
                style = Paint.Style.FILL
                maskFilter = BlurMaskFilter(40f * (1f + pulseAlpha), BlurMaskFilter.Blur.OUTER)
            }
            canvas.drawRect(left - 16f, top - 16f, right + 16f, bottom + 16f, pulseBlockPaint)
        }
        
        // Restore canvas state after drawing block effects
        canvas.restore()
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
                lockedDirection = null  // Reset direction lock
                
                // Check for double tap (rotate)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < 200) {  // Reduced from 250ms for faster response
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
                val currentTime = System.currentTimeMillis()
                
                // Determine movement direction if not locked
                if (lockedDirection == null) {
                    val absDeltaX = abs(deltaX)
                    val absDeltaY = abs(deltaY)
                    
                    // Check if movement exceeds threshold
                    if (absDeltaX > blockSize * minMovementThreshold || absDeltaY > blockSize * minMovementThreshold) {
                        // Determine dominant direction with stricter criteria
                        if (absDeltaX > absDeltaY * directionLockThreshold) {
                            lockedDirection = Direction.HORIZONTAL
                        } else if (absDeltaY > absDeltaX * directionLockThreshold) {
                            lockedDirection = Direction.VERTICAL
                        }
                        // If strict direction lock is enabled and we couldn't determine a clear direction, don't set one
                        // This prevents diagonal movements from being recognized
                    }
                }
                
                // Handle movement based on locked direction
                when (lockedDirection) {
                    Direction.HORIZONTAL -> {
                        if (abs(deltaX) > blockSize * minMovementThreshold) {
                            if (deltaX > 0) {
                                gameBoard.moveRight()
                            } else {
                                gameBoard.moveLeft()
                            }
                            lastTouchX = event.x
                            if (currentTime - lastMoveTime >= moveCooldown) {
                                gameHaptics?.vibrateForPieceMove()
                                lastMoveTime = currentTime
                            }
                            invalidate()
                        }
                    }
                    Direction.VERTICAL -> {
                        if (deltaY > blockSize * minMovementThreshold) {
                            gameBoard.moveDown()
                            lastTouchY = event.y
                            if (currentTime - lastMoveTime >= moveCooldown) {
                                gameHaptics?.vibrateForPieceMove()
                                lastMoveTime = currentTime
                            }
                            invalidate()
                        }
                    }
                    null -> {
                        // No direction lock yet, don't process movement
                    }
                }
            }
            
            MotionEvent.ACTION_UP -> {
                // Calculate movement speed for potential fling detection
                val moveTime = System.currentTimeMillis() - lastTapTime
                val deltaY = event.y - startY
                val deltaX = event.x - startX
                
                // Only allow hard drops with a deliberate downward swipe
                // Requires: predominantly vertical movement, minimum distance, and minimum velocity
                if (moveTime > 0 && 
                    deltaY > blockSize * minHardDropDistance && // Require longer swipe for hard drop
                    (deltaY / moveTime) * 1000 > minSwipeVelocity && 
                    abs(deltaX) < abs(deltaY) * 0.3f) { // Require more purely vertical movement (reduced from 0.5f to 0.3f)
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
                
                // Reset direction lock
                lockedDirection = null
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
     * Get the next piece that will be spawned
     */
    fun getNextPiece(): Tetromino? {
        return gameBoard.getNextPiece()
    }
    
    /**
     * Clean up resources when view is detached
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(gameLoopRunnable)
    }
    
    /**
     * Set the game board for this view
     */
    fun setGameBoard(board: GameBoard) {
        gameBoard = board
        
        // Reconnect callbacks to the new board
        gameBoard.onPieceMove = { onPieceMove?.invoke() }
        gameBoard.onPieceLock = { onPieceLock?.invoke() }
        gameBoard.onLineClear = { lineCount, clearedLines -> 
            Log.d(TAG, "Received line clear from GameBoard: $lineCount lines")
            try {
                onLineClear?.invoke(lineCount)
                // Use the lines that were cleared directly
                linesToPulse.clear()
                linesToPulse.addAll(clearedLines)
                Log.d(TAG, "Found ${linesToPulse.size} lines to pulse")
                startPulseAnimation(lineCount)
                Log.d(TAG, "Forwarded line clear callback")
            } catch (e: Exception) {
                Log.e(TAG, "Error forwarding line clear callback", e)
            }
        }
        
        invalidate()
    }
    
    /**
     * Set the haptics handler for this view
     */
    fun setHaptics(haptics: GameHaptics) {
        gameHaptics = haptics
    }
    
    /**
     * Resume the game
     */
    fun resume() {
        if (!isRunning) {
            isRunning = true
        }
        isPaused = false
        
        // Restart the game loop immediately
        handler.removeCallbacks(gameLoopRunnable)
        handler.post(gameLoopRunnable)
        
        // Force an update to ensure pieces move immediately
        update()
        invalidate()
    }
    
    /**
     * Start the pulse animation for line clear
     */
    private fun startPulseAnimation(lineCount: Int) {
        Log.d(TAG, "Starting pulse animation for $lineCount lines")
        
        // Cancel any existing animation
        pulseAnimator?.cancel()
        
        // Create new animation
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = when (lineCount) {
                4 -> 2000L  // Tetris - longer duration
                3 -> 1600L  // Triples
                2 -> 1200L  // Doubles
                1 -> 1000L   // Singles
                else -> 1000L
            }
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                pulseAlpha = animation.animatedValue as Float
                isPulsing = true
                invalidate()
                Log.d(TAG, "Pulse animation update: alpha = $pulseAlpha")
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isPulsing = false
                    pulseAlpha = 0f
                    linesToPulse.clear()
                    invalidate()
                    Log.d(TAG, "Pulse animation ended")
                }
            })
        }
        pulseAnimator?.start()
    }
}
