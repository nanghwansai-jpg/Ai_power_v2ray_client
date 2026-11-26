package com.aipower.v2ray.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.aipower.v2ray.core.vpn.ImportedXrayProfile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
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
    private val alias = KEY_ALIAS

    @Synchronized
    fun save(profile: ImportedXrayProfile) {
        require(profile.configJson.isNotBlank()) { "Cannot save an empty Xray configuration." }
        val payload = JSONObject()
            .put("version", FORMAT_VERSION)
            .put("displayName", profile.displayName)
            .put("outboundCount", profile.outboundCount)
            .put("configJson", profile.configJson)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(payload)
        val iv = cipher.iv
        require(iv.size == GCM_IV_BYTES) { "Unexpected AES-GCM nonce size." }

        val temp = File(vaultFile.parentFile, "$VAULT_FILE.tmp")
        FileOutputStream(temp).use { output ->
            output.write(iv)
            output.write(encrypted)
            output.fd.sync()
        }
        try {
            Files.move(
                temp.toPath(),
                vaultFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    @Synchronized
    fun load(): ImportedXrayProfile? {
        if (!vaultFile.isFile) return null
        val bytes = FileInputStream(vaultFile).use { it.readBytes() }
        require(bytes.size > GCM_IV_BYTES + GCM_TAG_BYTES) { "Encrypted profile vault is damaged." }

        val iv = bytes.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = bytes.copyOfRange(GCM_IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        val plaintext = cipher.doFinal(ciphertext)
        val payload = JSONObject(String(plaintext, Charsets.UTF_8))
        require(payload.optInt("version") == FORMAT_VERSION) { "Unsupported profile vault format." }

        val config = payload.getString("configJson")
        require(config.isNotBlank()) { "Saved profile is empty." }
        return ImportedXrayProfile(
            configJson = config,
            displayName = payload.optString("displayName", "Saved profile"),
            outboundCount = payload.optInt("outboundCount", 1).coerceAtLeast(1)
        )
    }

    @Synchronized
    fun delete() {
        if (vaultFile.exists() && !vaultFile.delete()) {
            error("Could not delete the encrypted profile vault.")
        }
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
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

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "ai_power_v2ray_profile_v1"
        private const val VAULT_FILE = "profiles.vault"
        private const val FORMAT_VERSION = 1
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
    }
}
