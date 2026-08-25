package com.agustinazorin.finanzas.feature.category.data

import com.agustinazorin.finanzas.core.database.dao.CategoryDao
import com.agustinazorin.finanzas.core.database.entity.CategoryEntity
import com.agustinazorin.finanzas.feature.category.domain.Category
import com.agustinazorin.finanzas.feature.category.domain.CategoryRepository
import com.agustinazorin.finanzas.feature.category.domain.CategoryWithChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao,
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeTree(): Flow<List<CategoryWithChildren>> =
        dao.observeAll().map { entities ->
            val domain = entities.map { it.toDomain() }
            val roots = domain.filter { it.parentCategoryId == null }
            val childrenByParent = domain.filter { it.parentCategoryId != null }.groupBy { it.parentCategoryId }
            roots.map { root -> CategoryWithChildren(root, childrenByParent[root.id].orEmpty()) }
        }

    override suspend fun addCategory(name: String, parentCategoryId: Long?): Long =
        dao.insert(CategoryEntity(parentCategoryId = parentCategoryId, name = name, isCustom = true))
}

private fun CategoryEntity.toDomain() = Category(
    id = id, parentCategoryId = parentCategoryId, name = name, icon = icon, isCustom = isCustom,
)
