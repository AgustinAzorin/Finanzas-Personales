package com.agustinazorin.finanzas.feature.transaction.domain.usecase

import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.TransactionSource
import com.agustinazorin.finanzas.engine.model.TransactionStatus
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.engine.split.ExpenseSplitCalculator
import com.agustinazorin.finanzas.engine.text.MerchantNormalizer
import com.agustinazorin.finanzas.feature.category.domain.CategoryRuleRepository
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionBeneficiary
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class AddExpenseUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRuleRepository: CategoryRuleRepository,
) {
    /**
     * @param sharedWithMemberIds "Beneficiarios" de un gasto compartido (CLAUDE.md, sección 30),
     * repartido en partes iguales entre ellos. Vacío = gasto no compartido, se atribuye completo
     * a [ownerMemberId] (o sin atribuir, si también es null).
     */
    suspend operator fun invoke(
        householdId: Long,
        accountId: Long,
        ownerMemberId: Long?,
        amount: Long,
        currency: String,
        date: LocalDate,
        categoryId: Long?,
        merchant: String? = null,
        note: String? = null,
        sharedWithMemberIds: List<Long> = emptyList(),
    ): Long {
        require(amount > 0) { "El monto de un gasto debe ser mayor a cero." }
        val now = Instant.now()
        val id = transactionRepository.createTransaction(
            Transaction(
                id = 0,
                householdId = householdId,
                accountId = accountId,
                ownerMemberId = ownerMemberId,
                amount = amount,
                currency = currency,
                direction = TransactionDirection.OUTFLOW,
                date = date,
                merchant = merchant,
                categoryId = categoryId,
                type = TransactionType.EXPENSE,
                source = TransactionSource.MANUAL,
                note = note,
                reconciliationHash = null,
                linkedTransactionId = null,
                status = TransactionStatus.CONFIRMED,
                createdAt = now,
                updatedAt = now,
            ),
        )
        if (sharedWithMemberIds.isNotEmpty()) {
            val shares = ExpenseSplitCalculator.splitEqually(Money(amount, currency), sharedWithMemberIds.distinct())
            transactionRepository.saveBeneficiaries(
                shares.map { (memberId, share) -> TransactionBeneficiary(transactionId = id, memberId = memberId, shareAmount = share.minorUnits) },
            )
        }
        // Aprende la categorización de este comercio para sugerirla en futuras capturas (CLAUDE.md, sección 39).
        if (!merchant.isNullOrBlank() && categoryId != null) {
            categoryRuleRepository.learn(MerchantNormalizer.normalize(merchant), categoryId)
        }
        return id
    }
}
