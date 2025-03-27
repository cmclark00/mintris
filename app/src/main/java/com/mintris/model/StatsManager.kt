package com.mintris.model

import android.content.Context
import android.content.SharedPreferences

class StatsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Lifetime stats
    private var totalGames: Int = 0
    private var totalScore: Long = 0
    private var totalLines: Int = 0
    private var totalPieces: Int = 0
    private var totalTime: Long = 0
    private var maxLevel: Int = 0
    private var maxScore: Int = 0
    private var maxLines: Int = 0
    
    // Line clear stats (lifetime)
    private var totalSingles: Int = 0
    private var totalDoubles: Int = 0
    private var totalTriples: Int = 0
    private var totalTetrises: Int = 0
    
    // Session stats
    private var sessionScore: Int = 0
    private var sessionLines: Int = 0
    private var sessionPieces: Int = 0
    private var sessionTime: Long = 0
    private var sessionLevel: Int = 0
    
    // Line clear stats (session)
    private var sessionSingles: Int = 0
    private var sessionDoubles: Int = 0
    private var sessionTriples: Int = 0
    private var sessionTetrises: Int = 0
    
    init {
        loadStats()
    }
    
    private fun loadStats() {
        totalGames = prefs.getInt(KEY_TOTAL_GAMES, 0)
        totalScore = prefs.getLong(KEY_TOTAL_SCORE, 0)
        totalLines = prefs.getInt(KEY_TOTAL_LINES, 0)
        totalPieces = prefs.getInt(KEY_TOTAL_PIECES, 0)
        totalTime = prefs.getLong(KEY_TOTAL_TIME, 0)
        maxLevel = prefs.getInt(KEY_MAX_LEVEL, 0)
        maxScore = prefs.getInt(KEY_MAX_SCORE, 0)
        maxLines = prefs.getInt(KEY_MAX_LINES, 0)
        
        // Load line clear stats
        totalSingles = prefs.getInt(KEY_TOTAL_SINGLES, 0)
        totalDoubles = prefs.getInt(KEY_TOTAL_DOUBLES, 0)
        totalTriples = prefs.getInt(KEY_TOTAL_TRIPLES, 0)
        totalTetrises = prefs.getInt(KEY_TOTAL_TETRISES, 0)
    }
    
    private fun saveStats() {
        prefs.edit()
            .putInt(KEY_TOTAL_GAMES, totalGames)
            .putLong(KEY_TOTAL_SCORE, totalScore)
            .putInt(KEY_TOTAL_LINES, totalLines)
            .putInt(KEY_TOTAL_PIECES, totalPieces)
            .putLong(KEY_TOTAL_TIME, totalTime)
            .putInt(KEY_MAX_LEVEL, maxLevel)
            .putInt(KEY_MAX_SCORE, maxScore)
            .putInt(KEY_MAX_LINES, maxLines)
            .putInt(KEY_TOTAL_SINGLES, totalSingles)
            .putInt(KEY_TOTAL_DOUBLES, totalDoubles)
            .putInt(KEY_TOTAL_TRIPLES, totalTriples)
            .putInt(KEY_TOTAL_TETRISES, totalTetrises)
            .apply()
    }
    
    fun startNewSession() {
        sessionScore = 0
        sessionLines = 0
        sessionPieces = 0
        sessionTime = 0
        sessionLevel = 0
        sessionSingles = 0
        sessionDoubles = 0
        sessionTriples = 0
        sessionTetrises = 0
    }
    
    fun updateSessionStats(score: Int, lines: Int, pieces: Int, time: Long, level: Int) {
        sessionScore = score
        sessionLines = lines
        sessionPieces = pieces
        sessionTime = time
        sessionLevel = level
    }
    
    fun recordLineClear(lineCount: Int) {
        when (lineCount) {
            1 -> {
                sessionSingles++
                totalSingles++
            }
            2 -> {
                sessionDoubles++
                totalDoubles++
            }
            3 -> {
                sessionTriples++
                totalTriples++
            }
            4 -> {
                sessionTetrises++
                totalTetrises++
            }
        }
    }
    
    fun endSession() {
        totalGames++
        totalScore += sessionScore
        totalLines += sessionLines
        totalPieces += sessionPieces
        totalTime += sessionTime
        
        if (sessionLevel > maxLevel) maxLevel = sessionLevel
        if (sessionScore > maxScore) maxScore = sessionScore
        if (sessionLines > maxLines) maxLines = sessionLines
        
        saveStats()
    }
    
    // Getters for lifetime stats
    fun getTotalGames(): Int = totalGames
    fun getTotalScore(): Long = totalScore
    fun getTotalLines(): Int = totalLines
    fun getTotalPieces(): Int = totalPieces
    fun getTotalTime(): Long = totalTime
    fun getMaxLevel(): Int = maxLevel
    fun getMaxScore(): Int = maxScore
    fun getMaxLines(): Int = maxLines
    
    // Getters for line clear stats (lifetime)
    fun getTotalSingles(): Int = totalSingles
    fun getTotalDoubles(): Int = totalDoubles
    fun getTotalTriples(): Int = totalTriples
    fun getTotalTetrises(): Int = totalTetrises
    
    // Getters for session stats
    fun getSessionScore(): Int = sessionScore
    fun getSessionLines(): Int = sessionLines
    fun getSessionPieces(): Int = sessionPieces
    fun getSessionTime(): Long = sessionTime
    fun getSessionLevel(): Int = sessionLevel
    
    // Getters for line clear stats (session)
    fun getSessionSingles(): Int = sessionSingles
    fun getSessionDoubles(): Int = sessionDoubles
    fun getSessionTriples(): Int = sessionTriples
    fun getSessionTetrises(): Int = sessionTetrises
    
    fun resetStats() {
        // Reset all lifetime stats
        totalGames = 0
        totalScore = 0
        totalLines = 0
        totalPieces = 0
        totalTime = 0
        maxLevel = 0
        maxScore = 0
        maxLines = 0
        
        // Reset line clear stats
        totalSingles = 0
        totalDoubles = 0
        totalTriples = 0
        totalTetrises = 0
        
        // Save the reset stats
        saveStats()
    }
    
    companion object {
        private const val PREFS_NAME = "mintris_stats"
        private const val KEY_TOTAL_GAMES = "total_games"
        private const val KEY_TOTAL_SCORE = "total_score"
        private const val KEY_TOTAL_LINES = "total_lines"
        private const val KEY_TOTAL_PIECES = "total_pieces"
        private const val KEY_TOTAL_TIME = "total_time"
        private const val KEY_MAX_LEVEL = "max_level"
        private const val KEY_MAX_SCORE = "max_score"
        private const val KEY_MAX_LINES = "max_lines"
        private const val KEY_TOTAL_SINGLES = "total_singles"
        private const val KEY_TOTAL_DOUBLES = "total_doubles"
        private const val KEY_TOTAL_TRIPLES = "total_triples"
        private const val KEY_TOTAL_TETRISES = "total_tetrises"
    }
} 