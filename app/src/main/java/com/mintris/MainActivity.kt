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
import com.mintris.game.TitleScreen
import android.view.HapticFeedbackConstants
import com.mintris.model.GameBoard
import com.mintris.audio.GameMusic
import com.mintris.model.HighScoreManager
import android.content.Intent

class MainActivity : AppCompatActivity() {
    
    // UI components
    private lateinit var binding: ActivityMainBinding
    private lateinit var gameView: GameView
    private lateinit var gameHaptics: GameHaptics
    private lateinit var gameBoard: GameBoard
    private lateinit var gameMusic: GameMusic
    private lateinit var titleScreen: TitleScreen
    private lateinit var highScoreManager: HighScoreManager
    
    // Game state
    private var isSoundEnabled = true
    private var isMusicEnabled = true
    private var selectedLevel = 1
    private val maxLevel = 20
    private var currentScore = 0
    private var currentLevel = 1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize game components
        gameBoard = GameBoard()
        gameHaptics = GameHaptics(this)
        gameView = binding.gameView
        titleScreen = binding.titleScreen
        gameMusic = GameMusic(this)
        highScoreManager = HighScoreManager(this)
        
        // Set up game view
        gameView.setGameBoard(gameBoard)
        gameView.setHaptics(gameHaptics)
        
        // Set up title screen
        titleScreen.onStartGame = {
            titleScreen.visibility = View.GONE
            gameView.visibility = View.VISIBLE
            binding.gameControlsContainer.visibility = View.VISIBLE
            startGame()
        }
        
        // Initially hide the game view and show title screen
        gameView.visibility = View.GONE
        binding.gameControlsContainer.visibility = View.GONE
        titleScreen.visibility = View.VISIBLE
        
        // Set up pause button to show settings menu
        binding.pauseButton.setOnClickListener {
            gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
            gameView.pause()
            gameMusic.pause()
            showPauseMenu()
            binding.pauseStartButton.visibility = View.GONE
            binding.resumeButton.visibility = View.VISIBLE
        }
        
        // Set up next piece preview
        binding.nextPieceView.setGameView(gameView)
        gameBoard.onNextPieceChanged = {
            binding.nextPieceView.invalidate()
        }
        
        // Set up music toggle
        binding.musicToggle.setOnClickListener {
            isMusicEnabled = !isMusicEnabled
            gameMusic.setEnabled(isMusicEnabled)
            updateMusicToggleUI()
        }
        
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
        binding.playAgainButton.setOnClickListener {
            gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
            hideGameOver()
            gameView.reset()
            startGame()
        }
        
        binding.resumeButton.setOnClickListener {
            gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
            hidePauseMenu()
            resumeGame()
        }
        
        binding.settingsButton.setOnClickListener {
            gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
            toggleSound()
        }

        // Set up pause menu buttons
        binding.pauseStartButton.setOnClickListener {
            gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
            hidePauseMenu()
            gameView.reset()
            startGame()
        }

        binding.pauseRestartButton.setOnClickListener {
            gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
            hidePauseMenu()
            gameView.reset()
            startGame()
        }

        binding.highScoresButton.setOnClickListener {
            gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
            showHighScores()
        }

        binding.pauseLevelUpButton.setOnClickListener {
            gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
            if (selectedLevel < maxLevel) {
                selectedLevel++
                updateLevelSelector()
            }
        }

        binding.pauseLevelDownButton.setOnClickListener {
            gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
            if (selectedLevel > 1) {
                selectedLevel--
                updateLevelSelector()
            }
        }
        
        // Initialize level selector
        updateLevelSelector()
        
        // Enable edge-to-edge display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
    }
    
    /**
     * Update UI with current game state
     */
    private fun updateUI(score: Int, level: Int, lines: Int) {
        binding.scoreText.text = score.toString()
        binding.currentLevelText.text = level.toString()
        binding.linesText.text = lines.toString()
        
        // Force redraw of next piece preview
        binding.nextPieceView.invalidate()
    }
    
    /**
     * Show game over screen
     */
    private fun showGameOver(score: Int) {
        binding.finalScoreText.text = getString(R.string.score) + ": " + score
        
        // Check if this is a high score
        if (highScoreManager.isHighScore(score)) {
            val intent = Intent(this, HighScoreEntryActivity::class.java).apply {
                putExtra("score", score)
                putExtra("level", currentLevel)
            }
            startActivity(intent)
        }
        
        binding.gameOverContainer.visibility = View.VISIBLE
        
        // Vibrate to indicate game over
        vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK)
    }
    
    /**
     * Hide game over screen
     */
    private fun hideGameOver() {
        binding.gameOverContainer.visibility = View.GONE
    }
    
    /**
     * Show settings menu
     */
    private fun showPauseMenu() {
        binding.pauseContainer.visibility = View.VISIBLE
        binding.pauseStartButton.visibility = View.VISIBLE
        binding.resumeButton.visibility = View.GONE
    }
    
    /**
     * Hide settings menu
     */
    private fun hidePauseMenu() {
        binding.pauseContainer.visibility = View.GONE
    }
    
    /**
     * Toggle sound on/off
     */
    private fun toggleSound() {
        isSoundEnabled = !isSoundEnabled
        binding.settingsButton.text = getString(
            if (isSoundEnabled) R.string.sound_on else R.string.sound_off
        )
        
        // Vibrate to provide feedback
        vibrate(VibrationEffect.EFFECT_CLICK)
    }
    
    /**
     * Update the level selector display
     */
    private fun updateLevelSelector() {
        binding.pauseLevelText.text = selectedLevel.toString()
        gameBoard.updateLevel(selectedLevel)
    }
    
    /**
     * Trigger device vibration with predefined effect
     */
    private fun vibrate(effectId: Int) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createPredefined(effectId))
    }
    
    private fun updateMusicToggleUI() {
        binding.musicToggle.setImageResource(
            if (isMusicEnabled) R.drawable.ic_volume_up
            else R.drawable.ic_volume_off
        )
    }
    
    private fun startGame() {
        gameView.start()
        gameMusic.setEnabled(isMusicEnabled)  // Explicitly set enabled state
        if (isMusicEnabled) {
            gameMusic.start()
        }
    }
    
    private fun restartGame() {
        gameBoard.reset()
        gameView.visibility = View.VISIBLE
        gameView.start()
        showPauseMenu()
    }
    
    private fun resumeGame() {
        gameView.resume()
        if (isMusicEnabled) {
            gameMusic.resume()
        }
        // Force a redraw to ensure pieces aren't frozen
        gameView.invalidate()
    }
    
    override fun onPause() {
        super.onPause()
        if (gameView.visibility == View.VISIBLE) {
            gameView.pause()
            gameMusic.pause()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // If we're on the title screen, don't auto-resume the game
        if (titleScreen.visibility == View.GONE && gameView.visibility == View.VISIBLE && binding.gameOverContainer.visibility == View.GONE && binding.pauseContainer.visibility == View.GONE) {
            resumeGame()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        gameMusic.release()
    }
    
    /**
     * Show title screen (for game restart)
     */
    private fun showTitleScreen() {
        gameView.reset()
        gameView.visibility = View.GONE
        binding.gameControlsContainer.visibility = View.GONE
        binding.gameOverContainer.visibility = View.GONE
        binding.pauseContainer.visibility = View.GONE
        titleScreen.visibility = View.VISIBLE
    }

    /**
     * Show high scores
     */
    private fun showHighScores() {
        val intent = Intent(this, HighScoresActivity::class.java)
        startActivity(intent)
    }
} 