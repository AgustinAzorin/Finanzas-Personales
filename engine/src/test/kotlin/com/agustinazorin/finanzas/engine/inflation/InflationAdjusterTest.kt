package com.agustinazorin.finanzas.engine.inflation

import com.agustinazorin.finanzas.engine.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InflationAdjusterTest {

    @Test
    fun `sin meses de por medio devuelve el mismo monto`() {
        val amount = Money(100_000_00, "ARS")
        val result = InflationAdjuster.adjust(
            amount = amount,
            from = LocalDate.of(2024, 1, 10),
            to = LocalDate.of(2024, 1, 25),
            rates = emptyList(),
        )
        assertEquals(amount, result)
    }

    @Test
    fun `compone las variaciones mensuales entre from y to`() {
        val rates = listOf(
            MonthlyInflationRate(YearMonth.of(2024, 2), BigDecimal("10")),
            MonthlyInflationRate(YearMonth.of(2024, 3), BigDecimal("20")),
        )
        val result = InflationAdjuster.adjust(
            amount = Money(100_000_00, "ARS"),
            from = LocalDate.of(2024, 1, 15),
            to = LocalDate.of(2024, 3, 1),
            rates = rates,
        )
        // 100.000 * 1.10 * 1.20 = 132.000
        assertEquals(Money(132_000_00, "ARS"), result)
    }

    @Test
    fun `falla si falta la tasa de un mes del rango`() {
        assertThrows(IllegalArgumentException::class.java) {
            InflationAdjuster.adjust(
                amount = Money(1_00, "ARS"),
                from = LocalDate.of(2024, 1, 1),
                to = LocalDate.of(2024, 3, 1),
                rates = listOf(MonthlyInflationRate(YearMonth.of(2024, 2), BigDecimal("5"))),
            )
        }
    }

    @Test
    fun `no permite ajustar hacia atras en el tiempo`() {
        assertThrows(IllegalArgumentException::class.java) {
            InflationAdjuster.adjust(
                amount = Money(1_00, "ARS"),
                from = LocalDate.of(2024, 3, 1),
                to = LocalDate.of(2024, 1, 1),
                rates = emptyList(),
            )
        }
    }
}
