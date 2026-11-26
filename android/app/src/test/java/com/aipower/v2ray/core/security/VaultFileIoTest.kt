package com.aipower.v2ray.core.security

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
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
        assertThrows(IllegalArgumentException::class.java) {
            readVaultStreamBounded(stream, 32L)
        }
    }
    @Test
    fun restoresBackupWhenPrimaryIsAnIncompleteNonAtomicReplacement() {
        val primary = temporaryFolder.newFile("profiles.vault").apply {
            writeText("partial-new-ciphertext")
        }
        val backup = temporaryFolder.newFile("profiles.vault.bak").apply {
            writeText("last-known-good-ciphertext")
        }

        recoverVaultReplacement(
            primary,
            backup,
            isPrimaryValid = { it.readText() == "authenticated-new-ciphertext" },
            moveFile = ::moveForTest,
            syncDirectory = {}
        )

        assertEquals("last-known-good-ciphertext", primary.readText())
        assertFalse(backup.exists())
    }

    @Test
    fun keepsAuthenticatedPrimaryAndRemovesStaleBackup() {
        val primary = temporaryFolder.newFile("profiles.vault").apply {
            writeText("authenticated-new-ciphertext")
        }
        val backup = temporaryFolder.newFile("profiles.vault.bak").apply {
            writeText("old-ciphertext")
        }

        recoverVaultReplacement(
            primary,
            backup,
            isPrimaryValid = { it.readText() == "authenticated-new-ciphertext" },
            moveFile = ::moveForTest,
            syncDirectory = {}
        )

        assertEquals("authenticated-new-ciphertext", primary.readText())
        assertFalse(backup.exists())
    }

    @Test
    fun restoresBackupWhenPrimaryIsMissing() {
        val primary = File(temporaryFolder.root, "profiles.vault")
        val backup = temporaryFolder.newFile("profiles.vault.bak").apply {
            writeText("last-known-good-ciphertext")
        }

        recoverVaultReplacement(
            primary,
            backup,
            isPrimaryValid = { error("Validator must not run without a primary") },
            moveFile = ::moveForTest,
            syncDirectory = {}
        )

        assertEquals("last-known-good-ciphertext", primary.readText())
        assertFalse(backup.exists())
    }

    @Test
    fun preservesBothFilesWhenKeystoreValidationIsUnavailable() {
        val primary = temporaryFolder.newFile("profiles.vault").apply {
            writeText("new-ciphertext")
        }
        val backup = temporaryFolder.newFile("profiles.vault.bak").apply {
            writeText("old-ciphertext")
        }

        assertThrows(VaultKeyUnavailableException::class.java) {
            recoverVaultReplacement(
                primary,
                backup,
                isPrimaryValid = { throw VaultKeyUnavailableException("Keystore unavailable") },
                moveFile = ::moveForTest,
                syncDirectory = {}
            )
        }

        assertTrue(primary.exists())
        assertTrue(backup.exists())
        assertEquals("new-ciphertext", primary.readText())
        assertEquals("old-ciphertext", backup.readText())
    }

    @Test
    fun keepsBackupWhenRecoveryMoveFails() {
        val primary = File(temporaryFolder.root, "profiles.vault")
        val backup = temporaryFolder.newFile("profiles.vault.bak").apply {
            writeText("last-known-good-ciphertext")
        }

        assertThrows(java.io.IOException::class.java) {
            recoverVaultReplacement(
                primary,
                backup,
                isPrimaryValid = { true },
                moveFile = { _, _ -> throw java.io.IOException("simulated move failure") },
                syncDirectory = {}
            )
        }

        assertFalse(primary.exists())
        assertTrue(backup.exists())
        assertEquals("last-known-good-ciphertext", backup.readText())
    }


    @Test
    fun restoresBackupAfterCrashBetweenPrimaryRemovalAndBackupRestore() {
        val primary = temporaryFolder.newFile("profiles.vault").apply { writeText("corrupt-primary") }
        val backup = temporaryFolder.newFile("profiles.vault.bak").apply { writeText("authenticated-backup") }

        // Simulate process death after recovery has removed an unauthenticated primary.
        assertTrue(primary.delete())
        recoverVaultReplacement(
            primary, backup,
            isPrimaryValid = { error("No primary should be validated") },
            moveFile = ::moveForTest,
            syncDirectory = {}
        )
        assertEquals("authenticated-backup", primary.readText())
        assertFalse(backup.exists())
    }

    @Test
    fun retriesRecoveryAfterDirectorySyncFailureWithoutLosingCommittedPrimary() {
        val primary = File(temporaryFolder.root, "profiles.vault")
        val backup = temporaryFolder.newFile("profiles.vault.bak").apply { writeText("authenticated-backup") }

        assertThrows(java.io.IOException::class.java) {
            recoverVaultReplacement(
                primary, backup,
                isPrimaryValid = { true },
                moveFile = ::moveForTest,
                syncDirectory = { throw java.io.IOException("simulated process interruption at directory sync") }
            )
        }
        assertEquals("authenticated-backup", primary.readText())
        assertFalse(backup.exists())

        recoverVaultReplacement(
            primary, backup,
            isPrimaryValid = { it.readText() == "authenticated-backup" },
            moveFile = ::moveForTest,
            syncDirectory = {}
        )
        assertEquals("authenticated-backup", primary.readText())
    }

    @Test
    fun corruptPrimaryCiphertextFallsBackToLastAuthenticatedBackup() {
        val primary = temporaryFolder.newFile("profiles.vault").apply { writeBytes(ByteArray(64) { 0x55 }) }
        val backup = temporaryFolder.newFile("profiles.vault.bak").apply { writeText("known-good-authenticated-ciphertext") }

        recoverVaultReplacement(
            primary, backup,
            isPrimaryValid = { it.readText() == "known-good-authenticated-ciphertext" },
            moveFile = ::moveForTest,
            syncDirectory = {}
        )
        assertEquals("known-good-authenticated-ciphertext", primary.readText())
        assertFalse(backup.exists())
    }

    private fun moveForTest(source: File, destination: File) {
        Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

}
