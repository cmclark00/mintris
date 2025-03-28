package com.mintris.model

import android.util.Log

/**
 * Represents the game board (grid) and manages game state
 */
class GameBoard(
    val width: Int = 10,
    val height: Int = 20
) {
    companion object {
        private const val TAG = "GameBoard"
    }

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
    private var isPlayerSoftDrop = false  // Track if the drop is player-initiated
    
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
    
    // Store the last cleared lines
    private val lastClearedLines = mutableListOf<Int>()
    
    // Add spawn protection variables
    private var pieceSpawnTime = 0L
    private val spawnGracePeriod = 250L  // Changed from 150ms to 250ms
    
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
            bag.addAll(TetrominoType.entries.toTypedArray())
            bag.shuffle()
        }
        
        // Take the next piece from the bag
        nextPiece = Tetromino(bag.removeAt(0))
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
        Log.d(TAG, "spawnPiece() started - current states: isHardDropInProgress=$isHardDropInProgress, isPieceLocking=$isPieceLocking")
        
        currentPiece = nextPiece
        spawnNextPiece()
        
        // Center the piece horizontally
        currentPiece?.apply {
            x = (width - getWidth()) / 2
            y = 0
            
            Log.d(TAG, "spawnPiece() - new piece spawned at position (${x},${y}), type=${type}")
            
            // Set the spawn time for the grace period
            pieceSpawnTime = System.currentTimeMillis()
            
            // Check if the piece can be placed (Game Over condition)
            if (!canMove(0, 0)) {
                isGameOver = true
                Log.d(TAG, "spawnPiece() - Game Over condition detected")
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
            // Only add soft drop points if it's a player-initiated drop
            if (isPlayerSoftDrop) {
                score += 1
            }
            onPieceMove?.invoke()
            true
        } else {
            // Check if we're within the spawn grace period
            val currentTime = System.currentTimeMillis()
            if (currentTime - pieceSpawnTime < spawnGracePeriod) {
                Log.d(TAG, "moveDown() - not locking piece due to spawn grace period (${currentTime - pieceSpawnTime}ms < ${spawnGracePeriod}ms)")
                return false
            }
            
            lockPiece()
            false
        }
    }
    
    /**
     * Player-initiated soft drop
     */
    fun softDrop() {
        isPlayerSoftDrop = true
        moveDown()
        isPlayerSoftDrop = false
    }
    
    /**
     * Hard drop the current piece
     */
    fun hardDrop() {
        if (isHardDropInProgress || isPieceLocking) {
            Log.d(TAG, "hardDrop() called but blocked: isHardDropInProgress=$isHardDropInProgress, isPieceLocking=$isPieceLocking")
            return  // Prevent multiple hard drops
        }
        
        // Check if we're within the spawn grace period
        val currentTime = System.currentTimeMillis()
        if (currentTime - pieceSpawnTime < spawnGracePeriod) {
            Log.d(TAG, "hardDrop() - blocked due to spawn grace period (${currentTime - pieceSpawnTime}ms < ${spawnGracePeriod}ms)")
            return
        }
        
        Log.d(TAG, "hardDrop() started - setting isHardDropInProgress=true")
        isHardDropInProgress = true
        val piece = currentPiece ?: return
        
        // Count how many cells the piece will drop
        var dropDistance = 0
        while (canMove(0, dropDistance + 1)) {
            dropDistance++
        }
        
        Log.d(TAG, "hardDrop() - piece will drop $dropDistance cells, position before: (${piece.x},${piece.y})")
        
        // Move piece down until it can't move anymore
        while (canMove(0, 1)) {
            piece.y++
            onPieceMove?.invoke()
        }
        
        Log.d(TAG, "hardDrop() - piece final position: (${piece.x},${piece.y})")
        
        // Add hard drop points (2 points per cell)
        score += dropDistance * 2
        
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
        if (isPieceLocking) {
            Log.d(TAG, "lockPiece() called but blocked: isPieceLocking=$isPieceLocking")
            return  // Prevent recursive locking
        }
        
        Log.d(TAG, "lockPiece() started - setting isPieceLocking=true, current isHardDropInProgress=$isHardDropInProgress")
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
        
        // IMPORTANT: Reset the hard drop flag before spawning a new piece
        // This prevents the immediate hard drop of the next piece
        if (isHardDropInProgress) {
            Log.d(TAG, "lockPiece() - resetting isHardDropInProgress=false BEFORE spawning new piece")
            isHardDropInProgress = false
        }
        
        // Log piece position before spawning new piece
        Log.d(TAG, "lockPiece() - about to spawn new piece at y=${piece.y}, isHardDropInProgress=$isHardDropInProgress")
        
        // Spawn new piece immediately
        spawnPiece()
        
        // Allow holding piece again after locking
        canHold = true
        
        // Reset locking state
        isPieceLocking = false
        Log.d(TAG, "lockPiece() completed - reset flags: isPieceLocking=false, isHardDropInProgress=$isHardDropInProgress")
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
        
        // Store the last cleared lines
        lastClearedLines.clear()
        lastClearedLines.addAll(linesToClear)
        
        // If lines were cleared, calculate score in background and trigger callback
        if (shiftAmount > 0) {
            // Log line clear
            Log.d(TAG, "Lines cleared: $shiftAmount")
            
            // Trigger line clear callback on main thread with the lines that were cleared
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                // Call the line clear callback with the cleared line count
                try {
                    Log.d(TAG, "Triggering onLineClear callback with $shiftAmount lines")
                    val clearedLines = getLastClearedLines()
                    onLineClear?.invoke(shiftAmount, clearedLines)
                    Log.d(TAG, "onLineClear callback completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onLineClear callback", e)
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
    
    /**
     * Get the list of lines that were most recently cleared
     */
    private fun getLastClearedLines(): List<Int> {
        return lastClearedLines.toList()
    }

    /**
     * Update the game state (called by game loop)
     */
    fun update() {
        if (!isGameOver) {
            moveDown()
        }
    }
} 