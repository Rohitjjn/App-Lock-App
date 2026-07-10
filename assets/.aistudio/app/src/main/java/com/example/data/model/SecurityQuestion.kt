package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_questions")
data class SecurityQuestion(
    @PrimaryKey val id: Int = 1,
    val question: String,
    val answerHash: String
)
