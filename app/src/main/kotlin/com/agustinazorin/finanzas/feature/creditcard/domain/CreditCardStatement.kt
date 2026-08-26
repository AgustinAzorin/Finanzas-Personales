package com.agustinazorin.finanzas.feature.creditcard.domain

import com.agustinazorin.finanzas.engine.model.CreditCardStatementStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Resumen de un ciclo de facturación de tarjeta (CLAUDE.md, sección 18). */
data class CreditCardStatement(
    val id: Long,
    val creditCardAccountId: Long,
    val periodStart: LocalDate,
    val closingDate: LocalDate,
    val dueDate: LocalDate,
    val totalAmount: Long,
    val paidAmount: Long,
    val status: CreditCardStatementStatus,
) {
    val outstandingAmount: Long get() = totalAmount - paidAmount
}

interface CreditCardStatementRepository {
    fun observeByAccount(creditCardAccountId: Long): Flow<List<CreditCardStatement>>
    suspend fun getById(id: Long): CreditCardStatement?

    /** Recalcula (creando si hace falta) el resumen del ciclo que cierra en [closingDate], sumando sus cuotas vigentes. */
    suspend fun recomputeStatement(creditCardAccountId: Long, periodStart: LocalDate, closingDate: LocalDate, dueDate: LocalDate)

    /**
     * Registra que ya se pagó [amount] de este resumen. El movimiento de dinero en sí se hace
     * aparte, como una Transferencia (Regla 2, CLAUDE.md sección 7) — acá sólo se actualiza el
     * seguimiento de cuánto de ese resumen ya está pago.
     */
    suspend fun registerPayment(statementId: Long, amount: Long)
}
