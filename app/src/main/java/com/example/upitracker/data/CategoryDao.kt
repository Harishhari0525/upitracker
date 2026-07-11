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
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Update
    suspend fun update(category: Category)

    // Changed REIGNORE to IGNORE to match Room's official strategy name
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<Category>)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Query("DELETE FROM category_suggestion_rules WHERE categoryName = :categoryName")
    suspend fun deleteRulesForCategory(categoryName: String)

    @Query("UPDATE transactions SET category = null WHERE category = :categoryName")
    suspend fun clearCategoryForTransactions(categoryName: String)

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getCategoryByNameCaseInsensitive(name: String): Category?

    /**
     * Our Atomic "Package Deal" operation.
     * If any of these three steps fail, the entire operation rolls back.
     */
    @Transaction
    suspend fun deleteCategoryAndCleanup(category: Category) {
        // Step 1: Delete the category itself
        delete(category)

        // Step 2: Clear out any automatic rules pointing to this category name
        deleteRulesForCategory(category.name)

        // Step 3: Remove the category name from all transactions that used it
        clearCategoryForTransactions(category.name)
    }

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
