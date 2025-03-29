package com.mintris.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.content.getSystemService

/**
 * Handles haptic feedback for game events
 */
class GameHaptics(private val context: Context) {

    private val TAG = "GameHaptics"
    
    // Vibrator service
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService<VibratorManager>()
            vibratorManager?.defaultVibrator ?: throw IllegalStateException("No vibrator available")
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    // Vibrate for line clear (more intense for more lines)
    fun vibrateForLineClear(lineCount: Int) {
        Log.d(TAG, "Attempting to vibrate for $lineCount lines")
        
        // Only proceed if the device has a vibrator and it's available
        if (!vibrator.hasVibrator()) return
        
        // Scale duration and amplitude based on line count
        // More lines = longer and stronger vibration
        val duration = when(lineCount) {
            1 -> 50L  // Single line: short vibration
            2 -> 80L  // Double line: slightly longer
            3 -> 120L // Triple line: even longer
            4 -> 200L // Tetris: longest vibration
            else -> 50L
        }
        
        val amplitude = when(lineCount) {
            1 -> 80  // Single line: mild vibration (80/255)
            2 -> 120 // Double line: medium vibration (120/255)
            3 -> 180 // Triple line: strong vibration (180/255)
            4 -> 255 // Tetris: maximum vibration (255/255)
            else -> 80
        }
        
        Log.d(TAG, "Vibration parameters - Duration: ${duration}ms, Amplitude: $amplitude")
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
                Log.d(TAG, "Vibration triggered successfully")
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
                Log.w(TAG, "Device does not support vibration effects (Android < 8.0)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering vibration", e)
        }
    }

    fun performHapticFeedback(view: View, feedbackType: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            @Suppress("DEPRECATION")
            view.performHapticFeedback(feedbackType)
        }
    }

    fun vibrateForPieceLock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(vibrationEffect)
        }
    }

    fun vibrateForPieceMove() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = (VibrationEffect.DEFAULT_AMPLITUDE * 0.3).toInt().coerceAtLeast(1)
            val vibrationEffect = VibrationEffect.createOneShot(20L, amplitude)
            vibrator.vibrate(vibrationEffect)
        }
    }
} 