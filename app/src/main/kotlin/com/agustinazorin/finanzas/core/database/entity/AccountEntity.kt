package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agustinazorin.finanzas.engine.model.AccountType
import java.time.LocalDate

@Entity(
    tableName = "accounts",
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
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val householdId: Long,
    val ownerMemberId: Long?,
    val name: String,
    val type: AccountType,
    val currency: String,
    /** Unidades mínimas de la moneda (ej: centavos). En cuentas de pasivo, negativo = deuda. */
    val initialBalance: Long,
    val initialBalanceDate: LocalDate,
    val isActive: Boolean = true,
)
