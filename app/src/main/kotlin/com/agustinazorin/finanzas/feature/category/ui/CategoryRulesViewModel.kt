package com.agustinazorin.finanzas.feature.category.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.feature.category.domain.CategoryRepository
import com.agustinazorin.finanzas.feature.category.domain.CategoryRule
import com.agustinazorin.finanzas.feature.category.domain.CategoryRuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryRuleUi(val rule: CategoryRule, val categoryName: String)

@HiltViewModel
class CategoryRulesViewModel @Inject constructor(
    private val categoryRuleRepository: CategoryRuleRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val rules: StateFlow<List<CategoryRuleUi>> = combine(
        categoryRuleRepository.observeAll(),
        categoryRepository.observeAll(),
    ) { rules, categories ->
        val nameById = categories.associate { it.id to it.name }
        rules.map { rule -> CategoryRuleUi(rule, nameById[rule.categoryId].orEmpty()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteRule(id: Long) {
        viewModelScope.launch { categoryRuleRepository.deleteRule(id) }
    }
}
