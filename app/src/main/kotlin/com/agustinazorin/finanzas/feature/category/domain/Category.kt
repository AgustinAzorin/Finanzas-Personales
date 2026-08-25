package com.agustinazorin.finanzas.feature.category.domain

import kotlinx.coroutines.flow.Flow

data class Category(
    val id: Long,
    val parentCategoryId: Long?,
    val name: String,
    val icon: String?,
    val isCustom: Boolean,
)

/** Categoría raíz con sus hijas ya agrupadas, lista para mostrar en un árbol. */
data class CategoryWithChildren(
    val category: Category,
    val children: List<Category>,
)

interface CategoryRepository {
    fun observeTree(): Flow<List<CategoryWithChildren>>
    fun observeAll(): Flow<List<Category>>
    suspend fun addCategory(name: String, parentCategoryId: Long?): Long
}
