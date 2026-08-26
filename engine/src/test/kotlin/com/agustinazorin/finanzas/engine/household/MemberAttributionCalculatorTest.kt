package com.agustinazorin.finanzas.engine.household

import com.agustinazorin.finanzas.engine.model.EngineTransaction
import com.agustinazorin.finanzas.engine.model.EngineTransactionShare
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.TransactionStatus
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MemberAttributionCalculatorTest {

    private val jan1 = LocalDate.of(2026, 1, 1)
    private val jan31 = LocalDate.of(2026, 1, 31)

    private fun transaction(
        id: Long,
        amount: Long,
        type: TransactionType,
        ownerMemberId: Long? = null,
        date: LocalDate = jan1,
        hasInstallments: Boolean = false,
        status: TransactionStatus = TransactionStatus.CONFIRMED,
        currency: String = "ARS",
    ) = EngineTransaction(
        id = id, accountId = 1, amount = Money(amount, currency),
        direction = if (type == TransactionType.EXPENSE) TransactionDirection.OUTFLOW else TransactionDirection.INFLOW,
        date = date, type = type, status = status, ownerMemberId = ownerMemberId, hasInstallments = hasInstallments,
    )

    @Test
    fun `un gasto compartido se reparte entre sus beneficiarios, no a quien pago`() {
        val supermercado = transaction(id = 1, amount = 100_000, type = TransactionType.EXPENSE, ownerMemberId = 10L)
        val shares = mapOf(
            1L to listOf(
                EngineTransactionShare(1L, memberId = 10L, shareAmount = Money(50_000, "ARS")),
                EngineTransactionShare(1L, memberId = 20L, shareAmount = Money(50_000, "ARS")),
            ),
        )

        val result = MemberAttributionCalculator.summarize(listOf(supermercado), shares, jan1, jan31)

        val member10 = result.first { it.memberId == 10L }
        val member20 = result.first { it.memberId == 20L }
        assertEquals(Money(50_000, "ARS"), member10.expense)
        assertEquals(Money(50_000, "ARS"), member20.expense)
    }

    @Test
    fun `un gasto sin beneficiarios se atribuye completo a quien lo pago`() {
        val alquiler = transaction(id = 1, amount = 450_000, type = TransactionType.EXPENSE, ownerMemberId = 10L)

        val result = MemberAttributionCalculator.summarize(listOf(alquiler), emptyMap(), jan1, jan31)

        assertEquals(1, result.size)
        assertEquals(Money(450_000, "ARS"), result.first { it.memberId == 10L }.expense)
    }

    @Test
    fun `un gasto sin dueño ni beneficiarios va al balde sin atribuir`() {
        val expensas = transaction(id = 1, amount = 120_000, type = TransactionType.EXPENSE, ownerMemberId = null)

        val result = MemberAttributionCalculator.summarize(listOf(expensas), emptyMap(), jan1, jan31)

        assertEquals(1, result.size)
        assertEquals(MemberAttributionCalculator.UNASSIGNED, result.first().memberId)
        assertEquals(Money(120_000, "ARS"), result.first().expense)
    }

    @Test
    fun `los ingresos se atribuyen directo a su dueño, sin split`() {
        val sueldo = transaction(id = 1, amount = 900_000, type = TransactionType.INCOME, ownerMemberId = 10L)

        val result = MemberAttributionCalculator.summarize(listOf(sueldo), emptyMap(), jan1, jan31)

        assertEquals(Money(900_000, "ARS"), result.first { it.memberId == 10L }.income)
        assertEquals(Money.zero("ARS"), result.first { it.memberId == 10L }.expense)
    }

    @Test
    fun `excluye compras en cuotas`() {
        val compraEnCuotas = transaction(id = 1, amount = 120_000, type = TransactionType.EXPENSE, ownerMemberId = 10L, hasInstallments = true)

        val result = MemberAttributionCalculator.summarize(listOf(compraEnCuotas), emptyMap(), jan1, jan31)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `excluye transacciones fuera del periodo`() {
        val gastoFueraDePeriodo = transaction(id = 1, amount = 10_000, type = TransactionType.EXPENSE, ownerMemberId = 10L, date = jan1.minusDays(1))

        val result = MemberAttributionCalculator.summarize(listOf(gastoFueraDePeriodo), emptyMap(), jan1, jan31)

        assertTrue(result.isEmpty())
    }
}
