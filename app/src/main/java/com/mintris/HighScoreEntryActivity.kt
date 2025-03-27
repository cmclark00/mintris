package com.mintris

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
            val name = nameInput.text.toString().trim()
            if (name.isNotEmpty()) {
                val highScore = HighScore(name, score, level)
                highScoreManager.addHighScore(highScore)
                finish()
            }
        }
    }
} 