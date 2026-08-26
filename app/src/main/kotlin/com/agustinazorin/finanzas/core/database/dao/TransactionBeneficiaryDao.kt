package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agustinazorin.finanzas.core.database.entity.TransactionBeneficiaryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionBeneficiaryDao {

    @Insert
    suspend fun insertAll(beneficiaries: List<TransactionBeneficiaryEntity>)

    @Query("SELECT * FROM transaction_beneficiaries WHERE transactionId = :transactionId")
    suspend fun getForTransaction(transactionId: Long): List<TransactionBeneficiaryEntity>

    /** Beneficiarios de todas las transacciones del hogar cuya fecha cae dentro del período (para reportes, ver CLAUDE.md sección 30). */
    @Query(
        "SELECT tb.* FROM transaction_beneficiaries tb " +
            "INNER JOIN transactions t ON t.id = tb.transactionId " +
            "WHERE t.householdId = :householdId AND t.date BETWEEN :start AND :end",
    )
    fun observeForHousehold(householdId: Long, start: LocalDate, end: LocalDate): Flow<List<TransactionBeneficiaryEntity>>
}
