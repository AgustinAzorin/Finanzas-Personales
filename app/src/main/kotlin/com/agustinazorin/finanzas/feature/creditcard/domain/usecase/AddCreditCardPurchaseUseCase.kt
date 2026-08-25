package com.agustinazorin.finanzas.feature.creditcard.domain.usecase

import com.agustinazorin.finanzas.engine.creditcard.CreditCardCycleCalculator
import com.agustinazorin.finanzas.engine.installments.InstallmentPlanner
import com.agustinazorin.finanzas.engine.model.InstallmentStatus
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.TransactionSource
import com.agustinazorin.finanzas.engine.model.TransactionStatus
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.engine.text.MerchantNormalizer
import com.agustinazorin.finanzas.feature.category.domain.CategoryRuleRepository
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCardRepository
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCardStatementRepository
import com.agustinazorin.finanzas.feature.installment.domain.Installment
import com.agustinazorin.finanzas.feature.installment.domain.InstallmentRepository
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Registra una compra con tarjeta de crédito (Regla 3, CLAUDE.md sección 7): siempre 1 Purchase
 * + N Installments, incluso cuando es "en 1 pago" (N=1). La deuda se reconoce de una sola vez al
 * momento de la compra (Regla 4: la Transaction "padre" es un OUTFLOW por el monto total, así
 * que el saldo/patrimonio cae de inmediato); el "gasto mensual" en cambio se reparte por cuota
 * únicamente cuando hay más de una (ver [com.agustinazorin.finanzas.engine.metrics.PeriodSummaryCalculator]).
 *
 * Cada cuota planificada actualiza (o crea) de inmediato el resumen del ciclo al que pertenece,
 * aunque sea un ciclo futuro: así el crédito disponible de la tarjeta cae por el total de la
 * compra ya mismo, no de a poco a medida que se van facturando las cuotas.
 */
class AddCreditCardPurchaseUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val installmentRepository: InstallmentRepository,
    private val creditCardRepository: CreditCardRepository,
    private val creditCardStatementRepository: CreditCardStatementRepository,
    private val categoryRuleRepository: CategoryRuleRepository,
) {
    suspend operator fun invoke(
        householdId: Long,
        creditCardAccountId: Long,
        ownerMemberId: Long?,
        totalAmount: Long,
        currency: String,
        purchaseDate: LocalDate,
        totalInstallments: Int,
        categoryId: Long?,
        merchant: String? = null,
        note: String? = null,
    ): Long {
        require(totalAmount > 0) { "El monto de la compra debe ser mayor a cero." }
        require(totalInstallments >= 1) { "Una compra debe tener al menos 1 cuota." }
        val creditCard = requireNotNull(creditCardRepository.getByAccount(creditCardAccountId)) {
            "Esta cuenta todavía no tiene configurada una tarjeta de crédito."
        }

        val now = Instant.now()
        val transactionId = transactionRepository.createTransaction(
            Transaction(
                id = 0,
                householdId = householdId,
                accountId = creditCardAccountId,
                ownerMemberId = ownerMemberId,
                amount = totalAmount,
                currency = currency,
                direction = TransactionDirection.OUTFLOW,
                date = purchaseDate,
                merchant = merchant,
                categoryId = categoryId,
                type = TransactionType.EXPENSE,
                source = TransactionSource.MANUAL,
                note = note,
                reconciliationHash = null,
                linkedTransactionId = null,
                status = TransactionStatus.CONFIRMED,
                hasInstallments = totalInstallments > 1,
                createdAt = now,
                updatedAt = now,
            ),
        )

        val plans = InstallmentPlanner.plan(
            purchaseDate = purchaseDate,
            totalAmount = Money(totalAmount, currency),
            totalInstallments = totalInstallments,
            closingDay = creditCard.closingDay,
            dueDay = creditCard.dueDay,
        )

        installmentRepository.createInstallments(
            plans.map { plan ->
                Installment(
                    id = 0,
                    transactionId = transactionId,
                    installmentNumber = plan.installmentNumber,
                    totalInstallments = plan.totalInstallments,
                    amount = plan.amount.minorUnits,
                    dueDate = plan.dueDate,
                    accountingDate = plan.accountingDate,
                    status = InstallmentStatus.PENDING,
                )
            },
        )

        plans.forEach { plan ->
            val cycle = CreditCardCycleCalculator.cycleContaining(plan.accountingDate, creditCard.closingDay, creditCard.dueDay)
            creditCardStatementRepository.recomputeStatement(
                creditCardAccountId = creditCardAccountId,
                periodStart = cycle.periodStart,
                closingDate = plan.accountingDate,
                dueDate = plan.dueDate,
            )
        }

        if (!merchant.isNullOrBlank() && categoryId != null) {
            categoryRuleRepository.learn(MerchantNormalizer.normalize(merchant), categoryId)
        }

        return transactionId
    }
}
