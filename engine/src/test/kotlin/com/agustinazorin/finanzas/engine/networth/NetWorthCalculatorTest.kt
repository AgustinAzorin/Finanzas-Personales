package com.agustinazorin.finanzas.engine.networth

import com.agustinazorin.finanzas.engine.model.AccountType
import com.agustinazorin.finanzas.engine.model.EngineAccount
import com.agustinazorin.finanzas.engine.model.EngineTransaction
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.TransactionStatus
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.money.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class NetWorthCalculatorTest {

    private val jan1 = LocalDate.of(2026, 1, 1)

    private fun account(id: Long, type: AccountType, initial: Long, currency: String = "ARS") = EngineAccount(
        id = id,
        type = type,
        currency = currency,
        initialBalance = Money(initial, currency),
        initialBalanceDate = jan1,
        isActive = true,
    )

    @Test
    fun `patrimonio es activos menos pasivos`() {
        val bank = account(1, AccountType.BANK_ACCOUNT, 500_000)
        val card = account(2, AccountType.CREDIT_CARD, 0)
        val purchase = EngineTransaction(
            id = 1,
            accountId = 2,
            amount = Money(120_000, "ARS"),
            direction = TransactionDirection.OUTFLOW,
            date = jan1.plusDays(1),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.CONFIRMED,
        )

        val netWorth = NetWorthCalculator.netWorthByCurrency(
            accounts = listOf(bank, card),
            transactions = listOf(purchase),
            asOf = jan1.plusDays(5),
        )

        assertEquals(Money(380_000, "ARS"), netWorth["ARS"])
    }

    @Test
    fun `una transferencia entre cuentas propias no cambia el patrimonio`() {
        val bank = account(1, AccountType.BANK_ACCOUNT, 300_000)
        val savings = account(2, AccountType.SAVINGS_ACCOUNT, 100_000)

        val outflow = EngineTransaction(
            id = 1, accountId = 1, amount = Money(50_000, "ARS"), direction = TransactionDirection.OUTFLOW,
            date = jan1.plusDays(1), type = TransactionType.TRANSFER, status = TransactionStatus.CONFIRMED,
            linkedTransactionId = 2,
        )
        val inflow = EngineTransaction(
            id = 2, accountId = 2, amount = Money(50_000, "ARS"), direction = TransactionDirection.INFLOW,
            date = jan1.plusDays(1), type = TransactionType.TRANSFER, status = TransactionStatus.CONFIRMED,
            linkedTransactionId = 1,
        )

        val netWorthBefore = NetWorthCalculator.netWorthByCurrency(listOf(bank, savings), emptyList(), jan1.plusDays(1))
        val netWorthAfter = NetWorthCalculator.netWorthByCurrency(listOf(bank, savings), listOf(outflow, inflow), jan1.plusDays(2))

        assertEquals(netWorthBefore["ARS"], netWorthAfter["ARS"])
    }

    @Test
    fun `cuentas inactivas no entran en el patrimonio`() {
        val active = account(1, AccountType.BANK_ACCOUNT, 100_000)
        val inactive = account(2, AccountType.BANK_ACCOUNT, 999_999).copy(isActive = false)

        val netWorth = NetWorthCalculator.netWorthByCurrency(listOf(active, inactive), emptyList(), jan1.plusDays(1))

        assertEquals(Money(100_000, "ARS"), netWorth["ARS"])
    }

    @Test
    fun `monedas distintas no se mezclan en el patrimonio`() {
        val ars = account(1, AccountType.BANK_ACCOUNT, 100_000, currency = "ARS")
        val usd = account(2, AccountType.SAVINGS_ACCOUNT, 1_200, currency = "USD")

        val netWorth = NetWorthCalculator.netWorthByCurrency(listOf(ars, usd), emptyList(), jan1.plusDays(1))

        assertEquals(Money(100_000, "ARS"), netWorth["ARS"])
        assertEquals(Money(1_200, "USD"), netWorth["USD"])
    }
}
