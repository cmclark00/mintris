package com.mintris.model

import android.content.Context
import android.content.SharedPreferences
import com.mintris.R
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Manages player progression, experience points, and unlockable rewards
 */
class PlayerProgressionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Player level and XP
    private var playerLevel: Int = 1
    private var playerXP: Long = 0
    private var totalXPEarned: Long = 0
    
    // Track unlocked rewards
    private val unlockedThemes = mutableSetOf<String>()
    private val unlockedBlocks = mutableSetOf<String>()
    private val unlockedPowers = mutableSetOf<String>()
    private val unlockedBadges = mutableSetOf<String>()
    
    // XP gained in the current session
    private var sessionXPGained: Long = 0
    
    init {
        loadProgress()
    }
    
    /**
     * Load player progression data from shared preferences
     */
    private fun loadProgress() {
        playerLevel = prefs.getInt(KEY_PLAYER_LEVEL, 1)
        playerXP = prefs.getLong(KEY_PLAYER_XP, 0)
        totalXPEarned = prefs.getLong(KEY_TOTAL_XP_EARNED, 0)
        
        // Load unlocked rewards
        val themesSet = prefs.getStringSet(KEY_UNLOCKED_THEMES, setOf()) ?: setOf()
        val blocksSet = prefs.getStringSet(KEY_UNLOCKED_BLOCKS, setOf()) ?: setOf()
        val powersSet = prefs.getStringSet(KEY_UNLOCKED_POWERS, setOf()) ?: setOf()
        val badgesSet = prefs.getStringSet(KEY_UNLOCKED_BADGES, setOf()) ?: setOf()
        
        unlockedThemes.addAll(themesSet)
        unlockedBlocks.addAll(blocksSet)
        unlockedPowers.addAll(powersSet)
        unlockedBadges.addAll(badgesSet)
        
        // Add default theme if nothing is unlocked
        if (unlockedThemes.isEmpty()) {
            unlockedThemes.add(THEME_CLASSIC)
        }
    }
    
    /**
     * Save player progression data to shared preferences
     */
    private fun saveProgress() {
        prefs.edit()
            .putInt(KEY_PLAYER_LEVEL, playerLevel)
            .putLong(KEY_PLAYER_XP, playerXP)
            .putLong(KEY_TOTAL_XP_EARNED, totalXPEarned)
            .putStringSet(KEY_UNLOCKED_THEMES, unlockedThemes)
            .putStringSet(KEY_UNLOCKED_BLOCKS, unlockedBlocks)
            .putStringSet(KEY_UNLOCKED_POWERS, unlockedPowers)
            .putStringSet(KEY_UNLOCKED_BADGES, unlockedBadges)
            .apply()
    }
    
    /**
     * Calculate XP required to reach a specific level
     */
    fun calculateXPForLevel(level: Int): Long {
        return (BASE_XP * level.toDouble().pow(XP_CURVE_FACTOR)).roundToInt().toLong()
    }
    
    /**
     * Calculate total XP required to reach a certain level from level 1
     */
    fun calculateTotalXPForLevel(level: Int): Long {
        var totalXP = 0L
        for (lvl in 1 until level) {
            totalXP += calculateXPForLevel(lvl)
        }
        return totalXP
    }
    
    /**
     * Calculate XP from a game session based on score, lines, level, etc.
     */
    fun calculateGameXP(score: Int, lines: Int, level: Int, gameTime: Long, 
                        tetrisCount: Int, perfectClearCount: Int): Long {
        // Base XP from score with level multiplier
        val scoreXP = (score * (1 + LEVEL_MULTIPLIER * level)).toLong()
        
        // XP from lines cleared
        val linesXP = lines * XP_PER_LINE
        
        // XP from special moves
        val tetrisBonus = tetrisCount * TETRIS_XP_BONUS
        val perfectClearBonus = perfectClearCount * PERFECT_CLEAR_XP_BONUS
        
        // Time bonus (to reward longer gameplay)
        val timeBonus = (gameTime / 60000) * TIME_XP_PER_MINUTE // XP per minute played
        
        // Calculate total XP
        return scoreXP + linesXP + tetrisBonus + perfectClearBonus + timeBonus
    }
    
    /**
     * Add XP to the player and handle level-ups
     * Returns a list of newly unlocked rewards
     */
    fun addXP(xpAmount: Long): List<String> {
        sessionXPGained = xpAmount
        playerXP += xpAmount
        totalXPEarned += xpAmount
        
        val newRewards = mutableListOf<String>()
        val oldLevel = playerLevel
        
        // Check for level ups
        var xpForNextLevel = calculateXPForLevel(playerLevel)
        while (playerXP >= xpForNextLevel) {
            playerXP -= xpForNextLevel
            playerLevel++
            
            // Check for new rewards at this level
            val levelRewards = checkLevelRewards(playerLevel)
            newRewards.addAll(levelRewards)
            
            // Calculate XP needed for the next level
            xpForNextLevel = calculateXPForLevel(playerLevel)
        }
        
        // Save progress if there were any changes
        if (oldLevel != playerLevel || newRewards.isNotEmpty()) {
            saveProgress()
        }
        
        return newRewards
    }
    
    /**
     * Check if the player unlocked new rewards at the current level
     */
    private fun checkLevelRewards(level: Int): List<String> {
        val newRewards = mutableListOf<String>()
        
        // Check for theme unlocks
        when (level) {
            5 -> {
                if (unlockedThemes.add(THEME_NEON)) {
                    newRewards.add("Unlocked Neon Theme!")
                }
            }
            10 -> {
                if (unlockedThemes.add(THEME_MONOCHROME)) {
                    newRewards.add("Unlocked Monochrome Theme!")
                }
            }
            15 -> {
                if (unlockedThemes.add(THEME_RETRO)) {
                    newRewards.add("Unlocked Retro Arcade Theme!")
                }
            }
            20 -> {
                if (unlockedThemes.add(THEME_MINIMALIST)) {
                    newRewards.add("Unlocked Minimalist Theme!")
                }
            }
            25 -> {
                if (unlockedThemes.add(THEME_GALAXY)) {
                    newRewards.add("Unlocked Galaxy Theme!")
                }
            }
        }
        
        // Check for power unlocks
        when (level) {
            8 -> {
                if (unlockedPowers.add(POWER_FREEZE_TIME)) {
                    newRewards.add("Unlocked Freeze Time Power!")
                }
            }
            12 -> {
                if (unlockedPowers.add(POWER_BLOCK_SWAP)) {
                    newRewards.add("Unlocked Block Swap Power!")
                }
            }
            18 -> {
                if (unlockedPowers.add(POWER_SAFE_LANDING)) {
                    newRewards.add("Unlocked Safe Landing Power!")
                }
            }
            30 -> {
                if (unlockedPowers.add(POWER_PERFECT_CLEAR)) {
                    newRewards.add("Unlocked Perfect Clear Power!")
                }
            }
        }
        
        // Check for block skin unlocks
        if (level % 7 == 0 && level <= 35) {
            val blockSkin = "block_skin_${level / 7}"
            if (unlockedBlocks.add(blockSkin)) {
                newRewards.add("Unlocked New Block Skin!")
            }
        }
        
        return newRewards
    }
    
    /**
     * Start a new progression session
     */
    fun startNewSession() {
        sessionXPGained = 0
    }
    
    // Getters
    fun getPlayerLevel(): Int = playerLevel
    fun getCurrentXP(): Long = playerXP
    fun getXPForNextLevel(): Long = calculateXPForLevel(playerLevel)
    fun getSessionXPGained(): Long = sessionXPGained
    fun getUnlockedThemes(): Set<String> = unlockedThemes.toSet()
    fun getUnlockedPowers(): Set<String> = unlockedPowers.toSet()
    fun getUnlockedBlocks(): Set<String> = unlockedBlocks.toSet()
    fun getUnlockedBadges(): Set<String> = unlockedBadges.toSet()
    
    /**
     * Check if a specific theme is unlocked
     */
    fun isThemeUnlocked(themeId: String): Boolean {
        return unlockedThemes.contains(themeId)
    }
    
    /**
     * Check if a specific power is unlocked
     */
    fun isPowerUnlocked(powerId: String): Boolean {
        return unlockedPowers.contains(powerId)
    }
    
    /**
     * Award a badge to the player
     */
    fun awardBadge(badgeId: String): Boolean {
        val newlyAwarded = unlockedBadges.add(badgeId)
        if (newlyAwarded) {
            saveProgress()
        }
        return newlyAwarded
    }
    
    /**
     * Reset all player progression data
     */
    fun resetProgress() {
        playerLevel = 1
        playerXP = 0
        totalXPEarned = 0
        
        unlockedThemes.clear()
        unlockedBlocks.clear()
        unlockedPowers.clear()
        unlockedBadges.clear()
        
        // Add default theme
        unlockedThemes.add(THEME_CLASSIC)
        
        saveProgress()
    }
    
    companion object {
        private const val PREFS_NAME = "mintris_progression"
        private const val KEY_PLAYER_LEVEL = "player_level"
        private const val KEY_PLAYER_XP = "player_xp"
        private const val KEY_TOTAL_XP_EARNED = "total_xp_earned"
        private const val KEY_UNLOCKED_THEMES = "unlocked_themes"
        private const val KEY_UNLOCKED_BLOCKS = "unlocked_blocks"
        private const val KEY_UNLOCKED_POWERS = "unlocked_powers"
        private const val KEY_UNLOCKED_BADGES = "unlocked_badges"
        
        // XP curve parameters
        private const val BASE_XP = 5000.0 // Base XP for level 1 (increased from 2500)
        private const val XP_CURVE_FACTOR = 2.2 // Exponential factor for XP curve (increased from 2.0)
        
        // XP calculation constants
        private const val LEVEL_MULTIPLIER = 0.1 // 10% bonus per level
        private const val XP_PER_LINE = 10L
        private const val TETRIS_XP_BONUS = 50L
        private const val PERFECT_CLEAR_XP_BONUS = 200L
        private const val TIME_XP_PER_MINUTE = 5L
        
        // Theme IDs with required levels
        const val THEME_CLASSIC = "theme_classic"
        const val THEME_NEON = "theme_neon"
        const val THEME_MONOCHROME = "theme_monochrome"
        const val THEME_RETRO = "theme_retro"
        const val THEME_MINIMALIST = "theme_minimalist"
        const val THEME_GALAXY = "theme_galaxy"
        
        // Map of themes to required levels
        val THEME_REQUIRED_LEVELS = mapOf(
            THEME_CLASSIC to 1,
            THEME_NEON to 5,
            THEME_MONOCHROME to 10,
            THEME_RETRO to 15,
            THEME_MINIMALIST to 20,
            THEME_GALAXY to 25
        )
        
        // Power IDs
        const val POWER_FREEZE_TIME = "power_freeze_time"
        const val POWER_BLOCK_SWAP = "power_block_swap"
        const val POWER_SAFE_LANDING = "power_safe_landing"
        const val POWER_PERFECT_CLEAR = "power_perfect_clear"
        
        // Map of powers to required levels
        val POWER_REQUIRED_LEVELS = mapOf(
            POWER_FREEZE_TIME to 8,
            POWER_BLOCK_SWAP to 12,
            POWER_SAFE_LANDING to 18,
            POWER_PERFECT_CLEAR to 30
        )
    }
    
    /**
     * Get the required level for a specific theme
     */
    fun getRequiredLevelForTheme(themeId: String): Int {
        return THEME_REQUIRED_LEVELS[themeId] ?: 1
    }
    
    /**
     * Get the required level for a specific power
     */
    fun getRequiredLevelForPower(powerId: String): Int {
        return POWER_REQUIRED_LEVELS[powerId] ?: 1
    }
} 