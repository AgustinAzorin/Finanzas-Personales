package com.agustinazorin.finanzas.feature.transaction.domain

import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.TransactionSource
import com.agustinazorin.finanzas.engine.model.TransactionStatus
import com.agustinazorin.finanzas.engine.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

data class Transaction(
    val id: Long,
    val householdId: Long,
    val accountId: Long,
    val ownerMemberId: Long?,
    val amount: Long,
    val currency: String,
    val direction: TransactionDirection,
    val date: LocalDate,
    val merchant: String?,
    val categoryId: Long?,
    val type: TransactionType,
    val source: TransactionSource,
    val note: String?,
    val reconciliationHash: String?,
    val linkedTransactionId: Long?,
    val status: TransactionStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Filtros de la pantalla de Transacciones (CLAUDE.md, sección 29). Un campo null = sin filtrar por ese criterio. */
data class TransactionFilter(
    val start: LocalDate,
    val end: LocalDate,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val memberId: Long? = null,
    val type: TransactionType? = null,
    val status: TransactionStatus? = null,
    val source: TransactionSource? = null,
)

interface TransactionRepository {
    fun observeRecent(householdId: Long, limit: Int): Flow<List<Transaction>>
    fun observeFiltered(householdId: Long, filter: TransactionFilter): Flow<List<Transaction>>
    fun observeAllUpTo(householdId: Long, asOf: LocalDate): Flow<List<Transaction>>
    suspend fun getAllUpTo(householdId: Long, asOf: LocalDate): List<Transaction>
    suspend fun getById(id: Long): Transaction?

    suspend fun createTransaction(transaction: Transaction): Long

    /** Única forma soportada de registrar una transferencia (Regla 1, CLAUDE.md sección 7). */
    suspend fun createTransfer(outflow: Transaction, inflow: Transaction)

    suspend fun updateTransaction(transaction: Transaction)
}
