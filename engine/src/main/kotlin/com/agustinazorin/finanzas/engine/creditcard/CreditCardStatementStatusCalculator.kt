package com.agustinazorin.finanzas.engine.creditcard

import com.agustinazorin.finanzas.engine.model.CreditCardStatementStatus
import java.time.LocalDate

/**
 * Deriva el estado de un resumen de tarjeta (CLAUDE.md, sección 18) a partir de sus montos y
 * fechas, sin depender de un job en background: se recalcula cada vez que se lee o se modifica
 * el resumen, así que siempre queda consistente con la fecha actual y los pagos registrados.
 */
object CreditCardStatementStatusCalculator {

    fun effectiveStatus(
        closingDate: LocalDate,
        totalAmount: Long,
        paidAmount: Long,
        asOf: LocalDate,
    ): CreditCardStatementStatus = when {
        totalAmount <= 0 || paidAmount >= totalAmount -> CreditCardStatementStatus.PAID
        paidAmount > 0 -> CreditCardStatementStatus.PARTIALLY_PAID
        !asOf.isBefore(closingDate) -> CreditCardStatementStatus.CLOSED
        else -> CreditCardStatementStatus.OPEN
    }
}
