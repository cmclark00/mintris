package com.mintris.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mintris.R

class HighScoreAdapter : RecyclerView.Adapter<HighScoreAdapter.HighScoreViewHolder>() {
    private var highScores: List<HighScore> = emptyList()

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
    }

    override fun getItemCount(): Int = highScores.size

    class HighScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rankText: TextView = itemView.findViewById(R.id.rankText)
        private val nameText: TextView = itemView.findViewById(R.id.nameText)
        private val scoreText: TextView = itemView.findViewById(R.id.scoreText)

        fun bind(highScore: HighScore, rank: Int) {
            rankText.text = "#$rank"
            nameText.text = highScore.name
            scoreText.text = highScore.score.toString()
        }
    }
} 