package com.agustinazorin.finanzas.engine.cashflow

import com.agustinazorin.finanzas.engine.model.CashFlowEvent
import com.agustinazorin.finanzas.engine.model.Certainty
import com.agustinazorin.finanzas.engine.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class CashFlowProjectionCalculatorTest {

    private val today = LocalDate.of(2026, 8, 25)

    @Test
    fun `project sin eventos devuelve solo el saldo actual`() {
        val points = CashFlowProjectionCalculator.project(today, Money(500_00, "ARS"), emptyList())

        assertEquals(1, points.size)
        assertEquals(Money(500_00, "ARS"), points[0].balance)
        assertNull(points[0].delta)
        assertEquals(Certainty.ACTUAL, points[0].certainty)
    }

    @Test
    fun `project ordena eventos por fecha y acumula el saldo`() {
        val events = listOf(
            CashFlowEvent(today.plusDays(10), "Tarjeta", Money(-300_00, "ARS"), Certainty.COMMITTED),
            CashFlowEvent(today.plusDays(1), "Sueldo", Money(900_00, "ARS"), Certainty.COMMITTED),
        )

        val points = CashFlowProjectionCalculator.project(today, Money(500_00, "ARS"), events)

        assertEquals(3, points.size)
        assertEquals("Saldo actual", points[0].label)
        assertEquals("Sueldo", points[1].label)
        assertEquals(Money(1400_00, "ARS"), points[1].balance)
        assertEquals("Tarjeta", points[2].label)
        assertEquals(Money(1100_00, "ARS"), points[2].balance)
    }

    @Test
    fun `project rechaza eventos en otra moneda`() {
        val events = listOf(CashFlowEvent(today.plusDays(1), "Compra USD", Money(-10_00, "USD"), Certainty.COMMITTED))

        assertThrows(IllegalArgumentException::class.java) {
            CashFlowProjectionCalculator.project(today, Money(500_00, "ARS"), events)
        }
    }

    @Test
    fun `totalCommitted suma solo salidas dentro del horizonte`() {
        val events = listOf(
            CashFlowEvent(today.plusDays(5), "Alquiler", Money(-450_00, "ARS"), Certainty.COMMITTED),
            CashFlowEvent(today.plusDays(29), "Servicios", Money(-120_00, "ARS"), Certainty.COMMITTED),
            CashFlowEvent(today.plusDays(31), "Fuera de rango", Money(-999_00, "ARS"), Certainty.COMMITTED),
            CashFlowEvent(today.plusDays(10), "Sueldo", Money(900_00, "ARS"), Certainty.COMMITTED),
        )

        val total = CashFlowProjectionCalculator.totalCommitted("ARS", today, 30, events)

        assertEquals(Money(570_00, "ARS"), total)
    }

    @Test
    fun `totalCommitted ignora eventos de otra moneda`() {
        val events = listOf(CashFlowEvent(today.plusDays(1), "Compra USD", Money(-10_00, "USD"), Certainty.COMMITTED))

        val total = CashFlowProjectionCalculator.totalCommitted("ARS", today, 30, events)

        assertEquals(Money.zero("ARS"), total)
    }

    @Test
    fun `totalCommitted rechaza horizonte negativo`() {
        assertThrows(IllegalArgumentException::class.java) {
            CashFlowProjectionCalculator.totalCommitted("ARS", today, -1, emptyList())
        }
    }

    @Test
    fun `firstDateBelow encuentra la primera caida por debajo del umbral`() {
        val events = listOf(
            CashFlowEvent(today.plusDays(5), "Alquiler", Money(-1000_00, "ARS"), Certainty.COMMITTED),
            CashFlowEvent(today.plusDays(10), "Sueldo", Money(2000_00, "ARS"), Certainty.COMMITTED),
        )
        val points = CashFlowProjectionCalculator.project(today, Money(500_00, "ARS"), events)

        val date = CashFlowProjectionCalculator.firstDateBelow(points, Money.zero("ARS"))

        assertEquals(today.plusDays(5), date)
    }

    @Test
    fun `firstDateBelow devuelve null si nunca cae por debajo`() {
        val events = listOf(CashFlowEvent(today.plusDays(5), "Sueldo", Money(100_00, "ARS"), Certainty.COMMITTED))
        val points = CashFlowProjectionCalculator.project(today, Money(500_00, "ARS"), events)

        assertNull(CashFlowProjectionCalculator.firstDateBelow(points, Money.zero("ARS")))
    }
}
