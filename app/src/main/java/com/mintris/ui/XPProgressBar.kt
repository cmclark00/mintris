package com.mintris.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.mintris.R

/**
 * Custom progress bar for displaying player XP with animation capabilities
 */
class XPProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paints for drawing
    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#383838")
        isAntiAlias = true
    }
    
    private val progressPaint = Paint().apply {
        color = Color.parseColor("#50C878") // Emerald green
        isAntiAlias = true
    }
    
    private val progressGlowPaint = Paint().apply {
        color = Color.parseColor("#70F098") // Lighter emerald for glow
        isAntiAlias = true
        setShadowLayer(10f, 0f, 0f, Color.parseColor("#50C878"))
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 40f
    }
    
    private val levelBadgePaint = Paint().apply {
        color = Color.parseColor("#FFD700") // Gold color for level badge
        isAntiAlias = true
    }
    
    private val levelBadgeTextPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 36f
        isFakeBoldText = true
    }
    
    // Progress bar dimensions
    private val progressRect = RectF()
    private val backgroundRect = RectF()
    private val cornerRadius = 25f
    
    // Progress animation
    private var progressAnimator: ValueAnimator? = null
    private var currentProgress = 0f
    private var targetProgress = 0f
    
    // XP values
    private var currentXP = 0L
    private var xpForNextLevel = 100L
    private var playerLevel = 1
    
    // Level up animation
    private var isLevelingUp = false
    private var levelUpAnimator: ValueAnimator? = null
    private var levelBadgeScale = 1f
    
    /**
     * Set the player's current level and XP values
     */
    fun setXPValues(level: Int, currentXP: Long, xpForNextLevel: Long) {
        this.playerLevel = level
        this.currentXP = currentXP
        this.xpForNextLevel = xpForNextLevel
        
        // Update progress value
        targetProgress = calculateProgressPercentage()
        
        // If not animating, set current progress immediately
        if (progressAnimator == null || !progressAnimator!!.isRunning) {
            currentProgress = targetProgress
        }
        
        invalidate()
    }
    
    /**
     * Animate adding XP to the bar
     */
    fun animateXPGain(xpGained: Long, newLevel: Int, newCurrentXP: Long, newXPForNextLevel: Long) {
        // Store original values before animation
        val startXP = currentXP
        val startLevel = playerLevel
        
        // Calculate percentage before XP gain
        val startProgress = calculateProgressPercentage()
        
        // Update to new values
        playerLevel = newLevel
        currentXP = newCurrentXP
        xpForNextLevel = newXPForNextLevel
        
        // Calculate new target progress
        targetProgress = calculateProgressPercentage()
        
        // Determine if level up occurred
        isLevelingUp = startLevel < newLevel
        
        // Animate progress bar
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(startProgress, targetProgress).apply {
            duration = 1500 // 1.5 seconds animation
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animation ->
                currentProgress = animation.animatedValue as Float
                invalidate()
            }
            
            // When animation completes, trigger level up animation if needed
            if (isLevelingUp) {
                levelUpAnimation()
            }
            
            start()
        }
    }
    
    /**
     * Create a level up animation effect
     */
    private fun levelUpAnimation() {
        levelUpAnimator?.cancel()
        levelUpAnimator = ValueAnimator.ofFloat(1f, 1.5f, 1f).apply {
            duration = 1000 // 1 second pulse animation
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animation ->
                levelBadgeScale = animation.animatedValue as Float
                invalidate()
            }
            
            start()
        }
    }
    
    /**
     * Calculate the current progress percentage
     */
    private fun calculateProgressPercentage(): Float {
        return if (xpForNextLevel > 0) {
            (currentXP.toFloat() / xpForNextLevel.toFloat()) * 100f
        } else {
            0f
        }
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Update progress bar dimensions based on view size
        val verticalPadding = h * 0.2f
        backgroundRect.set(
            h * 0.5f,  // Left margin = height/2 (for level badge)
            verticalPadding,
            w - paddingRight.toFloat(),
            h - verticalPadding
        )
        
        // Adjust text size based on height
        textPaint.textSize = h * 0.35f
        levelBadgeTextPaint.textSize = h * 0.3f
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw level badge
        val badgeRadius = height * 0.3f * levelBadgeScale
        val badgeCenterX = height * 0.25f
        val badgeCenterY = height * 0.5f
        
        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, levelBadgePaint)
        canvas.drawText(
            playerLevel.toString(),
            badgeCenterX,
            badgeCenterY + (levelBadgeTextPaint.textSize / 3),
            levelBadgeTextPaint
        )
        
        // Draw background bar
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint)
        
        // Draw progress bar
        progressRect.set(
            backgroundRect.left,
            backgroundRect.top,
            backgroundRect.left + (backgroundRect.width() * currentProgress / 100f),
            backgroundRect.bottom
        )
        
        // Only draw if there is progress to show
        if (progressRect.width() > 0) {
            // Draw glow effect first
            canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressGlowPaint)
            
            // Draw actual progress bar
            canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint)
        }
        
        // Draw progress text
        val progressText = "${currentXP}/${xpForNextLevel} XP"
        canvas.drawText(
            progressText,
            backgroundRect.centerX(),
            backgroundRect.centerY() + (textPaint.textSize / 3),
            textPaint
        )
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressAnimator?.cancel()
        levelUpAnimator?.cancel()
    }
} 