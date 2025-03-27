package com.mintris

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mintris.model.HighScoreAdapter
import com.mintris.model.HighScoreManager

class HighScoresActivity : AppCompatActivity() {
    private lateinit var highScoreManager: HighScoreManager
    private lateinit var highScoreAdapter: HighScoreAdapter
    private lateinit var highScoresList: RecyclerView
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.high_scores)

        highScoreManager = HighScoreManager(this)
        highScoresList = findViewById(R.id.highScoresList)
        backButton = findViewById(R.id.backButton)

        highScoreAdapter = HighScoreAdapter()
        highScoresList.layoutManager = LinearLayoutManager(this)
        highScoresList.adapter = highScoreAdapter

        updateHighScores()

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun updateHighScores() {
        val scores = highScoreManager.getHighScores()
        highScoreAdapter.updateHighScores(scores)
    }

    override fun onResume() {
        super.onResume()
        updateHighScores()
    }
} 