package com.agustinazorin.finanzas.feature.account.domain.usecase

import com.agustinazorin.finanzas.core.engine.toEngineAccount
import com.agustinazorin.finanzas.core.engine.toEngineAsset
import com.agustinazorin.finanzas.core.engine.toEngineLiability
import com.agustinazorin.finanzas.core.engine.toEngineTransaction
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.engine.networth.NetWorthCalculator
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.patrimonio.domain.AssetRepository
import com.agustinazorin.finanzas.feature.patrimonio.domain.LiabilityRepository
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class GetNetWorthUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val assetRepository: AssetRepository,
    private val liabilityRepository: LiabilityRepository,
) {
    operator fun invoke(householdId: Long, asOf: LocalDate = LocalDate.now()) =
        combine(
            accountRepository.observeActiveAccounts(householdId),
            transactionRepository.observeAllUpTo(householdId, asOf),
            assetRepository.observeActiveAssets(householdId),
            liabilityRepository.observeActiveLiabilities(householdId),
        ) { accounts, transactions, assets, liabilities ->
            NetWorthCalculator.netWorthByCurrency(
                accounts = accounts.map { it.toEngineAccount() },
                transactions = transactions.map { it.toEngineTransaction() },
                asOf = asOf,
                assets = assets.map { it.toEngineAsset() },
                liabilities = liabilities.map { it.toEngineLiability() },
            )
        }
}

fun Map<String, Money>.inCurrency(currency: String): Money = this[currency] ?: Money.zero(currency)
