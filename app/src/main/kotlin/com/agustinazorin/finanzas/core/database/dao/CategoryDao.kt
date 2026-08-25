package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agustinazorin.finanzas.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>): List<Long>

    @Query("SELECT * FROM categories ORDER BY parentCategoryId IS NOT NULL, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentCategoryId IS NULL ORDER BY name")
    fun observeRootCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentCategoryId = :parentId ORDER BY name")
    fun observeChildren(parentId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?
}
