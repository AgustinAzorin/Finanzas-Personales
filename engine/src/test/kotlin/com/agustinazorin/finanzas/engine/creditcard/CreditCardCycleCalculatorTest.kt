package com.agustinazorin.finanzas.engine.creditcard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CreditCardCycleCalculatorTest {

    @Test
    fun `una compra antes del cierre queda en el ciclo vigente`() {
        val cycle = CreditCardCycleCalculator.cycleContaining(
            date = LocalDate.of(2026, 8, 10),
            closingDay = 25,
            dueDay = 5,
        )
        assertEquals(LocalDate.of(2026, 8, 25), cycle.closingDate)
        assertEquals(LocalDate.of(2026, 7, 26), cycle.periodStart)
        assertEquals(LocalDate.of(2026, 9, 5), cycle.dueDate)
    }

    @Test
    fun `una compra el dia del cierre queda en ese mismo ciclo`() {
        val cycle = CreditCardCycleCalculator.cycleContaining(
            date = LocalDate.of(2026, 8, 25),
            closingDay = 25,
            dueDay = 5,
        )
        assertEquals(LocalDate.of(2026, 8, 25), cycle.closingDate)
    }

    @Test
    fun `una compra despues del cierre pasa al ciclo siguiente`() {
        val cycle = CreditCardCycleCalculator.cycleContaining(
            date = LocalDate.of(2026, 8, 26),
            closingDay = 25,
            dueDay = 5,
        )
        assertEquals(LocalDate.of(2026, 9, 25), cycle.closingDate)
        assertEquals(LocalDate.of(2026, 8, 26), cycle.periodStart)
    }

    @Test
    fun `recorta el dia de cierre a la duracion real del mes`() {
        val cycle = CreditCardCycleCalculator.cycleContaining(
            date = LocalDate.of(2026, 2, 20),
            closingDay = 31,
            dueDay = 10,
        )
        assertEquals(LocalDate.of(2026, 2, 28), cycle.closingDate)
    }

    @Test
    fun `si el vencimiento cae antes del cierre en el mismo mes, pasa al mes siguiente`() {
        val cycle = CreditCardCycleCalculator.cycleContaining(
            date = LocalDate.of(2026, 8, 10),
            closingDay = 25,
            dueDay = 10,
        )
        assertEquals(LocalDate.of(2026, 9, 10), cycle.dueDate)
    }

    @Test
    fun `nextCycle avanza exactamente un ciclo cruzando el fin de ano`() {
        val cycle = CreditCardCycleCalculator.cycleContaining(LocalDate.of(2026, 12, 20), closingDay = 25, dueDay = 5)
        val next = CreditCardCycleCalculator.nextCycle(cycle, closingDay = 25, dueDay = 5)
        assertEquals(LocalDate.of(2027, 1, 25), next.closingDate)
        assertEquals(LocalDate.of(2026, 12, 26), next.periodStart)
    }
}
