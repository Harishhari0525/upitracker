package com.example.upitracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RecurringRule)

    @Update
    suspend fun update(rule: RecurringRule)

    @Delete
    suspend fun delete(rule: RecurringRule)

    @Query("SELECT * FROM recurring_rules WHERE nextDueDate <= :currentDate")
    suspend fun getDueRules(currentDate: Long): List<RecurringRule>

    @Query("SELECT * FROM recurring_rules ORDER BY nextDueDate ASC")
    fun getAllRules(): Flow<List<RecurringRule>>

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun getRuleById(id: Int): RecurringRule?
}