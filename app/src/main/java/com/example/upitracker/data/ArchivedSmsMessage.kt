package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "archived_sms_messages",
    indices = [Index(value = ["originalSender", "originalBody", "originalTimestamp"], unique = true)]
)
data class ArchivedSmsMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalSender: String,
    val originalBody: String,
    val originalTimestamp: Long, // Timestamp from the SMS itself
    val backupTimestamp: Long    // Timestamp when this record was created in our app
)