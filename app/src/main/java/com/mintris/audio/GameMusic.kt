package com.mintris.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.os.Build
import android.util.Log
import com.mintris.R

class GameMusic(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var isEnabled = true
    private var isPrepared = false
    
    init {
        try {
            setupMediaPlayer()
        } catch (e: Exception) {
            Log.e("GameMusic", "Error initializing: ${e.message}")
        }
    }
    
    private fun setupMediaPlayer() {
        try {
            Log.d("GameMusic", "Setting up MediaPlayer")
            mediaPlayer = MediaPlayer.create(context, R.raw.game_music).apply {
                isLooping = true
                setVolume(0.5f, 0.5f)
                
                // Set audio attributes for better performance
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                }
                isPrepared = true
            }
            Log.d("GameMusic", "MediaPlayer setup complete")
        } catch (e: Exception) {
            Log.e("GameMusic", "Error setting up MediaPlayer", e)
            mediaPlayer = null
            isPrepared = false
        }
    }
    
    fun start() {
        if (isEnabled && mediaPlayer != null && isPrepared) {
            try {
                Log.d("GameMusic", "Starting music playback, isEnabled: $isEnabled")
                mediaPlayer?.start()
                Log.d("GameMusic", "Music playback started")
            } catch (e: Exception) {
                Log.e("GameMusic", "Error starting music: ${e.message}")
            }
        }
    }
    
    fun pause() {
        try {
            Log.d("GameMusic", "Pausing music playback")
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("GameMusic", "Error pausing music: ${e.message}")
        }
    }
    
    fun resume() {
        if (isEnabled && mediaPlayer != null && isPrepared) {
            try {
                Log.d("GameMusic", "Resuming music playback")
                mediaPlayer?.start()
            } catch (e: Exception) {
                Log.e("GameMusic", "Error resuming music: ${e.message}")
            }
        }
    }
    
    fun stop() {
        try {
            Log.d("GameMusic", "Stopping music playback")
            mediaPlayer?.stop()
            mediaPlayer?.prepare()
        } catch (e: Exception) {
            Log.e("GameMusic", "Error stopping music", e)
        }
    }
    
    fun setEnabled(enabled: Boolean) {
        Log.d("GameMusic", "Setting music enabled: $enabled")
        isEnabled = enabled
        
        if (!enabled && mediaPlayer?.isPlaying == true) {
            pause()
        } else if (enabled && mediaPlayer != null && isPrepared) {
            start()
        }
    }
    
    fun isEnabled(): Boolean = isEnabled
    
    fun release() {
        try {
            Log.d("GameMusic", "Releasing MediaPlayer")
            mediaPlayer?.release()
            mediaPlayer = null
            isPrepared = false
        } catch (e: Exception) {
            Log.e("GameMusic", "Error releasing music: ${e.message}")
        }
    }
} 