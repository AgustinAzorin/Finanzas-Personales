package com.agustinazorin.finanzas.core.security.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.agustinazorin.finanzas.R

/**
 * Envuelve el contenido principal de la app con una pantalla de bloqueo (CLAUDE.md, sección 43)
 * cuando el bloqueo automático está habilitado y la app volvió de segundo plano. No renderiza el
 * contenido protegido mientras está bloqueada: no alcanza con superponer, porque una captura de
 * pantalla o el selector de apps recientes podría seguir mostrándolo (por eso además se usa
 * `FLAG_SECURE` en MainActivity).
 */
@Composable
fun AppLockGate(content: @Composable () -> Unit) {
    val viewModel: AppLockViewModel = hiltViewModel()
    val isLocked by viewModel.isLocked.collectAsState()

    if (isLocked) {
        LockScreen(onUnlock = viewModel::unlock)
    } else {
        content()
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val onUnlockState = rememberUpdatedState(onUnlock)

    fun launchPrompt() {
        val currentActivity = activity ?: return
        errorMessage = null
        val executor = ContextCompat.getMainExecutor(currentActivity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onUnlockState.value()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                errorMessage = errString.toString()
            }
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(currentActivity.getString(R.string.security_lock_prompt_title))
            .setNegativeButtonText(currentActivity.getString(R.string.common_cancel))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK,
            )
            .build()
        BiometricPrompt(currentActivity, executor, callback).authenticate(promptInfo)
    }

    LaunchedEffect(Unit) { launchPrompt() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.security_lock_screen_title), style = MaterialTheme.typography.titleLarge)
            errorMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { launchPrompt() }) {
                Text(stringResource(R.string.security_unlock_button))
            }
        }
    }
}
