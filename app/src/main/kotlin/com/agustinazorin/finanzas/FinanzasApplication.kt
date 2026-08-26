package com.agustinazorin.finanzas

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.agustinazorin.finanzas.core.security.AppLockManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import net.sqlcipher.database.SQLiteDatabase

@HiltAndroidApp
class FinanzasApplication : Application() {

    @Inject lateinit var appLockManager: AppLockManager

    override fun onCreate() {
        super.onCreate()

        // SQLCipher (CLAUDE.md, sección 43) necesita cargar sus librerías nativas antes de que
        // cualquier código abra `finanzas.db` — incluyendo Room a través del SupportFactory que
        // arma DatabaseModule.
        SQLiteDatabase.loadLibs(this)

        // Observa el ciclo de vida de todo el proceso (no el de una Activity puntual) para saber
        // cuándo la app pasa a segundo plano y re-bloquearla (CLAUDE.md, sección 43).
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLockManager)
    }
}
