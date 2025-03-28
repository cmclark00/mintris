package com.mintris

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mintris.model.HighScore
import com.mintris.model.HighScoreManager

class HighScoreEntryActivity : AppCompatActivity() {
    private lateinit var highScoreManager: HighScoreManager
    private lateinit var nameInput: EditText
    private lateinit var scoreText: TextView
    private lateinit var saveButton: Button
    
    // Track if we already saved to prevent double-saving
    private var hasSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.high_score_entry)

        highScoreManager = HighScoreManager(this)
        nameInput = findViewById(R.id.nameInput)
        scoreText = findViewById(R.id.scoreText)
        saveButton = findViewById(R.id.saveButton)

        val score = intent.getIntExtra("score", 0)
        val level = intent.getIntExtra("level", 1)
        scoreText.text = getString(R.string.score) + ": $score"

        saveButton.setOnClickListener {
            // Only allow saving once
            if (!hasSaved) {
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    hasSaved = true
                    val highScore = HighScore(name, score, level)
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
        // If they haven't saved yet, consider it a cancel
        if (!hasSaved) {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }
} 