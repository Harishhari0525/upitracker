// In data/CategorySuggestionRuleDao.kt

package com.example.upitracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategorySuggestionRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRule(rule: CategorySuggestionRule)

    @Query("SELECT * FROM category_suggestion_rules WHERE keyword = :keyword LIMIT 1")
    suspend fun getRuleForKeyword(keyword: String): CategorySuggestionRule?

    // Optional: A function to get all rules if we build a UI for it later
    @Query("SELECT * FROM category_suggestion_rules ORDER BY keyword ASC")
    fun getAllRules(): kotlinx.coroutines.flow.Flow<List<CategorySuggestionRule>>
}