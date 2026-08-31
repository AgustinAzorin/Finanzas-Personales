package com.agustinazorin.finanzas

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.agustinazorin.finanzas.core.diagnostics.CrashDiagnostics
import com.agustinazorin.finanzas.core.security.AppLockManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FinanzasApplication : Application() {

    @Inject lateinit var appLockManager: AppLockManager

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // El punto más temprano posible del arranque del proceso: corre antes que la inyección de
        // campos de Hilt (que pasa al principio de onCreate), así que captura incluso una
        // excepción durante esa inyección.
        CrashDiagnostics.install(this)
    }

    override fun onCreate() {
        // `super.onCreate()` es lo que dispara la inyección de campos de Hilt (`appLockManager`),
        // que a su vez toca Android Keystore a través de SecurePrefs (CLAUDE.md, sección 43). Cada
        // paso del arranque se envuelve individualmente para poder saber, ante un crash, cuál fue
        // el último paso que se completó — sin esto, un crash acá no deja ningún rastro accesible
        // sin `adb logcat` (ver CrashDiagnostics).
        try {
            super.onCreate()
            CrashDiagnostics.recordStep(this, "Application.onCreate: inyección de Hilt completa")
        } catch (error: Throwable) {
            CrashDiagnostics.recordCaught(this, "Application.onCreate: inyección de Hilt", error)
            throw error
        }

        try {
            // SQLCipher (CLAUDE.md, sección 43) necesita cargar su librería nativa antes de que
            // cualquier código abra `finanzas.db` — incluyendo Room a través del
            // SupportOpenHelperFactory que arma DatabaseModule. `net.zetetic:sqlcipher-android`
            // (no confundir con el `net.zetetic:android-database-sqlcipher` original, discontinuado
            // en 2023 y sin soporte para tamaño de página de 16 KB) no expone un `loadLibs(context)`
            // propio: la carga es la estándar de JNI.
            System.loadLibrary("sqlcipher")
            CrashDiagnostics.recordStep(this, "Application.onCreate: SQLCipher.loadLibrary")
        } catch (error: Throwable) {
            CrashDiagnostics.recordCaught(this, "Application.onCreate: SQLCipher.loadLibrary", error)
            throw error
        }

        try {
            // Observa el ciclo de vida de todo el proceso (no el de una Activity puntual) para saber
            // cuándo la app pasa a segundo plano y re-bloquearla (CLAUDE.md, sección 43).
            ProcessLifecycleOwner.get().lifecycle.addObserver(appLockManager)
            CrashDiagnostics.recordStep(this, "Application.onCreate: observer de bloqueo agregado")
        } catch (error: Throwable) {
            CrashDiagnostics.recordCaught(this, "Application.onCreate: observer de bloqueo", error)
            throw error
        }
    }
}
