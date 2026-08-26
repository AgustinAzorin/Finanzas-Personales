package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agustinazorin.finanzas.engine.model.CreditCardStatementStatus
import java.time.LocalDate

/**
 * Resumen de un ciclo de facturación de tarjeta (CLAUDE.md, sección 18). [totalAmount] es la
 * suma de las [InstallmentEntity] cuyo `accountingDate` coincide con [closingDate] de esta
 * tarjeta; se recalcula cada vez que se agrega o cancela una cuota de ese ciclo, nunca se edita
 * a mano. Único por (tarjeta, cierre): nunca puede haber dos resúmenes para el mismo ciclo.
 */
@Entity(
    tableName = "credit_card_statements",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditCardAccountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("creditCardAccountId"),
        Index(value = ["creditCardAccountId", "closingDate"], unique = true),
    ],
)
data class CreditCardStatementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creditCardAccountId: Long,
    val periodStart: LocalDate,
    val closingDate: LocalDate,
    val dueDate: LocalDate,
    val totalAmount: Long,
    val paidAmount: Long,
    val status: CreditCardStatementStatus,
)
