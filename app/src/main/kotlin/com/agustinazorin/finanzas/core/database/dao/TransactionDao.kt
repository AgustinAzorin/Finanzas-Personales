package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.agustinazorin.finanzas.core.database.entity.TransactionEntity
import com.agustinazorin.finanzas.engine.model.TransactionSource
import com.agustinazorin.finanzas.engine.model.TransactionStatus
import com.agustinazorin.finanzas.engine.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET linkedTransactionId = :linkedId WHERE id = :id")
    suspend fun setLinkedTransactionId(id: Long, linkedId: Long)

    /**
     * Única forma soportada de crear una transferencia: inserta ambas filas (OUTFLOW en la
     * cuenta origen, INFLOW en la cuenta destino) y las vincula entre sí, todo dentro de una
     * misma transacción de base de datos (Regla 1, CLAUDE.md sección 7). Nunca deben quedar
     * huérfanas.
     */
    @Transaction
    suspend fun insertTransfer(outflow: TransactionEntity, inflow: TransactionEntity) {
        val outflowId = insert(outflow)
        val inflowId = insert(inflow.copy(linkedTransactionId = outflowId))
        setLinkedTransactionId(outflowId, inflowId)
    }

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query(
        "SELECT * FROM transactions WHERE householdId = :householdId " +
            "AND status != 'DUPLICATE' ORDER BY date DESC, id DESC LIMIT :limit",
    )
    fun observeRecent(householdId: Long, limit: Int): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE householdId = :householdId " +
            "AND date BETWEEN :start AND :end " +
            "AND (:accountId IS NULL OR accountId = :accountId) " +
            "AND (:categoryId IS NULL OR categoryId = :categoryId) " +
            "AND (:memberId IS NULL OR ownerMemberId = :memberId) " +
            "AND (:type IS NULL OR type = :type) " +
            "AND (:status IS NULL OR status = :status) " +
            "AND (:source IS NULL OR source = :source) " +
            "ORDER BY date DESC, id DESC",
    )
    fun observeFiltered(
        householdId: Long,
        start: LocalDate,
        end: LocalDate,
        accountId: Long?,
        categoryId: Long?,
        memberId: Long?,
        type: TransactionType?,
        status: TransactionStatus?,
        source: TransactionSource?,
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE householdId = :householdId AND date <= :asOf")
    suspend fun getAllUpTo(householdId: Long, asOf: LocalDate): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE householdId = :householdId AND date <= :asOf")
    fun observeAllUpTo(householdId: Long, asOf: LocalDate): Flow<List<TransactionEntity>>
}
