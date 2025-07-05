package com.example.upitracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<Category>)

    // ✨ NEW: Insert a single category ✨
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: Category)

    // ✨ NEW: Update an existing category ✨
    @Update
    suspend fun update(category: Category)

    // ✨ NEW: Delete a category ✨
    @Delete
    suspend fun delete(category: Category)

    // ✨ NEW: Check if a category name exists (for validation) ✨
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?
}