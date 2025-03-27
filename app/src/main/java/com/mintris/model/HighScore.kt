package com.mintris.model

data class HighScore(
    val name: String,
    val score: Int,
    val level: Int,
    val date: Long = System.currentTimeMillis()
) 