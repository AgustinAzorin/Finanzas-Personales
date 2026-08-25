package com.agustinazorin.finanzas.feature.transaction.domain.usecase

import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.TransactionSource
import com.agustinazorin.finanzas.engine.model.TransactionStatus
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class AddIncomeUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
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
    ): Long {
        require(amount > 0) { "El monto de un ingreso debe ser mayor a cero." }
        val now = Instant.now()
        return transactionRepository.createTransaction(
            Transaction(
                id = 0,
                householdId = householdId,
                accountId = accountId,
                ownerMemberId = ownerMemberId,
                amount = amount,
                currency = currency,
                direction = TransactionDirection.INFLOW,
                date = date,
                merchant = merchant,
                categoryId = categoryId,
                type = TransactionType.INCOME,
                source = TransactionSource.MANUAL,
                note = note,
                reconciliationHash = null,
                linkedTransactionId = null,
                status = TransactionStatus.CONFIRMED,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}
