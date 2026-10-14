package com.example.util

import java.util.Base64
import java.security.MessageDigest
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
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().encodeToString(hashedBytes)
        
        return "$saltBase64:$hashBase64"
    }

    /**
     * Verifies the password against a stored "saltBase64:hashBase64" hash composite.
     */
    fun verify(password: String, storedHash: String?): Boolean {
        // Missing or damaged credentials must never authorize an unlock.
        if (storedHash == null || storedHash.length > 128) return false
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        
        val salt = try {
            Base64.getDecoder().decode(parts[0])
        } catch (e: Exception) {
            return false
        }
        val storedHashBytes = try {
            Base64.getDecoder().decode(parts[1])
        } catch (e: Exception) {
            return false
        }
        
        if (salt.size != SALT_LENGTH || storedHashBytes.size != KEY_LENGTH / 8) return false

        val testHashBytes = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return MessageDigest.isEqual(testHashBytes, storedHashBytes)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = try {
            PBEKeySpec(password, salt, iterations, keyLength)
        } finally {
            password.fill('\u0000')
        }
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
