package com.agustinazorin.finanzas.engine.balance

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

class BalanceCalculatorTest {

    private val jan1 = LocalDate.of(2026, 1, 1)

    private fun account(id: Long = 1, type: AccountType = AccountType.BANK_ACCOUNT, initial: Long = 100_000) =
        EngineAccount(
            id = id,
            type = type,
            currency = "ARS",
            initialBalance = Money(initial, "ARS"),
            initialBalanceDate = jan1,
            isActive = true,
        )

    private fun txn(
        id: Long,
        accountId: Long = 1,
        amount: Long,
        direction: TransactionDirection,
        date: LocalDate = jan1.plusDays(1),
        type: TransactionType = TransactionType.EXPENSE,
        status: TransactionStatus = TransactionStatus.CONFIRMED,
    ) = EngineTransaction(
        id = id,
        accountId = accountId,
        amount = Money(amount, "ARS"),
        direction = direction,
        date = date,
        type = type,
        status = status,
    )

    @Test
    fun `saldo suma inflows y resta outflows sobre el saldo inicial`() {
        val acc = account(initial = 100_000)
        val txns = listOf(
            txn(1, amount = 20_000, direction = TransactionDirection.OUTFLOW, type = TransactionType.EXPENSE),
            txn(2, amount = 50_000, direction = TransactionDirection.INFLOW, type = TransactionType.INCOME),
        )
        val balance = BalanceCalculator.accountBalance(acc, txns, asOf = jan1.plusDays(10))
        assertEquals(Money(130_000, "ARS"), balance.balance)
    }

    @Test
    fun `excluye transacciones IGNORED y DUPLICATE del saldo`() {
        val acc = account(initial = 100_000)
        val txns = listOf(
            txn(1, amount = 20_000, direction = TransactionDirection.OUTFLOW, status = TransactionStatus.IGNORED),
            txn(2, amount = 30_000, direction = TransactionDirection.OUTFLOW, status = TransactionStatus.DUPLICATE),
        )
        val balance = BalanceCalculator.accountBalance(acc, txns, asOf = jan1.plusDays(10))
        assertEquals(Money(100_000, "ARS"), balance.balance)
    }

    @Test
    fun `incluye transacciones PENDING_REVIEW porque el dinero ya se movio`() {
        val acc = account(initial = 100_000)
        val txns = listOf(
            txn(1, amount = 20_000, direction = TransactionDirection.OUTFLOW, status = TransactionStatus.PENDING_REVIEW),
        )
        val balance = BalanceCalculator.accountBalance(acc, txns, asOf = jan1.plusDays(10))
        assertEquals(Money(80_000, "ARS"), balance.balance)
    }

    @Test
    fun `no cuenta transacciones posteriores a la fecha de corte`() {
        val acc = account(initial = 100_000)
        val txns = listOf(
            txn(1, amount = 20_000, direction = TransactionDirection.OUTFLOW, date = jan1.plusDays(20)),
        )
        val balance = BalanceCalculator.accountBalance(acc, txns, asOf = jan1.plusDays(10))
        assertEquals(Money(100_000, "ARS"), balance.balance)
    }

    @Test
    fun `una tarjeta de credito acumula deuda como saldo negativo con cada compra`() {
        val card = account(id = 2, type = AccountType.CREDIT_CARD, initial = 0)
        val purchase = txn(
            id = 1,
            accountId = 2,
            amount = 45_000,
            direction = TransactionDirection.OUTFLOW,
            type = TransactionType.EXPENSE,
        )
        val balance = BalanceCalculator.accountBalance(card, listOf(purchase), asOf = jan1.plusDays(10))
        assertEquals(Money(-45_000, "ARS"), balance.balance)
    }

    @Test
    fun `pagar el resumen de tarjeta es una transferencia que reduce la deuda`() {
        val bank = account(id = 1, type = AccountType.BANK_ACCOUNT, initial = 200_000)
        val card = account(id = 2, type = AccountType.CREDIT_CARD, initial = 0)

        val purchase = txn(1, accountId = 2, amount = 45_000, direction = TransactionDirection.OUTFLOW, type = TransactionType.EXPENSE)
        val paymentOutflow = txn(2, accountId = 1, amount = 45_000, direction = TransactionDirection.OUTFLOW, type = TransactionType.TRANSFER)
        val paymentInflow = txn(3, accountId = 2, amount = 45_000, direction = TransactionDirection.INFLOW, type = TransactionType.TRANSFER)

        val txns = listOf(purchase, paymentOutflow, paymentInflow)
        val asOf = jan1.plusDays(10)

        val bankBalance = BalanceCalculator.accountBalance(bank, txns, asOf)
        val cardBalance = BalanceCalculator.accountBalance(card, txns, asOf)

        assertEquals(Money(155_000, "ARS"), bankBalance.balance)
        assertEquals(Money(0, "ARS"), cardBalance.balance)
    }
}
