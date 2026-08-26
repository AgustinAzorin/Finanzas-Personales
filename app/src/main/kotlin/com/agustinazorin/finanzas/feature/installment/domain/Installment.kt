package com.agustinazorin.finanzas.feature.installment.domain

import com.agustinazorin.finanzas.engine.model.InstallmentStatus
import com.agustinazorin.finanzas.engine.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Una cuota de una compra con tarjeta (CLAUDE.md, sección 16). */
data class Installment(
    val id: Long,
    val transactionId: Long,
    val installmentNumber: Int,
    val totalInstallments: Int,
    val amount: Long,
    val dueDate: LocalDate,
    val accountingDate: LocalDate,
    val status: InstallmentStatus,
)

/** Una cuota junto con el tipo (EXPENSE/INCOME) y la moneda de su compra "padre", para el motor financiero. */
data class InstallmentForSummary(val installment: Installment, val type: TransactionType, val currency: String)

interface InstallmentRepository {
    fun observeByTransaction(transactionId: Long): Flow<List<Installment>>
    fun observeUpcoming(creditCardAccountId: Long): Flow<List<Installment>>
    fun observeAllUpTo(householdId: Long, asOf: LocalDate): Flow<List<InstallmentForSummary>>
    fun observeUpcomingForHousehold(householdId: Long): Flow<List<InstallmentForSummary>>
    suspend fun createInstallments(installments: List<Installment>)
}
