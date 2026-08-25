package com.agustinazorin.finanzas.feature.recurring.domain

import com.agustinazorin.finanzas.engine.model.Periodicity
import com.agustinazorin.finanzas.engine.model.RecurringType
import kotlinx.coroutines.flow.Flow

data class RecurringTransaction(
    val id: Long,
    val householdId: Long,
    val type: RecurringType,
    val name: String,
    val estimatedAmount: Long,
    val currency: String,
    val periodicity: Periodicity,
    val dueDay: Int,
    val categoryId: Long?,
    val accountId: Long?,
    val memberId: Long?,
    val isActive: Boolean,
)

interface RecurringTransactionRepository {
    fun observeAll(householdId: Long): Flow<List<RecurringTransaction>>
    fun observeActive(householdId: Long): Flow<List<RecurringTransaction>>

    suspend fun create(
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
    ): Long

    suspend fun setActive(id: Long, isActive: Boolean)
}
