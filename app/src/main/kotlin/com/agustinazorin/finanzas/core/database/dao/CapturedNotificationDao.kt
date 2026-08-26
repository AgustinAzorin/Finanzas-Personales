package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.agustinazorin.finanzas.core.database.entity.CapturedNotificationEntity
import com.agustinazorin.finanzas.core.database.entity.TransactionEntity
import com.agustinazorin.finanzas.engine.model.CaptureStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface CapturedNotificationDao {

    @Insert
    suspend fun insert(capture: CapturedNotificationEntity): Long

    @Update
    suspend fun update(capture: CapturedNotificationEntity)

    @Query("SELECT * FROM captured_notifications WHERE id = :id")
    suspend fun getById(id: Long): CapturedNotificationEntity?

    @Query("SELECT * FROM captured_notifications WHERE status = 'PENDING_REVIEW' ORDER BY postedAt DESC")
    fun observePendingReview(): Flow<List<CapturedNotificationEntity>>

    @Query("SELECT COUNT(*) FROM captured_notifications WHERE status = 'PENDING_REVIEW'")
    fun observePendingReviewCount(): Flow<Int>

    /**
     * Transacciones ya cargadas en una ventana de fechas, para que el motor de conciliación
     * (DuplicateCandidateFinder, CLAUDE.md sección 38) las compare contra una nueva captura antes
     * de crear una Transaction nueva.
     */
    @Query("SELECT * FROM transactions WHERE status != 'DUPLICATE' AND date BETWEEN :start AND :end")
    suspend fun getTransactionsBetween(start: LocalDate, end: LocalDate): List<TransactionEntity>

    @Query("UPDATE captured_notifications SET status = :status, linkedTransactionId = :linkedTransactionId WHERE id = :id")
    suspend fun setResolution(id: Long, status: CaptureStatus, linkedTransactionId: Long?)

    @Query(
        "SELECT EXISTS(SELECT 1 FROM captured_notifications " +
            "WHERE packageName = :packageName AND rawText IS :rawText AND postedAt = :postedAt)",
    )
    suspend fun existsExactDuplicate(packageName: String, rawText: String?, postedAt: Instant): Boolean
}
