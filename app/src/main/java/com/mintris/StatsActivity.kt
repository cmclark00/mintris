package com.mintris

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mintris.databinding.ActivityStatsBinding
import com.mintris.model.StatsManager
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatsBinding
    private lateinit var statsManager: StatsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        statsManager = StatsManager(this)

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