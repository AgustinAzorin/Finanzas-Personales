package com.agustinazorin.finanzas.feature.currency.domain

import com.agustinazorin.finanzas.engine.model.RateSource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Cuántas unidades de [baseCurrency] equivalen a 1 unidad de [currency] en [date] (CLAUDE.md, sección 41). */
data class ExchangeRate(
    val id: Long,
    val currency: String,
    val baseCurrency: String,
    val rate: BigDecimal,
    val date: LocalDate,
    val source: RateSource,
    val fetchedAt: Instant,
)

interface ExchangeRateRepository {
    fun observeLatest(currency: String, baseCurrency: String): Flow<ExchangeRate?>
    fun observeHistory(currency: String, baseCurrency: String): Flow<List<ExchangeRate>>

    suspend fun recordRate(
        currency: String,
        baseCurrency: String,
        rate: BigDecimal,
        date: LocalDate,
        source: RateSource,
    )
}
