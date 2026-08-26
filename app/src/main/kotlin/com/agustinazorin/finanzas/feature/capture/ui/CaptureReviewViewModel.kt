package com.agustinazorin.finanzas.feature.capture.ui

import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.text.MerchantNormalizer
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.capture.domain.CapturedNotification
import com.agustinazorin.finanzas.feature.capture.domain.CapturedNotificationRepository
import com.agustinazorin.finanzas.feature.capture.domain.DuplicateCandidate
import com.agustinazorin.finanzas.feature.capture.domain.usecase.ConfirmCapturedNotificationUseCase
import com.agustinazorin.finanzas.feature.category.domain.Category
import com.agustinazorin.finanzas.feature.category.domain.CategoryRepository
import com.agustinazorin.finanzas.feature.category.domain.CategoryRuleRepository
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMember
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMemberRepository
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CaptureReviewOptions(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val members: List<HouseholdMember> = emptyList(),
)

@HiltViewModel
class CaptureReviewViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val capturedNotificationRepository: CapturedNotificationRepository,
    private val confirmCapturedNotificationUseCase: ConfirmCapturedNotificationUseCase,
    private val categoryRuleRepository: CategoryRuleRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    householdMemberRepository: HouseholdMemberRepository,
) : HouseholdScopedViewModel(householdRepository) {

    val pendingCaptures: StateFlow<List<CapturedNotification>> =
        capturedNotificationRepository.observePendingReview()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val options: StateFlow<CaptureReviewOptions> = householdId.filterNotNull().flatMapLatest { id ->
        combine(
            accountRepository.observeActiveAccounts(id),
            categoryRepository.observeAll(),
            householdMemberRepository.observeActiveMembers(id),
        ) { accounts, categories, members -> CaptureReviewOptions(accounts, categories, members) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaptureReviewOptions())

    suspend fun suggestCategory(merchant: String?): Long? =
        merchant?.takeIf { it.isNotBlank() }
            ?.let { categoryRuleRepository.suggestCategory(MerchantNormalizer.normalize(it)) }

    /** Candidatas de conciliación antes de confirmar (CLAUDE.md, sección 38): nunca se mergea sola. */
    suspend fun findDuplicates(
        accountId: Long,
        amount: Long,
        currency: String,
        date: LocalDate,
        merchantNormalized: String?,
    ): List<DuplicateCandidate> =
        capturedNotificationRepository.findDuplicateCandidates(accountId, amount, currency, date, merchantNormalized)

    fun confirm(
        captureId: Long,
        accountId: Long,
        ownerMemberId: Long?,
        amount: Long,
        currency: String,
        direction: TransactionDirection,
        date: LocalDate,
        merchant: String?,
        categoryId: Long?,
        note: String?,
    ) {
        viewModelScope.launch {
            val id = householdId.value ?: return@launch
            confirmCapturedNotificationUseCase(
                captureId = captureId,
                householdId = id,
                accountId = accountId,
                ownerMemberId = ownerMemberId,
                amount = amount,
                currency = currency,
                direction = direction,
                date = date,
                merchant = merchant,
                categoryId = categoryId,
                note = note,
            )
        }
    }

    fun discard(captureId: Long) {
        viewModelScope.launch { capturedNotificationRepository.markDiscarded(captureId) }
    }

    fun linkToDuplicate(captureId: Long, existingTransactionId: Long) {
        viewModelScope.launch { capturedNotificationRepository.markDuplicate(captureId, existingTransactionId) }
    }
}
