package com.agustinazorin.finanzas.engine.creditcard

import com.agustinazorin.finanzas.engine.model.CreditCardStatementStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CreditCardStatementStatusCalculatorTest {

    private val closingDate = LocalDate.of(2026, 8, 25)

    @Test
    fun `antes del cierre y sin pagos esta abierto`() {
        val status = CreditCardStatementStatusCalculator.effectiveStatus(
            closingDate = closingDate, totalAmount = 100_000, paidAmount = 0, asOf = LocalDate.of(2026, 8, 10),
        )
        assertEquals(CreditCardStatementStatus.OPEN, status)
    }

    @Test
    fun `en la fecha de cierre o despues, sin pagos, esta cerrado`() {
        val status = CreditCardStatementStatusCalculator.effectiveStatus(
            closingDate = closingDate, totalAmount = 100_000, paidAmount = 0, asOf = closingDate,
        )
        assertEquals(CreditCardStatementStatus.CLOSED, status)
    }

    @Test
    fun `con un pago parcial esta parcialmente pagado incluso antes del cierre`() {
        val status = CreditCardStatementStatusCalculator.effectiveStatus(
            closingDate = closingDate, totalAmount = 100_000, paidAmount = 40_000, asOf = LocalDate.of(2026, 8, 10),
        )
        assertEquals(CreditCardStatementStatus.PARTIALLY_PAID, status)
    }

    @Test
    fun `pagado por completo o de mas esta pagado`() {
        assertEquals(
            CreditCardStatementStatus.PAID,
            CreditCardStatementStatusCalculator.effectiveStatus(closingDate, 100_000, 100_000, closingDate.plusDays(5)),
        )
        assertEquals(
            CreditCardStatementStatus.PAID,
            CreditCardStatementStatusCalculator.effectiveStatus(closingDate, 100_000, 150_000, closingDate.plusDays(5)),
        )
    }

    @Test
    fun `un resumen sin monto esta pagado`() {
        assertEquals(
            CreditCardStatementStatus.PAID,
            CreditCardStatementStatusCalculator.effectiveStatus(closingDate, 0, 0, LocalDate.of(2026, 8, 1)),
        )
    }
}
