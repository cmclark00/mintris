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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val duration = when (lineCount) {
                4 -> 100L // Tetris
                3 -> 80L
                2 -> 60L
                1 -> 40L
                else -> 0L
            }
            
            val amplitude = when (lineCount) {
                4 -> VibrationEffect.DEFAULT_AMPLITUDE
                3 -> (VibrationEffect.DEFAULT_AMPLITUDE * 0.8).toInt()
                2 -> (VibrationEffect.DEFAULT_AMPLITUDE * 0.6).toInt()
                1 -> (VibrationEffect.DEFAULT_AMPLITUDE * 0.4).toInt()
                else -> 0
            }

            if (duration > 0 && amplitude > 0) {
                val vibrationEffect = VibrationEffect.createOneShot(duration, amplitude)
                vibrator.vibrate(vibrationEffect)
            }
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