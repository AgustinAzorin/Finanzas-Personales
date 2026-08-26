package com.agustinazorin.finanzas.feature.currency.data

import com.agustinazorin.finanzas.core.database.dao.ExchangeRateDao
import com.agustinazorin.finanzas.core.database.entity.ExchangeRateEntity
import com.agustinazorin.finanzas.engine.model.RateSource
import com.agustinazorin.finanzas.feature.currency.domain.ExchangeRate
import com.agustinazorin.finanzas.feature.currency.domain.ExchangeRateRepository
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExchangeRateRepositoryImpl @Inject constructor(
    private val dao: ExchangeRateDao,
) : ExchangeRateRepository {

    override fun observeLatest(currency: String, baseCurrency: String): Flow<ExchangeRate?> =
        dao.observeLatest(currency, baseCurrency).map { it?.toDomain() }

    override fun observeHistory(currency: String, baseCurrency: String): Flow<List<ExchangeRate>> =
        dao.observeHistory(currency, baseCurrency).map { list -> list.map { it.toDomain() } }

    override suspend fun recordRate(
        currency: String,
        baseCurrency: String,
        rate: BigDecimal,
        date: LocalDate,
        source: RateSource,
    ) {
        dao.upsert(
            ExchangeRateEntity(
                currency = currency,
                baseCurrency = baseCurrency,
                rate = rate.toDouble(),
                date = date,
                source = source,
                fetchedAt = Instant.now(),
            ),
        )
    }
}

private fun ExchangeRateEntity.toDomain() = ExchangeRate(
    id = id,
    currency = currency,
    baseCurrency = baseCurrency,
    rate = BigDecimal.valueOf(rate),
    date = date,
    source = source,
    fetchedAt = fetchedAt,
)
