package com.example.upitracker.util

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object PortableBackupCrypto {
    private val magic = "UPIPORT1".toByteArray(Charsets.US_ASCII)
    private const val iterations = 210_000
    private const val saltSize = 16
    private const val ivSize = 12

    fun isPortable(input: InputStream): Boolean {
        if (!input.markSupported()) return false
        input.mark(magic.size + 1)
        val header = ByteArray(magic.size)
        val read = input.read(header)
        input.reset()
        return read == magic.size && header.contentEquals(magic)
    }

    fun encrypt(source: InputStream, output: OutputStream, password: CharArray) {
        require(password.size >= 8) { "Backup password must contain at least 8 characters" }
        val random = SecureRandom()
        val salt = ByteArray(saltSize).also(random::nextBytes)
        val iv = ByteArray(ivSize).also(random::nextBytes)
        val key = deriveKey(password, salt, iterations)
        val data = DataOutputStream(output.buffered())
        data.write(magic)
        data.writeInt(iterations)
        data.write(salt)
        data.write(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            updateAAD(magic)
        }
        CipherOutputStream(data, cipher).use { encrypted -> source.copyTo(encrypted, 64 * 1024) }
        password.fill('\u0000')
    }

    fun decrypt(input: InputStream, output: OutputStream, password: CharArray) {
        val data = DataInputStream(input.buffered())
        val header = ByteArray(magic.size).also(data::readFully)
        require(header.contentEquals(magic)) { "Not a portable backup" }
        val rounds = data.readInt()
        require(rounds in 100_000..1_000_000) { "Invalid backup key settings" }
        val salt = ByteArray(saltSize).also(data::readFully)
        val iv = ByteArray(ivSize).also(data::readFully)
        val key = deriveKey(password, salt, rounds)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            updateAAD(magic)
        }
        CipherInputStream(data, cipher).use { decrypted -> decrypted.copyTo(output, 64 * 1024) }
        password.fill('\u0000')
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, rounds: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, rounds, 256)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
