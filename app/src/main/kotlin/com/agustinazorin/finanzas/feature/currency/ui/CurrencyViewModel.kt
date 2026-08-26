package com.agustinazorin.finanzas.feature.currency.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.engine.model.RateSource
import com.agustinazorin.finanzas.feature.currency.domain.ExchangeRate
import com.agustinazorin.finanzas.feature.currency.domain.ExchangeRateRepository
import com.agustinazorin.finanzas.feature.currency.domain.InflationRate
import com.agustinazorin.finanzas.feature.currency.domain.InflationRateRepository
import com.agustinazorin.finanzas.feature.currency.domain.usecase.RefreshExchangeRateUseCase
import com.agustinazorin.finanzas.feature.currency.domain.usecase.RefreshInflationRatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val CURRENCY = "USD"
private const val BASE_CURRENCY = "ARS"

data class CurrencyUiState(
    val latestUsdRate: ExchangeRate? = null,
    val rateHistory: List<ExchangeRate> = emptyList(),
    val inflationRates: List<InflationRate> = emptyList(),
)

data class RefreshStatus(val isLoading: Boolean = false, val errorMessage: String? = null)

/**
 * Cotizaciones e inflación (CLAUDE.md, secciones 41 y 42): pantalla de referencia pública,
 * independiente del hogar. Combina lo cargado por API (RateSource.API, sólo por acción explícita
 * del usuario) con lo que el usuario carga a mano (RateSource.MANUAL) — la app nunca depende de
 * la red para poder usarse (sección 2).
 */
@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val inflationRateRepository: InflationRateRepository,
    private val refreshExchangeRateUseCase: RefreshExchangeRateUseCase,
    private val refreshInflationRatesUseCase: RefreshInflationRatesUseCase,
) : ViewModel() {

    val uiState: StateFlow<CurrencyUiState> = combine(
        exchangeRateRepository.observeLatest(CURRENCY, BASE_CURRENCY),
        exchangeRateRepository.observeHistory(CURRENCY, BASE_CURRENCY),
        inflationRateRepository.observeAll(),
    ) { latest, history, inflation ->
        CurrencyUiState(latestUsdRate = latest, rateHistory = history.reversed(), inflationRates = inflation.reversed())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CurrencyUiState())

    private val _rateRefresh = MutableStateFlow(RefreshStatus())
    val rateRefresh: StateFlow<RefreshStatus> = _rateRefresh

    private val _inflationRefresh = MutableStateFlow(RefreshStatus())
    val inflationRefresh: StateFlow<RefreshStatus> = _inflationRefresh

    fun refreshExchangeRate() {
        viewModelScope.launch {
            _rateRefresh.value = RefreshStatus(isLoading = true)
            _rateRefresh.value = refreshExchangeRateUseCase().fold(
                onSuccess = { RefreshStatus() },
                onFailure = { RefreshStatus(errorMessage = it.message) },
            )
        }
    }

    fun refreshInflation() {
        viewModelScope.launch {
            _inflationRefresh.value = RefreshStatus(isLoading = true)
            _inflationRefresh.value = refreshInflationRatesUseCase().fold(
                onSuccess = { RefreshStatus() },
                onFailure = { RefreshStatus(errorMessage = it.message) },
            )
        }
    }

    fun addManualRate(rate: BigDecimal, date: LocalDate) {
        viewModelScope.launch {
            exchangeRateRepository.recordRate(CURRENCY, BASE_CURRENCY, rate, date, RateSource.MANUAL)
        }
    }

    fun addManualInflation(month: YearMonth, percent: BigDecimal) {
        viewModelScope.launch {
            inflationRateRepository.recordRate(month, percent, RateSource.MANUAL)
        }
    }
}
