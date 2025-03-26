package com.mintris

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mintris.databinding.ActivityMainBinding
import com.mintris.game.GameHaptics
import com.mintris.game.GameView
import com.mintris.game.NextPieceView
import android.view.HapticFeedbackConstants

class MainActivity : AppCompatActivity() {
    
    // UI components
    private lateinit var binding: ActivityMainBinding
    private lateinit var gameView: GameView
    private lateinit var scoreText: TextView
    private lateinit var levelText: TextView
    private lateinit var linesText: TextView
    private lateinit var gameOverContainer: LinearLayout
    private lateinit var pauseContainer: LinearLayout
    private lateinit var playAgainButton: Button
    private lateinit var resumeButton: Button
    private lateinit var settingsButton: Button
    private lateinit var finalScoreText: TextView
    private lateinit var nextPieceView: NextPieceView
    private lateinit var levelDownButton: Button
    private lateinit var levelUpButton: Button
    private lateinit var selectedLevelText: TextView
    private lateinit var gameHaptics: GameHaptics
    
    // Game state
    private var isSoundEnabled = true
    private var selectedLevel = 1
    private val maxLevel = 10
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize game haptics
        gameHaptics = GameHaptics(this)
        
        // Set up game view
        gameView = binding.gameView
        scoreText = binding.scoreText
        levelText = binding.levelText
        linesText = binding.linesText
        gameOverContainer = binding.gameOverContainer
        pauseContainer = binding.pauseContainer
        playAgainButton = binding.playAgainButton
        resumeButton = binding.resumeButton
        settingsButton = binding.settingsButton
        finalScoreText = binding.finalScoreText
        nextPieceView = binding.nextPieceView
        levelDownButton = binding.levelDownButton
        levelUpButton = binding.levelUpButton
        selectedLevelText = binding.selectedLevelText
        
        // Connect the next piece view to the game view
        nextPieceView.setGameView(gameView)
        
        // Set up callbacks
        gameView.onGameStateChanged = { score, level, lines ->
            updateUI(score, level, lines)
        }
        
        gameView.onGameOver = { score ->
            showGameOver(score)
        }
        
        gameView.onLineClear = { lineCount ->
            // Use enhanced haptic feedback for line clears
            if (isSoundEnabled) {
                gameHaptics.vibrateForLineClear(lineCount)
            }
        }
        
        // Add callbacks for piece movement and locking
        gameView.onPieceMove = {
            if (isSoundEnabled) {
                gameHaptics.vibrateForPieceMove()
            }
        }
        
        gameView.onPieceLock = {
            if (isSoundEnabled) {
                gameHaptics.vibrateForPieceLock()
            }
        }
        
        // Set up button click listeners with haptic feedback
        playAgainButton.setOnClickListener {
            gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
            hideGameOver()
            gameView.reset()
            setGameLevel(selectedLevel)
            gameView.start()
        }
        
        resumeButton.setOnClickListener {
            gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
            hidePauseMenu()
            gameView.start()
        }
        
        settingsButton.setOnClickListener {
            gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
            toggleSound()
        }
        
        // Set up level selector with haptic feedback
        levelDownButton.setOnClickListener {
            if (selectedLevel > 1) {
                gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
                selectedLevel--
                updateLevelSelector()
            }
        }
        
        levelUpButton.setOnClickListener {
            if (selectedLevel < maxLevel) {
                gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
                selectedLevel++
                updateLevelSelector()
            }
        }
        
        // Initialize level selector
        updateLevelSelector()
        
        // Start game when clicking the screen initially
        setupTouchToStart()
        
        // Start with the game paused
        gameView.pause()
        
        // Enable edge-to-edge display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
    }
    
    /**
     * Set up touch-to-start behavior for initial screen
     */
    private fun setupTouchToStart() {
        val touchToStart = View.OnClickListener {
            if (gameView.isGameOver()) {
                hideGameOver()
                gameView.reset()
                gameView.start()
            } else if (pauseContainer.visibility == View.VISIBLE) {
                hidePauseMenu()
                gameView.start()
            } else {
                gameView.start()
            }
        }
        
        // Add the click listener to the game view
        gameView.setOnClickListener(touchToStart)
    }
    
    /**
     * Update UI with current game state
     */
    private fun updateUI(score: Int, level: Int, lines: Int) {
        scoreText.text = score.toString()
        levelText.text = level.toString()
        linesText.text = lines.toString()
        
        // Force redraw of next piece preview
        nextPieceView.invalidate()
    }
    
    /**
     * Show game over screen
     */
    private fun showGameOver(score: Int) {
        finalScoreText.text = getString(R.string.score) + ": " + score
        gameOverContainer.visibility = View.VISIBLE
        
        // Vibrate to indicate game over
        vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK)
    }
    
    /**
     * Hide game over screen
     */
    private fun hideGameOver() {
        gameOverContainer.visibility = View.GONE
    }
    
    /**
     * Show pause menu
     */
    private fun showPauseMenu() {
        pauseContainer.visibility = View.VISIBLE
    }
    
    /**
     * Hide pause menu
     */
    private fun hidePauseMenu() {
        pauseContainer.visibility = View.GONE
    }
    
    /**
     * Toggle sound on/off
     */
    private fun toggleSound() {
        isSoundEnabled = !isSoundEnabled
        settingsButton.text = getString(
            if (isSoundEnabled) R.string.sound_on else R.string.sound_off
        )
        
        // Vibrate to provide feedback
        vibrate(VibrationEffect.EFFECT_CLICK)
    }
    
    /**
     * Trigger device vibration with predefined effect
     */
    private fun vibrate(effectId: Int) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createPredefined(effectId))
    }
    
    /**
     * Update the level selector display
     */
    private fun updateLevelSelector() {
        selectedLevelText.text = selectedLevel.toString()
    }
    
    /**
     * Set the game level
     */
    private fun setGameLevel(level: Int) {
        gameView.gameBoard.level = level
        gameView.gameBoard.lines = (level - 1) * 10
        gameView.gameBoard.dropInterval = (1000 * Math.pow(0.8, (level - 1).toDouble())).toLong()
        
        // Update UI
        levelText.text = level.toString()
        linesText.text = gameView.gameBoard.lines.toString()
    }
    
    override fun onPause() {
        super.onPause()
        if (!gameView.isGameOver()) {
            gameView.pause()
            showPauseMenu()
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (gameOverContainer.visibility == View.VISIBLE) {
            hideGameOver()
            gameView.reset()
            return
        }
        
        if (pauseContainer.visibility == View.GONE) {
            gameView.pause()
            showPauseMenu()
        } else {
            hidePauseMenu()
            gameView.start()
        }
    }
} 