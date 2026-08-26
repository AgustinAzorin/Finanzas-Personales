package com.agustinazorin.finanzas.feature.category.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.core.ui.components.EmptyState

@Composable
fun CategoryRulesScreen(viewModel: CategoryRulesViewModel = hiltViewModel()) {
    val rules by viewModel.rules.collectAsState()

    if (rules.isEmpty()) {
        EmptyState(stringResource(R.string.category_rules_empty))
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        items(rules, key = { it.rule.id }) { ruleUi ->
            ListItem(
                headlineContent = { Text(ruleUi.rule.merchantNormalized) },
                supportingContent = { Text(ruleUi.categoryName) },
                trailingContent = {
                    TextButton(onClick = { viewModel.deleteRule(ruleUi.rule.id) }) {
                        Text(stringResource(R.string.common_delete))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
