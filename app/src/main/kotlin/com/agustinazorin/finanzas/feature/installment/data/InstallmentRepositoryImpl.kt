package com.agustinazorin.finanzas.feature.installment.data

import com.agustinazorin.finanzas.core.database.dao.InstallmentDao
import com.agustinazorin.finanzas.core.database.dao.InstallmentWithType
import com.agustinazorin.finanzas.core.database.entity.InstallmentEntity
import com.agustinazorin.finanzas.feature.installment.domain.Installment
import com.agustinazorin.finanzas.feature.installment.domain.InstallmentForSummary
import com.agustinazorin.finanzas.feature.installment.domain.InstallmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class InstallmentRepositoryImpl @Inject constructor(
    private val dao: InstallmentDao,
) : InstallmentRepository {

    override fun observeByTransaction(transactionId: Long): Flow<List<Installment>> =
        dao.observeByTransaction(transactionId).map { list -> list.map { it.toDomain() } }

    override fun observeUpcoming(creditCardAccountId: Long): Flow<List<Installment>> =
        dao.observeUpcoming(creditCardAccountId).map { list -> list.map { it.toDomain() } }

    override fun observeAllUpTo(householdId: Long, asOf: LocalDate): Flow<List<InstallmentForSummary>> =
        dao.observeAllUpTo(householdId, asOf).map { list -> list.map { it.toDomain() } }

    override fun observeUpcomingForHousehold(householdId: Long): Flow<List<InstallmentForSummary>> =
        dao.observeUpcomingForHousehold(householdId).map { list -> list.map { it.toDomain() } }

    override suspend fun createInstallments(installments: List<Installment>) {
        dao.insertAll(installments.map { it.toEntity() })
    }
}

private fun InstallmentEntity.toDomain() = Installment(
    id = id, transactionId = transactionId, installmentNumber = installmentNumber, totalInstallments = totalInstallments,
    amount = amount, dueDate = dueDate, accountingDate = accountingDate, status = status,
)

private fun Installment.toEntity() = InstallmentEntity(
    id = id, transactionId = transactionId, installmentNumber = installmentNumber, totalInstallments = totalInstallments,
    amount = amount, dueDate = dueDate, accountingDate = accountingDate, status = status,
)

private fun InstallmentWithType.toDomain() =
    InstallmentForSummary(installment = installment.toDomain(), type = type, currency = currency)
