package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agustinazorin.finanzas.engine.model.Periodicity
import com.agustinazorin.finanzas.engine.model.RecurringType

/**
 * Representa un ingreso o gasto recurrente esperado (CLAUDE.md, sección 15).
 * Nunca genera una [TransactionEntity] real automáticamente: sólo produce eventos
 * proyectados (ver `UpcomingCommitmentsCalculator` en :engine).
 */
@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = HouseholdEntity::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = HouseholdMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("householdId"), Index("categoryId"), Index("accountId"), Index("memberId")],
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val householdId: Long,
    val type: RecurringType,
    val name: String,
    /** Siempre positivo, en unidades mínimas de la moneda. */
    val estimatedAmount: Long,
    val currency: String,
    val periodicity: Periodicity,
    val dueDay: Int,
    val categoryId: Long?,
    val accountId: Long?,
    val memberId: Long?,
    val isActive: Boolean = true,
)
