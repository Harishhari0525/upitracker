package com.example.upitracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("UPDATE category_suggestion_rules SET categoryName = :newName WHERE categoryName = :oldName")
    suspend fun updateCategoryNameInRules(oldName: String, newName: String)

    @Query("DELETE FROM category_suggestion_rules WHERE categoryName = :categoryNameToDelete")
    suspend fun deleteRulesForCategory(categoryNameToDelete: String)

    // --- NEW QUERIES FOR THE RETROACTIVE CASCADE ENGINE ---

    /**
     * Finds and updates all historical transactions that match the new keyword rule.
     * It uses SQL's 'LIKE' operator with wildcards to find the keyword anywhere inside the description.
     */
    @Query("""
        UPDATE transactions 
        SET category = :categoryName 
        WHERE category IS NULL 
        AND description LIKE '%' || :keyword || '%'
    """)
    suspend fun applyRuleToPastTransactions(keyword: String, categoryName: String)

    @Transaction
    suspend fun insertRuleAndApplyRetroactively(rule: CategorySuggestionRule) {
        // Step 1: Save the new rule to the database
        insert(rule)

        // Step 2: Sweep through past history and update matching uncategorized items
        applyRuleToPastTransactions(
            keyword = rule.keyword,
            categoryName = rule.categoryName
        )
    }

    @Query("DELETE FROM category_suggestion_rules")
    suspend fun deleteAll()
}