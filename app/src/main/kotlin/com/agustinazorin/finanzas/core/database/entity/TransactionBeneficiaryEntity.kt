package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * "Beneficiario" de un gasto compartido (CLAUDE.md, sección 30): cuánto de una [TransactionEntity]
 * le corresponde económicamente a un miembro del hogar, más allá de quién la pagó
 * ([TransactionEntity.ownerMemberId], el "Responsable").
 *
 * [shareAmount] es el monto ya calculado (ver
 * [com.agustinazorin.finanzas.engine.split.ExpenseSplitCalculator]), nunca un porcentaje: se
 * persiste el resultado final para no repetir el redondeo en cada lectura. La suma de los shares
 * de una misma transacción siempre es igual a su monto total.
 *
 * Sólo existe para transacciones EXPENSE sin cuotas (una transacción sin filas acá no está
 * compartida: se atribuye completa a su `ownerMemberId`).
 */
@Entity(
    tableName = "transaction_beneficiaries",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = HouseholdMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("transactionId"),
        Index("memberId"),
        Index(value = ["transactionId", "memberId"], unique = true),
    ],
)
data class TransactionBeneficiaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val memberId: Long,
    /** Siempre positivo, en unidades mínimas de la moneda de la transacción. */
    val shareAmount: Long,
)
