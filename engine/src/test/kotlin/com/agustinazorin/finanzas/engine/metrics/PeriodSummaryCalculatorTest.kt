package com.agustinazorin.finanzas.engine.metrics

import com.agustinazorin.finanzas.engine.model.EngineTransaction
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.TransactionStatus
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PeriodSummaryCalculatorTest {

    private val start = LocalDate.of(2026, 8, 1)
    private val end = LocalDate.of(2026, 8, 31)

    private fun txn(
        id: Long,
        amount: Long,
        type: TransactionType,
        date: LocalDate = start.plusDays(1),
        status: TransactionStatus = TransactionStatus.CONFIRMED,
        direction: TransactionDirection = if (type == TransactionType.INCOME) TransactionDirection.INFLOW else TransactionDirection.OUTFLOW,
    ) = EngineTransaction(
        id = id, accountId = 1, amount = Money(amount, "ARS"), direction = direction,
        date = date, type = type, status = status,
    )

    @Test
    fun `suma gastos e ingresos del periodo`() {
        val txns = listOf(
            txn(1, 100_000, TransactionType.EXPENSE),
            txn(2, 50_000, TransactionType.EXPENSE),
            txn(3, 900_000, TransactionType.INCOME),
        )
        val summary = PeriodSummaryCalculator.summarize(txns, start, end).getValue("ARS")
        assertEquals(Money(150_000, "ARS"), summary.totalExpense)
        assertEquals(Money(900_000, "ARS"), summary.totalIncome)
    }

    @Test
    fun `excluye transferencias y ajustes del gasto e ingreso real`() {
        val txns = listOf(
            txn(1, 45_000, TransactionType.TRANSFER),
            txn(2, 10_000, TransactionType.ADJUSTMENT),
            txn(3, 900_000, TransactionType.INCOME),
        )
        val summary = PeriodSummaryCalculator.summarize(txns, start, end).getValue("ARS")
        assertEquals(Money(0, "ARS"), summary.totalExpense)
        assertEquals(Money(900_000, "ARS"), summary.totalIncome)
    }

    @Test
    fun `excluye transacciones fuera del rango de fechas`() {
        val txns = listOf(
            txn(1, 100_000, TransactionType.EXPENSE, date = start.minusDays(1)),
            txn(2, 100_000, TransactionType.EXPENSE, date = end.plusDays(1)),
        )
        val summary = PeriodSummaryCalculator.summarize(txns, start, end)
        assertEquals(true, summary.isEmpty())
    }

    @Test
    fun `tasa de ahorro es null cuando no hubo ingresos`() {
        val txns = listOf(txn(1, 100_000, TransactionType.EXPENSE))
        val summary = PeriodSummaryCalculator.summarize(txns, start, end).getValue("ARS")
        assertNull(summary.savingsRate)
    }

    @Test
    fun `tasa de ahorro se calcula como (ingresos menos gastos) sobre ingresos`() {
        val txns = listOf(
            txn(1, 300_000, TransactionType.EXPENSE),
            txn(2, 1_000_000, TransactionType.INCOME),
        )
        val summary = PeriodSummaryCalculator.summarize(txns, start, end).getValue("ARS")
        assertEquals(0.7, summary.savingsRate!!, 0.0001)
    }
}
