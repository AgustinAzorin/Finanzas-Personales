package com.agustinazorin.finanzas.feature.household.ui

import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.model.MemberType
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMember
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMemberRepository
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HouseholdMembersViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val memberRepository: HouseholdMemberRepository,
) : HouseholdScopedViewModel(householdRepository) {

    val members: StateFlow<List<HouseholdMember>> = householdId.filterNotNull()
        .flatMapLatest { memberRepository.observeMembers(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addMember(name: String, type: MemberType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = householdId.value ?: return@launch
            memberRepository.addMember(id, name, type)
        }
    }

    fun setMemberActive(memberId: Long, isActive: Boolean) {
        viewModelScope.launch { memberRepository.setMemberActive(memberId, isActive) }
    }
}
