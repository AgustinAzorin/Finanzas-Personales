package com.agustinazorin.finanzas.feature.security.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.core.backup.restartApp
import java.time.LocalDate

private const val MIN_PASSWORD_LENGTH = 8

private sealed interface SecurityDialog {
    data object None : SecurityDialog
    data object ExportPassword : SecurityDialog
    data class ImportPassword(val source: Uri) : SecurityDialog
}

@Composable
fun SecurityScreen(viewModel: SecurityViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val isLockEnabled by viewModel.isLockEnabled.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    var dialog by remember { mutableStateOf<SecurityDialog>(SecurityDialog.None) }

    // Se guarda acá (y no dentro del diálogo, que ya se cerró) porque el resultado del selector
    // de destino del SAF llega de forma asincrónica, después de que el diálogo de contraseña
    // desapareció.
    var pendingExportPassword by remember { mutableStateOf<CharArray?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val password = pendingExportPassword
        pendingExportPassword = null
        if (uri != null && password != null) {
            viewModel.exportBackup(uri, password)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) dialog = SecurityDialog.ImportPassword(uri)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.security_lock_section_title), style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.security_lock_toggle_label), modifier = Modifier.padding(top = 12.dp))
            Switch(
                checked = isLockEnabled,
                onCheckedChange = { enabled ->
                    val canAuthenticate = BiometricManager.from(context).canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK,
                    ) == BiometricManager.BIOMETRIC_SUCCESS
                    viewModel.setLockEnabled(enabled, canAuthenticate)
                },
            )
        }
        Text(
            stringResource(R.string.security_lock_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        Text(stringResource(R.string.security_backup_section_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.security_backup_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = { dialog = SecurityDialog.ExportPassword }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.security_export_backup))
        }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.security_import_backup))
        }

        if (operationState is SecurityOperationState.InProgress) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
                Text(stringResource(R.string.security_operation_in_progress))
            }
        }
    }

    when (val currentDialog = dialog) {
        SecurityDialog.None -> Unit
        SecurityDialog.ExportPassword -> ExportPasswordDialog(
            onDismiss = { dialog = SecurityDialog.None },
            onConfirm = { password ->
                dialog = SecurityDialog.None
                pendingExportPassword = password.toCharArray()
                exportLauncher.launch(defaultBackupFileName())
            },
        )
        is SecurityDialog.ImportPassword -> ImportPasswordDialog(
            onDismiss = { dialog = SecurityDialog.None },
            onConfirm = { password ->
                dialog = SecurityDialog.None
                viewModel.importBackup(currentDialog.source, password.toCharArray())
            },
        )
    }

    when (val state = operationState) {
        SecurityOperationState.ExportSuccess -> AlertDialog(
            onDismissRequest = viewModel::dismissOperationState,
            title = { Text(stringResource(R.string.security_export_success_title)) },
            text = { Text(stringResource(R.string.security_export_success_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissOperationState) { Text(stringResource(R.string.common_ok)) }
            },
        )
        SecurityOperationState.ImportSuccessRestartRequired -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.security_import_success_title)) },
            text = { Text(stringResource(R.string.security_import_success_message)) },
            confirmButton = {
                TextButton(onClick = { restartApp(context) }) { Text(stringResource(R.string.security_restart_now)) }
            },
        )
        is SecurityOperationState.Error -> AlertDialog(
            onDismissRequest = viewModel::dismissOperationState,
            title = { Text(stringResource(R.string.security_error_title)) },
            text = { Text(stringResource(state.messageRes)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissOperationState) { Text(stringResource(R.string.common_ok)) }
            },
        )
        else -> Unit
    }
}

private fun defaultBackupFileName(): String = "finanzas_backup_${LocalDate.now()}.finanzasbackup"

@Composable
private fun ExportPasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.security_export_backup)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.security_export_password_hint), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.security_password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.security_password_confirm_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        password.length < MIN_PASSWORD_LENGTH -> errorRes = R.string.security_password_too_short
                        password != confirmPassword -> errorRes = R.string.security_password_mismatch
                        else -> onConfirm(password)
                    }
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun ImportPasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.security_import_backup)) },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.security_password_label)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }, enabled = password.isNotEmpty()) {
                Text(stringResource(R.string.security_import_backup))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
