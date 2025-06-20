package com.example.upitracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorySuggestionRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CategorySuggestionRule)

    @Update
    suspend fun update(rule: CategorySuggestionRule)

    @Delete
    suspend fun delete(rule: CategorySuggestionRule)

    @Query("SELECT * FROM category_suggestion_rules ORDER BY priority DESC")
    fun getAllRules(): Flow<List<CategorySuggestionRule>>
}