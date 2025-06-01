package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "archived_sms_messages")
data class ArchivedSmsMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalSender: String,
    val originalBody: String,
    val originalTimestamp: Long, // Timestamp from the SMS itself
    val backupTimestamp: Long    // Timestamp when this record was created in our app
)