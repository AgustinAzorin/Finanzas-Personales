package com.agustinazorin.finanzas.engine.creditcard

import com.agustinazorin.finanzas.engine.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CreditCardAvailableCreditCalculatorTest {

    @Test
    fun `resta la deuda vigente del limite`() {
        val available = CreditCardAvailableCreditCalculator.compute(
            creditLimit = Money(1_000_000, "ARS"),
            outstandingBalance = Money(300_000, "ARS"),
        )
        assertEquals(Money(700_000, "ARS"), available)
    }

    @Test
    fun `nunca es negativo aunque se supere el limite`() {
        val available = CreditCardAvailableCreditCalculator.compute(
            creditLimit = Money(1_000_000, "ARS"),
            outstandingBalance = Money(1_500_000, "ARS"),
        )
        assertEquals(Money(0, "ARS"), available)
    }

    @Test
    fun `sin deuda, el disponible es todo el limite`() {
        val available = CreditCardAvailableCreditCalculator.compute(
            creditLimit = Money(1_000_000, "ARS"),
            outstandingBalance = Money(0, "ARS"),
        )
        assertEquals(Money(1_000_000, "ARS"), available)
    }

    @Test
    fun `no permite mezclar monedas`() {
        assertThrows(IllegalArgumentException::class.java) {
            CreditCardAvailableCreditCalculator.compute(Money(1_000_000, "ARS"), Money(100, "USD"))
        }
    }
}
