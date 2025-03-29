package com.mintris

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mintris.databinding.ActivityStatsBinding
import com.mintris.model.StatsManager
import com.mintris.model.PlayerProgressionManager
import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatsBinding
    private lateinit var statsManager: StatsManager
    private lateinit var progressionManager: PlayerProgressionManager
    private var currentTheme = PlayerProgressionManager.THEME_CLASSIC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        statsManager = StatsManager(this)
        progressionManager = PlayerProgressionManager(this)
        
        // Load and apply theme
        currentTheme = loadThemePreference()
        applyTheme(currentTheme)

        // Set up back button
        binding.backButton.setOnClickListener {
            finish()
        }

        // Set up reset stats button
        binding.resetStatsButton.setOnClickListener {
            showResetConfirmationDialog()
        }

        updateStats()
    }

    private fun loadThemePreference(): String {
        return progressionManager.getSelectedTheme()
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

        // Apply text color to all TextViews
        binding.totalGamesText.setTextColor(textColor)
        binding.totalScoreText.setTextColor(textColor)
        binding.totalLinesText.setTextColor(textColor)
        binding.totalPiecesText.setTextColor(textColor)
        binding.totalTimeText.setTextColor(textColor)
        binding.totalSinglesText.setTextColor(textColor)
        binding.totalDoublesText.setTextColor(textColor)
        binding.totalTriplesText.setTextColor(textColor)
        binding.totalTetrisesText.setTextColor(textColor)
        binding.maxLevelText.setTextColor(textColor)
        binding.maxScoreText.setTextColor(textColor)
        binding.maxLinesText.setTextColor(textColor)

        // Apply theme to buttons
        binding.backButton.setTextColor(textColor)
        binding.resetStatsButton.setTextColor(textColor)
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset Stats")
            .setMessage("Are you sure you want to reset all your stats? This cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                statsManager.resetStats()
                updateStats()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateStats() {
        // Format time duration
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        timeFormat.timeZone = TimeZone.getTimeZone("UTC")

        // Update lifetime stats
        binding.totalGamesText.text = getString(R.string.total_games, statsManager.getTotalGames())
        binding.totalScoreText.text = getString(R.string.total_score, statsManager.getTotalScore())
        binding.totalLinesText.text = getString(R.string.total_lines, statsManager.getTotalLines())
        binding.totalPiecesText.text = getString(R.string.total_pieces, statsManager.getTotalPieces())
        binding.totalTimeText.text = getString(R.string.total_time, timeFormat.format(statsManager.getTotalTime()))

        // Update line clear stats
        binding.totalSinglesText.text = getString(R.string.singles, statsManager.getTotalSingles())
        binding.totalDoublesText.text = getString(R.string.doubles, statsManager.getTotalDoubles())
        binding.totalTriplesText.text = getString(R.string.triples, statsManager.getTotalTriples())
        binding.totalTetrisesText.text = getString(R.string.tetrises, statsManager.getTotalTetrises())

        // Update best performance stats
        binding.maxLevelText.text = getString(R.string.max_level, statsManager.getMaxLevel())
        binding.maxScoreText.text = getString(R.string.max_score, statsManager.getMaxScore())
        binding.maxLinesText.text = getString(R.string.max_lines, statsManager.getMaxLines())
    }
} 