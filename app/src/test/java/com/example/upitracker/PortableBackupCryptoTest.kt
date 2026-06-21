package com.example.upitracker

import com.example.upitracker.util.PortableBackupCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
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
}
