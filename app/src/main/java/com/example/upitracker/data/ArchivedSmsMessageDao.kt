package com.example.upitracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchivedSmsMessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignore if we somehow try to insert the exact same raw SMS again
    suspend fun insertArchivedSms(archivedSmsMessage: ArchivedSmsMessage): Long

    // For future cleanup based on retention policy
    @Query("DELETE FROM archived_sms_messages WHERE backupTimestamp < :cutoffTimestamp")
    suspend fun deleteOldArchivedSms(cutoffTimestamp: Long)

    // Optional: For viewing archived messages if you add a UI for it later
    @Query("SELECT * FROM archived_sms_messages ORDER BY originalTimestamp DESC")
    fun getAllArchivedSms(): Flow<List<ArchivedSmsMessage>>

    @Query("""
        SELECT originalSender FROM archived_sms_messages
        WHERE originalTimestamp BETWEEN :timestamp - :toleranceMs AND :timestamp + :toleranceMs
        ORDER BY ABS(originalTimestamp - :timestamp) ASC
        LIMIT 1
    """)
    suspend fun findSenderNear(timestamp: Long, toleranceMs: Long = 300000L): String?

    @Query("SELECT * FROM archived_sms_messages WHERE parseStatus = 'UNMATCHED' ORDER BY originalTimestamp DESC LIMIT :limit")
    fun getRecentParserFailures(limit: Int = 100): Flow<List<ArchivedSmsMessage>>

    // Optional: For clearing all archived messages
    @Query("DELETE FROM archived_sms_messages")
    suspend fun deleteAllArchivedSms()
}
