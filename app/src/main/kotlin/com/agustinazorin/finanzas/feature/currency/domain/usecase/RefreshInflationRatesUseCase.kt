package com.agustinazorin.finanzas.feature.currency.domain.usecase

import com.agustinazorin.finanzas.core.network.InflacionApiClient
import com.agustinazorin.finanzas.engine.model.RateSource
import com.agustinazorin.finanzas.feature.currency.domain.InflationRateRepository
import javax.inject.Inject

/**
 * Trae la serie histórica de inflación mensual de api.argentinadatos.com y la guarda mes por mes
 * (CLAUDE.md, sección 42). Sólo se ejecuta por acción explícita del usuario; el ajuste por
 * inflación en sí sigue siendo exclusivamente para análisis, nunca modifica montos nominales.
 */
class RefreshInflationRatesUseCase @Inject constructor(
    private val inflacionApiClient: InflacionApiClient,
    private val inflationRateRepository: InflationRateRepository,
) {
    /** Devuelve la cantidad de meses guardados si la descarga fue exitosa. */
    suspend operator fun invoke(): Result<Int> =
        inflacionApiClient.fetchMonthlyInflation().map { monthlyRates ->
            monthlyRates.forEach { entry ->
                inflationRateRepository.recordRate(entry.month, entry.percent, RateSource.API)
            }
            monthlyRates.size
        }
}
