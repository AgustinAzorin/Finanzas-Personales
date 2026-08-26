package com.agustinazorin.finanzas.engine.money

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {

    @Test
    fun `convert multiplica por la tasa y cambia de moneda`() {
        val result = Money(10_00, "USD").convert(BigDecimal("1000"), "ARS")
        assertEquals(Money(10_000_00, "ARS"), result)
    }

    @Test
    fun `convert redondea al entero mas cercano`() {
        val result = Money(1, "USD").convert(BigDecimal("0.5"), "ARS")
        assertEquals(Money(1, "ARS"), result)
    }

    @Test
    fun `convert no permite tasas negativas o cero`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(1000, "USD").convert(BigDecimal.ZERO, "ARS")
        }
    }

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
