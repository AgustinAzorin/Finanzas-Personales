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
    /** true si esta compra tiene cuotas asociadas (Regla 3, CLAUDE.md sección 7); ver [com.agustinazorin.finanzas.core.database.entity.TransactionEntity.hasInstallments]. */
    val hasInstallments: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * "Beneficiario" de un gasto compartido (CLAUDE.md, sección 30): cuánto de una [Transaction] le
 * corresponde económicamente a un miembro del hogar, más allá de quién la pagó
 * ([Transaction.ownerMemberId]). Ver [com.agustinazorin.finanzas.core.database.entity.TransactionBeneficiaryEntity].
 */
data class TransactionBeneficiary(
    val id: Long = 0,
    val transactionId: Long,
    val memberId: Long,
    val shareAmount: Long,
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

    /** Guarda el reparto de un gasto compartido (CLAUDE.md, sección 30). [beneficiaries] ya trae calculado el share de cada uno. */
    suspend fun saveBeneficiaries(beneficiaries: List<TransactionBeneficiary>)

    suspend fun getBeneficiaries(transactionId: Long): List<TransactionBeneficiary>

    /** Beneficiarios de todos los gastos compartidos del hogar en el período, para reportes personales y del hogar (roadmap Fase 3). */
    fun observeBeneficiariesForHousehold(householdId: Long, start: LocalDate, end: LocalDate): Flow<List<TransactionBeneficiary>>
}
