package com.mintris.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.mintris.R
import com.mintris.model.PlayerProgressionManager

/**
 * UI component for selecting game themes
 */
class ThemeSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val themesGrid: GridLayout
    private val availableThemesLabel: TextView
    
    // Callback when a theme is selected
    var onThemeSelected: ((String) -> Unit)? = null
    
    // Currently selected theme
    private var selectedTheme: String = PlayerProgressionManager.THEME_CLASSIC
    
    // Theme cards
    private val themeCards = mutableMapOf<String, CardView>()
    
    init {
        // Inflate the layout
        LayoutInflater.from(context).inflate(R.layout.theme_selector, this, true)
        
        // Get references to views
        themesGrid = findViewById(R.id.themes_grid)
        availableThemesLabel = findViewById(R.id.available_themes_label)
    }
    
    /**
     * Update the theme selector with unlocked themes
     */
    fun updateThemes(unlockedThemes: Set<String>, currentTheme: String) {
        // Clear existing theme cards
        themesGrid.removeAllViews()
        themeCards.clear()
        
        // Update selected theme
        selectedTheme = currentTheme
        
        // Get all possible themes and their details
        val allThemes = getThemes()
        
        // Add theme cards to the grid
        allThemes.forEach { (themeId, themeInfo) ->
            val isUnlocked = unlockedThemes.contains(themeId)
            val isSelected = themeId == selectedTheme
            
            val themeCard = createThemeCard(themeId, themeInfo, isUnlocked, isSelected)
            themeCards[themeId] = themeCard
            themesGrid.addView(themeCard)
        }
    }
    
    /**
     * Create a card for a theme
     */
    private fun createThemeCard(
        themeId: String,
        themeInfo: ThemeInfo,
        isUnlocked: Boolean,
        isSelected: Boolean
    ): CardView {
        // Create the card
        val card = CardView(context).apply {
            id = View.generateViewId()
            radius = 12f
            cardElevation = if (isSelected) 8f else 2f
            useCompatPadding = true
            
            // Set card background color based on theme
            setCardBackgroundColor(themeInfo.primaryColor)
            
            // Set card dimensions
            val cardSize = resources.getDimensionPixelSize(R.dimen.theme_card_size)
            layoutParams = GridLayout.LayoutParams().apply {
                width = cardSize
                height = cardSize
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            
            // Apply locked/selected state visuals
            alpha = if (isUnlocked) 1.0f else 0.5f
            
            // Add stroke for selected theme
            if (isSelected) {
                setContentPadding(4, 4, 4, 4)
            }
        }
        
        // Create theme content container
        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Create the theme preview
        val themePreview = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // Set the background color
            setBackgroundColor(themeInfo.primaryColor)
        }
        
        // Add a label with the theme name
        val themeLabel = TextView(context).apply {
            text = themeInfo.displayName
            setTextColor(themeInfo.textColor)
            textSize = 14f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            
            // Position at the bottom of the card
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                setMargins(4, 4, 4, 8)
            }
        }
        
        // Add level requirement for locked themes
        val levelRequirement = TextView(context).apply {
            text = "Level ${themeInfo.unlockLevel}"
            setTextColor(Color.WHITE)
            textSize = 12f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            visibility = if (isUnlocked) View.GONE else View.VISIBLE
            
            // Position at the center of the card
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            // Make text bold and more visible for better readability
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }
        
        // Add a lock icon if the theme is locked
        val lockOverlay = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // Add lock icon or visual indicator
            setBackgroundResource(R.drawable.lock_overlay)
            visibility = if (isUnlocked) View.GONE else View.VISIBLE
        }
        
        // Add all elements to container
        container.addView(themePreview)
        container.addView(themeLabel)
        container.addView(lockOverlay)
        container.addView(levelRequirement)
        
        // Add container to card
        card.addView(container)
        
        // Set up click listener only for unlocked themes
        if (isUnlocked) {
            card.setOnClickListener {
                // Only trigger callback if this isn't already the selected theme
                if (themeId != selectedTheme) {
                    // Update visual state
                    themeCards[selectedTheme]?.cardElevation = 2f
                    card.cardElevation = 8f
                    
                    // Update selected theme
                    selectedTheme = themeId
                    
                    // Notify listener
                    onThemeSelected?.invoke(themeId)
                }
            }
        }
        
        return card
    }
    
    /**
     * Data class for theme information
     */
    data class ThemeInfo(
        val displayName: String,
        val primaryColor: Int,
        val secondaryColor: Int,
        val textColor: Int,
        val unlockLevel: Int
    )
    
    /**
     * Get all available themes with their details
     */
    private fun getThemes(): Map<String, ThemeInfo> {
        return mapOf(
            PlayerProgressionManager.THEME_CLASSIC to ThemeInfo(
                displayName = "Classic",
                primaryColor = Color.parseColor("#000000"),
                secondaryColor = Color.parseColor("#1F1F1F"),
                textColor = Color.WHITE,
                unlockLevel = 1
            ),
            PlayerProgressionManager.THEME_NEON to ThemeInfo(
                displayName = "Neon",
                primaryColor = Color.parseColor("#0D0221"),
                secondaryColor = Color.parseColor("#650D89"),
                textColor = Color.parseColor("#FF00FF"),
                unlockLevel = 5
            ),
            PlayerProgressionManager.THEME_MONOCHROME to ThemeInfo(
                displayName = "Monochrome",
                primaryColor = Color.parseColor("#1A1A1A"),
                secondaryColor = Color.parseColor("#333333"),
                textColor = Color.LTGRAY,
                unlockLevel = 10
            ),
            PlayerProgressionManager.THEME_RETRO to ThemeInfo(
                displayName = "Retro",
                primaryColor = Color.parseColor("#3F2832"),
                secondaryColor = Color.parseColor("#087E8B"),
                textColor = Color.parseColor("#FF5A5F"),
                unlockLevel = 15
            ),
            PlayerProgressionManager.THEME_MINIMALIST to ThemeInfo(
                displayName = "Minimalist",
                primaryColor = Color.parseColor("#FFFFFF"),
                secondaryColor = Color.parseColor("#F0F0F0"),
                textColor = Color.BLACK,
                unlockLevel = 20
            ),
            PlayerProgressionManager.THEME_GALAXY to ThemeInfo(
                displayName = "Galaxy",
                primaryColor = Color.parseColor("#0B0C10"),
                secondaryColor = Color.parseColor("#1F2833"),
                textColor = Color.parseColor("#66FCF1"),
                unlockLevel = 25
            )
        )
    }
} 