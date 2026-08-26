package com.agustinazorin.finanzas.feature.category.data

import com.agustinazorin.finanzas.core.database.dao.CategoryRuleDao
import com.agustinazorin.finanzas.core.database.entity.CategoryRuleEntity
import com.agustinazorin.finanzas.feature.category.domain.CategoryRule
import com.agustinazorin.finanzas.feature.category.domain.CategoryRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class CategoryRuleRepositoryImpl @Inject constructor(
    private val dao: CategoryRuleDao,
) : CategoryRuleRepository {

    override fun observeAll(): Flow<List<CategoryRule>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Matching exacto sobre el comercio normalizado (misma semántica que CategoryRuleEngine, sin cargar toda la tabla). */
    override suspend fun suggestCategory(merchantNormalized: String?): Long? {
        if (merchantNormalized.isNullOrBlank()) return null
        return dao.findByMerchant(merchantNormalized)?.categoryId
    }

    override suspend fun learn(merchantNormalized: String, categoryId: Long) {
        if (merchantNormalized.isBlank()) return
        val existing = dao.findByMerchant(merchantNormalized)
        val now = Instant.now()
        dao.upsert(
            CategoryRuleEntity(
                id = existing?.id ?: 0,
                merchantNormalized = merchantNormalized,
                categoryId = categoryId,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun deleteRule(id: Long) {
        dao.deleteById(id)
    }
}

private fun CategoryRuleEntity.toDomain() = CategoryRule(
    id = id, merchantNormalized = merchantNormalized, categoryId = categoryId,
    createdAt = createdAt, updatedAt = updatedAt,
)
