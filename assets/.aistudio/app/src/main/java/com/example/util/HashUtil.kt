package com.example.util

import android.util.Base64
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object HashUtil {
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    /**
     * Hashes the password using PBKDF2WithHmacSHA256 with a unique random salt.
     * Returns a string formatted as "saltBase64:hashBase64".
     */
    fun hash(password: String): String {
        val rand = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        rand.nextBytes(salt)
        
        val hashedBytes = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
        
        return "$saltBase64:$hashBase64"
    }

    /**
     * Verifies the password against a stored "saltBase64:hashBase64" hash composite.
     */
    fun verify(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        
        val salt = try {
            Base64.decode(parts[0], Base64.NO_WRAP)
        } catch (e: Exception) {
            return false
        }
        val storedHashBytes = try {
            Base64.decode(parts[1], Base64.NO_WRAP)
        } catch (e: Exception) {
            return false
        }
        
        val testHashBytes = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return testHashBytes.contentEquals(storedHashBytes)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLength)
        return try {
            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            skf.generateSecret(spec).encoded
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Hash algorithm not supported", e)
        } catch (e: InvalidKeySpecException) {
            throw RuntimeException("Invalid key spec", e)
        } finally {
            spec.clearPassword()
        }
    }
}
