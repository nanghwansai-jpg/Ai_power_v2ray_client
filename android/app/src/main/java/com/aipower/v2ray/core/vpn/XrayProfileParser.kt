package com.aipower.v2ray.core.vpn

import org.json.JSONObject

data class ImportedXrayProfile(
    val configJson: String,
    val displayName: String,
    val outboundCount: Int
)

/**
 * Converts supported share text or Xray JSON into normalized Xray JSON.
 * Parsing is performed locally by the pinned XTLS/libXray native library.
 */
class XrayProfileParser {
    fun parse(text: String): ImportedXrayProfile {
        require(text.isNotBlank()) { "Paste a V2Ray/Xray share link or Xray JSON first." }
        require(text.length <= MAX_INPUT_CHARS) { "Profile input is too large." }

        val normalized = LibXrayRuntime().convertShareLinksToXrayJsonV4(text.trim())
        require(normalized.isNotBlank()) { "The profile parser returned no Xray configuration." }

        val config = JSONObject(normalized)
        val outbounds = config.optJSONArray("outbounds")
            ?: error("The imported profile has no outbounds.")
        require(outbounds.length() > 0) { "The imported profile contains no usable outbound." }

        val first = outbounds.optJSONObject(0)
        val displayName = first?.optString("tag")
            ?.takeIf { !it.isNullOrBlank() }
            ?: "Imported profile"

        return ImportedXrayProfile(
            configJson = config.toString(),
            displayName = displayName,
            outboundCount = outbounds.length()
        )
    }

    companion object {
        private const val MAX_INPUT_CHARS = 1_000_000
    }
}
