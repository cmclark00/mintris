package com.mintris

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.HapticFeedbackConstants
import androidx.appcompat.app.AppCompatActivity
import com.mintris.databinding.ActivityMainBinding
import com.mintris.game.GameHaptics
import com.mintris.game.GameView
import com.mintris.game.TitleScreen
import com.mintris.model.GameBoard
import com.mintris.audio.GameMusic
import com.mintris.model.HighScoreManager
import com.mintris.model.PlayerProgressionManager
import com.mintris.model.StatsManager
import com.mintris.ui.ProgressionScreen
import com.mintris.ui.ThemeSelector
import com.mintris.ui.BlockSkinSelector
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Rect
import android.util.Log
import android.view.KeyEvent

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    // UI components
    private lateinit var binding: ActivityMainBinding
    private lateinit var gameView: GameView
    private lateinit var gameHaptics: GameHaptics
    private lateinit var gameBoard: GameBoard
    private lateinit var gameMusic: GameMusic
    private lateinit var titleScreen: TitleScreen
    private lateinit var highScoreManager: HighScoreManager
    private lateinit var statsManager: StatsManager
    private lateinit var progressionManager: PlayerProgressionManager
    private lateinit var progressionScreen: ProgressionScreen
    private lateinit var themeSelector: ThemeSelector
    private lateinit var blockSkinSelector: BlockSkinSelector
    
    // Game state
    private var isSoundEnabled = true
    private var isMusicEnabled = true
    private var selectedLevel = 1
    private val maxLevel = 20
    private var currentScore = 0
    private var currentLevel = 1
    private var gameStartTime: Long = 0
    private var piecesPlaced: Int = 0
    private var currentTheme = PlayerProgressionManager.THEME_CLASSIC
    
    // Activity result launcher for high score entry
    private lateinit var highScoreEntryLauncher: ActivityResultLauncher<Intent>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Register activity result launcher for high score entry
        highScoreEntryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // No matter what the result is, we just show the game over container
            progressionScreen.visibility = View.GONE
            binding.gameOverContainer.visibility = View.VISIBLE
        }
        
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Disable Android back gesture to prevent accidental app exits
        disableAndroidBackGesture()
        
        // Initialize game components
        gameBoard = GameBoard()
        gameHaptics = GameHaptics(this)
        gameView = binding.gameView
        titleScreen = binding.titleScreen
        gameMusic = GameMusic(this)
        highScoreManager = HighScoreManager(this)
        statsManager = StatsManager(this)
        progressionManager = PlayerProgressionManager(this)
        themeSelector = binding.themeSelector
        blockSkinSelector = binding.blockSkinSelector
        
        // Load and apply theme preference
        currentTheme = progressionManager.getSelectedTheme()
        applyTheme(currentTheme)
        
        // Load and apply block skin preference
        gameView.setBlockSkin(progressionManager.getSelectedBlockSkin())
        
        // Set up game view
        gameView.setGameBoard(gameBoard)
        gameView.setHaptics(gameHaptics)
        
        // Set up progression screen
        progressionScreen = binding.progressionScreen
        progressionScreen.visibility = View.GONE
        progressionScreen.onContinue = {
            progressionScreen.visibility = View.GONE
            binding.gameOverContainer.visibility = View.VISIBLE
        }
        
        // Set up theme selector
        themeSelector.onThemeSelected = { themeId: String ->
            // Apply the new theme
            applyTheme(themeId)
            
            // Provide haptic feedback as a cue that the theme changed
            gameHaptics.vibrateForPieceLock()
            
            // Refresh the pause menu to immediately show theme changes
            if (binding.pauseContainer.visibility == View.VISIBLE) {
                showPauseMenu()
            }
        }
        
        // Set up block skin selector
        blockSkinSelector.onBlockSkinSelected = { skinId: String ->
            // Apply the new block skin
            gameView.setBlockSkin(skinId)
            
            // Save the selection
            progressionManager.setSelectedBlockSkin(skinId)
            
            // Provide haptic feedback
            gameHaptics.vibrateForPieceLock()
        }
        
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
            Log.d(TAG, "Received line clear callback: $lineCount lines")
            // Use enhanced haptic feedback for line clears
            if (isSoundEnabled) {
                Log.d(TAG, "Sound is enabled, triggering haptic feedback")
                try {
                    gameHaptics.vibrateForLineClear(lineCount)
                    Log.d(TAG, "Haptic feedback triggered successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error triggering haptic feedback", e)
                }
            } else {
                Log.d(TAG, "Sound is disabled, skipping haptic feedback")
            }
            // Record line clear in stats
            statsManager.recordLineClear(lineCount)
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
            piecesPlaced++
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
        
        // Set up stats button
        binding.statsButton.setOnClickListener {
            gameHaptics.performHapticFeedback(it, HapticFeedbackConstants.VIRTUAL_KEY)
            val intent = Intent(this, StatsActivity::class.java)
            startActivity(intent)
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
        binding.comboText.text = gameBoard.getCombo().toString()
        
        // Update current level for stats
        currentLevel = level
        
        // Force redraw of next piece preview
        binding.nextPieceView.invalidate()
    }
    
    /**
     * Show game over screen
     */
    private fun showGameOver(score: Int) {
        val gameTime = System.currentTimeMillis() - gameStartTime
        
        // Update session stats
        statsManager.updateSessionStats(
            score = score,
            lines = gameBoard.lines,
            pieces = piecesPlaced,
            time = gameTime,
            level = currentLevel
        )
        
        // Calculate XP earned
        val xpGained = progressionManager.calculateGameXP(
            score = score,
            lines = gameBoard.lines,
            level = currentLevel,
            gameTime = gameTime,
            tetrisCount = statsManager.getSessionTetrises(),
            perfectClearCount = 0 // Implement perfect clear tracking if needed
        )
        
        // Add XP and check for rewards
        val newRewards = progressionManager.addXP(xpGained)
        
        // End session and save stats
        statsManager.endSession()
        
        // Update session stats display
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        timeFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        binding.sessionScoreText.text = getString(R.string.session_score, score)
        binding.sessionLinesText.text = getString(R.string.session_lines, gameBoard.lines)
        binding.sessionPiecesText.text = getString(R.string.session_pieces, piecesPlaced)
        binding.sessionTimeText.text = getString(R.string.session_time, timeFormat.format(gameTime))
        binding.sessionLevelText.text = getString(R.string.session_level, currentLevel)
        
        // Update session line clear stats
        binding.sessionSinglesText.text = getString(R.string.singles, statsManager.getSessionSingles())
        binding.sessionDoublesText.text = getString(R.string.doubles, statsManager.getSessionDoubles())
        binding.sessionTriplesText.text = getString(R.string.triples, statsManager.getSessionTriples())
        binding.sessionTetrisesText.text = getString(R.string.tetrises, statsManager.getSessionTetrises())
        
        // Flag to track if high score screen will be shown
        var showingHighScore = false
        
        // Show progression screen first with XP animation
        binding.gameOverContainer.visibility = View.GONE
        progressionScreen.visibility = View.VISIBLE
        progressionScreen.applyTheme(currentTheme)
        progressionScreen.showProgress(progressionManager, xpGained, newRewards, currentTheme)
        
        // Override the continue button behavior if high score needs to be shown
        val originalOnContinue = progressionScreen.onContinue
        
        progressionScreen.onContinue = {
            // If this is a high score, show high score entry screen
            if (highScoreManager.isHighScore(score)) {
                showingHighScore = true
                showHighScoreEntry(score)
            } else {
                // Just show game over screen normally
                progressionScreen.visibility = View.GONE
                binding.gameOverContainer.visibility = View.VISIBLE
                
                // Update theme selector if new themes were unlocked
                if (newRewards.any { it.contains("Theme") }) {
                    updateThemeSelector()
                }
            }
        }
        
        // Vibrate to indicate game over
        vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK)
    }
    
    /**
     * Show high score entry screen
     */
    private fun showHighScoreEntry(score: Int) {
        val intent = Intent(this, HighScoreEntryActivity::class.java).apply {
            putExtra("score", score)
            putExtra("level", currentLevel)
        }
        // Use the launcher instead of startActivity
        highScoreEntryLauncher.launch(intent)
    }
    
    /**
     * Hide game over screen
     */
    private fun hideGameOver() {
        binding.gameOverContainer.visibility = View.GONE
        progressionScreen.visibility = View.GONE
    }
    
    /**
     * Show settings menu
     */
    private fun showPauseMenu() {
        binding.pauseContainer.visibility = View.VISIBLE
        binding.pauseStartButton.visibility = View.VISIBLE
        binding.resumeButton.visibility = View.GONE
        
        // Update level badge
        binding.pauseLevelBadge.setLevel(progressionManager.getPlayerLevel())
        binding.pauseLevelBadge.setThemeColor(getThemeColor(currentTheme))
        
        // Get theme color
        val textColor = getThemeColor(currentTheme)
        
        // Apply theme color to pause container background
        val backgroundColor = when (currentTheme) {
            PlayerProgressionManager.THEME_CLASSIC -> Color.BLACK
            PlayerProgressionManager.THEME_NEON -> Color.parseColor("#0D0221")
            PlayerProgressionManager.THEME_MONOCHROME -> Color.parseColor("#1A1A1A")
            PlayerProgressionManager.THEME_RETRO -> Color.parseColor("#3F2832")
            PlayerProgressionManager.THEME_MINIMALIST -> Color.WHITE
            PlayerProgressionManager.THEME_GALAXY -> Color.parseColor("#0B0C10")
            else -> Color.BLACK
        }
        binding.pauseContainer.setBackgroundColor(backgroundColor)
        
        // Apply theme colors to buttons
        binding.pauseStartButton.setTextColor(textColor)
        binding.pauseRestartButton.setTextColor(textColor)
        binding.resumeButton.setTextColor(textColor)
        binding.highScoresButton.setTextColor(textColor)
        binding.statsButton.setTextColor(textColor)
        binding.pauseLevelText.setTextColor(textColor)
        binding.pauseLevelUpButton.setTextColor(textColor)
        binding.pauseLevelDownButton.setTextColor(textColor)
        binding.settingsButton.setTextColor(textColor)
        binding.musicToggle.setColorFilter(textColor)
        
        // Apply theme colors to text elements
        binding.settingsTitle.setTextColor(textColor)
        binding.selectLevelText.setTextColor(textColor)
        binding.musicText.setTextColor(textColor)
        
        // Update theme selector
        updateThemeSelector()
        
        // Update block skin selector
        blockSkinSelector.updateBlockSkins(
            progressionManager.getUnlockedBlocks(),
            gameView.getCurrentBlockSkin(),
            progressionManager.getPlayerLevel()
        )
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
        gameMusic.setEnabled(isMusicEnabled)
        if (isMusicEnabled) {
            gameMusic.start()
        }
        gameStartTime = System.currentTimeMillis()
        piecesPlaced = 0
        statsManager.startNewSession()
        progressionManager.startNewSession()
        gameBoard.updateLevel(selectedLevel)
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
        
        // Update theme selector with available themes when pause screen appears
        updateThemeSelector()
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
        titleScreen.applyTheme(currentTheme)
    }

    /**
     * Show high scores
     */
    private fun showHighScores() {
        val intent = Intent(this, HighScoresActivity::class.java)
        startActivity(intent)
    }
    
    /**
     * Update the theme selector with unlocked themes
     */
    private fun updateThemeSelector() {
        binding.themeSelector.updateThemes(
            unlockedThemes = progressionManager.getUnlockedThemes(),
            currentTheme = currentTheme
        )
    }
    
    /**
     * Apply a theme to the game
     */
    private fun applyTheme(themeId: String) {
        // Only apply if the theme is unlocked
        if (!progressionManager.isThemeUnlocked(themeId)) return
        
        // Save the selected theme
        currentTheme = themeId
        progressionManager.setSelectedTheme(themeId)

        // Apply theme to title screen if it's visible
        if (titleScreen.visibility == View.VISIBLE) {
            titleScreen.applyTheme(themeId)
        }
        
        // Apply theme colors based on theme ID
        when (themeId) {
            PlayerProgressionManager.THEME_CLASSIC -> {
                // Default black theme
                binding.root.setBackgroundColor(Color.BLACK)
            }
            PlayerProgressionManager.THEME_NEON -> {
                // Neon theme with dark purple background
                binding.root.setBackgroundColor(Color.parseColor("#0D0221"))
            }
            PlayerProgressionManager.THEME_MONOCHROME -> {
                // Monochrome dark gray
                binding.root.setBackgroundColor(Color.parseColor("#1A1A1A"))
            }
            PlayerProgressionManager.THEME_RETRO -> {
                // Retro arcade theme
                binding.root.setBackgroundColor(Color.parseColor("#3F2832"))
            }
            PlayerProgressionManager.THEME_MINIMALIST -> {
                // Minimalist white theme
                binding.root.setBackgroundColor(Color.WHITE)
                
                // Update text colors for visibility
                binding.scoreText.setTextColor(Color.BLACK)
                binding.currentLevelText.setTextColor(Color.BLACK)
                binding.linesText.setTextColor(Color.BLACK)
                binding.comboText.setTextColor(Color.BLACK)
            }
            PlayerProgressionManager.THEME_GALAXY -> {
                // Galaxy dark blue theme
                binding.root.setBackgroundColor(Color.parseColor("#0B0C10"))
            }
        }
        
        // Apply theme to progression screen if it's visible and initialized
        if (::progressionScreen.isInitialized && progressionScreen.visibility == View.VISIBLE) {
            progressionScreen.applyTheme(themeId)
        }
        
        // Apply theme color to the stats button
        val textColor = getThemeColor(currentTheme)
        binding.statsButton.setTextColor(textColor)
        
        // Update the game view to apply theme
        gameView.invalidate()
    }
    
    /**
     * Get the appropriate color for the current theme
     */
    private fun getThemeColor(themeId: String): Int {
        return when (themeId) {
            PlayerProgressionManager.THEME_CLASSIC -> Color.WHITE
            PlayerProgressionManager.THEME_NEON -> Color.parseColor("#FF00FF")
            PlayerProgressionManager.THEME_MONOCHROME -> Color.LTGRAY
            PlayerProgressionManager.THEME_RETRO -> Color.parseColor("#FF5A5F")
            PlayerProgressionManager.THEME_MINIMALIST -> Color.BLACK
            PlayerProgressionManager.THEME_GALAXY -> Color.parseColor("#66FCF1")
            else -> Color.WHITE
        }
    }

    /**
     * Disables the Android system back gesture to prevent accidental exits
     */
    private fun disableAndroidBackGesture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Set the entire window to be excluded from the system gesture areas
            window.decorView.post {
                // Create a list of rectangles representing the edges of the screen to exclude from system gestures
                val gestureInsets = window.decorView.rootWindowInsets?.systemGestureInsets
                if (gestureInsets != null) {
                    val leftEdge = Rect(0, 0, 50, window.decorView.height)
                    val rightEdge = Rect(window.decorView.width - 50, 0, window.decorView.width, window.decorView.height)
                    val bottomEdge = Rect(0, window.decorView.height - 50, window.decorView.width, window.decorView.height)
                    
                    window.decorView.systemGestureExclusionRects = listOf(leftEdge, rightEdge, bottomEdge)
                }
            }
        }
        
        // Add an on back pressed callback to handle back button/gesture
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // If we're playing the game, handle it as a pause action instead of exiting
                    if (gameView.visibility == View.VISIBLE && !gameView.isPaused && !gameView.isGameOver()) {
                        gameView.pause()
                        gameMusic.pause()
                        showPauseMenu()
                        binding.pauseStartButton.visibility = View.GONE
                        binding.resumeButton.visibility = View.VISIBLE
                    } else if (binding.pauseContainer.visibility == View.VISIBLE) {
                        // If pause menu is showing, handle as a resume
                        resumeGame()
                    } else if (binding.gameOverContainer.visibility == View.VISIBLE) {
                        // If game over is showing, go back to title
                        hideGameOver()
                        showTitleScreen()
                    } else if (titleScreen.visibility == View.VISIBLE) {
                        // If title screen is showing, allow normal back behavior (exit app)
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            })
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 (R) to Android 12 (S), use the WindowInsetsController to disable gestures
            window.insetsController?.systemBarsBehavior = 
                android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    /**
     * Completely block the hardware back button during gameplay
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // If back button is pressed
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Handle back button press as a pause action during gameplay
            if (gameView.visibility == View.VISIBLE && !gameView.isPaused && !gameView.isGameOver()) {
                gameView.pause()
                gameMusic.pause()
                showPauseMenu()
                binding.pauseStartButton.visibility = View.GONE
                binding.resumeButton.visibility = View.VISIBLE
                return true // Consume the event
            } else if (binding.pauseContainer.visibility == View.VISIBLE) {
                // If pause menu is showing, handle as a resume
                resumeGame()
                return true // Consume the event
            } else if (binding.gameOverContainer.visibility == View.VISIBLE) {
                // If game over is showing, go back to title
                hideGameOver()
                showTitleScreen()
                return true // Consume the event
            }
        }
        return super.onKeyDown(keyCode, event)
    }
} 