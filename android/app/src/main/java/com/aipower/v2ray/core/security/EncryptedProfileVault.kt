package com.aipower.v2ray.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.aipower.v2ray.core.vpn.ImportedXrayProfile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

/**
 * Single-profile encrypted vault backed by an Android Keystore AES-256 key.
 * Ciphertext is stored in noBackupFilesDir and is never synchronized to cloud.
 */
class EncryptedProfileVault(context: Context) {
    private val vaultFile = File(context.noBackupFilesDir, VAULT_FILE)
    private val backupFile = File(context.noBackupFilesDir, BACKUP_FILE)
    private val alias = KEY_ALIAS

    fun save(profile: ImportedXrayProfile) = synchronized(PROCESS_LOCK) {
        recoverInterruptedReplacement()
        require(profile.configJson.isNotBlank()) { "Cannot save an empty Xray configuration." }
        require(profile.configJson.length <= MAX_CONFIG_CHARS) {
            "Xray configuration is too large."
        }
        require(profile.displayName.length <= MAX_DISPLAY_NAME_CHARS) {
            "Profile name is too large."
        }

        val payload = JSONObject()
            .put("version", FORMAT_VERSION)
            .put("displayName", profile.displayName)
            .put("outboundCount", profile.outboundCount)
            .put("configJson", profile.configJson)
            .toString()
            .toByteArray(Charsets.UTF_8)
        require(payload.size <= MAX_PLAINTEXT_BYTES) {
            "Encrypted profile payload is too large."
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(AAD)
        val encrypted = cipher.doFinal(payload)
        val iv = cipher.iv
        require(iv.size == GCM_IV_BYTES) { "Unexpected AES-GCM nonce size." }

        val parent = vaultFile.parentFile ?: error("Vault directory is unavailable.")
        check(parent.isDirectory || parent.mkdirs()) { "Could not create the vault directory." }
        val temp = File.createTempFile("$VAULT_FILE.tmp-", ".new", parent)
        try {
            FileOutputStream(temp).use { output ->
                output.write(iv)
                output.write(encrypted)
                output.fd.sync()
            }
            setOwnerOnlyPermissions(temp)
            FileOutputStream(temp, true).use { it.fd.sync() }

            // Keep the old authenticated ciphertext until the new temp file is ready.
            if (vaultFile.exists()) {
                if (backupFile.exists() && !backupFile.delete()) {
                    error("Could not clear the previous vault recovery file.")
                }
                moveWithFallback(vaultFile, backupFile)
                syncParentDirectory(parent)
            }

            try {
                moveWithFallback(temp, vaultFile)
                syncParentDirectory(parent)
            } catch (error: Exception) {
                if (!vaultFile.exists() && backupFile.exists()) {
                    runCatching { moveWithFallback(backupFile, vaultFile) }
                    syncParentDirectory(parent)
                }
                throw error
            }

            if (backupFile.exists() && !backupFile.delete()) {
                // The new vault is already committed; a later recovery pass removes
                // this stale backup after verifying that the primary file exists.
            }
            syncParentDirectory(parent)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    fun load(): ImportedXrayProfile? = synchronized(PROCESS_LOCK) {
        recoverInterruptedReplacement()
        if (!vaultFile.isFile) return@synchronized null

        val bytes = readVaultBounded(vaultFile, MAX_VAULT_BYTES)
        require(bytes.size.toLong() in MIN_VAULT_BYTES..MAX_VAULT_BYTES) {
            "Encrypted profile vault has an invalid size."
        }

        val iv = bytes.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = bytes.copyOfRange(GCM_IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val key = getExistingKey()
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        } catch (error: KeyPermanentlyInvalidatedException) {
            throw VaultKeyUnavailableException("Encrypted profile key was invalidated.", error)
        } catch (error: UnrecoverableKeyException) {
            throw VaultKeyUnavailableException("Encrypted profile key cannot be recovered.", error)
        } catch (error: InvalidKeyException) {
            throw VaultKeyUnavailableException("Encrypted profile key is unusable.", error)
        }

        cipher.updateAAD(AAD)
        val plaintext = try {
            cipher.doFinal(ciphertext)
        } catch (error: AEADBadTagException) {
            throw error
        }
        require(plaintext.size <= MAX_PLAINTEXT_BYTES) {
            "Decrypted profile payload is too large."
        }
        val payload = JSONObject(String(plaintext, Charsets.UTF_8))
        require(payload.optInt("version") == FORMAT_VERSION) {
            "Unsupported profile vault format."
        }

        val config = payload.getString("configJson")
        require(config.isNotBlank()) { "Saved profile is empty." }
        require(config.length <= MAX_CONFIG_CHARS) { "Saved profile is too large." }
        val displayName = payload.optString("displayName", "Saved profile")
        require(displayName.length <= MAX_DISPLAY_NAME_CHARS) {
            "Saved profile name is too large."
        }
        ImportedXrayProfile(
            configJson = config,
            displayName = displayName,
            outboundCount = payload.optInt("outboundCount", 1).coerceAtLeast(1)
        )
    }

    fun delete() = synchronized(PROCESS_LOCK) {
        recoverInterruptedReplacement()
        if (vaultFile.exists() && !vaultFile.delete()) {
            error("Could not delete the encrypted profile vault.")
        }
        if (backupFile.exists() && !backupFile.delete()) {
            error("Could not delete the encrypted profile recovery file.")
        }
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    private fun recoverInterruptedReplacement() {
        val parent = vaultFile.parentFile ?: return
        if (!vaultFile.exists() && backupFile.exists()) {
            moveWithFallback(backupFile, vaultFile)
            syncParentDirectory(parent)
        } else if (vaultFile.exists() && backupFile.exists()) {
            // Primary is the commit point. The backup is stale after a completed swap.
            if (!backupFile.delete()) {
                error("Could not remove stale encrypted profile recovery file.")
            }
        }
        parent.listFiles()
            ?.filter { it.name.startsWith("$VAULT_FILE.tmp-") }
            ?.forEach { runCatching { it.delete() } }
    }

    private fun getExistingKey(): SecretKey {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            return (keyStore.getKey(alias, null) as? SecretKey)
                ?: throw VaultKeyUnavailableException("Encrypted profile key is missing.")
        } catch (error: VaultKeyUnavailableException) {
            throw error
        } catch (error: KeyPermanentlyInvalidatedException) {
            throw VaultKeyUnavailableException("Encrypted profile key was invalidated.", error)
        } catch (error: UnrecoverableKeyException) {
            throw VaultKeyUnavailableException("Encrypted profile key cannot be recovered.", error)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun setOwnerOnlyPermissions(file: File) {
        check(file.setReadable(false, false) && file.setReadable(true, true)) {
            "Could not restrict encrypted vault file read permissions."
        }
        check(file.setWritable(false, false) && file.setWritable(true, true)) {
            "Could not restrict encrypted vault file write permissions."
        }
    }

    private fun moveWithFallback(source: File, destination: File) {
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun syncParentDirectory(directory: File) {
        // Directory fsync is not supported by every Android filesystem/provider.
        runCatching {
            FileChannel.open(directory.toPath(), StandardOpenOption.READ).use { it.force(true) }
        }
    }

    companion object {
        private val PROCESS_LOCK = Any()
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "ai_power_v2ray_profile_v1"
        private const val VAULT_FILE = "profiles.vault"
        private const val BACKUP_FILE = "profiles.vault.bak"
        private const val FORMAT_VERSION = 1
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        private const val MAX_CONFIG_CHARS = 1_000_000
        private const val MAX_DISPLAY_NAME_CHARS = 256
        private const val MAX_PLAINTEXT_BYTES = 2_000_000
        private const val MIN_VAULT_BYTES = GCM_IV_BYTES + GCM_TAG_BYTES + 1L
        private const val MAX_VAULT_BYTES = 2L * 1024L * 1024L
        private val AAD = "com.aipower.v2ray.profile.v1".toByteArray(Charsets.UTF_8)
    }
}

/** Reads at most maxBytes + 1 bytes, so a file that grows after stat cannot bypass the cap. */
internal fun readVaultBounded(file: File, maxBytes: Long): ByteArray {
    require(maxBytes >= 0) { "Maximum vault size must not be negative." }
    require(file.length() <= maxBytes) {
        "Encrypted profile vault exceeds the permitted size."
    }

    FileInputStream(file).use { input ->
        readVaultStreamBounded(input, maxBytes)
    }
}

internal fun readVaultStreamBounded(input: java.io.InputStream, maxBytes: Long): ByteArray {
    require(maxBytes >= 0 && maxBytes < Int.MAX_VALUE) {
        "Maximum vault size is outside the supported range."
    }
    val output = ByteArrayOutputStream(minOf(maxBytes, 8L * 1024L).toInt())
    val buffer = ByteArray(8 * 1024)
    var total = 0L
    while (true) {
        val remainingIncludingProbe = maxBytes + 1L - total
        if (remainingIncludingProbe <= 0L) {
            error("Encrypted profile vault exceeds the permitted size.")
        }
        val requested = minOf(buffer.size.toLong(), remainingIncludingProbe).toInt()
        val count = input.read(buffer, 0, requested)
        if (count == -1) break
        if (count == 0) continue
        total += count
        require(total <= maxBytes) {
            "Encrypted profile vault exceeds the permitted size."
        }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

class VaultKeyUnavailableException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
