package com.mintris.model

/**
 * Represents a Tetris piece (Tetromino)
 */
enum class TetrominoType {
    I, J, L, O, S, T, Z
}

class Tetromino(val type: TetrominoType) {
    
    // Each tetromino has 4 rotations (0, 90, 180, 270 degrees)
    private val blocks: Array<Array<BooleanArray>> = getBlocks(type)
    private var currentRotation = 0
    
    // Current position in the game grid
    var x = 0
    var y = 0
    
    /**
     * Get the current shape of the tetromino based on rotation
     */
    fun getCurrentShape(): Array<BooleanArray> {
        return blocks[currentRotation]
    }
    
    /**
     * Get the width of the current tetromino shape
     */
    fun getWidth(): Int {
        return blocks[currentRotation][0].size
    }
    
    /**
     * Get the height of the current tetromino shape
     */
    fun getHeight(): Int {
        return blocks[currentRotation].size
    }
    
    /**
     * Rotate the tetromino clockwise
     */
    fun rotateClockwise() {
        currentRotation = (currentRotation + 1) % 4
    }
    
    /**
     * Rotate the tetromino counter-clockwise
     */
    fun rotateCounterClockwise() {
        currentRotation = (currentRotation + 3) % 4
    }
    
    /**
     * Check if the tetromino's block exists at the given coordinates
     */
    fun isBlockAt(blockX: Int, blockY: Int): Boolean {
        val shape = blocks[currentRotation]
        return if (blockY >= 0 && blockY < shape.size && 
                  blockX >= 0 && blockX < shape[blockY].size) {
            shape[blockY][blockX]
        } else {
            false
        }
    }
    
    companion object {
        /**
         * Get the block patterns for each tetromino type and all its rotations
         */
        private fun getBlocks(type: TetrominoType): Array<Array<BooleanArray>> {
            return when (type) {
                TetrominoType.I -> arrayOf(
                    // Rotation 0°
                    arrayOf(
                        booleanArrayOf(false, false, false, false),
                        booleanArrayOf(true, true, true, true),
                        booleanArrayOf(false, false, false, false),
                        booleanArrayOf(false, false, false, false)
                    ),
                    // Rotation 90°
                    arrayOf(
                        booleanArrayOf(false, false, true, false),
                        booleanArrayOf(false, false, true, false),
                        booleanArrayOf(false, false, true, false),
                        booleanArrayOf(false, false, true, false)
                    ),
                    // Rotation 180°
                    arrayOf(
                        booleanArrayOf(false, false, false, false),
                        booleanArrayOf(false, false, false, false),
                        booleanArrayOf(true, true, true, true),
                        booleanArrayOf(false, false, false, false)
                    ),
                    // Rotation 270°
                    arrayOf(
                        booleanArrayOf(false, true, false, false),
                        booleanArrayOf(false, true, false, false),
                        booleanArrayOf(false, true, false, false),
                        booleanArrayOf(false, true, false, false)
                    )
                )
                TetrominoType.J -> arrayOf(
                    // Rotation 0°
                    arrayOf(
                        booleanArrayOf(true, false, false),
                        booleanArrayOf(true, true, true),
                        booleanArrayOf(false, false, false)
                    ),
                    // Rotation 90°
                    arrayOf(
                        booleanArrayOf(false, true, true),
                        booleanArrayOf(false, true, false),
                        booleanArrayOf(false, true, false)
                    ),
                    // Rotation 180°
                    arrayOf(
                        booleanArrayOf(false, false, false),
                        booleanArrayOf(true, true, true),
                        booleanArrayOf(false, false, true)
                    ),
                    // Rotation 270°
                    arrayOf(
                        booleanArrayOf(false, true, false),
                        booleanArrayOf(false, true, false),
                        booleanArrayOf(true, true, false)
                    )
                )
                TetrominoType.L -> arrayOf(
                    // Rotation 0°
                    arrayOf(
                        booleanArrayOf(false, false, true),
                        booleanArrayOf(true, true, true),
                        booleanArrayOf(false, false, false)
                    ),
                    // Rotation 90°
                    arrayOf(
                        booleanArrayOf(false, true, false),
                        booleanArrayOf(false, true, false),
                        booleanArrayOf(false, true, true)
                    ),
                    // Rotation 180°
                    arrayOf(
                        booleanArrayOf(false, false, false),
                        booleanArrayOf(true, true, true),
                        booleanArrayOf(true, false, false)
                    ),
                    // Rotation 270°
                    arrayOf(
                        booleanArrayOf(true, true, false),
                        booleanArrayOf(false, true, false),
                        booleanArrayOf(false, true, false)
                    )
                )
                TetrominoType.O -> arrayOf(
                    // All rotations are the same for O
                    arrayOf(
                        booleanArrayOf(false, true, true, false),
                        booleanArrayOf(false, true, true, false),
                        booleanArrayOf(false, false, false, false)
                    ),
                    arrayOf(
                        booleanArrayOf(false, true, true, false),
                        booleanArrayOf(false, true, true, false),
                        booleanArrayOf(false, false, false, false)
                    ),
                    arrayOf(
                        booleanArrayOf(false, true, true, false),
                        booleanArrayOf(false, true, true, false),
                        booleanArrayOf(false, false, false, false)
                    ),
                    arrayOf(
                        booleanArrayOf(false, true, true, false),
                        booleanArrayOf(false, true, true, false),
                        booleanArrayOf(false, false, false, false)
                    )
                )
                TetrominoType.S -> arrayOf(
                    // Rotation 0°
                    arrayOf(
                        booleanArrayOf(false, true, true),
                        booleanArrayOf(true, true, false),
                        booleanArrayOf(false, false, false)
                    ),
                    // Rotation 90°
                    arrayOf(
                        booleanArrayOf(false, true, false),
                        booleanArrayOf(false, true, true),
                        booleanArrayOf(false, false, true)
                    ),
                    // Rotation 180°
                    arrayOf(
                        booleanArrayOf(false, false, false),
                        booleanArrayOf(false, true, true),
                        booleanArrayOf(true, true, false)
                    ),
                    // Rotation 270°
                    arrayOf(
                        booleanArrayOf(true, false, false),
                        booleanArrayOf(true, true, false),
                        booleanArrayOf(false, true, false)
                    )
                )
                TetrominoType.T -> arrayOf(
                    // Rotation 0°
                    arrayOf(
                        booleanArrayOf(false, true, false),
                        booleanArrayOf(true, true, true),
                        booleanArrayOf(false, false, false)
                    ),
                    // Rotation 90°
                    arrayOf(
                        booleanArrayOf(false, true, false),
                        booleanArrayOf(false, true, true),
                        booleanArrayOf(false, true, false)
                    ),
                    // Rotation 180°
                    arrayOf(
                        booleanArrayOf(false, false, false),
                        booleanArrayOf(true, true, true),
                        booleanArrayOf(false, true, false)
                    ),
                    // Rotation 270°
                    arrayOf(
                        booleanArrayOf(false, true, false),
                        booleanArrayOf(true, true, false),
                        booleanArrayOf(false, true, false)
                    )
                )
                TetrominoType.Z -> arrayOf(
                    // Rotation 0°
                    arrayOf(
                        booleanArrayOf(true, true, false),
                        booleanArrayOf(false, true, true),
                        booleanArrayOf(false, false, false)
                    ),
                    // Rotation 90°
                    arrayOf(
                        booleanArrayOf(false, false, true),
                        booleanArrayOf(false, true, true),
                        booleanArrayOf(false, true, false)
                    ),
                    // Rotation 180°
                    arrayOf(
                        booleanArrayOf(false, false, false),
                        booleanArrayOf(true, true, false),
                        booleanArrayOf(false, true, true)
                    ),
                    // Rotation 270°
                    arrayOf(
                        booleanArrayOf(false, true, false),
                        booleanArrayOf(true, true, false),
                        booleanArrayOf(true, false, false)
                    )
                )
            }
        }
    }
} 