package com.agustinazorin.finanzas.core.security

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Genera (una única vez) y recupera la passphrase con la que SQLCipher cifra `finanzas.db`
 * (CLAUDE.md, sección 43). Se genera localmente con [SecureRandom] la primera vez que se necesita
 * y se guarda cifrada en [SecurePrefs] — nunca hardcodeada, nunca en texto plano.
 */
@Singleton
class DatabasePassphraseProvider @Inject constructor(private val securePrefs: SecurePrefs) {

    @Synchronized
    fun getOrCreatePassphrase(): ByteArray {
        securePrefs.getDatabasePassphrase()?.let { return it }
        val generated = ByteArray(PASSPHRASE_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        securePrefs.setDatabasePassphrase(generated)
        return generated
    }

    private companion object {
        const val PASSPHRASE_LENGTH_BYTES = 32
    }
}
