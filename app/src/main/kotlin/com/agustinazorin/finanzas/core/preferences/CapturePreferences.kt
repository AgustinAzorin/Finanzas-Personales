package com.agustinazorin.finanzas.core.preferences

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "capture_prefs"
private const val KEY_ENABLED_PACKAGES = "enabled_packages"

/**
 * Apps cuyas notificaciones el usuario habilitó explícitamente para captura automática
 * (CLAUDE.md, sección 37: "Escuchar aplicaciones configurables. No hardcodear únicamente
 * Mercado Pago."). [FinanzasNotificationListenerService][com.agustinazorin.finanzas.core.capture.FinanzasNotificationListenerService]
 * sólo procesa notificaciones de paquetes en este set, incluso si el sistema operativo le dio
 * acceso a la app entera a leer todas las notificaciones del dispositivo.
 */
@Singleton
class CapturePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var enabledPackages: Set<String>
        get() = prefs.getStringSet(KEY_ENABLED_PACKAGES, emptySet()).orEmpty()
        private set(value) = prefs.edit { putStringSet(KEY_ENABLED_PACKAGES, value) }

    fun setPackageEnabled(packageName: String, enabled: Boolean) {
        enabledPackages = if (enabled) enabledPackages + packageName else enabledPackages - packageName
    }
}

/** Apps con parser específico conocido, ofrecidas como atajo en la pantalla de configuración de captura. */
object KnownCaptureApps {
    const val MERCADO_PAGO_PACKAGE = "com.mercadopago.wallet"
}
