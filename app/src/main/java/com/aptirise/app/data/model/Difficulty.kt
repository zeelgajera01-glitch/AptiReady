package com.aptirise.app.data.model

enum class Difficulty {
    EASY,
    MEDIUM,
    HARD;

    fun getDisplayName(): String {
        return when (this) {
            EASY -> "Easy"
            MEDIUM -> "Medium"
            HARD -> "Hard"
        }
    }
}