package com.mintris.model

import kotlin.random.Random

/**
 * Represents the game board (grid) and manages game state
 */
class GameBoard(
    val width: Int = 10,
    val height: Int = 20
) {
    // Board grid to track locked pieces
    // True = occupied, False = empty
    private val grid = Array(height) { BooleanArray(width) { false } }
    
    // Current active tetromino
    private var currentPiece: Tetromino? = null
    
    // Next tetromino to be played
    private var nextPiece: Tetromino? = null
    
    // Hold piece
    private var holdPiece: Tetromino? = null
    private var canHold = true
    
    // 7-bag randomizer
    private val bag = mutableListOf<TetrominoType>()
    
    // Game state
    var score = 0
    var level = 1
    var startingLevel = 1  // Add this line to track the starting level
    var lines = 0
    var isGameOver = false
    var isHardDropInProgress = false  // Make public
    var isPieceLocking = false  // Make public
    
    // Scoring state
    private var combo = 0
    private var lastClearWasTetris = false
    private var lastClearWasPerfect = false
    private var lastClearWasAllClear = false
    private var lastPieceClearedLines = false  // Track if the last piece placed cleared lines
    
    // Animation state
    var linesToClear = mutableListOf<Int>()
    var isLineClearAnimationInProgress = false
    
    // Initial game speed (milliseconds per drop)
    var dropInterval = 1000L
    
    // Callbacks for game events
    var onPieceMove: (() -> Unit)? = null
    var onPieceLock: (() -> Unit)? = null
    var onNextPieceChanged: (() -> Unit)? = null
    var onLineClear: ((Int, List<Int>) -> Unit)? = null
    
    init {
        spawnNextPiece()
        spawnPiece()
    }
    
    /**
     * Generates the next tetromino piece using 7-bag randomizer
     */
    private fun spawnNextPiece() {
        // If bag is empty, refill it with all piece types
        if (bag.isEmpty()) {
            bag.addAll(TetrominoType.values())
            bag.shuffle()
        }
        
        // Take the next piece from the bag
        nextPiece = Tetromino(bag.removeFirst())
        onNextPieceChanged?.invoke()
    }
    
    /**
     * Hold the current piece
     */
    fun holdPiece() {
        if (!canHold) return
        
        val current = currentPiece
        if (holdPiece == null) {
            // If no piece is held, hold current piece and spawn new one
            holdPiece = current
            spawnNextPiece()
            spawnPiece()
        } else {
            // Swap current piece with held piece
            currentPiece = holdPiece
            holdPiece = current
            // Reset position of swapped piece
            currentPiece?.apply {
                x = (width - getWidth()) / 2
                y = 0
            }
        }
        canHold = false
    }
    
    /**
     * Get the currently held piece
     */
    fun getHoldPiece(): Tetromino? = holdPiece
    
    /**
     * Get the next piece that will be spawned
     */
    fun getNextPiece(): Tetromino? = nextPiece
    
    /**
     * Spawns the current tetromino at the top of the board
     */
    fun spawnPiece() {
        currentPiece = nextPiece
        spawnNextPiece()
        
        // Center the piece horizontally
        currentPiece?.apply {
            x = (width - getWidth()) / 2
            y = 0
            
            // Check if the piece can be placed (Game Over condition)
            if (!canMove(0, 0)) {
                isGameOver = true
            }
        }
    }
    
    /**
     * Move the current piece left
     */
    fun moveLeft() {
        if (canMove(-1, 0)) {
            currentPiece?.x = currentPiece?.x?.minus(1) ?: 0
            onPieceMove?.invoke()
        }
    }
    
    /**
     * Move the current piece right
     */
    fun moveRight() {
        if (canMove(1, 0)) {
            currentPiece?.x = currentPiece?.x?.plus(1) ?: 0
            onPieceMove?.invoke()
        }
    }
    
    /**
     * Move the current piece down (soft drop)
     */
    fun moveDown(): Boolean {
        // Don't allow movement if a hard drop is in progress or piece is locking
        if (isHardDropInProgress || isPieceLocking) return false
        
        return if (canMove(0, 1)) {
            currentPiece?.y = currentPiece?.y?.plus(1) ?: 0
            onPieceMove?.invoke()
            true
        } else {
            lockPiece()
            false
        }
    }
    
    /**
     * Hard drop the current piece
     */
    fun hardDrop() {
        if (isHardDropInProgress || isPieceLocking) return  // Prevent multiple hard drops
        
        isHardDropInProgress = true
        val piece = currentPiece ?: return
        
        // Move piece down until it can't move anymore
        while (canMove(0, 1)) {
            piece.y++
            onPieceMove?.invoke()
        }
        
        // Lock the piece immediately
        lockPiece()
    }
    
    /**
     * Rotate the current piece clockwise
     */
    fun rotate() {
        currentPiece?.let {
            // Save current rotation
            val originalX = it.x
            val originalY = it.y
            
            // Try to rotate
            it.rotateClockwise()
            
            // Wall kick logic - try to move the piece if rotation causes collision
            if (!canMove(0, 0)) {
                // Try to move left
                if (canMove(-1, 0)) {
                    it.x--
                }
                // Try to move right
                else if (canMove(1, 0)) {
                    it.x++
                }
                // Try to move 2 spaces (for I piece)
                else if (canMove(-2, 0)) {
                    it.x -= 2
                }
                else if (canMove(2, 0)) {
                    it.x += 2
                }
                // Try to move up for floor kicks
                else if (canMove(0, -1)) {
                    it.y--
                }
                // Revert if can't find a valid position
                else {
                    it.rotateCounterClockwise()
                    it.x = originalX
                    it.y = originalY
                }
            }
        }
    }
    
    /**
     * Check if the current piece can move to the given position
     */
    fun canMove(deltaX: Int, deltaY: Int): Boolean {
        val piece = currentPiece ?: return false
        
        val newX = piece.x + deltaX
        val newY = piece.y + deltaY
        
        for (y in 0 until piece.getHeight()) {
            for (x in 0 until piece.getWidth()) {
                if (piece.isBlockAt(x, y)) {
                    val boardX = newX + x
                    val boardY = newY + y
                    
                    // Check if the position is outside the board horizontally
                    if (boardX < 0 || boardX >= width) {
                        return false
                    }
                    
                    // Check if the position is below the board
                    if (boardY >= height) {
                        return false
                    }
                    
                    // Check if the position is already occupied (but not if it's above the board)
                    if (boardY >= 0 && grid[boardY][boardX]) {
                        return false
                    }
                }
            }
        }
        
        return true
    }
    
    /**
     * Lock the current piece in place
     */
    private fun lockPiece() {
        if (isPieceLocking) return  // Prevent recursive locking
        isPieceLocking = true
        
        val piece = currentPiece ?: return
        
        // Add the piece to the grid
        for (y in 0 until piece.getHeight()) {
            for (x in 0 until piece.getWidth()) {
                if (piece.isBlockAt(x, y)) {
                    val boardX = piece.x + x
                    val boardY = piece.y + y
                    
                    // Only add to grid if within bounds
                    if (boardY >= 0 && boardY < height && boardX >= 0 && boardX < width) {
                        grid[boardY][boardX] = true
                    }
                }
            }
        }
        
        // Trigger the piece lock vibration
        onPieceLock?.invoke()
        
        // Find and clear lines immediately
        findAndClearLines()
        
        // Spawn new piece immediately
        spawnPiece()
        
        // Allow holding piece again after locking
        canHold = true
        
        // Reset both states after everything is done
        isPieceLocking = false
        isHardDropInProgress = false
    }
    
    /**
     * Find and clear completed lines immediately
     */
    private fun findAndClearLines() {
        // Quick scan for completed lines
        var shiftAmount = 0
        var y = height - 1
        val linesToClear = mutableListOf<Int>()
        
        while (y >= 0) {
            if (grid[y].all { it }) {
                // Line is full, add to lines to clear
                linesToClear.add(y)
                shiftAmount++
            } else if (shiftAmount > 0) {
                // Shift this row down by shiftAmount
                System.arraycopy(grid[y], 0, grid[y + shiftAmount], 0, width)
            }
            y--
        }
        
        // If lines were cleared, calculate score in background and trigger callback
        if (shiftAmount > 0) {
            android.util.Log.d("GameBoard", "Lines cleared: $shiftAmount")
            // Trigger line clear callback on main thread with the lines that were cleared
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                android.util.Log.d("GameBoard", "Triggering onLineClear callback with $shiftAmount lines")
                try {
                    onLineClear?.invoke(shiftAmount, linesToClear)  // Pass the lines that were cleared
                    android.util.Log.d("GameBoard", "onLineClear callback completed successfully")
                } catch (e: Exception) {
                    android.util.Log.e("GameBoard", "Error in onLineClear callback", e)
                }
            }
            
            // Clear top rows after callback
            for (y in 0 until shiftAmount) {
                java.util.Arrays.fill(grid[y], false)
            }
            
            Thread {
                calculateScore(shiftAmount)
            }.start()
        }
        
        // Update combo based on whether this piece cleared lines
        if (shiftAmount > 0) {
            if (lastPieceClearedLines) {
                combo++
            } else {
                combo = 1  // Start new combo
            }
        } else {
            combo = 0  // Reset combo if no lines cleared
        }
        lastPieceClearedLines = shiftAmount > 0
    }
    
    /**
     * Calculate score for cleared lines
     */
    private fun calculateScore(clearedLines: Int) {
        // Pre-calculated score multipliers for better performance
        val baseScore = when (clearedLines) {
            1 -> 40
            2 -> 100
            3 -> 300
            4 -> 1200
            else -> 0
        }
        
        // Check for perfect clear (no blocks left)
        val isPerfectClear = !grid.any { row -> row.any { it } }
        
        // Check for all clear (no blocks in playfield)
        val isAllClear = !grid.any { row -> row.any { it } } && 
                        currentPiece == null && 
                        nextPiece == null
        
        // Calculate combo multiplier
        val comboMultiplier = if (combo > 0) {
            when (combo) {
                1 -> 1.0
                2 -> 1.5
                3 -> 2.0
                4 -> 2.5
                else -> 3.0
            }
        } else 1.0
        
        // Calculate back-to-back Tetris bonus
        val backToBackMultiplier = if (clearedLines == 4 && lastClearWasTetris) 1.5 else 1.0
        
        // Calculate perfect clear bonus
        val perfectClearMultiplier = if (isPerfectClear) {
            when (clearedLines) {
                1 -> 2.0
                2 -> 3.0
                3 -> 4.0
                4 -> 5.0
                else -> 1.0
            }
        } else 1.0
        
        // Calculate all clear bonus
        val allClearMultiplier = if (isAllClear) 2.0 else 1.0
        
        // Calculate T-Spin bonus
        val tSpinMultiplier = if (isTSpin()) {
            when (clearedLines) {
                1 -> 2.0
                2 -> 4.0
                3 -> 6.0
                else -> 1.0
            }
        } else 1.0
        
        // Calculate final score with all multipliers
        val finalScore = (baseScore * level * comboMultiplier * 
                        backToBackMultiplier * perfectClearMultiplier * 
                        allClearMultiplier * tSpinMultiplier).toInt()
        
        // Update score on main thread
        Thread {
            score += finalScore
        }.start()
        
        // Update line clear state
        lastClearWasTetris = clearedLines == 4
        lastClearWasPerfect = isPerfectClear
        lastClearWasAllClear = isAllClear
        
        // Update lines cleared and level
        lines += clearedLines
        // Calculate level based on lines cleared, but ensure it's never below the starting level
        level = Math.max((lines / 10) + 1, startingLevel)
        
        // Update game speed based on level (NES formula)
        dropInterval = (1000 * Math.pow(0.8, (level - 1).toDouble())).toLong()
    }
    
    /**
     * Check if the last move was a T-Spin
     */
    private fun isTSpin(): Boolean {
        val piece = currentPiece ?: return false
        if (piece.type != TetrominoType.T) return false
        
        // Count occupied corners around the T piece
        var occupiedCorners = 0
        val centerX = piece.x + 1
        val centerY = piece.y + 1
        
        // Check all four corners
        if (isOccupied(centerX - 1, centerY - 1)) occupiedCorners++
        if (isOccupied(centerX + 1, centerY - 1)) occupiedCorners++
        if (isOccupied(centerX - 1, centerY + 1)) occupiedCorners++
        if (isOccupied(centerX + 1, centerY + 1)) occupiedCorners++
        
        // T-Spin requires at least 3 occupied corners
        return occupiedCorners >= 3
    }
    
    /**
     * Get the ghost piece position (preview of where piece will land)
     */
    fun getGhostY(): Int {
        val piece = currentPiece ?: return 0
        var ghostY = piece.y
        
        // Find how far the piece can move down
        while (true) {
            if (canMove(0, ghostY - piece.y + 1)) {
                ghostY++
            } else {
                break
            }
        }
        
        // Ensure ghostY doesn't exceed the board height
        return ghostY.coerceAtMost(height - 1)
    }
    
    /**
     * Get the current tetromino
     */
    fun getCurrentPiece(): Tetromino? = currentPiece
    
    /**
     * Check if a cell in the grid is occupied
     */
    fun isOccupied(x: Int, y: Int): Boolean {
        return if (x in 0 until width && y in 0 until height) {
            grid[y][x]
        } else {
            false
        }
    }
    
    /**
     * Check if a line is completely filled
     */
    fun isLineFull(y: Int): Boolean {
        return if (y in 0 until height) {
            grid[y].all { it }
        } else {
            false
        }
    }
    
    /**
     * Update the current level and adjust game parameters
     */
    fun updateLevel(newLevel: Int) {
        level = newLevel.coerceIn(1, 20)
        startingLevel = level  // Store the starting level
        // Update game speed based on level (NES formula)
        dropInterval = (1000 * Math.pow(0.8, (level - 1).toDouble())).toLong()
    }
    
    /**
     * Start a new game
     */
    fun startGame() {
        reset()
        // Initialize pieces
        spawnNextPiece()
        spawnPiece()
    }
    
    /**
     * Reset the game board
     */
    fun reset() {
        // Clear the grid
        for (y in 0 until height) {
            for (x in 0 until width) {
                grid[y][x] = false
            }
        }
        
        // Reset game state
        score = 0
        level = startingLevel  // Use starting level instead of resetting to 1
        lines = 0
        isGameOver = false
        dropInterval = (1000 * Math.pow(0.8, (level - 1).toDouble())).toLong()  // Set speed based on current level
        
        // Reset scoring state
        combo = 0
        lastClearWasTetris = false
        lastClearWasPerfect = false
        lastClearWasAllClear = false
        lastPieceClearedLines = false
        
        // Reset piece state
        holdPiece = null
        canHold = true
        bag.clear()
        
        // Clear current and next pieces
        currentPiece = null
        nextPiece = null
    }
    
    /**
     * Clear completed lines and move blocks down (legacy method, kept for reference)
     */
    private fun clearLines(): Int {
        return linesToClear.size // Return the number of lines that will be cleared
    }
    
    /**
     * Get the current combo count
     */
    fun getCombo(): Int = combo
} 