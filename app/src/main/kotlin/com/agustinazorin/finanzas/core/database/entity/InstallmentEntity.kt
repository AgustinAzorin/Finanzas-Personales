package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agustinazorin.finanzas.engine.model.InstallmentStatus
import java.time.LocalDate

/**
 * Una cuota de una compra con tarjeta (CLAUDE.md, sección 16). [accountingDate] siempre coincide
 * con el `closingDate` de un ciclo de la tarjeta (ver InstallmentPlanner, :engine): así se puede
 * agrupar por [accountingDate] para construir el total de un [CreditCardStatementEntity] sin
 * necesidad de guardar una referencia redundante al resumen.
 */
@Entity(
    tableName = "installments",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("transactionId"), Index("accountingDate"), Index("dueDate"), Index("status")],
)
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val installmentNumber: Int,
    val totalInstallments: Int,
    val amount: Long,
    val dueDate: LocalDate,
    val accountingDate: LocalDate,
    val status: InstallmentStatus,
)
