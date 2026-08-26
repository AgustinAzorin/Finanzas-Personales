package com.agustinazorin.finanzas.feature.category.domain

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Regla de categorización aprendida de una corrección del usuario (CLAUDE.md, sección 39):
 * "merchantNormalized = 'MCDONALDS' -> category = RESTAURANTES". Transparente y editable: el
 * usuario siempre puede ver y borrar sus reglas desde la pantalla de categorización automática.
 */
data class CategoryRule(
    val id: Long,
    val merchantNormalized: String,
    val categoryId: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

interface CategoryRuleRepository {
    fun observeAll(): Flow<List<CategoryRule>>
    suspend fun suggestCategory(merchantNormalized: String?): Long?

    /** Crea o reemplaza la regla para ese comercio: sólo puede haber una categoría por comercio normalizado. */
    suspend fun learn(merchantNormalized: String, categoryId: Long)

    suspend fun deleteRule(id: Long)
}
