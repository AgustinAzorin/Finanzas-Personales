package com.agustinazorin.finanzas.feature.recurring.data

import com.agustinazorin.finanzas.core.database.dao.RecurringTransactionDao
import com.agustinazorin.finanzas.core.database.entity.RecurringTransactionEntity
import com.agustinazorin.finanzas.engine.model.Periodicity
import com.agustinazorin.finanzas.engine.model.RecurringType
import com.agustinazorin.finanzas.feature.recurring.domain.RecurringTransaction
import com.agustinazorin.finanzas.feature.recurring.domain.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecurringTransactionRepositoryImpl @Inject constructor(
    private val dao: RecurringTransactionDao,
) : RecurringTransactionRepository {

    override fun observeAll(householdId: Long): Flow<List<RecurringTransaction>> =
        dao.observeAll(householdId).map { list -> list.map { it.toDomain() } }

    override fun observeActive(householdId: Long): Flow<List<RecurringTransaction>> =
        dao.observeActive(householdId).map { list -> list.map { it.toDomain() } }

    override suspend fun create(
        householdId: Long,
        type: RecurringType,
        name: String,
        estimatedAmount: Long,
        currency: String,
        periodicity: Periodicity,
        dueDay: Int,
        categoryId: Long?,
        accountId: Long?,
        memberId: Long?,
    ): Long = dao.insert(
        RecurringTransactionEntity(
            householdId = householdId, type = type, name = name, estimatedAmount = estimatedAmount,
            currency = currency, periodicity = periodicity, dueDay = dueDay, categoryId = categoryId,
            accountId = accountId, memberId = memberId, isActive = true,
        ),
    )

    override suspend fun setActive(id: Long, isActive: Boolean) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(isActive = isActive))
    }
}

private fun RecurringTransactionEntity.toDomain() = RecurringTransaction(
    id = id, householdId = householdId, type = type, name = name, estimatedAmount = estimatedAmount,
    currency = currency, periodicity = periodicity, dueDay = dueDay, categoryId = categoryId,
    accountId = accountId, memberId = memberId, isActive = isActive,
)
