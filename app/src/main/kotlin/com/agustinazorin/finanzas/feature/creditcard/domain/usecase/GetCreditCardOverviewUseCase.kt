package com.agustinazorin.finanzas.feature.creditcard.domain.usecase

import com.agustinazorin.finanzas.engine.creditcard.CreditCardAvailableCreditCalculator
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCard
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCardRepository
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCardStatement
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCardStatementRepository
import com.agustinazorin.finanzas.feature.installment.domain.Installment
import com.agustinazorin.finanzas.feature.installment.domain.InstallmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/** Vista consolidada de una tarjeta para la pantalla "Tarjetas" (CLAUDE.md, secciones 17 y 18). */
data class CreditCardOverview(
    val creditCard: CreditCard,
    val currency: String,
    val outstandingBalance: Long,
    val availableCredit: Long,
    val statements: List<CreditCardStatement>,
    val upcomingInstallments: List<Installment>,
)

class GetCreditCardOverviewUseCase @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val creditCardStatementRepository: CreditCardStatementRepository,
    private val installmentRepository: InstallmentRepository,
    private val accountRepository: AccountRepository,
) {
    operator fun invoke(accountId: Long): Flow<CreditCardOverview> =
        creditCardRepository.observeByAccount(accountId).filterNotNull().flatMapLatest { creditCard ->
            combine(
                accountRepository.observeAccount(accountId).filterNotNull(),
                creditCardStatementRepository.observeByAccount(accountId),
                installmentRepository.observeUpcoming(accountId),
            ) { account, statements, upcoming ->
                val outstanding = statements.sumOf { it.outstandingAmount }
                val available = CreditCardAvailableCreditCalculator.compute(
                    creditLimit = Money(creditCard.creditLimit, account.currency),
                    outstandingBalance = Money(outstanding, account.currency),
                ).minorUnits
                CreditCardOverview(
                    creditCard = creditCard,
                    currency = account.currency,
                    outstandingBalance = outstanding,
                    availableCredit = available,
                    statements = statements,
                    upcomingInstallments = upcoming,
                )
            }
        }
}
