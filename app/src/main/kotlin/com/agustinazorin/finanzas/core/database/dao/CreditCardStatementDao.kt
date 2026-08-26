package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.agustinazorin.finanzas.core.database.entity.CreditCardStatementEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CreditCardStatementDao {

    @Insert
    suspend fun insert(statement: CreditCardStatementEntity): Long

    @Update
    suspend fun update(statement: CreditCardStatementEntity)

    @Query("SELECT * FROM credit_card_statements WHERE id = :id")
    suspend fun getById(id: Long): CreditCardStatementEntity?

    @Query("SELECT * FROM credit_card_statements WHERE creditCardAccountId = :creditCardAccountId AND closingDate = :closingDate")
    suspend fun findByCycle(creditCardAccountId: Long, closingDate: LocalDate): CreditCardStatementEntity?

    @Query("SELECT * FROM credit_card_statements WHERE creditCardAccountId = :creditCardAccountId ORDER BY closingDate DESC")
    fun observeByAccount(creditCardAccountId: Long): Flow<List<CreditCardStatementEntity>>

    /** Deuda vigente de la tarjeta: la suma de lo que falta pagar en todos sus resúmenes (CLAUDE.md, sección 17). */
    @Query("SELECT COALESCE(SUM(totalAmount - paidAmount), 0) FROM credit_card_statements WHERE creditCardAccountId = :creditCardAccountId")
    suspend fun getOutstandingBalance(creditCardAccountId: Long): Long

    /** Suma de las cuotas (no canceladas) de un ciclo, para recalcular el total de su resumen. */
    @Query(
        "SELECT COALESCE(SUM(i.amount), 0) FROM installments i " +
            "JOIN transactions t ON t.id = i.transactionId " +
            "WHERE t.accountId = :creditCardAccountId AND i.accountingDate = :closingDate AND i.status != 'CANCELLED'",
    )
    suspend fun sumInstallmentsForCycle(creditCardAccountId: Long, closingDate: LocalDate): Long
}
