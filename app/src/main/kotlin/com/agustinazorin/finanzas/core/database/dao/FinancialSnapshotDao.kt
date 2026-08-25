package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agustinazorin.finanzas.core.database.entity.FinancialSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialSnapshotDao {

    /**
     * Grabar el snapshot de hoy dos veces es intencionalmente idempotente: el índice único
     * (householdId, date, currency) hace que la segunda llamada reemplace a la primera en vez de
     * duplicar la fila (CLAUDE.md, sección 22: "snapshots periódicos", no uno por cada apertura
     * de pantalla).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: FinancialSnapshotEntity)

    @Query("SELECT * FROM financial_snapshots WHERE householdId = :householdId AND currency = :currency ORDER BY date")
    fun observeHistory(householdId: Long, currency: String): Flow<List<FinancialSnapshotEntity>>
}
