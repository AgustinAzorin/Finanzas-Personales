package com.agustinazorin.finanzas.feature.currency.data

import com.agustinazorin.finanzas.core.database.dao.InflationRateDao
import com.agustinazorin.finanzas.core.database.entity.InflationRateEntity
import com.agustinazorin.finanzas.engine.model.RateSource
import com.agustinazorin.finanzas.feature.currency.domain.InflationRate
import com.agustinazorin.finanzas.feature.currency.domain.InflationRateRepository
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InflationRateRepositoryImpl @Inject constructor(
    private val dao: InflationRateDao,
) : InflationRateRepository {

    override fun observeAll(): Flow<List<InflationRate>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun recordRate(month: YearMonth, percent: BigDecimal, source: RateSource) {
        dao.upsert(
            InflationRateEntity(
                yearMonth = month.atDay(1),
                monthlyRatePercent = percent.toDouble(),
                source = source,
                fetchedAt = Instant.now(),
            ),
        )
    }
}

private fun InflationRateEntity.toDomain() = InflationRate(
    id = id,
    month = YearMonth.from(yearMonth),
    percent = BigDecimal.valueOf(monthlyRatePercent),
    source = source,
    fetchedAt = fetchedAt,
)
