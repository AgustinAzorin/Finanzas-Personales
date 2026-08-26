package com.agustinazorin.finanzas.feature.receipt.ui

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.receipt.ReceiptImageStore
import com.agustinazorin.finanzas.core.receipt.ReceiptImageTarget
import com.agustinazorin.finanzas.core.receipt.ReceiptProcessingResult
import com.agustinazorin.finanzas.core.receipt.ReceiptProcessor
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.model.ReceiptSource
import com.agustinazorin.finanzas.engine.model.TransactionSource
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.category.domain.Category
import com.agustinazorin.finanzas.feature.category.domain.CategoryRepository
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import com.agustinazorin.finanzas.feature.receipt.domain.AfipReceiptInfo
import com.agustinazorin.finanzas.feature.receipt.domain.Receipt
import com.agustinazorin.finanzas.feature.receipt.domain.ReceiptRepository
import com.agustinazorin.finanzas.feature.transaction.domain.usecase.AddExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReceiptUiState(
    val receipts: List<Receipt> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
)

/** Lo recién fotografiado, procesado, y a la espera de que el usuario lo revise. */
data class PendingCapture(val receiptId: Long, val result: ReceiptProcessingResult)

/**
 * Comprobantes (CLAUDE.md, sección 40): fotografiar, extraer datos del QR AFIP/ARCA o, a falta
 * de QR, texto por OCR, y opcionalmente crear un gasto a partir de eso — siempre con confirmación
 * explícita del usuario, nunca automático (sección 38: "nunca inventar información financiera
 * sin confirmación").
 */
@HiltViewModel
class ReceiptViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val receiptRepository: ReceiptRepository,
    private val receiptImageStore: ReceiptImageStore,
    private val receiptProcessor: ReceiptProcessor,
    private val addExpenseUseCase: AddExpenseUseCase,
) : HouseholdScopedViewModel(householdRepository) {

    val uiState: StateFlow<ReceiptUiState> = householdId.filterNotNull().flatMapLatest { id ->
        combine(
            receiptRepository.observeAll(id),
            accountRepository.observeActiveAccounts(id),
            categoryRepository.observeAll(),
        ) { receipts, accounts, categories -> ReceiptUiState(receipts, accounts, categories) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReceiptUiState())

    private val _pendingCapture = MutableStateFlow<PendingCapture?>(null)
    val pendingCapture: StateFlow<PendingCapture?> = _pendingCapture

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    fun createCameraTarget(): ReceiptImageTarget = receiptImageStore.createPendingImage()

    fun onImageCaptured(imagePath: String) = processAndSave(imagePath)

    fun onImagePicked(uri: Uri) {
        val path = receiptImageStore.copyToReceiptsDir(uri) ?: return
        processAndSave(path)
    }

    private fun processAndSave(imagePath: String) {
        viewModelScope.launch {
            val id = householdId.value ?: return@launch
            _isProcessing.value = true
            val result = receiptProcessor.process(imagePath)
            val receiptId = receiptRepository.createReceipt(
                householdId = id,
                imagePath = imagePath,
                source = result.toReceiptSource(),
                qrRawContent = (result as? ReceiptProcessingResult.Afip)?.rawQrContent,
                ocrText = (result as? ReceiptProcessingResult.Ocr)?.text,
                afip = (result as? ReceiptProcessingResult.Afip)?.data?.let {
                    AfipReceiptInfo(
                        cuitEmisor = it.cuitEmisor,
                        pointOfSale = it.pointOfSale,
                        invoiceType = it.invoiceType,
                        invoiceNumber = it.invoiceNumber,
                        amount = it.amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact(),
                        currency = if (it.currency == "PES") "ARS" else it.currency,
                        date = it.date,
                        authorizationCode = it.authorizationCode,
                    )
                },
            )
            _isProcessing.value = false
            _pendingCapture.value = PendingCapture(receiptId, result)
        }
    }

    fun dismissPendingCapture() {
        _pendingCapture.value = null
    }

    fun confirmCreateExpense(receiptId: Long, accountId: Long, currency: String, categoryId: Long?, amount: Long, merchant: String?, date: LocalDate) {
        viewModelScope.launch {
            val id = householdId.value ?: return@launch
            val transactionId = addExpenseUseCase(
                householdId = id,
                accountId = accountId,
                ownerMemberId = null,
                amount = amount,
                currency = currency,
                date = date,
                categoryId = categoryId,
                merchant = merchant,
                source = TransactionSource.QR,
            )
            receiptRepository.linkToTransaction(receiptId, transactionId)
            _pendingCapture.value = null
        }
    }
}

private fun ReceiptProcessingResult.toReceiptSource(): ReceiptSource = when (this) {
    is ReceiptProcessingResult.Afip -> ReceiptSource.QR
    is ReceiptProcessingResult.Ocr -> ReceiptSource.OCR
    ReceiptProcessingResult.None -> ReceiptSource.MANUAL
}
