package dev.paarudev.prana.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore Security Manager.
 *
 * Backed by Snapdragon hardware security / TEE.
 * Generates and securely isolates master encryption keys for SQLCipher and local persistence.
 */
class KeystoreManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "PranaMasterKey_AES256"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val PREFS_NAME = "prana_secure_vault"
        private const val ENCRYPTED_DB_KEY = "encrypted_db_passphrase"
        private const val IV_KEY = "db_passphrase_iv"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    init {
        ensureMasterKey()
    }

    private fun ensureMasterKey() {
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getMasterKey(): SecretKey {
        return keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
    }

    /**
     * Retrieves or generates a 256-bit passphrase for SQLCipher,
     * encrypted at rest by the Keystore master key.
     */
    fun getOrCreateDatabasePassphrase(): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedHex = prefs.getString(ENCRYPTED_DB_KEY, null)
        val ivHex = prefs.getString(IV_KEY, null)

        if (encryptedHex != null && ivHex != null) {
            try {
                val iv = hexToBytes(ivHex)
                val encrypted = hexToBytes(encryptedHex)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
                return cipher.doFinal(encrypted)
            } catch (e: Exception) {
                // If decryption fails, regenerate
            }
        }

        // Generate fresh 32-byte (256-bit) cryptographically strong random passphrase
        val freshKey = ByteArray(32)
        SecureRandom().nextBytes(freshKey)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(freshKey)

        prefs.edit()
            .putString(ENCRYPTED_DB_KEY, bytesToHex(encrypted))
            .putString(IV_KEY, bytesToHex(iv))
            .apply()

        return freshKey
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in 0 until hex.length step 2) {
            result[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
        }
        return result
    }
}
