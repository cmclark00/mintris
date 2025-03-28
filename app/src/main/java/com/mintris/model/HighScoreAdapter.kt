package com.mintris.model

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mintris.R

class HighScoreAdapter : RecyclerView.Adapter<HighScoreAdapter.HighScoreViewHolder>() {
    private var highScores: List<HighScore> = emptyList()
    private var currentTheme = "theme_classic" // Default theme
    private var textColor = Color.WHITE // Default text color

    fun updateHighScores(newHighScores: List<HighScore>) {
        highScores = newHighScores
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HighScoreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_high_score, parent, false)
        return HighScoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: HighScoreViewHolder, position: Int) {
        val highScore = highScores[position]
        holder.bind(highScore, position + 1)

        // Apply current text color to elements
        holder.rankText.setTextColor(textColor)
        holder.nameText.setTextColor(textColor)
        holder.scoreText.setTextColor(textColor)
    }

    override fun getItemCount(): Int = highScores.size

    class HighScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankText: TextView = itemView.findViewById(R.id.rankText)
        val nameText: TextView = itemView.findViewById(R.id.nameText)
        val scoreText: TextView = itemView.findViewById(R.id.scoreText)

        fun bind(highScore: HighScore, rank: Int) {
            rankText.text = "#$rank"
            nameText.text = highScore.name
            scoreText.text = highScore.score.toString()
        }
    }

    fun applyTheme(themeId: String) {
        currentTheme = themeId
        
        // Update text color based on theme
        textColor = when (themeId) {
            "theme_classic" -> Color.WHITE
            "theme_neon" -> Color.parseColor("#FF00FF")
            "theme_monochrome" -> Color.LTGRAY
            "theme_retro" -> Color.parseColor("#FF5A5F")
            "theme_minimalist" -> Color.BLACK
            "theme_galaxy" -> Color.parseColor("#66FCF1")
            else -> Color.WHITE
        }
        
        notifyDataSetChanged()
    }
} 