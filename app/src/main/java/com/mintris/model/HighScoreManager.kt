package com.mintris.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class HighScoreManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type: Type = object : TypeToken<List<HighScore>>() {}.type

    companion object {
        private const val PREFS_NAME = "mintris_highscores"
        private const val KEY_HIGHSCORES = "highscores"
        private const val MAX_HIGHSCORES = 5
    }

    fun getHighScores(): List<HighScore> {
        val json = prefs.getString(KEY_HIGHSCORES, null)
        return if (json != null) {
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun addHighScore(highScore: HighScore) {
        val currentScores = getHighScores().toMutableList()
        currentScores.add(highScore)
        
        // Sort by score (descending) and keep only top 5
        currentScores.sortByDescending { it.score }
        val topScores = currentScores.take(MAX_HIGHSCORES)
        
        // Save to SharedPreferences
        val json = gson.toJson(topScores)
        prefs.edit().putString(KEY_HIGHSCORES, json).apply()
    }

    fun isHighScore(score: Int): Boolean {
        val currentScores = getHighScores()
        return currentScores.size < MAX_HIGHSCORES || 
               score > (currentScores.lastOrNull()?.score ?: 0)
    }
} 