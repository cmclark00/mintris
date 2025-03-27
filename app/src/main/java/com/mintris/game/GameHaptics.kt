package com.mintris.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

class GameHaptics(private val context: Context) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun performHapticFeedback(view: View, feedbackType: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            @Suppress("DEPRECATION")
            view.performHapticFeedback(feedbackType)
        }
    }

    fun vibrateForLineClear(lineCount: Int) {
        android.util.Log.d("GameHaptics", "Attempting to vibrate for $lineCount lines")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val duration = when (lineCount) {
                4 -> 200L // Tetris - doubled from 100L
                3 -> 160L // Triples - doubled from 80L
                2 -> 120L // Doubles - doubled from 60L
                1 -> 80L  // Singles - doubled from 40L
                else -> 0L
            }
            
            val amplitude = when (lineCount) {
                4 -> 255 // Full amplitude for Tetris
                3 -> 230 // 90% amplitude for triples
                2 -> 180 // 70% amplitude for doubles
                1 -> 128 // 50% amplitude for singles
                else -> 0
            }

            android.util.Log.d("GameHaptics", "Vibration parameters - Duration: ${duration}ms, Amplitude: $amplitude")
            if (duration > 0 && amplitude > 0) {
                try {
                    val vibrationEffect = VibrationEffect.createOneShot(duration, amplitude)
                    vibrator.vibrate(vibrationEffect)
                    android.util.Log.d("GameHaptics", "Vibration triggered successfully")
                } catch (e: Exception) {
                    android.util.Log.e("GameHaptics", "Error triggering vibration", e)
                }
            }
        } else {
            android.util.Log.w("GameHaptics", "Device does not support vibration effects (Android < 8.0)")
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