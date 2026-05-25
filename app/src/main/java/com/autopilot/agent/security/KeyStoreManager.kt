package com.autopilot.agent.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.autopilot.agent.util.Constants
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages encryption and decryption of sensitive data (API keys)
 * using Android KeyStore with AES-256-GCM.
 */
@Singleton
class KeyStoreManager @Inject constructor() {

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(Constants.KEYSTORE_PROVIDER).apply { load(null) }
    }

    /**
     * Encrypt a plaintext string using AES-256-GCM.
     * @param plainText The text to encrypt (e.g., API key).
     * @return Pair of (encrypted data as Base64, IV as Base64).
     */
    fun encrypt(plainText: String): Pair<String, String> {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

        return Pair(encryptedBase64, ivBase64)
    }

    /**
     * Decrypt an encrypted string using AES-256-GCM.
     * @param encryptedData The encrypted data as Base64.
     * @param iv The initialization vector as Base64.
     * @return The decrypted plaintext string.
     */
    fun decrypt(encryptedData: String, iv: String): String {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)

        val ivBytes = Base64.decode(iv, Base64.NO_WRAP)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val encryptedBytes = Base64.decode(encryptedData, Base64.NO_WRAP)
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Get existing key from KeyStore or create a new one.
     */
    private fun getOrCreateKey(): SecretKey {
        val existingKey = keyStore.getEntry(Constants.KEYSTORE_ALIAS, null)
        if (existingKey is KeyStore.SecretKeyEntry) {
            return existingKey.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            Constants.KEYSTORE_PROVIDER
        )

        val keySpec = KeyGenParameterSpec.Builder(
            Constants.KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    /**
     * Delete the encryption key from KeyStore.
     * This effectively invalidates all encrypted data.
     */
    fun deleteKey() {
        if (keyStore.containsAlias(Constants.KEYSTORE_ALIAS)) {
            keyStore.deleteEntry(Constants.KEYSTORE_ALIAS)
        }
    }

    /**
     * Check if an encryption key exists in the KeyStore.
     */
    fun hasKey(): Boolean {
        return keyStore.containsAlias(Constants.KEYSTORE_ALIAS)
    }
}
