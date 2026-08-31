package com.agustinazorin.finanzas.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Almacén de secretos locales (CLAUDE.md, sección 43): nunca SharedPreferences sin proteger.
 * [EncryptedSharedPreferences] cifra claves y valores con una [MasterKey] respaldada por Android
 * Keystore, así que la clave real nunca sale del hardware ni queda escrita en disco.
 */
@Singleton
class SecurePrefs @Inject constructor(@ApplicationContext private val context: Context) {

    // Esto corre en el arranque de la app (FinanzasApplication -> AppLockManager -> acá), antes de
    // que se dibuje cualquier pantalla. Android Keystore puede dejar la clave maestra en un estado
    // irrecuperable (el sistema la invalida tras un cambio de credenciales del dispositivo, un
    // restore, o corrupción del archivo cifrado) — sin manejar esa falla acá, la app queda en un
    // loop de crash permanente apenas se abre, sin ninguna forma de recuperarse. Ante esa falla se
    // borran el archivo y la clave de Keystore, y se reintenta una única vez con una clave nueva.
    // En una instalación nueva esto no pierde nada; si ya existía una base de datos cifrada con la
    // passphrase vieja, esa base ya era irrecuperable de todos modos (la clave de Keystore que la
    // protegía es justamente la que se perdió).
    private val prefs: SharedPreferences by lazy {
        runCatching { buildEncryptedPrefs() }.getOrElse { error ->
            Log.w(TAG, "No se pudo abrir SecurePrefs, se regenera la clave", error)
            context.deleteSharedPreferences(FILE_NAME)
            runCatching {
                KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(MASTER_KEY_ALIAS)
            }
            buildEncryptedPrefs()
        }
    }

    private fun buildEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** La passphrase de SQLCipher (CLAUDE.md sección 43: clave generada localmente), o null si todavía no se generó. */
    fun getDatabasePassphrase(): ByteArray? =
        prefs.getString(KEY_DB_PASSPHRASE, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    fun setDatabasePassphrase(passphrase: ByteArray) {
        prefs.edit { putString(KEY_DB_PASSPHRASE, Base64.encodeToString(passphrase, Base64.NO_WRAP)) }
    }

    fun isAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_APP_LOCK_ENABLED, enabled) }
    }

    private companion object {
        const val TAG = "SecurePrefs"
        const val FILE_NAME = "finanzas_secure_prefs"
        const val MASTER_KEY_ALIAS = "finanzas_secure_prefs_master_key"
        const val KEY_DB_PASSPHRASE = "db_passphrase"
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    }
}
