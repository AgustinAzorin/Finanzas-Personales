package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agustinazorin.finanzas.core.database.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {

    /** Idempotente por (currency, baseCurrency, date): ver KDoc de [ExchangeRateEntity]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rate: ExchangeRateEntity)

    @Query(
        "SELECT * FROM exchange_rates WHERE currency = :currency AND baseCurrency = :baseCurrency " +
            "ORDER BY date DESC LIMIT 1",
    )
    fun observeLatest(currency: String, baseCurrency: String): Flow<ExchangeRateEntity?>

    @Query(
        "SELECT * FROM exchange_rates WHERE currency = :currency AND baseCurrency = :baseCurrency ORDER BY date",
    )
    fun observeHistory(currency: String, baseCurrency: String): Flow<List<ExchangeRateEntity>>
}
