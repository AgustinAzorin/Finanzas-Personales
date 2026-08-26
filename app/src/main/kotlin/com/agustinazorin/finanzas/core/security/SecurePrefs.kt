package com.agustinazorin.finanzas.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Almacén de secretos locales (CLAUDE.md, sección 43): nunca SharedPreferences sin proteger.
 * [EncryptedSharedPreferences] cifra claves y valores con una [MasterKey] respaldada por Android
 * Keystore, así que la clave real nunca sale del hardware ni queda escrita en disco.
 */
@Singleton
class SecurePrefs @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
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
        const val FILE_NAME = "finanzas_secure_prefs"
        const val KEY_DB_PASSPHRASE = "db_passphrase"
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    }
}
