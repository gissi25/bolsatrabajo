package sv.ues.fia.eisi.bt.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 65536
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    fun hash(password: String): String {
        val salt = generateSalt()
        val hash = pbkdf2(password.toCharArray(), salt)
        return bytesToHex(salt) + ":" + bytesToHex(hash)
    }

    fun verify(password: String, storedHash: String): Boolean {
        return try {
            val parts = storedHash.split(":")
            if (parts.size != 2) return false

            val salt = hexToBytes(parts[0])
            val expectedHash = hexToBytes(parts[1])
            val actualHash = pbkdf2(password.toCharArray(), salt)

            MessageDigest.isEqual(expectedHash, actualHash)
        } catch (e: Exception) {
            false
        }
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray): ByteArray {
        return try {
            val spec: KeySpec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            factory.generateSecret(spec).encoded
        } catch (e: Exception) {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(salt)
            md.digest(password.joinToString("").toByteArray())
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}