package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import com.agustinazorin.finanzas.core.database.entity.InstallmentEntity
import com.agustinazorin.finanzas.engine.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Una cuota junto con el tipo (EXPENSE/INCOME) y la moneda de su Transaction "padre": el motor
 * financiero los necesita, pero no se duplican en la cuota misma (CLAUDE.md, sección 16 no los
 * lista como campos propios de Installment).
 */
data class InstallmentWithType(
    @Embedded val installment: InstallmentEntity,
    val type: TransactionType,
    val currency: String,
)

@Dao
interface InstallmentDao {

    @Insert
    suspend fun insert(installment: InstallmentEntity): Long

    @Insert
    suspend fun insertAll(installments: List<InstallmentEntity>): List<Long>

    @Query("SELECT * FROM installments WHERE transactionId = :transactionId ORDER BY installmentNumber")
    fun observeByTransaction(transactionId: Long): Flow<List<InstallmentEntity>>

    @Query(
        "SELECT i.* FROM installments i JOIN transactions t ON t.id = i.transactionId " +
            "WHERE t.accountId = :creditCardAccountId AND i.status = 'PENDING' ORDER BY i.accountingDate",
    )
    fun observeUpcoming(creditCardAccountId: Long): Flow<List<InstallmentEntity>>

    @Query(
        "SELECT i.*, t.type as type, t.currency as currency FROM installments i JOIN transactions t ON t.id = i.transactionId " +
            "WHERE t.householdId = :householdId AND i.accountingDate <= :asOf",
    )
    fun observeAllUpTo(householdId: Long, asOf: LocalDate): Flow<List<InstallmentWithType>>

    @Query(
        "UPDATE installments SET status = 'PAID' WHERE status = 'PENDING' AND accountingDate = :closingDate " +
            "AND transactionId IN (SELECT id FROM transactions WHERE accountId = :creditCardAccountId)",
    )
    suspend fun markPaidForCycle(creditCardAccountId: Long, closingDate: LocalDate)
}
