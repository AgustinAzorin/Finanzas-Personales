package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Configuración de tarjeta de crédito (CLAUDE.md, sección 17), 1 a 1 con una [AccountEntity] de
 * tipo CREDIT_CARD: por eso [accountId] es directamente la clave primaria, no hace falta un id
 * propio. `availableCredit` no se persiste acá: se calcula (CreditCardAvailableCreditCalculator,
 * :engine) a partir de [creditLimit] y la deuda vigente en los resúmenes, para que nunca quede
 * desactualizado.
 */
@Entity(
    tableName = "credit_cards",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CreditCardEntity(
    @PrimaryKey val accountId: Long,
    val closingDay: Int,
    val dueDay: Int,
    val creditLimit: Long,
)
