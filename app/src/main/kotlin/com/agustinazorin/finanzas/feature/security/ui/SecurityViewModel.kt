package com.agustinazorin.finanzas.feature.security.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.core.backup.BackupManager
import com.agustinazorin.finanzas.core.security.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SecurityOperationState {
    data object Idle : SecurityOperationState
    data object InProgress : SecurityOperationState
    data object ExportSuccess : SecurityOperationState

    /** El import ya terminó y la base vieja quedó cerrada: hay que reiniciar la app para seguir. */
    data object ImportSuccessRestartRequired : SecurityOperationState
    data class Error(@StringRes val messageRes: Int) : SecurityOperationState
}

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
    private val backupManager: BackupManager,
) : ViewModel() {

    val isLockEnabled: StateFlow<Boolean> = appLockManager.isLockEnabled

    private val _operationState = MutableStateFlow<SecurityOperationState>(SecurityOperationState.Idle)
    val operationState: StateFlow<SecurityOperationState> = _operationState.asStateFlow()

    fun setLockEnabled(enabled: Boolean, canAuthenticate: Boolean) {
        if (enabled && !canAuthenticate) {
            _operationState.value = SecurityOperationState.Error(R.string.security_lock_unavailable)
            return
        }
        appLockManager.setLockEnabled(enabled)
    }

    fun exportBackup(destination: Uri, password: CharArray) {
        viewModelScope.launch {
            _operationState.value = SecurityOperationState.InProgress
            val result = backupManager.export(destination, password)
            _operationState.value = result.fold(
                onSuccess = { SecurityOperationState.ExportSuccess },
                onFailure = { SecurityOperationState.Error(R.string.security_export_error) },
            )
        }
    }

    fun importBackup(source: Uri, password: CharArray) {
        viewModelScope.launch {
            _operationState.value = SecurityOperationState.InProgress
            val result = backupManager.import(source, password)
            _operationState.value = result.fold(
                onSuccess = { SecurityOperationState.ImportSuccessRestartRequired },
                onFailure = { SecurityOperationState.Error(R.string.security_import_error) },
            )
        }
    }

    fun dismissOperationState() {
        _operationState.value = SecurityOperationState.Idle
    }
}
