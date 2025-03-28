package com.mintris

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mintris.databinding.HighScoreEntryBinding
import com.mintris.model.HighScore
import com.mintris.model.HighScoreManager
import com.mintris.model.PlayerProgressionManager
import android.graphics.Color

class HighScoreEntryActivity : AppCompatActivity() {
    private lateinit var binding: HighScoreEntryBinding
    private lateinit var highScoreManager: HighScoreManager
    private lateinit var progressionManager: PlayerProgressionManager
    private var currentTheme = PlayerProgressionManager.THEME_CLASSIC
    private var score: Int = 0
    
    // Track if we already saved to prevent double-saving
    private var hasSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HighScoreEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        highScoreManager = HighScoreManager(this)
        progressionManager = PlayerProgressionManager(this)
        
        // Load and apply theme
        currentTheme = loadThemePreference()
        applyTheme(currentTheme)

        score = intent.getIntExtra("score", 0)
        binding.scoreText.text = "Score: $score"

        binding.saveButton.setOnClickListener {
            // Only allow saving once
            if (!hasSaved) {
                val name = binding.nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    hasSaved = true
                    val highScore = HighScore(name, score, 1)
                    highScoreManager.addHighScore(highScore)
                    
                    // Set result and finish
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }
    
    // Prevent accidental back button press from causing issues
    override fun onBackPressed() {
        super.onBackPressed()
        // If they haven't saved yet, consider it a cancel
        if (!hasSaved) {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    private fun loadThemePreference(): String {
        val prefs = getSharedPreferences("mintris_settings", MODE_PRIVATE)
        return prefs.getString("selected_theme", PlayerProgressionManager.THEME_CLASSIC) ?: PlayerProgressionManager.THEME_CLASSIC
    }

    private fun applyTheme(themeId: String) {
        // Set background color
        val backgroundColor = when (themeId) {
            PlayerProgressionManager.THEME_CLASSIC -> Color.BLACK
            PlayerProgressionManager.THEME_NEON -> Color.parseColor("#0D0221")
            PlayerProgressionManager.THEME_MONOCHROME -> Color.parseColor("#1A1A1A")
            PlayerProgressionManager.THEME_RETRO -> Color.parseColor("#3F2832")
            PlayerProgressionManager.THEME_MINIMALIST -> Color.WHITE
            PlayerProgressionManager.THEME_GALAXY -> Color.parseColor("#0B0C10")
            else -> Color.BLACK
        }
        binding.root.setBackgroundColor(backgroundColor)

        // Set text color
        val textColor = when (themeId) {
            PlayerProgressionManager.THEME_CLASSIC -> Color.WHITE
            PlayerProgressionManager.THEME_NEON -> Color.parseColor("#FF00FF")
            PlayerProgressionManager.THEME_MONOCHROME -> Color.LTGRAY
            PlayerProgressionManager.THEME_RETRO -> Color.parseColor("#FF5A5F")
            PlayerProgressionManager.THEME_MINIMALIST -> Color.BLACK
            PlayerProgressionManager.THEME_GALAXY -> Color.parseColor("#66FCF1")
            else -> Color.WHITE
        }

        // Apply text color to score and input
        binding.scoreText.setTextColor(textColor)
        binding.nameInput.setTextColor(textColor)
        binding.nameInput.setHintTextColor(Color.argb(128, Color.red(textColor), Color.green(textColor), Color.blue(textColor)))

        // Apply theme to submit button
        binding.saveButton.setTextColor(textColor)
    }
} 