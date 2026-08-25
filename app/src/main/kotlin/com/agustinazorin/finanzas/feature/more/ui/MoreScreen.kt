package com.agustinazorin.finanzas.feature.more.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.navigation.SecondaryRoutes

private data class MoreItem(val route: String, val labelRes: Int, val icon: ImageVector)

private val moreItems = listOf(
    MoreItem(SecondaryRoutes.CATEGORIES, R.string.categories_title, Icons.Filled.Category),
    MoreItem(SecondaryRoutes.HOUSEHOLD_MEMBERS, R.string.household_members_title, Icons.Filled.Groups),
    MoreItem(SecondaryRoutes.INCOME, R.string.income_title, Icons.Filled.Payments),
    MoreItem(SecondaryRoutes.RECURRING, R.string.recurring_title, Icons.Filled.Repeat),
    MoreItem(SecondaryRoutes.CAPTURE_REVIEW, R.string.capture_review_title, Icons.Filled.Inbox),
    MoreItem(SecondaryRoutes.CAPTURE_SETTINGS, R.string.capture_settings_title, Icons.Filled.NotificationsActive),
    MoreItem(SecondaryRoutes.CATEGORY_RULES, R.string.category_rules_title, Icons.Filled.Rule),
)

@Composable
fun MoreScreen(onNavigate: (String) -> Unit, viewModel: MoreViewModel = hiltViewModel()) {
    val pendingReviewCount by viewModel.pendingReviewCount.collectAsState()

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(moreItems, key = { it.route }) { item ->
            val showBadge = item.route == SecondaryRoutes.CAPTURE_REVIEW && pendingReviewCount > 0
            ListItem(
                headlineContent = { Text(stringResource(item.labelRes)) },
                leadingContent = { Icon(item.icon, contentDescription = null) },
                trailingContent = if (showBadge) { { Text(pendingReviewCount.toString()) } } else null,
                modifier = Modifier.fillMaxWidth().clickable { onNavigate(item.route) },
            )
        }
    }
}
