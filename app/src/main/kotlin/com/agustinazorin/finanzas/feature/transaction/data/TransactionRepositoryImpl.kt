package com.agustinazorin.finanzas.feature.transaction.data

import com.agustinazorin.finanzas.core.database.dao.TransactionBeneficiaryDao
import com.agustinazorin.finanzas.core.database.dao.TransactionDao
import com.agustinazorin.finanzas.core.database.entity.TransactionBeneficiaryEntity
import com.agustinazorin.finanzas.core.database.entity.TransactionEntity
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionBeneficiary
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionFilter
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val beneficiaryDao: TransactionBeneficiaryDao,
) : TransactionRepository {

    override fun observeRecent(householdId: Long, limit: Int): Flow<List<Transaction>> =
        dao.observeRecent(householdId, limit).map { list -> list.map { it.toDomain() } }

    override fun observeFiltered(householdId: Long, filter: TransactionFilter): Flow<List<Transaction>> =
        dao.observeFiltered(
            householdId = householdId,
            start = filter.start,
            end = filter.end,
            accountId = filter.accountId,
            categoryId = filter.categoryId,
            memberId = filter.memberId,
            type = filter.type,
            status = filter.status,
            source = filter.source,
        ).map { list -> list.map { it.toDomain() } }

    override fun observeAllUpTo(householdId: Long, asOf: LocalDate): Flow<List<Transaction>> =
        dao.observeAllUpTo(householdId, asOf).map { list -> list.map { it.toDomain() } }

    override suspend fun getAllUpTo(householdId: Long, asOf: LocalDate): List<Transaction> =
        dao.getAllUpTo(householdId, asOf).map { it.toDomain() }

    override suspend fun getById(id: Long): Transaction? = dao.getById(id)?.toDomain()

    override suspend fun createTransaction(transaction: Transaction): Long =
        dao.insert(transaction.toEntity())

    override suspend fun createTransfer(outflow: Transaction, inflow: Transaction) {
        dao.insertTransfer(outflow.toEntity(), inflow.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        dao.update(transaction.toEntity())
    }

    override suspend fun saveBeneficiaries(beneficiaries: List<TransactionBeneficiary>) {
        if (beneficiaries.isEmpty()) return
        beneficiaryDao.insertAll(beneficiaries.map { it.toEntity() })
    }

    override suspend fun getBeneficiaries(transactionId: Long): List<TransactionBeneficiary> =
        beneficiaryDao.getForTransaction(transactionId).map { it.toDomain() }

    override fun observeBeneficiariesForHousehold(householdId: Long, start: LocalDate, end: LocalDate): Flow<List<TransactionBeneficiary>> =
        beneficiaryDao.observeForHousehold(householdId, start, end).map { list -> list.map { it.toDomain() } }
}

private fun TransactionEntity.toDomain() = Transaction(
    id = id, householdId = householdId, accountId = accountId, ownerMemberId = ownerMemberId,
    amount = amount, currency = currency, direction = direction, date = date, merchant = merchant,
    categoryId = categoryId, type = type, source = source, note = note,
    reconciliationHash = reconciliationHash, linkedTransactionId = linkedTransactionId,
    status = status, hasInstallments = hasInstallments, createdAt = createdAt, updatedAt = updatedAt,
)

private fun Transaction.toEntity() = TransactionEntity(
    id = id, householdId = householdId, accountId = accountId, ownerMemberId = ownerMemberId,
    amount = amount, currency = currency, direction = direction, date = date, merchant = merchant,
    categoryId = categoryId, type = type, source = source, note = note,
    reconciliationHash = reconciliationHash, linkedTransactionId = linkedTransactionId,
    status = status, hasInstallments = hasInstallments, createdAt = createdAt, updatedAt = updatedAt,
)

private fun TransactionBeneficiaryEntity.toDomain() = TransactionBeneficiary(
    id = id, transactionId = transactionId, memberId = memberId, shareAmount = shareAmount,
)

private fun TransactionBeneficiary.toEntity() = TransactionBeneficiaryEntity(
    id = id, transactionId = transactionId, memberId = memberId, shareAmount = shareAmount,
)
