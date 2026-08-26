package com.agustinazorin.finanzas.engine.installments

import com.agustinazorin.finanzas.engine.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class InstallmentPlannerTest {

    @Test
    fun `una compra en 1 pago genera una sola cuota por el total`() {
        val plans = InstallmentPlanner.plan(
            purchaseDate = LocalDate.of(2026, 8, 10),
            totalAmount = Money(100_000, "ARS"),
            totalInstallments = 1,
            closingDay = 25,
            dueDay = 5,
        )
        assertEquals(1, plans.size)
        assertEquals(Money(100_000, "ARS"), plans.single().amount)
        assertEquals(LocalDate.of(2026, 8, 25), plans.single().accountingDate)
        assertEquals(LocalDate.of(2026, 9, 5), plans.single().dueDate)
    }

    @Test
    fun `divide el monto en partes iguales entre las cuotas`() {
        val plans = InstallmentPlanner.plan(
            purchaseDate = LocalDate.of(2026, 8, 10),
            totalAmount = Money(300_000, "ARS"),
            totalInstallments = 3,
            closingDay = 25,
            dueDay = 5,
        )
        assertEquals(listOf(100_000L, 100_000L, 100_000L), plans.map { it.amount.minorUnits })
    }

    @Test
    fun `ajusta el resto de la division entera en la ultima cuota`() {
        val plans = InstallmentPlanner.plan(
            purchaseDate = LocalDate.of(2026, 8, 10),
            totalAmount = Money(100_000, "ARS"),
            totalInstallments = 3,
            closingDay = 25,
            dueDay = 5,
        )
        assertEquals(listOf(33_333L, 33_333L, 33_334L), plans.map { it.amount.minorUnits })
    }

    @Test
    fun `la suma de las cuotas siempre es exactamente el monto total`() {
        val total = Money(1_000_007, "ARS")
        for (n in 1..24) {
            val plans = InstallmentPlanner.plan(
                purchaseDate = LocalDate.of(2026, 3, 15),
                totalAmount = total,
                totalInstallments = n,
                closingDay = 10,
                dueDay = 20,
            )
            val sum = plans.sumOf { it.amount.minorUnits }
            assertEquals("con $n cuotas debe sumar el total exacto", total.minorUnits, sum)
        }
    }

    @Test
    fun `cada cuota se imputa a un ciclo de facturacion sucesivo`() {
        val plans = InstallmentPlanner.plan(
            purchaseDate = LocalDate.of(2026, 8, 10),
            totalAmount = Money(300_000, "ARS"),
            totalInstallments = 3,
            closingDay = 25,
            dueDay = 5,
        )
        assertEquals(
            listOf(LocalDate.of(2026, 8, 25), LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 25)),
            plans.map { it.accountingDate },
        )
        assertEquals(
            listOf(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 5)),
            plans.map { it.dueDate },
        )
    }

    @Test
    fun `numera las cuotas de 1 a N`() {
        val plans = InstallmentPlanner.plan(
            purchaseDate = LocalDate.of(2026, 8, 10),
            totalAmount = Money(300_000, "ARS"),
            totalInstallments = 3,
            closingDay = 25,
            dueDay = 5,
        )
        assertEquals(listOf(1, 2, 3), plans.map { it.installmentNumber })
        assertEquals(listOf(3, 3, 3), plans.map { it.totalInstallments })
    }

    @Test
    fun `rechaza cero o menos cuotas`() {
        assertThrows(IllegalArgumentException::class.java) {
            InstallmentPlanner.plan(LocalDate.of(2026, 8, 10), Money(1000, "ARS"), 0, 25, 5)
        }
    }

    @Test
    fun `rechaza monto no positivo`() {
        assertThrows(IllegalArgumentException::class.java) {
            InstallmentPlanner.plan(LocalDate.of(2026, 8, 10), Money(0, "ARS"), 3, 25, 5)
        }
    }
}
