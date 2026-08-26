package com.agustinazorin.finanzas.feature.currency.domain

import com.agustinazorin.finanzas.engine.model.RateSource
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

/** Variación porcentual de inflación de un mes calendario (CLAUDE.md, sección 42). */
data class InflationRate(
    val id: Long,
    val month: YearMonth,
    val percent: BigDecimal,
    val source: RateSource,
    val fetchedAt: Instant,
)

interface InflationRateRepository {
    fun observeAll(): Flow<List<InflationRate>>

    suspend fun recordRate(month: YearMonth, percent: BigDecimal, source: RateSource)
}
