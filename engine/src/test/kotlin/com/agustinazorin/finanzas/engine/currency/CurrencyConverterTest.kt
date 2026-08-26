package com.agustinazorin.finanzas.engine.currency

import com.agustinazorin.finanzas.engine.money.Money
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyConverterTest {

    @Test
    fun `convierte y suma monedas distintas a la moneda base`() {
        val result = CurrencyConverter.toBaseCurrency(
            amountsByCurrency = mapOf("ARS" to Money(100_000_00, "ARS"), "USD" to Money(10_00, "USD")),
            baseCurrency = "ARS",
            ratesToBase = mapOf("USD" to BigDecimal("1000")),
        )
        assertEquals(Money(110_000_00, "ARS"), result.total)
        assertTrue(result.missingRates.isEmpty())
    }

    @Test
    fun `moneda base no necesita tasa propia`() {
        val result = CurrencyConverter.toBaseCurrency(
            amountsByCurrency = mapOf("ARS" to Money(5_00, "ARS")),
            baseCurrency = "ARS",
            ratesToBase = emptyMap(),
        )
        assertEquals(Money(5_00, "ARS"), result.total)
    }

    @Test
    fun `moneda sin tasa conocida se reporta como faltante y no se suma`() {
        val result = CurrencyConverter.toBaseCurrency(
            amountsByCurrency = mapOf("ARS" to Money(1_00, "ARS"), "EUR" to Money(1_00, "EUR")),
            baseCurrency = "ARS",
            ratesToBase = emptyMap(),
        )
        assertEquals(Money(1_00, "ARS"), result.total)
        assertEquals(setOf("EUR"), result.missingRates)
    }
}
