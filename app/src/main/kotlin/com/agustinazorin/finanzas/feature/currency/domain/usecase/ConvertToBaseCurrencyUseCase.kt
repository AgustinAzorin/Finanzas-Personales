package com.agustinazorin.finanzas.feature.currency.domain.usecase

import com.agustinazorin.finanzas.engine.currency.CurrencyConversionResult
import com.agustinazorin.finanzas.engine.currency.CurrencyConverter
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.currency.domain.ExchangeRateRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * Convierte un total ya agrupado por moneda (ej: el resultado de
 * [com.agustinazorin.finanzas.feature.account.domain.usecase.GetNetWorthUseCase]) a una única
 * moneda base, usando la última cotización guardada de cada moneda distinta a la base
 * (CLAUDE.md, sección 41). Nunca inventa una tasa: si no hay ninguna cargada para una moneda,
 * esa moneda queda listada en [CurrencyConversionResult.missingRates] en vez de ignorarse
 * silenciosamente.
 */
class ConvertToBaseCurrencyUseCase @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
) {
    operator fun invoke(amountsByCurrency: Map<String, Money>, baseCurrency: String): Flow<CurrencyConversionResult> {
        val foreignCurrencies = amountsByCurrency.keys - baseCurrency
        if (foreignCurrencies.isEmpty()) {
            return flowOf(
                CurrencyConversionResult(
                    total = amountsByCurrency[baseCurrency] ?: Money.zero(baseCurrency),
                    missingRates = emptySet(),
                ),
            )
        }
        val rateFlows = foreignCurrencies.map { currency -> exchangeRateRepository.observeLatest(currency, baseCurrency) }
        return combine(rateFlows) { latestRates ->
            val ratesToBase = foreignCurrencies.zip(latestRates.toList())
                .mapNotNull { (currency, rate) -> rate?.let { currency to it.rate } }
                .toMap()
            CurrencyConverter.toBaseCurrency(amountsByCurrency, baseCurrency, ratesToBase)
        }
    }
}
