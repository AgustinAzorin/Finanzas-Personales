package com.agustinazorin.finanzas.feature.account.domain.usecase

import com.agustinazorin.finanzas.engine.model.AccountType
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

private val LIQUID_ACCOUNT_TYPES = setOf(
    AccountType.CASH, AccountType.BANK_ACCOUNT, AccountType.SAVINGS_ACCOUNT,
    AccountType.MERCADO_PAGO, AccountType.DIGITAL_WALLET,
)

/** Dinero líquido disponible ahora mismo, sumado en [currency] (CLAUDE.md, secciones 5 y 24). */
class GetAvailableLiquidityUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val getAccountBalancesUseCase: GetAccountBalancesUseCase,
) {
    operator fun invoke(householdId: Long, currency: String, asOf: LocalDate = LocalDate.now()) =
        combine(
            accountRepository.observeActiveAccounts(householdId),
            getAccountBalancesUseCase(householdId, asOf),
        ) { accounts, balances ->
            val balanceByAccount = balances.associateBy { it.accountId }
            val liquidBalances = accounts
                .filter { it.type in LIQUID_ACCOUNT_TYPES && it.currency == currency }
                .mapNotNull { balanceByAccount[it.id]?.balance }
            Money.sum(liquidBalances, currency)
        }
}
