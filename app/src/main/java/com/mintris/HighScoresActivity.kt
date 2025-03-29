package com.mintris

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mintris.databinding.HighScoresBinding
import com.mintris.model.HighScoreAdapter
import com.mintris.model.HighScoreManager
import com.mintris.model.PlayerProgressionManager
import android.graphics.Color
import android.util.Log

class HighScoresActivity : AppCompatActivity() {
    private lateinit var binding: HighScoresBinding
    private lateinit var highScoreManager: HighScoreManager
    private lateinit var highScoreAdapter: HighScoreAdapter
    private lateinit var progressionManager: PlayerProgressionManager
    private var currentTheme = PlayerProgressionManager.THEME_CLASSIC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = HighScoresBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            highScoreManager = HighScoreManager(this)
            progressionManager = PlayerProgressionManager(this)
            
            // Load and apply theme
            currentTheme = loadThemePreference()
            
            // Initialize adapter before applying theme
            highScoreAdapter = HighScoreAdapter()
            
            // Set up RecyclerView
            binding.highScoresList.layoutManager = LinearLayoutManager(this)
            binding.highScoresList.adapter = highScoreAdapter
            
            // Now apply theme to UI
            applyTheme(currentTheme)
            
            // Load high scores
            updateHighScores()
            
            // Set up back button
            binding.backButton.setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            Log.e("HighScoresActivity", "Error in onCreate", e)
            // Show an error message if necessary, or finish gracefully
            finish()
        }
    }

    private fun loadThemePreference(): String {
        return progressionManager.getSelectedTheme()
    }

    private fun applyTheme(themeId: String) {
        try {
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
    
            // Apply theme to back button
            binding.backButton.setTextColor(textColor)
    
            // Update adapter theme
            highScoreAdapter.applyTheme(themeId)
        } catch (e: Exception) {
            Log.e("HighScoresActivity", "Error applying theme: $themeId", e)
        }
    }

    private fun updateHighScores() {
        try {
            val scores = highScoreManager.getHighScores()
            highScoreAdapter.updateHighScores(scores)
        } catch (e: Exception) {
            Log.e("HighScoresActivity", "Error updating high scores", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            updateHighScores()
        } catch (e: Exception) {
            Log.e("HighScoresActivity", "Error in onResume", e)
        }
    }
} 