package com.agustinazorin.finanzas.core.security.ui

import androidx.lifecycle.ViewModel
import com.agustinazorin.finanzas.core.security.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
) : ViewModel() {

    val isLocked: StateFlow<Boolean> = appLockManager.isLocked

    fun unlock() = appLockManager.unlock()
}
