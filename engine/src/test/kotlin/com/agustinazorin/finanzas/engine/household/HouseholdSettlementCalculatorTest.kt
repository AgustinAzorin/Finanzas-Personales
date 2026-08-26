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

class HouseholdSettlementCalculatorTest {

    private val jan1 = LocalDate.of(2026, 1, 1)

    private fun expense(
        id: Long,
        amount: Long,
        payerMemberId: Long?,
        currency: String = "ARS",
        status: TransactionStatus = TransactionStatus.CONFIRMED,
    ) = EngineTransaction(
        id = id, accountId = 1, amount = Money(amount, currency), direction = TransactionDirection.OUTFLOW,
        date = jan1, type = TransactionType.EXPENSE, status = status, ownerMemberId = payerMemberId,
    )

    @Test
    fun `un gasto compartido 50-50 genera una deuda del beneficiario hacia quien pago`() {
        val supermercado = expense(id = 1, amount = 100_000, payerMemberId = 10L)
        val shares = mapOf(
            1L to listOf(
                EngineTransactionShare(1L, memberId = 10L, shareAmount = Money(50_000, "ARS")),
                EngineTransactionShare(1L, memberId = 20L, shareAmount = Money(50_000, "ARS")),
            ),
        )

        val debts = HouseholdSettlementCalculator.calculate(listOf(supermercado), shares)

        assertEquals(1, debts.size)
        assertEquals(HouseholdDebt("ARS", owedByMemberId = 20L, owedToMemberId = 10L, amount = Money(50_000, "ARS")), debts.first())
    }

    @Test
    fun `las deudas cruzadas entre el mismo par se netean a un unico sentido`() {
        val gastoA = expense(id = 1, amount = 100_000, payerMemberId = 10L)
        val gastoB = expense(id = 2, amount = 30_000, payerMemberId = 20L)
        val shares = mapOf(
            1L to listOf(
                EngineTransactionShare(1L, memberId = 10L, shareAmount = Money(50_000, "ARS")),
                EngineTransactionShare(1L, memberId = 20L, shareAmount = Money(50_000, "ARS")),
            ),
            2L to listOf(
                EngineTransactionShare(2L, memberId = 10L, shareAmount = Money(30_000, "ARS")),
            ),
        )

        val debts = HouseholdSettlementCalculator.calculate(listOf(gastoA, gastoB), shares)

        assertEquals(1, debts.size)
        assertEquals(HouseholdDebt("ARS", owedByMemberId = 20L, owedToMemberId = 10L, amount = Money(20_000, "ARS")), debts.first())
    }

    @Test
    fun `deudas exactamente iguales entre el mismo par se cancelan del todo`() {
        val gastoA = expense(id = 1, amount = 60_000, payerMemberId = 10L)
        val gastoB = expense(id = 2, amount = 60_000, payerMemberId = 20L)
        val shares = mapOf(
            1L to listOf(
                EngineTransactionShare(1L, memberId = 10L, shareAmount = Money(30_000, "ARS")),
                EngineTransactionShare(1L, memberId = 20L, shareAmount = Money(30_000, "ARS")),
            ),
            2L to listOf(
                EngineTransactionShare(2L, memberId = 10L, shareAmount = Money(30_000, "ARS")),
                EngineTransactionShare(2L, memberId = 20L, shareAmount = Money(30_000, "ARS")),
            ),
        )

        val debts = HouseholdSettlementCalculator.calculate(listOf(gastoA, gastoB), shares)

        assertTrue(debts.isEmpty())
    }

    @Test
    fun `un gasto sin quien pago no genera deuda`() {
        val gasto = expense(id = 1, amount = 100_000, payerMemberId = null)
        val shares = mapOf(
            1L to listOf(EngineTransactionShare(1L, memberId = 20L, shareAmount = Money(100_000, "ARS"))),
        )

        val debts = HouseholdSettlementCalculator.calculate(listOf(gasto), shares)

        assertTrue(debts.isEmpty())
    }

    @Test
    fun `la parte del propio pagador no genera deuda consigo mismo`() {
        val gasto = expense(id = 1, amount = 100_000, payerMemberId = 10L)
        val shares = mapOf(
            1L to listOf(
                EngineTransactionShare(1L, memberId = 10L, shareAmount = Money(60_000, "ARS")),
                EngineTransactionShare(1L, memberId = 20L, shareAmount = Money(40_000, "ARS")),
            ),
        )

        val debts = HouseholdSettlementCalculator.calculate(listOf(gasto), shares)

        assertEquals(1, debts.size)
        assertEquals(HouseholdDebt("ARS", owedByMemberId = 20L, owedToMemberId = 10L, amount = Money(40_000, "ARS")), debts.first())
    }

    @Test
    fun `monedas distintas no se mezclan en las deudas`() {
        val gastoArs = expense(id = 1, amount = 10_000, payerMemberId = 10L, currency = "ARS")
        val gastoUsd = expense(id = 2, amount = 100, payerMemberId = 20L, currency = "USD")
        val shares = mapOf(
            1L to listOf(EngineTransactionShare(1L, memberId = 20L, shareAmount = Money(10_000, "ARS"))),
            2L to listOf(EngineTransactionShare(2L, memberId = 10L, shareAmount = Money(100, "USD"))),
        )

        val debts = HouseholdSettlementCalculator.calculate(listOf(gastoArs, gastoUsd), shares)

        assertEquals(2, debts.size)
        assertTrue(debts.contains(HouseholdDebt("ARS", owedByMemberId = 20L, owedToMemberId = 10L, amount = Money(10_000, "ARS"))))
        assertTrue(debts.contains(HouseholdDebt("USD", owedByMemberId = 10L, owedToMemberId = 20L, amount = Money(100, "USD"))))
    }
}
