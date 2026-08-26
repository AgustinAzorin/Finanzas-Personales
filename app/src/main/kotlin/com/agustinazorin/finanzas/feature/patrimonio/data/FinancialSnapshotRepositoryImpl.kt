package com.agustinazorin.finanzas.feature.patrimonio.data

import com.agustinazorin.finanzas.core.database.dao.FinancialSnapshotDao
import com.agustinazorin.finanzas.core.database.entity.FinancialSnapshotEntity
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.patrimonio.domain.FinancialSnapshot
import com.agustinazorin.finanzas.feature.patrimonio.domain.FinancialSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class FinancialSnapshotRepositoryImpl @Inject constructor(
    private val dao: FinancialSnapshotDao,
) : FinancialSnapshotRepository {

    override fun observeHistory(householdId: Long, currency: String): Flow<List<FinancialSnapshot>> =
        dao.observeHistory(householdId, currency).map { list -> list.map { it.toDomain() } }

    override suspend fun recordSnapshot(
        householdId: Long,
        date: LocalDate,
        netWorth: Money,
        totalAssets: Money,
        totalLiabilities: Money,
        availableLiquidity: Money,
    ) {
        val currency = netWorth.currency
        require(totalAssets.currency == currency && totalLiabilities.currency == currency && availableLiquidity.currency == currency) {
            "Todos los montos de un snapshot deben estar en la misma moneda ($currency)."
        }
        dao.upsert(
            FinancialSnapshotEntity(
                householdId = householdId,
                date = date,
                currency = currency,
                netWorth = netWorth.minorUnits,
                totalAssets = totalAssets.minorUnits,
                totalLiabilities = totalLiabilities.minorUnits,
                availableLiquidity = availableLiquidity.minorUnits,
                createdAt = Instant.now(),
            ),
        )
    }
}

private fun FinancialSnapshotEntity.toDomain() = FinancialSnapshot(
    id = id, householdId = householdId, date = date, currency = currency, netWorth = netWorth,
    totalAssets = totalAssets, totalLiabilities = totalLiabilities, availableLiquidity = availableLiquidity,
    createdAt = createdAt,
)
