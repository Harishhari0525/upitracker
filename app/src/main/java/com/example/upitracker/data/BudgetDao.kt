package com.example.upitracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY categoryName ASC")
    fun getAllActiveBudgets(): Flow<List<Budget>>

    @Query("UPDATE budgets SET categoryName = :newName WHERE categoryName = :oldName")
    suspend fun updateCategoryName(oldName: String, newName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: Budget)

    @Update
    suspend fun update(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM budgets WHERE categoryName = :categoryName")
    suspend fun deleteByCategoryName(categoryName: String)

    // Optional: A method to get a specific budget by its ID
    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Int): Budget?

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
