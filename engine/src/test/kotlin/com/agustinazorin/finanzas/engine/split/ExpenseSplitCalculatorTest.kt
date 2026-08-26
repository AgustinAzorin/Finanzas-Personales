package com.agustinazorin.finanzas.engine.split

import com.agustinazorin.finanzas.engine.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExpenseSplitCalculatorTest {

    @Test
    fun `reparte en partes exactamente iguales cuando el monto es divisible`() {
        val shares = ExpenseSplitCalculator.splitEqually(Money(10_000, "ARS"), listOf(1L, 2L))

        assertEquals(Money(5_000, "ARS"), shares[1L])
        assertEquals(Money(5_000, "ARS"), shares[2L])
    }

    @Test
    fun `el resto se reparte de a un centavo empezando por los primeros beneficiarios, sin perder ni inventar plata`() {
        val shares = ExpenseSplitCalculator.splitEqually(Money(10_000, "ARS"), listOf(1L, 2L, 3L))

        assertEquals(Money(3_334, "ARS"), shares[1L])
        assertEquals(Money(3_333, "ARS"), shares[2L])
        assertEquals(Money(3_333, "ARS"), shares[3L])
        val sum = shares.values.fold(Money.zero("ARS")) { acc, money -> acc + money }
        assertEquals(Money(10_000, "ARS"), sum)
    }

    @Test
    fun `un solo beneficiario se lleva el total`() {
        val shares = ExpenseSplitCalculator.splitEqually(Money(7_777, "ARS"), listOf(1L))

        assertEquals(Money(7_777, "ARS"), shares[1L])
    }

    @Test
    fun `rechaza lista vacia de beneficiarios`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExpenseSplitCalculator.splitEqually(Money(1_000, "ARS"), emptyList())
        }
    }

    @Test
    fun `rechaza beneficiarios repetidos`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExpenseSplitCalculator.splitEqually(Money(1_000, "ARS"), listOf(1L, 1L))
        }
    }

    @Test
    fun `rechaza montos no positivos`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExpenseSplitCalculator.splitEqually(Money(0, "ARS"), listOf(1L, 2L))
        }
    }
}
