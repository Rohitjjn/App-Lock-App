package com.example.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class BiometricHelper() {

    companion object {
        private const val KEY_NAME = "applock_biometric_key"
        private const val KEYSTORE_NAME = "AndroidKeyStore"
    }

    init {
        try {
            generateSecretKey()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateSecretKey() {
        val keyStore = KeyStore.getInstance(KEYSTORE_NAME).apply { load(null) }
        
        if (keyStore.containsAlias(KEY_NAME)) return

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_NAME
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .build()

        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    private fun getCipher(): Cipher {
        val cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}")
        val keyStore = KeyStore.getInstance(KEYSTORE_NAME).apply { load(null) }
        val secretKey = keyStore.getKey(KEY_NAME, null) as SecretKey
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    fun getCryptoObject(): BiometricPrompt.CryptoObject? {
        return try {
            BiometricPrompt.CryptoObject(getCipher())
        } catch (e: Exception) {
            null
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        executor: Executor,
        title: String = "App Lock Auth",
        subtitle: String = "Authenticate to proceed",
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (Int, CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        // Bypass CryptoObject to prevent hardware Keystore thread-blocking and UI stutters
        biometricPrompt.authenticate(promptInfo)
    }
}
