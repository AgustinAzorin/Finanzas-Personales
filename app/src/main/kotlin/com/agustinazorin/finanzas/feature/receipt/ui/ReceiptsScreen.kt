package com.agustinazorin.finanzas.feature.receipt.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.core.receipt.ReceiptImageTarget
import com.agustinazorin.finanzas.core.receipt.ReceiptProcessingResult
import com.agustinazorin.finanzas.core.ui.components.LabeledDropdown
import com.agustinazorin.finanzas.core.ui.format.parseMoneyInput
import com.agustinazorin.finanzas.engine.receipt.AfipReceiptData
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.category.domain.Category
import com.agustinazorin.finanzas.feature.receipt.domain.Receipt
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReceiptsScreen(viewModel: ReceiptViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val pendingCapture by viewModel.pendingCapture.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var pendingTarget by remember { mutableStateOf<ReceiptImageTarget?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val target = pendingTarget
        pendingTarget = null
        if (success && target != null) viewModel.onImageCaptured(target.path)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onImagePicked(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val target = viewModel.createCameraTarget()
                        pendingTarget = target
                        cameraLauncher.launch(target.uri)
                    },
                    enabled = !isProcessing,
                ) { Text(stringResource(R.string.receipt_take_photo)) }
                OutlinedButton(
                    onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !isProcessing,
                ) { Text(stringResource(R.string.receipt_pick_gallery)) }
            }
        }

        if (isProcessing) {
            item { CircularProgressIndicator() }
        }

        if (state.receipts.isEmpty()) {
            item { Text(stringResource(R.string.receipt_empty), style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(state.receipts, key = { it.id }) { receipt -> ReceiptRow(receipt) }
        }
    }

    pendingCapture?.let { capture ->
        when (val result = capture.result) {
            is ReceiptProcessingResult.Afip -> ConfirmExpenseDialog(
                afip = result.data,
                accounts = state.accounts,
                categories = state.categories,
                onDismiss = viewModel::dismissPendingCapture,
                onConfirm = { accountId, currency, categoryId, amount, merchant, date ->
                    viewModel.confirmCreateExpense(capture.receiptId, accountId, currency, categoryId, amount, merchant, date)
                },
            )
            is ReceiptProcessingResult.Ocr -> InfoDialog(
                title = stringResource(R.string.receipt_ocr_title),
                body = result.text,
                onDismiss = viewModel::dismissPendingCapture,
            )
            ReceiptProcessingResult.None -> InfoDialog(
                title = stringResource(R.string.receipt_saved_title),
                body = stringResource(R.string.receipt_saved_no_data),
                onDismiss = viewModel::dismissPendingCapture,
            )
        }
    }
}

@Composable
private fun ReceiptRow(receipt: Receipt) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReceiptThumbnail(receipt.imagePath)
            Column {
                Text(receipt.capturedAt.toString(), style = MaterialTheme.typography.bodyMedium)
                val statusRes = when {
                    receipt.transactionId != null -> R.string.receipt_status_linked
                    receipt.afip != null -> R.string.receipt_status_afip_unlinked
                    receipt.ocrText != null -> R.string.receipt_status_ocr
                    else -> R.string.receipt_status_manual
                }
                Text(stringResource(statusRes), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ReceiptThumbnail(path: String) {
    val bitmap by produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { decodeSampledBitmap(path, targetSizePx = 96) }
    }
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
        )
    } else {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

private fun decodeSampledBitmap(path: String, targetSizePx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > targetSizePx * 2) sampleSize *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
}

@Composable
private fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_ok)) } },
    )
}

@Composable
private fun ConfirmExpenseDialog(
    afip: AfipReceiptData,
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long, currency: String, categoryId: Long?, amount: Long, merchant: String?, date: LocalDate) -> Unit,
) {
    var account by remember { mutableStateOf(accounts.firstOrNull()) }
    var category by remember { mutableStateOf<Category?>(null) }
    var amountText by remember { mutableStateOf(afip.amount.toPlainString().replace('.', ',')) }
    var merchant by remember { mutableStateOf("") }
    val categoryOptions: List<Category?> = listOf(null) + categories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.receipt_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.receipt_afip_summary, afip.pointOfSale, afip.invoiceNumber, afip.date.toString()))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.common_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(stringResource(R.string.common_merchant)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LabeledDropdown(
                    label = stringResource(R.string.common_account),
                    options = accounts,
                    selected = account,
                    optionLabel = { it.name },
                    onSelected = { account = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                LabeledDropdown(
                    label = stringResource(R.string.common_category),
                    options = categoryOptions,
                    selected = category,
                    optionLabel = { it?.name ?: stringResource(R.string.receipt_no_category) },
                    onSelected = { category = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedAccount = account ?: return@TextButton
                    val amount = parseMoneyInput(amountText) ?: return@TextButton
                    onConfirm(selectedAccount.id, selectedAccount.currency, category?.id, amount, merchant.ifBlank { null }, afip.date)
                },
            ) { Text(stringResource(R.string.receipt_create_expense)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
