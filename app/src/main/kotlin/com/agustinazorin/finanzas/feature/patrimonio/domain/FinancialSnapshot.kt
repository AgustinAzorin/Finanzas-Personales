package com.agustinazorin.finanzas.feature.patrimonio.domain

import com.agustinazorin.finanzas.engine.money.Money
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

data class FinancialSnapshot(
    val id: Long,
    val householdId: Long,
    val date: LocalDate,
    val currency: String,
    val netWorth: Long,
    val totalAssets: Long,
    val totalLiabilities: Long,
    val availableLiquidity: Long,
    val createdAt: Instant,
)

interface FinancialSnapshotRepository {
    fun observeHistory(householdId: Long, currency: String): Flow<List<FinancialSnapshot>>

    suspend fun recordSnapshot(
        householdId: Long,
        date: LocalDate,
        netWorth: Money,
        totalAssets: Money,
        totalLiabilities: Money,
        availableLiquidity: Money,
    )
}
