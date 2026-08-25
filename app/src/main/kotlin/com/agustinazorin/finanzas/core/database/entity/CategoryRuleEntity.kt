package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Regla de categorización aprendida de una corrección del usuario (CLAUDE.md, sección 39):
 * "merchantNormalized = 'MCDONALDS' -> category = RESTAURANTES". Transparente y editable desde
 * la pantalla de reglas; nunca se aplica en silencio sin que el usuario pueda verla o borrarla.
 */
@Entity(
    tableName = "category_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("merchantNormalized", unique = true), Index("categoryId")],
)
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantNormalized: String,
    val categoryId: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
