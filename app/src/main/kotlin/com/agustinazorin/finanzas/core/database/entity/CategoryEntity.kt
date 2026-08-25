package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentCategoryId"],
            // Por diseño no se puede borrar una categoría que todavía tiene hijas (CLAUDE.md, sección 12).
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("parentCategoryId")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentCategoryId: Long?,
    val name: String,
    val icon: String? = null,
    val isCustom: Boolean = false,
)
