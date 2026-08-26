package com.agustinazorin.finanzas.engine.balance

import com.agustinazorin.finanzas.engine.model.AccountBalance
import com.agustinazorin.finanzas.engine.model.EngineAccount
import com.agustinazorin.finanzas.engine.model.EngineTransaction
import com.agustinazorin.finanzas.engine.money.Money
import java.time.LocalDate

/**
 * Calcula el saldo real de una cuenta: `initialBalance + Σ INFLOW - Σ OUTFLOW` de las
 * transacciones confirmadas o pendientes de revisión, hasta una fecha dada.
 *
 * Válido tanto para cuentas de activo como de pasivo (ver [com.agustinazorin.finanzas.engine.model.TransactionDirection]):
 * en una cuenta de pasivo un saldo negativo representa deuda.
 */
object BalanceCalculator {

    fun accountBalance(
        account: EngineAccount,
        transactions: List<EngineTransaction>,
        asOf: LocalDate = LocalDate.now(),
    ): AccountBalance {
        require(!asOf.isBefore(account.initialBalanceDate)) {
            "No se puede calcular el saldo a una fecha anterior a initialBalanceDate."
        }

        val relevant = transactions.asSequence()
            .filter { it.accountId == account.id }
            .filter { it.countsTowardsBalance }
            .filter { !it.date.isBefore(account.initialBalanceDate) && !it.date.isAfter(asOf) }
            .toList()

        val delta = Money.sum(relevant.map { it.signedAmount }, account.currency)
        return AccountBalance(
            accountId = account.id,
            asOf = asOf,
            balance = account.initialBalance + delta,
        )
    }

    fun allAccountBalances(
        accounts: List<EngineAccount>,
        transactions: List<EngineTransaction>,
        asOf: LocalDate = LocalDate.now(),
    ): List<AccountBalance> = accounts.map { accountBalance(it, transactions, asOf) }
}
