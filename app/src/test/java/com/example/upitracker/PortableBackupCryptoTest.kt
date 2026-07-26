package com.example.upitracker

import com.example.upitracker.util.PortableBackupCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PortableBackupCryptoTest {
    @Test fun portableBackupRoundTrips() {
        val source = ByteArray(150_000) { (it % 251).toByte() }
        val encrypted = ByteArrayOutputStream()
        PortableBackupCrypto.encrypt(ByteArrayInputStream(source), encrypted, "correct horse battery".toCharArray())
        val encoded = encrypted.toByteArray()
        assertTrue(PortableBackupCrypto.isPortable(ByteArrayInputStream(encoded).buffered()))

        val restored = ByteArrayOutputStream()
        PortableBackupCrypto.decrypt(ByteArrayInputStream(encoded), restored, "correct horse battery".toCharArray())
        assertArrayEquals(source, restored.toByteArray())
    }

    @Test fun passwordsAreClearedWhenEncryptionFails() {
        val password = "short".toCharArray()
        try {
            PortableBackupCrypto.encrypt(
                ByteArrayInputStream(byteArrayOf(1)),
                ByteArrayOutputStream(),
                password
            )
            fail("Expected the short password to be rejected")
        } catch (_: IllegalArgumentException) {
            assertTrue(password.all { it == '\u0000' })
        }
    }

    @Test fun passwordsAreClearedWhenDecryptionFails() {
        val encrypted = ByteArrayOutputStream()
        PortableBackupCrypto.encrypt(
            ByteArrayInputStream(byteArrayOf(1, 2, 3)),
            encrypted,
            "correct password".toCharArray()
        )
        val password = "incorrect password".toCharArray()
        try {
            PortableBackupCrypto.decrypt(
                ByteArrayInputStream(encrypted.toByteArray()),
                ByteArrayOutputStream(),
                password
            )
            fail("Expected authentication to fail")
        } catch (_: Exception) {
            assertTrue(password.all { it == '\u0000' })
        }
    }
}
