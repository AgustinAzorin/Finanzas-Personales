package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agustinazorin.finanzas.core.database.entity.ReceiptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    @Insert
    suspend fun insert(receipt: ReceiptEntity): Long

    @Query("UPDATE receipts SET transactionId = :transactionId WHERE id = :receiptId")
    suspend fun linkToTransaction(receiptId: Long, transactionId: Long)

    @Query("SELECT * FROM receipts WHERE householdId = :householdId ORDER BY capturedAt DESC")
    fun observeAll(householdId: Long): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE householdId = :householdId AND transactionId IS NULL ORDER BY capturedAt DESC")
    fun observeUnlinked(householdId: Long): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE transactionId = :transactionId")
    fun observeByTransaction(transactionId: Long): Flow<List<ReceiptEntity>>
}
