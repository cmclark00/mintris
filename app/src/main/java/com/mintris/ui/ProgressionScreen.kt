package com.mintris.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.mintris.R
import com.mintris.model.PlayerProgressionManager

/**
 * Screen that displays player progression, XP gain, and unlocked rewards
 */
class ProgressionScreen @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // UI components
    private val xpProgressBar: XPProgressBar
    private val xpGainText: TextView
    private val playerLevelText: TextView
    private val rewardsContainer: LinearLayout
    private val continueButton: TextView
    
    // Current theme
    private var currentTheme: String = PlayerProgressionManager.THEME_CLASSIC
    
    // Callback for when the player dismisses the screen
    var onContinue: (() -> Unit)? = null
    
    init {
        orientation = VERTICAL
        
        // Inflate the layout
        LayoutInflater.from(context).inflate(R.layout.progression_screen, this, true)
        
        // Get references to views
        xpProgressBar = findViewById(R.id.xp_progress_bar)
        xpGainText = findViewById(R.id.xp_gain_text)
        playerLevelText = findViewById(R.id.player_level_text)
        rewardsContainer = findViewById(R.id.rewards_container)
        continueButton = findViewById(R.id.continue_button)
        
        // Set up button click listener
        continueButton.setOnClickListener {
            onContinue?.invoke()
        }
    }
    
    /**
     * Display progression data and animate XP gain
     */
    fun showProgress(
        progressionManager: PlayerProgressionManager,
        xpGained: Long,
        newRewards: List<String>,
        themeId: String = PlayerProgressionManager.THEME_CLASSIC
    ) {
        // Update current theme
        currentTheme = themeId
        
        // Hide rewards container initially if there are no new rewards
        rewardsContainer.visibility = if (newRewards.isEmpty()) View.GONE else View.INVISIBLE
        
        // Set initial progress bar state
        val playerLevel = progressionManager.getPlayerLevel()
        val currentXP = progressionManager.getCurrentXP()
        val xpForNextLevel = progressionManager.getXPForNextLevel()
        
        // Update texts
        playerLevelText.text = "Player Level: $playerLevel"
        xpGainText.text = "+$xpGained XP"
        
        // Update level up text visibility
        val progressionTitle = findViewById<TextView>(R.id.progression_title)
        progressionTitle.visibility = if (newRewards.any { it.contains("Level") }) View.VISIBLE else View.GONE
        
        // Start with initial animations
        AnimatorSet().apply {
            // Fade in the XP gain text
            val xpTextAnimator = ObjectAnimator.ofFloat(xpGainText, "alpha", 0f, 1f).apply {
                duration = 800
                interpolator = AccelerateDecelerateInterpolator()
            }
            
            // Set up the XP progress bar animation sequence
            val xpBarAnimator = ObjectAnimator.ofFloat(xpProgressBar, "alpha", 0f, 1f).apply {
                duration = 800
                interpolator = AccelerateDecelerateInterpolator()
            }
            
            // Play animations in sequence
            play(xpTextAnimator)
            play(xpBarAnimator).after(xpTextAnimator)
            start()
        }
        
        // Set initial progress bar state
        xpProgressBar.setXPValues(playerLevel, currentXP - xpGained, xpForNextLevel)
        
        // Animate the XP gain after a short delay
        postDelayed({
            xpProgressBar.animateXPGain(xpGained, playerLevel, currentXP, xpForNextLevel)
        }, 1000) // Increased delay to 1 second for better visual flow
        
        // If there are new rewards, show them with animation after XP bar animation
        if (newRewards.isNotEmpty()) {
            // Create reward cards
            rewardsContainer.removeAllViews()
            newRewards.forEach { reward ->
                val rewardCard = createRewardCard(reward)
                rewardsContainer.addView(rewardCard)
            }
            
            // Apply theme to newly created reward cards
            updateRewardCardColors(themeId)
            
            // Show rewards with animation after XP bar animation
            postDelayed({
                rewardsContainer.visibility = View.VISIBLE
                
                // Animate each reward card
                for (i in 0 until rewardsContainer.childCount) {
                    val card = rewardsContainer.getChildAt(i)
                    card.alpha = 0f
                    card.translationY = 100f
                    
                    // Stagger animation for each card
                    card.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(600) // Increased duration for smoother animation
                        .setStartDelay((i * 200).toLong()) // Increased delay between cards
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
            }, 2500) // Increased delay to wait for XP bar animation to finish
        }
    }
    
    /**
     * Create a card view to display a reward
     */
    private fun createRewardCard(rewardText: String): CardView {
        val card = CardView(context).apply {
            radius = 8f
            cardElevation = 4f
            useCompatPadding = true
            
            // Set background color based on current theme
            val backgroundColor = when (currentTheme) {
                PlayerProgressionManager.THEME_CLASSIC -> Color.BLACK
                PlayerProgressionManager.THEME_NEON -> Color.parseColor("#0D0221")
                PlayerProgressionManager.THEME_MONOCHROME -> Color.parseColor("#1A1A1A")
                PlayerProgressionManager.THEME_RETRO -> Color.parseColor("#3F2832")
                PlayerProgressionManager.THEME_MINIMALIST -> Color.WHITE
                PlayerProgressionManager.THEME_GALAXY -> Color.parseColor("#0B0C10")
                else -> Color.BLACK
            }
            setCardBackgroundColor(backgroundColor)
            
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8)
            }
        }
        
        // Add reward text
        val textView = TextView(context).apply {
            text = rewardText
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(16, 16, 16, 16)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            // Add some visual styling
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        card.addView(textView)
        return card
    }
    
    /**
     * Apply the current theme to the progression screen
     */
    fun applyTheme(themeId: String) {
        currentTheme = themeId
        
        // Get reference to the title text
        val progressionTitle = findViewById<TextView>(R.id.progression_title)
        val rewardsTitle = findViewById<TextView>(R.id.rewards_title)
        
        // Theme color for XP progress bar level badge
        val xpThemeColor: Int
        
        // Apply theme colors based on theme ID
        when (themeId) {
            PlayerProgressionManager.THEME_CLASSIC -> {
                // Default black theme
                setBackgroundColor(Color.BLACK)
                progressionTitle.setTextColor(Color.WHITE)
                playerLevelText.setTextColor(Color.WHITE)
                xpGainText.setTextColor(Color.WHITE)
                continueButton.setTextColor(Color.WHITE)
                rewardsTitle.setTextColor(Color.WHITE)
                xpThemeColor = Color.WHITE
            }
            PlayerProgressionManager.THEME_NEON -> {
                // Neon theme with dark purple background
                setBackgroundColor(Color.parseColor("#0D0221"))
                progressionTitle.setTextColor(Color.parseColor("#FF00FF"))
                playerLevelText.setTextColor(Color.parseColor("#FF00FF"))
                xpGainText.setTextColor(Color.WHITE)
                continueButton.setTextColor(Color.parseColor("#FF00FF"))
                rewardsTitle.setTextColor(Color.WHITE)
                xpThemeColor = Color.parseColor("#FF00FF")
            }
            PlayerProgressionManager.THEME_MONOCHROME -> {
                // Monochrome dark gray
                setBackgroundColor(Color.parseColor("#1A1A1A"))
                progressionTitle.setTextColor(Color.LTGRAY)
                playerLevelText.setTextColor(Color.LTGRAY)
                xpGainText.setTextColor(Color.WHITE)
                continueButton.setTextColor(Color.LTGRAY)
                rewardsTitle.setTextColor(Color.WHITE)
                xpThemeColor = Color.LTGRAY
            }
            PlayerProgressionManager.THEME_RETRO -> {
                // Retro arcade theme
                setBackgroundColor(Color.parseColor("#3F2832"))
                progressionTitle.setTextColor(Color.parseColor("#FF5A5F"))
                playerLevelText.setTextColor(Color.parseColor("#FF5A5F"))
                xpGainText.setTextColor(Color.WHITE)
                continueButton.setTextColor(Color.parseColor("#FF5A5F"))
                rewardsTitle.setTextColor(Color.WHITE)
                xpThemeColor = Color.parseColor("#FF5A5F")
            }
            PlayerProgressionManager.THEME_MINIMALIST -> {
                // Minimalist white theme
                setBackgroundColor(Color.WHITE)
                progressionTitle.setTextColor(Color.BLACK)
                playerLevelText.setTextColor(Color.BLACK)
                xpGainText.setTextColor(Color.BLACK)
                continueButton.setTextColor(Color.BLACK)
                rewardsTitle.setTextColor(Color.BLACK)
                xpThemeColor = Color.BLACK
            }
            PlayerProgressionManager.THEME_GALAXY -> {
                // Galaxy dark blue theme
                setBackgroundColor(Color.parseColor("#0B0C10"))
                progressionTitle.setTextColor(Color.parseColor("#66FCF1"))
                playerLevelText.setTextColor(Color.parseColor("#66FCF1"))
                xpGainText.setTextColor(Color.WHITE)
                continueButton.setTextColor(Color.parseColor("#66FCF1"))
                rewardsTitle.setTextColor(Color.WHITE)
                xpThemeColor = Color.parseColor("#66FCF1")
            }
            else -> {
                // Default fallback
                setBackgroundColor(Color.BLACK)
                progressionTitle.setTextColor(Color.WHITE)
                playerLevelText.setTextColor(Color.WHITE)
                xpGainText.setTextColor(Color.WHITE)
                continueButton.setTextColor(Color.WHITE)
                rewardsTitle.setTextColor(Color.WHITE)
                xpThemeColor = Color.WHITE
            }
        }
        
        // Update XP progress bar theme color
        xpProgressBar.setThemeColor(xpThemeColor)
        
        // Update reward card colors
        updateRewardCardColors(themeId)
    }
    
    /**
     * Update colors of existing reward cards to match the theme
     */
    private fun updateRewardCardColors(themeId: String) {
        val backgroundColor = when (themeId) {
            PlayerProgressionManager.THEME_CLASSIC -> Color.BLACK
            PlayerProgressionManager.THEME_NEON -> Color.parseColor("#0D0221")
            PlayerProgressionManager.THEME_MONOCHROME -> Color.parseColor("#1A1A1A")
            PlayerProgressionManager.THEME_RETRO -> Color.parseColor("#3F2832")
            PlayerProgressionManager.THEME_MINIMALIST -> Color.WHITE
            PlayerProgressionManager.THEME_GALAXY -> Color.parseColor("#0B0C10")
            else -> Color.BLACK
        }
        
        for (i in 0 until rewardsContainer.childCount) {
            val card = rewardsContainer.getChildAt(i) as? CardView
            card?.setCardBackgroundColor(backgroundColor)
        }
    }
} 