package com.agustinazorin.finanzas.feature.patrimonio.domain.usecase

import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.account.domain.usecase.GetAvailableLiquidityUseCase
import com.agustinazorin.finanzas.feature.account.domain.usecase.GetNetWorthUseCase
import com.agustinazorin.finanzas.feature.account.domain.usecase.inCurrency
import com.agustinazorin.finanzas.feature.patrimonio.domain.AssetRepository
import com.agustinazorin.finanzas.feature.patrimonio.domain.FinancialSnapshotRepository
import com.agustinazorin.finanzas.feature.patrimonio.domain.LiabilityRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Graba el snapshot patrimonial de hoy (CLAUDE.md, sección 22). "Si hace falta" no es una
 * comprobación explícita acá: el índice único (householdId, date, currency) del
 * [FinancialSnapshotRepository] hace que grabar dos veces el mismo día sea un simple reemplazo
 * (upsert), así que esto puede llamarse en cada visita a la pantalla de Patrimonio sin
 * preocuparse por duplicar filas ni por saber si "ya se grabó hoy".
 */
class RecordSnapshotIfNeededUseCase @Inject constructor(
    private val getNetWorthUseCase: GetNetWorthUseCase,
    private val getAvailableLiquidityUseCase: GetAvailableLiquidityUseCase,
    private val assetRepository: AssetRepository,
    private val liabilityRepository: LiabilityRepository,
    private val financialSnapshotRepository: FinancialSnapshotRepository,
) {
    suspend operator fun invoke(householdId: Long, currency: String, today: LocalDate = LocalDate.now()) {
        val netWorth = getNetWorthUseCase(householdId, today).first().inCurrency(currency)
        val liquidity = getAvailableLiquidityUseCase(householdId, currency, today).first()

        val assets = assetRepository.observeActiveAssets(householdId).first()
        val totalAssets = Money.sum(
            assets.filter { it.currency == currency }.map { Money(it.currentValue, currency) },
            currency,
        )

        val liabilities = liabilityRepository.observeActiveLiabilities(householdId).first()
        val totalLiabilities = Money.sum(
            liabilities.filter { it.currency == currency }.map { Money(it.outstandingAmount, currency) },
            currency,
        )

        financialSnapshotRepository.recordSnapshot(
            householdId = householdId,
            date = today,
            netWorth = netWorth,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            availableLiquidity = liquidity,
        )
    }
}
