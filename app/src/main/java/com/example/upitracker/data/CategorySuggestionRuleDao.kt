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

    @Query("""
        SELECT COUNT(*) FROM category_suggestion_rules
        WHERE fieldToMatch = :field
          AND matcher = :matcher
          AND LOWER(keyword) = LOWER(:keyword)
          AND LOWER(categoryName) = LOWER(:categoryName)
    """)
    suspend fun countEquivalentRules(
        field: String,
        matcher: String,
        keyword: String,
        categoryName: String
    ): Int

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
        WHERE (category IS NULL OR TRIM(category) = '')
        AND description LIKE '%' || :keyword || '%'
    """)
    suspend fun applyRuleToPastTransactions(keyword: String, categoryName: String)

    @Query("""
        SELECT id FROM transactions
        WHERE (category IS NULL OR TRIM(category) = '') AND isArchived = 0 AND pendingDeletionTimestamp IS NULL
          AND (
            (:field = 'DESCRIPTION' AND (
              (:matcher = 'CONTAINS' AND LOWER(description) LIKE '%' || LOWER(:keyword) || '%') OR
              (:matcher = 'EQUALS' AND LOWER(description) = LOWER(:keyword)) OR
              (:matcher = 'STARTS_WITH' AND LOWER(description) LIKE LOWER(:keyword) || '%') OR
              (:matcher = 'ENDS_WITH' AND LOWER(description) LIKE '%' || LOWER(:keyword))
            )) OR
            (:field = 'SENDER_OR_RECEIVER' AND (
              (:matcher = 'CONTAINS' AND LOWER(senderOrReceiver) LIKE '%' || LOWER(:keyword) || '%') OR
              (:matcher = 'EQUALS' AND LOWER(senderOrReceiver) = LOWER(:keyword)) OR
              (:matcher = 'STARTS_WITH' AND LOWER(senderOrReceiver) LIKE LOWER(:keyword) || '%') OR
              (:matcher = 'ENDS_WITH' AND LOWER(senderOrReceiver) LIKE '%' || LOWER(:keyword))
            ))
          )
    """)
    suspend fun findMatchingTransactionIds(field: String, matcher: String, keyword: String): List<Int>

    @Query("UPDATE transactions SET category = :categoryName WHERE id IN (:ids)")
    suspend fun categorizeTransactions(ids: List<Int>, categoryName: String)

    @Query("UPDATE transactions SET category = NULL WHERE id IN (:ids) AND category = :categoryName")
    suspend fun undoCategorization(ids: List<Int>, categoryName: String)

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
