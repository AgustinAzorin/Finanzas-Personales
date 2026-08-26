package com.agustinazorin.finanzas.feature.currency.domain.usecase

import com.agustinazorin.finanzas.core.network.DolarApiClient
import com.agustinazorin.finanzas.engine.model.RateSource
import com.agustinazorin.finanzas.feature.currency.domain.ExchangeRateRepository
import javax.inject.Inject

/**
 * Trae la cotización del dólar blue de dolarapi.com y la guarda como cotización USD/ARS
 * (CLAUDE.md, sección 41). Se usa el valor de venta: es el que necesita alguien que quiere saber
 * cuántos pesos equivalen sus dólares. Sólo se ejecuta por acción explícita del usuario.
 */
class RefreshExchangeRateUseCase @Inject constructor(
    private val dolarApiClient: DolarApiClient,
    private val exchangeRateRepository: ExchangeRateRepository,
) {
    suspend operator fun invoke(): Result<Unit> =
        dolarApiClient.fetchBlueQuote().map { quote ->
            exchangeRateRepository.recordRate(
                currency = "USD",
                baseCurrency = "ARS",
                rate = quote.venta,
                date = quote.date,
                source = RateSource.API,
            )
        }
}
