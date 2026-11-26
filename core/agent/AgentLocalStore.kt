package com.aipower.v2ray.core.agent

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Local-first settings and bounded diagnostic history. Gemini credentials are AES-GCM encrypted with Android Keystore. */
class AgentLocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("agent_local_v1", Context.MODE_PRIVATE)

    fun putSetting(key: String, value: String) {
        require(key in ALLOWED_SETTINGS)
        prefs.edit().putString("setting.$key", value.take(256)).apply()
    }

    fun setting(key: String, default: String = ""): String =
        if (key in ALLOWED_SETTINGS) prefs.getString("setting.$key", default) ?: default else default

    fun log(level: String, component: String, message: String) {
        val safe = redact(message).take(500)
        synchronized(LOCK) {
            val old = prefs.getString("diagnostics", "").orEmpty()
            val line = "${System.currentTimeMillis()}|${level.take(12)}|${component.take(32)}|$safe"
            val entries = (old.lineSequence().filter { it.isNotBlank() } + line).toList().takeLast(MAX_LOGS)
            prefs.edit().putString("diagnostics", entries.joinToString("\n")).apply()
        }
    }

    fun logs(): List<String> = prefs.getString("diagnostics", "").orEmpty()
        .lineSequence().filter { it.isNotBlank() }.toList().takeLast(MAX_LOGS).reversed()

    fun clearLogs() { prefs.edit().remove("diagnostics").apply() }

    fun saveGeminiKey(value: String) {
        require(value.length in 10..512)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit().putString("gemini.key", android.util.Base64.encodeToString(cipher.iv + encrypted, android.util.Base64.NO_WRAP)).apply()
    }

    fun geminiKey(): String? {
        val packed = prefs.getString("gemini.key", null) ?: return null
        return runCatching {
            val bytes = android.util.Base64.decode(packed, android.util.Base64.NO_WRAP)
            require(bytes.size in 29..1024)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        }.getOrNull()
    }

    fun hasGeminiKey(): Boolean = prefs.contains("gemini.key")
    fun deleteGeminiKey() { prefs.edit().remove("gemini.key").apply() }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun redact(value: String): String {
        val credentials = Regex("(?i)(password|uuid|token|api[_-]?key|authorization)(\\s*[:=]\\s*)[^\\s,;]+")
        val profiles = Regex("(?i)(vless|vmess|trojan|ss|socks)://[^\\s]+")
        return profiles.replace(credentials.replace(value) { m ->
            m.groupValues[1] + m.groupValues[2] + "[REDACTED]"
        }, "[PROFILE_REDACTED]")
    }

    companion object {
        private val LOCK = Any()
        private const val MAX_LOGS = 500
        private const val KEY_ALIAS = "ai_power_gemini_settings_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val ALLOWED_SETTINGS = setOf("auto_reconnect", "offline_mode", "startup_assistant", "dns_mode", "routing_mode", "benchmark_limit")
    }
}
