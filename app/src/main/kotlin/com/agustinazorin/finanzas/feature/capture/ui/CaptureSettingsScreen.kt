package com.agustinazorin.finanzas.feature.capture.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.agustinazorin.finanzas.R

@Composable
fun CaptureSettingsScreen(viewModel: CaptureSettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var state by remember { mutableStateOf(viewModel.currentState()) }
    var customPackage by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state = viewModel.currentState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun refresh() { state = viewModel.currentState() }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(stringResource(R.string.capture_settings_description), style = MaterialTheme.typography.bodyMedium)
        }

        item {
            Text(
                stringResource(
                    if (state.isListenerPermissionGranted) {
                        R.string.capture_settings_permission_granted
                    } else {
                        R.string.capture_settings_permission_missing
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (!state.isListenerPermissionGranted) {
            item {
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) {
                    Text(stringResource(R.string.capture_settings_open_system_settings))
                }
            }
        }

        item {
            Text(stringResource(R.string.capture_settings_apps_title), style = MaterialTheme.typography.titleMedium)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = viewModel.mercadoPagoPackage in state.enabledPackages,
                    onCheckedChange = {
                        viewModel.setPackageEnabled(viewModel.mercadoPagoPackage, it)
                        refresh()
                    },
                )
                Text(stringResource(R.string.capture_settings_mercado_pago))
            }
        }

        val customPackages = state.enabledPackages.filter { it != viewModel.mercadoPagoPackage }.sorted()
        items(customPackages, key = { it }) { packageName ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(packageName, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { viewModel.setPackageEnabled(packageName, false); refresh() }) {
                    Text(stringResource(R.string.common_delete))
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customPackage,
                    onValueChange = { customPackage = it },
                    label = { Text(stringResource(R.string.capture_settings_custom_package_hint)) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                Button(
                    enabled = customPackage.isNotBlank(),
                    onClick = {
                        viewModel.setPackageEnabled(customPackage.trim(), true)
                        customPackage = ""
                        refresh()
                    },
                ) { Text(stringResource(R.string.common_add)) }
            }
        }
    }
}
