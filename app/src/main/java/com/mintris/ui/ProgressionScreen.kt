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
import android.widget.Button
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
    private val continueButton: Button
    
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
        newRewards: List<String>
    ) {
        // Hide rewards container initially if there are no new rewards
        rewardsContainer.visibility = if (newRewards.isEmpty()) View.GONE else View.INVISIBLE
        
        // Set initial progress bar state
        val playerLevel = progressionManager.getPlayerLevel()
        val currentXP = progressionManager.getCurrentXP()
        val xpForNextLevel = progressionManager.getXPForNextLevel()
        
        // Update texts
        playerLevelText.text = "Player Level: $playerLevel"
        xpGainText.text = "+$xpGained XP"
        
        // Begin animation sequence
        xpProgressBar.setXPValues(playerLevel, currentXP, xpForNextLevel)
        
        // Animate XP gain text entrance
        val xpTextAnimator = ObjectAnimator.ofFloat(xpGainText, "alpha", 0f, 1f).apply {
            duration = 500
        }
        
        // Schedule animation for the XP bar after text appears
        postDelayed({
            xpProgressBar.animateXPGain(xpGained, playerLevel, currentXP, xpForNextLevel)
        }, 600)
        
        // If there are new rewards, show them with animation
        if (newRewards.isNotEmpty()) {
            // Create reward cards
            rewardsContainer.removeAllViews()
            newRewards.forEach { reward ->
                val rewardCard = createRewardCard(reward)
                rewardsContainer.addView(rewardCard)
            }
            
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
                        .setDuration(400)
                        .setStartDelay((i * 150).toLong())
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
            }, 2000) // Wait for XP bar animation to finish
        }
        
        // Start with initial animations
        AnimatorSet().apply {
            play(xpTextAnimator)
            start()
        }
    }
    
    /**
     * Create a card view to display a reward
     */
    private fun createRewardCard(rewardText: String): CardView {
        val card = CardView(context).apply {
            radius = 0f
            cardElevation = 0f
            useCompatPadding = true
            setCardBackgroundColor(Color.TRANSPARENT)
            
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
            setPadding(16, 12, 16, 12)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        
        card.addView(textView)
        return card
    }
} 