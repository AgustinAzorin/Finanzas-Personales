package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agustinazorin.finanzas.engine.model.LiabilityType
import java.time.LocalDate

/**
 * Una obligación sin cuenta propia detrás (CLAUDE.md, sección 11): préstamo personal, deuda
 * informal, etc. No incluye deuda de tarjeta ni cuotas futuras (Fase 2: Account(CREDIT_CARD) +
 * CreditCardStatement + Installment).
 */
@Entity(
    tableName = "liabilities",
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
data class LiabilityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val householdId: Long,
    val ownerMemberId: Long?,
    val name: String,
    val type: LiabilityType,
    /** Unidades mínimas de la moneda. Monto original de la obligación. */
    val principal: Long,
    /** Unidades mínimas de la moneda. Saldo pendiente actual; siempre positivo. */
    val outstandingAmount: Long,
    val currency: String,
    val dueDate: LocalDate?,
    val interestRate: Double?,
    val isActive: Boolean = true,
)
