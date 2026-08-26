package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agustinazorin.finanzas.core.database.entity.InflationRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InflationRateDao {

    /** Idempotente por yearMonth: ver KDoc de [InflationRateEntity]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rate: InflationRateEntity)

    @Query("SELECT * FROM inflation_rates ORDER BY yearMonth")
    fun observeAll(): Flow<List<InflationRateEntity>>
}
