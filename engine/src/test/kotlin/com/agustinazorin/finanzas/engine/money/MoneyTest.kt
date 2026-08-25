package com.agustinazorin.finanzas.engine.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {

    @Test
    fun `suma monetos de la misma moneda`() {
        val result = Money(1000, "ARS") + Money(500, "ARS")
        assertEquals(Money(1500, "ARS"), result)
    }

    @Test
    fun `no permite sumar monedas distintas`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(1000, "ARS") + Money(500, "USD")
        }
    }

    @Test
    fun `sumByCurrency agrupa sin mezclar monedas`() {
        val amounts = listOf(Money(1000, "ARS"), Money(500, "ARS"), Money(100, "USD"))
        val result = amounts.sumByCurrency()
        assertEquals(Money(1500, "ARS"), result["ARS"])
        assertEquals(Money(100, "USD"), result["USD"])
    }
}
