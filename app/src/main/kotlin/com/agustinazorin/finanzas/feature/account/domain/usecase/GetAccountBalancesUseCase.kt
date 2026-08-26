package com.agustinazorin.finanzas.feature.account.domain.usecase

import com.agustinazorin.finanzas.core.engine.toEngineAccount
import com.agustinazorin.finanzas.core.engine.toEngineTransaction
import com.agustinazorin.finanzas.engine.balance.BalanceCalculator
import com.agustinazorin.finanzas.engine.model.AccountBalance
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/** Saldo real de cada cuenta activa del hogar, a una fecha dada (por defecto, hoy). */
class GetAccountBalancesUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(householdId: Long, asOf: LocalDate = LocalDate.now()) =
        combine(
            accountRepository.observeActiveAccounts(householdId),
            transactionRepository.observeAllUpTo(householdId, asOf),
        ) { accounts, transactions ->
            BalanceCalculator.allAccountBalances(
                accounts = accounts.map { it.toEngineAccount() },
                transactions = transactions.map { it.toEngineTransaction() },
                asOf = asOf,
            )
        }

    suspend fun accountBalance(account: Account, householdId: Long, asOf: LocalDate = LocalDate.now()): AccountBalance {
        val transactions = transactionRepository.getAllUpTo(householdId, asOf)
        return BalanceCalculator.accountBalance(
            account = account.toEngineAccount(),
            transactions = transactions.map { it.toEngineTransaction() },
            asOf = asOf,
        )
    }
}
