package com.agustinazorin.finanzas.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Fase 0 asume un único hogar por dispositivo. La mayoría de los ViewModels necesitan su id
 * antes de poder armar cualquier Flow de datos; esta clase base resuelve ese id una sola vez
 * (creándolo si hiciera falta) y lo expone como [householdId] para que las subclases arranquen
 * sus propios flujos con `householdId.filterNotNull().flatMapLatest { ... }`.
 */
abstract class HouseholdScopedViewModel(
    householdRepository: HouseholdRepository,
) : ViewModel() {

    private val _householdId = MutableStateFlow<Long?>(null)
    protected val householdId: StateFlow<Long?> = _householdId

    init {
        viewModelScope.launch {
            _householdId.value = householdRepository.requireHouseholdId()
        }
    }
}
