package com.aipower.v2ray.core.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.fail

class NativeV4ResponseParserTest {
    private val id = "123e4567-e89b-12d3-a456-426614174000"

    private fun response(
        method: String = "testXray",
        data: String = "{}",
        code: String = "OK",
        success: Boolean = true,
        requestId: String = "\"$id\"",
        error: String = "null"
    ): String = """{"apiVersion":4,"requestId":$requestId,"method":"$method","success":$success,"code":"$code","data":$data,"error":$error}"""

    @Test fun acceptsSuccessSentinelAndTypedConfigResult() {
        assertEquals(
            NativeV4Result.ConfigValid,
            NativeV4ResponseParser.parse(response(), id, "testXray")
        )
    }

    @Test fun acceptsRunAndStopTypedResults() {
        assertEquals(NativeV4Result.Started, NativeV4ResponseParser.parse(response("runXray", """{"started":true}"""), id, "runXray"))
        assertEquals(NativeV4Result.Stopped, NativeV4ResponseParser.parse(response("stopXray", """{"stopped":true}"""), id, "stopXray"))
    }

    @Test fun acceptsConvertedProfileWithBoundedOutboundList() {
        val parsed = NativeV4ResponseParser.parse(
            response("convertShareLinksToXrayJson", """{"outbounds":[{"protocol":"freedom"}]}"""),
            id,
            "convertShareLinksToXrayJson"
        )
        assertTrue(parsed is NativeV4Result.ConvertedProfile)
        assertTrue((parsed as NativeV4Result.ConvertedProfile).configJson.contains("outbounds"))
    }

    @Test fun rejectsSuccessWithNonOkCode() {
        expectFailure {
            NativeV4ResponseParser.parse(response(code = "INVALID_CONFIG"), id, "testXray")
        }
    }

    @Test fun rejectsFailureWithOkCode() {
        expectFailure {
            NativeV4ResponseParser.parse(
                response(success = false, code = "OK", data = "null", error = """{"category":"ENGINE","retryable":false}"""),
                id,
                "testXray"
            )
        }
    }

    @Test fun mapsAllowlistedStructuredFailureToSanitizedException() {
        try {
            NativeV4ResponseParser.parse(
                response(success = false, code = "CORE_BUSY", data = "null", error = """{"category":"ENGINE","retryable":true}"""),
                id,
                "testXray"
            )
            fail("Expected NativeV4Exception")
        } catch (e: NativeV4Exception) {
            assertEquals(NativeV4ErrorCode.CORE_BUSY, e.code)
            assertEquals("ENGINE", e.category)
            assertTrue(e.retryable)
            assertEquals("Native V4 operation failed: CORE_BUSY", e.message)
        }
    }

    @Test fun acceptsNullCorrelationOnlyForInvalidRequest() {
        try {
            NativeV4ResponseParser.parse(
                response(success = false, code = "INVALID_REQUEST", requestId = "null", method = "null", data = "null",
                    error = """{"category":"PROTOCOL","retryable":false}"""),
                id,
                "testXray"
            )
            fail("Expected NativeV4Exception")
        } catch (e: NativeV4Exception) {
            assertEquals(NativeV4ErrorCode.INVALID_REQUEST, e.code)
        }
    }

    @Test fun rejectsNullCorrelationForOtherFailureCodes() {
        expectFailure {
            NativeV4ResponseParser.parse(
                response(success = false, code = "CORE_BUSY", requestId = "null", method = "testXray", data = "null",
                    error = """{"category":"ENGINE","retryable":true}"""),
                id,
                "testXray"
            )
        }
    }

    @Test fun rejectsDuplicateAndUnknownEnvelopeFields() {
        val duplicate = """{"apiVersion":4,"requestId":"$id","method":"testXray","success":true,"code":"OK","data":{},"error":null,"success":true}"""
        expectFailure { NativeV4ResponseParser.parse(duplicate, id, "testXray") }
        val unknown = response().dropLast(1) + ""","extra":true}"""
        expectFailure { NativeV4ResponseParser.parse(unknown, id, "testXray") }
    }

    @Test fun rejectsRequestAndMethodCorrelationMismatch() {
        expectFailure { NativeV4ResponseParser.parse(response(), "123e4567-e89b-12d3-a456-426614174001", "testXray") }
        expectFailure { NativeV4ResponseParser.parse(response("runXray", """{"started":true}"""), id, "testXray") }
    }

    @Test fun rejectsFractionalApiVersionAndTrailingJson() {
        val fractional = response().replace("\"apiVersion\":4", "\"apiVersion\":4.0")
        expectFailure { NativeV4ResponseParser.parse(fractional, id, "testXray") }
        expectFailure { NativeV4ResponseParser.parse(response() + " {}", id, "testXray") }
    }

    @Test fun rejectsUnpairedEscapedSurrogatesAndAcceptsPair() {
        val highOnly = response().replace("\"method\":\"testXray\"", "\"method\":\"\\uD800\"")
        expectFailure { NativeV4ResponseParser.parse(highOnly, id, "testXray") }
        val lowOnly = response().replace("\"method\":\"testXray\"", "\"method\":\"\\uDC00\"")
        expectFailure { NativeV4ResponseParser.parse(lowOnly, id, "testXray") }
        val pair = response("convertShareLinksToXrayJson", """{"outbounds":[{"tag":"\uD83D\uDE80"}]}""")
        val parsed = NativeV4ResponseParser.parse(pair, id, "convertShareLinksToXrayJson")
        assertTrue(parsed is NativeV4Result.ConvertedProfile)
        assertTrue((parsed as NativeV4Result.ConvertedProfile).configJson.contains("🚀"))
    }

    @Test fun rejectsUnpairedRawUtf16SurrogateBeforeByteCounting() {
        val malformed = response().replace("testXray", "test" + '\uD800')
        expectFailure { NativeV4ResponseParser.parse(malformed, id, "testXray") }
    }

    @Test fun rejectsMethodSpecificDataShapeViolations() {
        expectFailure { NativeV4ResponseParser.parse(response("runXray", """{"started":false}"""), id, "runXray") }
        expectFailure { NativeV4ResponseParser.parse(response("convertShareLinksToXrayJson", """{"outbounds":[]}"""), id, "convertShareLinksToXrayJson") }
        expectFailure { NativeV4ResponseParser.parse(response("testXray", """{"unexpected":true}"""), id, "testXray") }
    }

    private fun expectFailure(block: () -> Unit) {
        try {
            block()
            fail("Expected strict parser rejection")
        } catch (_: IllegalArgumentException) {
            // Expected: malformed, oversized, or inconsistent response.
        }
    }
}