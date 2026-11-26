package com.aipower.v2ray.core.vpn

/**
 * Test-source-only response mutator. This file lives under src/test and is never
 * part of either debug or release application APKs.
 */
internal object NativeV4FaultInjection {
    const val RELEASE_EXCLUSION_SENTINEL = "AI_POWER_V4_FAULT_INJECTION_TEST_ONLY"

    fun duplicateEnvelopeKey(valid: String): String =
        valid.dropLast(1) + ",\"success\":true}"

    fun wrongApiVersion(valid: String): String =
        valid.replace("\"apiVersion\":4", "\"apiVersion\":5")

    fun wrongMethod(valid: String): String =
        valid.replace("\"method\":\"testXray\"", "\"method\":\"runXray\"")

    fun oversizedControlResponse(valid: String): String {
        val padding = "x".repeat(65 * 1024)
        return valid.dropLast(1) + ",\"padding\":\"" + padding + "\"}"
    }

    fun deeplyNestedResponse(valid: String, depth: Int = 40): String {
        val nested = "[".repeat(depth) + "0" + "]".repeat(depth)
        return valid.replace("\"data\":{}", "\"data\":{\"nested\":" + nested + "}")
    }

    fun malformedUnicode(valid: String): String =
        valid.replace("testXray", "test" + '\uD800')

    fun contradictorySuccessEnvelope(valid: String): String =
        valid.replace("\"error\":null", "\"error\":{\"category\":\"ENGINE\",\"retryable\":false}")
}
