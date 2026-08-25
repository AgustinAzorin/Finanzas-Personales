package com.agustinazorin.finanzas.feature.capture.ui

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import com.agustinazorin.finanzas.core.preferences.CapturePreferences
import com.agustinazorin.finanzas.core.preferences.KnownCaptureApps
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class CaptureSettingsUiState(
    val isListenerPermissionGranted: Boolean = false,
    val enabledPackages: Set<String> = emptySet(),
)

@HiltViewModel
class CaptureSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val capturePreferences: CapturePreferences,
) : ViewModel() {

    val mercadoPagoPackage: String get() = KnownCaptureApps.MERCADO_PAGO_PACKAGE

    /**
     * El permiso de NotificationListenerService sólo se puede consultar leyendo el estado del
     * sistema (no hay Flow/callback nativo), así que la pantalla lo vuelve a pedir en cada
     * `ON_RESUME` para reflejar el cambio apenas el usuario vuelve de Ajustes.
     */
    fun currentState(): CaptureSettingsUiState = CaptureSettingsUiState(
        isListenerPermissionGranted = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName),
        enabledPackages = capturePreferences.enabledPackages,
    )

    fun setPackageEnabled(packageName: String, enabled: Boolean) {
        capturePreferences.setPackageEnabled(packageName, enabled)
    }
}
