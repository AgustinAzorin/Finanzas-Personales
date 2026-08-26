package com.agustinazorin.finanzas.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bloqueo automático de la app (CLAUDE.md, sección 43). Se registra como observer del ciclo de
 * vida del proceso completo ([androidx.lifecycle.ProcessLifecycleOwner], en [FinanzasApplication])
 * para detectar cuando la app pasa a segundo plano — no la de una Activity puntual, porque
 * rotaciones o cambios de configuración no deben re-bloquear la app.
 *
 * Cualquier paso a segundo plano re-bloquea si el bloqueo está habilitado (incluyendo, por
 * ejemplo, abrir el selector de archivos del sistema para exportar un backup). Es una decisión
 * deliberada: preferimos el comportamiento más conservador antes que una lista de excepciones que
 * podría debilitar la protección por error.
 */
@Singleton
class AppLockManager @Inject constructor(
    private val securePrefs: SecurePrefs,
) : DefaultLifecycleObserver {

    private val _isLockEnabled = MutableStateFlow(securePrefs.isAppLockEnabled())
    val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    private val _isLocked = MutableStateFlow(securePrefs.isAppLockEnabled())
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    fun setLockEnabled(enabled: Boolean) {
        securePrefs.setAppLockEnabled(enabled)
        _isLockEnabled.value = enabled
        if (!enabled) _isLocked.value = false
    }

    fun unlock() {
        _isLocked.value = false
    }

    override fun onStop(owner: LifecycleOwner) {
        if (_isLockEnabled.value) _isLocked.value = true
    }
}
