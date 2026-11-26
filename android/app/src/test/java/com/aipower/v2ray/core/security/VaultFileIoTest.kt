package com.aipower.v2ray.core.security

import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VaultFileIoTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun readsFileWithinLimit() {
        val expected = ByteArray(32) { it.toByte() }
        val file = temporaryFolder.newFile("vault.bin").apply { writeBytes(expected) }

        assertArrayEquals(expected, readVaultBounded(file, 32L))
    }

    @Test
    fun rejectsFileLargerThanLimitBeforeReading() {
        val file = temporaryFolder.newFile("large.bin").apply { writeBytes(ByteArray(33)) }

        assertThrows(IllegalArgumentException::class.java) {
            readVaultBounded(file, 32L)
        }
    }

    @Test
    fun rejectsStreamThatExceedsLimitAfterInitialFileCheck() {
        val stream = ByteArrayInputStream(ByteArray(33))
        assertThrows(IllegalStateException::class.java) {
            readVaultStreamBounded(stream, 32L)
        }
    }
}
