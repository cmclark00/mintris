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
 * UI component for selecting block skins
 */
class BlockSkinSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val skinsGrid: GridLayout
    private val availableSkinsLabel: TextView
    
    // Callback when a block skin is selected
    var onBlockSkinSelected: ((String) -> Unit)? = null
    
    // Currently selected block skin
    private var selectedSkin: String = "block_skin_1"
    
    // Block skin cards
    private val skinCards = mutableMapOf<String, CardView>()
    
    // Player level for determining what should be unlocked
    private var playerLevel: Int = 1
    
    init {
        // Inflate the layout
        LayoutInflater.from(context).inflate(R.layout.block_skin_selector, this, true)
        
        // Get references to views
        skinsGrid = findViewById(R.id.skins_grid)
        availableSkinsLabel = findViewById(R.id.available_skins_label)
    }
    
    /**
     * Update the block skin selector with unlocked skins
     */
    fun updateBlockSkins(unlockedSkins: Set<String>, currentSkin: String, playerLevel: Int = 1) {
        // Store player level
        this.playerLevel = playerLevel
        
        // Clear existing skin cards
        skinsGrid.removeAllViews()
        skinCards.clear()
        
        // Update selected skin
        selectedSkin = currentSkin
        
        // Get all possible skins and their details
        val allSkins = getBlockSkins()
        
        // Add skin cards to the grid
        allSkins.forEach { (skinId, skinInfo) ->
            val isUnlocked = unlockedSkins.contains(skinId) || playerLevel >= skinInfo.unlockLevel
            val isSelected = skinId == selectedSkin
            
            val skinCard = createBlockSkinCard(skinId, skinInfo, isUnlocked, isSelected)
            skinCards[skinId] = skinCard
            skinsGrid.addView(skinCard)
        }
    }
    
    /**
     * Create a card for a block skin
     */
    private fun createBlockSkinCard(
        skinId: String,
        skinInfo: BlockSkinInfo,
        isUnlocked: Boolean,
        isSelected: Boolean
    ): CardView {
        // Create the card
        val card = CardView(context).apply {
            id = View.generateViewId()
            radius = 12f
            cardElevation = if (isSelected) 8f else 2f
            useCompatPadding = true
            
            // Set card background color based on skin
            setCardBackgroundColor(skinInfo.backgroundColor)
            
            // Add more noticeable visual indicator for selected skin
            if (isSelected) {
                setContentPadding(4, 4, 4, 4)
                // Create a gradient drawable for the border
                val gradientDrawable = android.graphics.drawable.GradientDrawable().apply {
                    setColor(skinInfo.backgroundColor)
                    setStroke(6, Color.WHITE)  // Thicker border
                    cornerRadius = 12f
                }
                background = gradientDrawable
                // Add glow effect via elevation
                cardElevation = 12f
            }
            
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
        }
        
        // Create block skin content container
        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Create the block skin preview
        val blockSkinPreview = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // Set the background color
            setBackgroundColor(skinInfo.backgroundColor)
        }
        
        // Add a label with the skin name
        val skinLabel = TextView(context).apply {
            text = skinInfo.displayName
            setTextColor(skinInfo.textColor)
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
        
        // Add level requirement for locked skins
        val levelRequirement = TextView(context).apply {
            text = "Level ${skinInfo.unlockLevel}"
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
        
        // Add a lock icon if the skin is locked
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
        container.addView(blockSkinPreview)
        container.addView(skinLabel)
        container.addView(lockOverlay)
        container.addView(levelRequirement)
        
        // Add container to card
        card.addView(container)
        
        // Set up click listener only for unlocked skins
        if (isUnlocked) {
            card.setOnClickListener {
                // Only trigger callback if this isn't already the selected skin
                if (skinId != selectedSkin) {
                    // Update previously selected card
                    skinCards[selectedSkin]?.let { prevCard ->
                        prevCard.cardElevation = 2f
                        // Reset any special styling
                        prevCard.background = null
                        prevCard.setCardBackgroundColor(getBlockSkins()[selectedSkin]?.backgroundColor ?: Color.BLACK)
                    }
                    
                    // Update visual state of newly selected card
                    card.cardElevation = 12f
                    
                    // Flash animation for selection feedback
                    val flashColor = Color.WHITE
                    val originalColor = skinInfo.backgroundColor
                    
                    // Create animator for flash effect
                    val flashAnimator = android.animation.ValueAnimator.ofArgb(flashColor, originalColor)
                    flashAnimator.duration = 300 // 300ms
                    flashAnimator.addUpdateListener { animator ->
                        val color = animator.animatedValue as Int
                        card.setCardBackgroundColor(color)
                    }
                    flashAnimator.start()
                    
                    // Update selected skin
                    selectedSkin = skinId
                    
                    // Notify listener
                    onBlockSkinSelected?.invoke(skinId)
                }
            }
        }
        
        return card
    }
    
    /**
     * Data class for block skin information
     */
    data class BlockSkinInfo(
        val displayName: String,
        val backgroundColor: Int,
        val textColor: Int,
        val unlockLevel: Int
    )
    
    /**
     * Get all available block skins with their details
     */
    private fun getBlockSkins(): Map<String, BlockSkinInfo> {
        return mapOf(
            "block_skin_1" to BlockSkinInfo(
                displayName = "Classic",
                backgroundColor = Color.BLACK,
                textColor = Color.WHITE,
                unlockLevel = 1
            ),
            "block_skin_2" to BlockSkinInfo(
                displayName = "Neon",
                backgroundColor = Color.parseColor("#0D0221"),
                textColor = Color.parseColor("#FF00FF"),
                unlockLevel = 7
            ),
            "block_skin_3" to BlockSkinInfo(
                displayName = "Retro",
                backgroundColor = Color.parseColor("#3F2832"),
                textColor = Color.parseColor("#FF5A5F"),
                unlockLevel = 14
            ),
            "block_skin_4" to BlockSkinInfo(
                displayName = "Minimalist",
                backgroundColor = Color.WHITE,
                textColor = Color.BLACK,
                unlockLevel = 21
            ),
            "block_skin_5" to BlockSkinInfo(
                displayName = "Galaxy",
                backgroundColor = Color.parseColor("#0B0C10"),
                textColor = Color.parseColor("#66FCF1"),
                unlockLevel = 28
            )
        )
    }
} 