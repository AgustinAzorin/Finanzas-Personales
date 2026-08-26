package com.agustinazorin.finanzas.feature.capture.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.core.ui.components.EmptyState
import com.agustinazorin.finanzas.core.ui.components.LabeledDropdown
import com.agustinazorin.finanzas.core.ui.format.formatAsMoney
import com.agustinazorin.finanzas.core.ui.format.label
import com.agustinazorin.finanzas.core.ui.format.parseMoneyInput
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.text.MerchantNormalizer
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.capture.domain.CapturedNotification
import com.agustinazorin.finanzas.feature.capture.domain.DuplicateCandidate
import com.agustinazorin.finanzas.feature.category.domain.Category
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMember
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CAPTURE_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("d MMM HH:mm")

@Composable
fun CaptureReviewScreen(viewModel: CaptureReviewViewModel = hiltViewModel()) {
    val pendingCaptures by viewModel.pendingCaptures.collectAsState()
    val options by viewModel.options.collectAsState()

    if (pendingCaptures.isEmpty()) {
        EmptyState(stringResource(R.string.capture_review_empty))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(pendingCaptures, key = { it.id }) { capture ->
            CaptureReviewCard(
                capture = capture,
                options = options,
                onDiscard = { viewModel.discard(capture.id) },
                onConfirm = { accountId, ownerMemberId, amount, currency, direction, date, merchant, categoryId, note ->
                    viewModel.confirm(capture.id, accountId, ownerMemberId, amount, currency, direction, date, merchant, categoryId, note)
                },
                onLinkToDuplicate = { existingTransactionId -> viewModel.linkToDuplicate(capture.id, existingTransactionId) },
                findDuplicates = { accountId, amount, currency, date, merchantNormalized ->
                    viewModel.findDuplicates(accountId, amount, currency, date, merchantNormalized)
                },
                suggestCategory = { merchant -> viewModel.suggestCategory(merchant) },
            )
        }
    }
}

@Composable
private fun CaptureReviewCard(
    capture: CapturedNotification,
    options: CaptureReviewOptions,
    onDiscard: () -> Unit,
    onConfirm: (
        accountId: Long,
        ownerMemberId: Long?,
        amount: Long,
        currency: String,
        direction: TransactionDirection,
        date: LocalDate,
        merchant: String?,
        categoryId: Long?,
        note: String?,
    ) -> Unit,
    onLinkToDuplicate: (existingTransactionId: Long) -> Unit,
    findDuplicates: suspend (accountId: Long, amount: Long, currency: String, date: LocalDate, merchantNormalized: String?) -> List<DuplicateCandidate>,
    suggestCategory: suspend (merchant: String?) -> Long?,
) {
    val coroutineScope = rememberCoroutineScope()
    val captureDate = remember(capture.id) { capture.postedAt.atZone(ZoneId.systemDefault()).toLocalDate() }

    var direction by remember(capture.id) { mutableStateOf(capture.parsedDirection ?: TransactionDirection.OUTFLOW) }
    var amountText by remember(capture.id) {
        mutableStateOf(capture.parsedAmount?.let { it.formatAsMoney(capture.parsedCurrency ?: "ARS").dropWhile { c -> !c.isDigit() } }.orEmpty())
    }
    var merchant by remember(capture.id) { mutableStateOf(capture.parsedMerchant.orEmpty()) }
    var note by remember(capture.id) { mutableStateOf("") }
    var accountId by remember(capture.id) { mutableStateOf(options.accounts.firstOrNull()?.id) }
    var ownerMemberId by remember(capture.id) { mutableStateOf<Long?>(null) }
    var category by remember(capture.id) { mutableStateOf<Category?>(null) }
    var duplicates by remember(capture.id) { mutableStateOf<List<DuplicateCandidate>?>(null) }

    LaunchedEffect(capture.id, merchant) {
        if (category == null && merchant.isNotBlank()) {
            val suggestedId = suggestCategory(merchant)
            category = options.categories.firstOrNull { it.id == suggestedId }
        }
    }

    val account = options.accounts.firstOrNull { it.id == accountId }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                CAPTURE_DATE_TIME_FORMAT.format(capture.postedAt.atZone(ZoneId.systemDefault())),
                style = MaterialTheme.typography.labelMedium,
            )
            if (!capture.isParsed) {
                Text(stringResource(R.string.capture_review_unparsed), style = MaterialTheme.typography.bodySmall)
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val directions = listOf(TransactionDirection.OUTFLOW, TransactionDirection.INFLOW)
                directions.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = direction == option,
                        onClick = { direction = option },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = directions.size),
                    ) {
                        Text((if (option == TransactionDirection.OUTFLOW) TransactionType.EXPENSE else TransactionType.INCOME).label())
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.common_amount)) },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it; category = null },
                label = { Text(stringResource(R.string.common_merchant)) },
                modifier = Modifier.fillMaxWidth(),
            )

            LabeledDropdown(
                label = stringResource(R.string.common_account),
                options = options.accounts,
                selected = account,
                optionLabel = Account::name,
                onSelected = { accountId = it.id },
                modifier = Modifier.fillMaxWidth(),
            )

            val categoryOptions: List<Category?> = listOf(null) + options.categories
            LabeledDropdown(
                label = stringResource(R.string.common_category),
                options = categoryOptions,
                selected = category,
                optionLabel = { it?.name ?: stringResource(R.string.common_none) },
                onSelected = { category = it },
                modifier = Modifier.fillMaxWidth(),
            )

            if (options.members.isNotEmpty()) {
                val memberOptions: List<HouseholdMember?> = listOf(null) + options.members
                LabeledDropdown(
                    label = stringResource(R.string.capture_review_member),
                    options = memberOptions,
                    selected = options.members.firstOrNull { it.id == ownerMemberId },
                    optionLabel = { it?.name ?: stringResource(R.string.common_none) },
                    onSelected = { ownerMemberId = it?.id },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.common_note)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDiscard) { Text(stringResource(R.string.capture_review_discard)) }
                Button(
                    enabled = accountId != null && parseMoneyInput(amountText)?.let { it > 0 } == true,
                    onClick = {
                        val currentAccountId = accountId ?: return@Button
                        val amount = parseMoneyInput(amountText) ?: return@Button
                        val currency = account?.currency ?: capture.parsedCurrency ?: "ARS"
                        val merchantNormalized = merchant.takeIf { it.isNotBlank() }?.let(MerchantNormalizer::normalize)
                        coroutineScope.launch {
                            val candidates = findDuplicates(currentAccountId, amount, currency, captureDate, merchantNormalized)
                            if (candidates.isNotEmpty()) {
                                duplicates = candidates
                            } else {
                                onConfirm(
                                    currentAccountId, ownerMemberId, amount, currency, direction, captureDate,
                                    merchant.ifBlank { null }, category?.id, note.ifBlank { null },
                                )
                            }
                        }
                    },
                ) { Text(stringResource(R.string.capture_review_confirm)) }
            }
        }
    }

    val pendingDuplicates = duplicates
    if (pendingDuplicates != null) {
        AlertDialog(
            onDismissRequest = { duplicates = null },
            title = { Text(stringResource(R.string.capture_review_duplicate_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.capture_review_duplicate_dialog_message))
                    pendingDuplicates.forEach { candidate ->
                        TextButton(
                            onClick = {
                                onLinkToDuplicate(candidate.transaction.id)
                                duplicates = null
                            },
                        ) {
                            Text(
                                "${candidate.transaction.date} · " +
                                    candidate.transaction.amount.formatAsMoney(candidate.transaction.currency) +
                                    (candidate.transaction.merchant?.let { " · $it" } ?: ""),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentAccountId = accountId ?: return@TextButton
                        val amount = parseMoneyInput(amountText) ?: return@TextButton
                        val currency = account?.currency ?: capture.parsedCurrency ?: "ARS"
                        onConfirm(
                            currentAccountId, ownerMemberId, amount, currency, direction, captureDate,
                            merchant.ifBlank { null }, category?.id, note.ifBlank { null },
                        )
                        duplicates = null
                    },
                ) { Text(stringResource(R.string.capture_review_duplicate_create_new)) }
            },
            dismissButton = { TextButton(onClick = { duplicates = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}
