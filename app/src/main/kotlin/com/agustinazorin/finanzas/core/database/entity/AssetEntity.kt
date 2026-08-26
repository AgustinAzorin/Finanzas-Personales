package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agustinazorin.finanzas.engine.model.AssetCategory
import java.time.LocalDate

/** Un activo sin cuenta corriente propia (CLAUDE.md, sección 10): vehículo, inmueble, efectivo físico, etc. */
@Entity(
    tableName = "assets",
    foreignKeys = [
        ForeignKey(
            entity = HouseholdEntity::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = HouseholdMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerMemberId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("householdId"), Index("ownerMemberId")],
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val householdId: Long,
    val ownerMemberId: Long?,
    val name: String,
    val category: AssetCategory,
    val currency: String,
    /** Unidades mínimas de la moneda (ej: centavos). Siempre positivo. */
    val currentValue: Long,
    val valuationDate: LocalDate,
    val isActive: Boolean = true,
)
