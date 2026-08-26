package com.agustinazorin.finanzas.core.backup

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cifra el archivo de backup con una contraseña que elige el usuario (CLAUDE.md, sección 44:
 * tiene que poder "recuperar el estado completo de la aplicación en otro dispositivo"). A
 * propósito NO se usa la clave de Android Keystore acá: esa clave está atada al hardware del
 * dispositivo que hizo el backup y jamás sale de ahí, así que un backup cifrado con ella sería
 * indescifrable en cualquier otro dispositivo.
 *
 * Formato del archivo: `[salt: 16 bytes][iv: 12 bytes][ciphertext + tag de GCM]`. Salt e IV no
 * son secretos, sólo tienen que ser aleatorios por backup.
 */
internal object PasswordEncryption {
    private const val SALT_SIZE_BYTES = 16
    private const val IV_SIZE_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256

    fun encrypt(plainBytes: ByteArray, password: CharArray): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_SIZE_BYTES).also { random.nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plainBytes)
        return salt + iv + ciphertext
    }

    /** Lanza [GeneralSecurityException] si la contraseña es incorrecta o el archivo está corrupto (falla la verificación del tag de GCM). */
    fun decrypt(encryptedBytes: ByteArray, password: CharArray): ByteArray {
        require(encryptedBytes.size > SALT_SIZE_BYTES + IV_SIZE_BYTES) { "Archivo de backup inválido." }
        val salt = encryptedBytes.copyOfRange(0, SALT_SIZE_BYTES)
        val iv = encryptedBytes.copyOfRange(SALT_SIZE_BYTES, SALT_SIZE_BYTES + IV_SIZE_BYTES)
        val ciphertext = encryptedBytes.copyOfRange(SALT_SIZE_BYTES + IV_SIZE_BYTES, encryptedBytes.size)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
}
