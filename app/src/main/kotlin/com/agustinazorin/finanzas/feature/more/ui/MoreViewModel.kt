package com.agustinazorin.finanzas.feature.more.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.feature.capture.domain.CapturedNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    capturedNotificationRepository: CapturedNotificationRepository,
) : ViewModel() {

    val pendingReviewCount: StateFlow<Int> = capturedNotificationRepository.observePendingReviewCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
