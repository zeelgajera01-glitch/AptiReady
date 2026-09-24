package com.example.aptiready.data.model

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