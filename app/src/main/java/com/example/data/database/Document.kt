package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val language: String, // en, pt, es, fr etc.
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
