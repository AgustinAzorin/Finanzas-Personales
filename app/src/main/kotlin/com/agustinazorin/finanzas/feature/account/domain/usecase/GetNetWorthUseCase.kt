package com.agustinazorin.finanzas.feature.account.domain.usecase

import com.agustinazorin.finanzas.core.engine.toEngineAccount
import com.agustinazorin.finanzas.core.engine.toEngineTransaction
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.engine.networth.NetWorthCalculator
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Patrimonio neto por moneda (Activos - Pasivos, Regla 4 de CLAUDE.md). En Fase 0 se deriva
 * sólo de cuentas: Asset/Liability independientes llegan en Fase 5.
 */
class GetNetWorthUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(householdId: Long, asOf: LocalDate = LocalDate.now()) =
        combine(
            accountRepository.observeActiveAccounts(householdId),
            transactionRepository.observeAllUpTo(householdId, asOf),
        ) { accounts, transactions ->
            NetWorthCalculator.netWorthByCurrency(
                accounts = accounts.map { it.toEngineAccount() },
                transactions = transactions.map { it.toEngineTransaction() },
                asOf = asOf,
            )
        }
}

/** Patrimonio en la moneda base del hogar, o cero si todavía no hay cuentas en esa moneda. */
fun Map<String, Money>.inCurrency(currency: String): Money = this[currency] ?: Money.zero(currency)
