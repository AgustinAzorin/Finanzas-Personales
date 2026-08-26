package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.TransactionSource
import com.agustinazorin.finanzas.engine.model.TransactionStatus
import com.agustinazorin.finanzas.engine.model.TransactionType
import java.time.Instant
import java.time.LocalDate

/**
 * Entidad central (CLAUDE.md, sección 14).
 *
 * Una transferencia entre cuentas se modela como DOS filas —una OUTFLOW en la cuenta origen
 * y una INFLOW en la cuenta destino, ambas type=TRANSFER— vinculadas por [linkedTransactionId].
 * Ver [TransactionDao.insertTransfer] para la única forma soportada de crearlas: siempre
 * juntas, en una misma transacción de base de datos (Regla 1, CLAUDE.md sección 7).
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = HouseholdEntity::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            // No se permite borrar una cuenta con historial: se desactiva (isActive = false).
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = HouseholdMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerMemberId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedTransactionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("householdId"),
        Index("accountId"),
        Index("ownerMemberId"),
        Index("categoryId"),
        Index("linkedTransactionId"),
        Index("date"),
        Index("status"),
        Index("reconciliationHash"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val householdId: Long,
    val accountId: Long,
    val ownerMemberId: Long?,
    /** Siempre positivo, en unidades mínimas de la moneda; el signo lo da [direction]. */
    val amount: Long,
    val currency: String,
    val direction: TransactionDirection,
    val date: LocalDate,
    val merchant: String?,
    val categoryId: Long?,
    val type: TransactionType,
    val source: TransactionSource,
    val note: String?,
    val reconciliationHash: String?,
    val linkedTransactionId: Long?,
    val status: TransactionStatus,
    /**
     * true cuando esta es la compra "padre" de una compra con tarjeta en cuotas (Regla 3,
     * CLAUDE.md sección 7 y 16): existen filas en `installments` con `transactionId = id`, y el
     * gasto económico de esta transacción se cuenta a través de esas cuotas, nunca por esta
     * fecha. No afecta el saldo de la cuenta (Regla 4): la deuda se reconoce igual, de una sola
     * vez, al momento de la compra.
     */
    val hasInstallments: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)
