package com.agustinazorin.finanzas.core.database

import com.agustinazorin.finanzas.core.database.dao.CategoryDao
import com.agustinazorin.finanzas.core.database.entity.CategoryEntity

/**
 * Árbol de categorías por defecto (CLAUDE.md, sección 12). Se inserta una única vez, al crear
 * la base de datos por primera vez. El usuario puede agregar categorías propias (isCustom = true)
 * sin que esto las modifique.
 */
private val defaultCategoryTree: Map<String, List<String>> = linkedMapOf(
    "Vivienda" to listOf("Alquiler", "Expensas", "Luz", "Gas", "Internet"),
    "Alimentación" to listOf("Supermercado", "Delivery", "Restaurantes"),
    "Transporte" to listOf("SUBE", "Combustible", "Taxi", "Mantenimiento"),
    "Salud" to listOf("Medicamentos", "Consultas", "Obra social"),
)

suspend fun seedDefaultCategories(categoryDao: CategoryDao) {
    if (categoryDao.count() > 0) return

    for ((rootName, childNames) in defaultCategoryTree) {
        val rootId = categoryDao.insert(CategoryEntity(parentCategoryId = null, name = rootName, isCustom = false))
        val children = childNames.map { childName ->
            CategoryEntity(parentCategoryId = rootId, name = childName, isCustom = false)
        }
        categoryDao.insertAll(children)
    }
}
