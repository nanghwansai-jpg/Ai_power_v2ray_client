package com.aipower.v2ray.core.vpn

internal sealed interface NativeV4Result {
    data class ConvertedProfile(val configJson: String) : NativeV4Result
    data object ConfigValid : NativeV4Result
    data object Started : NativeV4Result
    data object Stopped : NativeV4Result
}
internal enum class NativeV4ErrorCode {
    INVALID_REQUEST, UNSUPPORTED_API_VERSION, UNSUPPORTED_METHOD, REQUEST_TOO_LARGE,
    RESPONSE_TOO_LARGE, INVALID_CONFIG, CORE_BUSY, CORE_START_FAILED, CORE_STOP_FAILED, INTERNAL_ERROR
}
internal class NativeV4Exception(val code: NativeV4ErrorCode, val category: String, val retryable: Boolean) :
    Exception("Native V4 operation failed: " + code.name)

/** Strict dependency-free decoder for the app-owned InvokeV4 response boundary. */
internal object NativeV4ResponseParser {
    private const val MAX_CONTROL_BYTES = 64 * 1024
    private const val MAX_PROFILE_BYTES = 2 * 1024 * 1024
    private const val MAX_ABSOLUTE_BYTES = 4 * 1024 * 1024
    private const val MAX_DEPTH = 32
    private const val MAX_FIELDS = 64
    private const val MAX_ELEMENTS = 4096
    private const val MAX_OUTBOUNDS = 256
    private val envelope = setOf("apiVersion", "requestId", "method", "success", "code", "data", "error")
    private val errorFields = setOf("category", "retryable")
    private val categories = setOf("PROTOCOL", "LIMIT", "VALIDATION", "ENGINE", "INTERNAL")
    private val methods = setOf("convertShareLinksToXrayJson", "testXray", "runXray", "stopXray")
    private val uuid = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")

    fun parse(raw: String, expectedRequestId: String, expectedMethod: String): NativeV4Result {
        require(uuid.matches(expectedRequestId)) { "Invalid expected request ID" }
        require(expectedMethod in methods) { "Unsupported expected method" }
        val maxBytes = if (expectedMethod == "convertShareLinksToXrayJson") MAX_PROFILE_BYTES else MAX_CONTROL_BYTES
        require(strictUtf8Length(raw) in 2..minOf(maxBytes, MAX_ABSOLUTE_BYTES)) { "Invalid native V4 response size" }

        val root = Json(raw).parseObject()
        require(root.keys == envelope) { "Invalid native V4 envelope fields" }
        require(integer(root["apiVersion"]) == 4L) { "Native V4 API version mismatch" }
        val success = bool(root["success"])
        val codeText = str(root["code"])
        val requestId = nullableString(root["requestId"])
        val method = nullableString(root["method"])

        // INVALID_REQUEST may retain either independently validated correlation field.
        // Native extracts a valid UUID and a valid ASCII method before validating the
        // complete request envelope, so either field can legitimately be null.
        if (!success && codeText == "INVALID_REQUEST") {
            require(requestId == null || requestId == expectedRequestId) {
                "Native V4 request ID mismatch"
            }
            require(method == null || method == expectedMethod) {
                "Native V4 method mismatch"
            }
            require(root["data"] === Null) { "Invalid failure data" }
            val e = validateError(root["error"])
            throw NativeV4Exception(NativeV4ErrorCode.INVALID_REQUEST, e.first, e.second)
        }

        require(requestId == expectedRequestId) { "Native V4 request ID mismatch" }
        require(method == expectedMethod) { "Native V4 method mismatch" }
        if (success) {
            // OK is a success sentinel, not an error enum member.
            require(codeText == "OK") { "Success response must use OK" }
            require(root["error"] === Null) { "Success response must not contain error" }
            return validateSuccess(expectedMethod, root["data"])
        }

        require(codeText != "OK") { "Failure response must not use OK" }
        require(root["data"] === Null) { "Failure response must not contain data" }
        val code = runCatching { NativeV4ErrorCode.valueOf(codeText) }.getOrElse {
            throw IllegalArgumentException("Unknown native V4 response code")
        }
        val e = validateError(root["error"])
        throw NativeV4Exception(code, e.first, e.second)
    }

    private fun validateSuccess(method: String, value: Value?): NativeV4Result = when (method) {
        "convertShareLinksToXrayJson" -> {
            val obj = obj(value)
            require(obj.keys == setOf("outbounds")) { "Invalid converted profile fields" }
            val outbounds = arr(obj["outbounds"])
            require(outbounds.isNotEmpty() && outbounds.size <= MAX_OUTBOUNDS) { "Invalid converted outbound count" }
            outbounds.forEach { require(it is Obj) { "Invalid converted outbound" } }
            NativeV4Result.ConvertedProfile(render(value!!))
        }
        "testXray" -> {
            require(obj(value).isEmpty()) { "Invalid testXray result" }
            NativeV4Result.ConfigValid
        }
        "runXray" -> {
            val data = obj(value)
            require(data.keys == setOf("started") && bool(data["started"])) { "Invalid runXray result" }
            NativeV4Result.Started
        }
        "stopXray" -> {
            val data = obj(value)
            require(data.keys == setOf("stopped") && bool(data["stopped"])) { "Invalid stopXray result" }
            NativeV4Result.Stopped
        }
        else -> throw IllegalArgumentException("Unsupported native V4 method")
    }

    private fun validateError(value: Value?): Pair<String, Boolean> {
        val e = obj(value)
        require(e.keys == errorFields) { "Invalid native V4 error fields" }
        val category = str(e["category"])
        require(category in categories) { "Invalid native V4 error category" }
        return category to bool(e["retryable"])
    }

    // Count UTF-8 bytes ourselves: String.toByteArray may replace malformed UTF-16 surrogates.
    private fun strictUtf8Length(s: String): Int {
        var bytes = 0L
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.code <= 0x7f -> { bytes++; i++ }
                c.code <= 0x7ff -> { bytes += 2; i++ }
                c.isHighSurrogate() -> {
                    require(i + 1 < s.length && s[i + 1].isLowSurrogate()) { "Malformed UTF-16 response" }
                    bytes += 4; i += 2
                }
                c.isLowSurrogate() -> throw IllegalArgumentException("Malformed UTF-16 response")
                else -> { bytes += 3; i++ }
            }
            require(bytes <= MAX_ABSOLUTE_BYTES) { "Native V4 response exceeds absolute size limit" }
        }
        return bytes.toInt()
    }

    private fun obj(v: Value?): Map<String, Value> = (v as? Obj)?.v ?: throw IllegalArgumentException("Expected JSON object")
    private fun arr(v: Value?): List<Value> = (v as? Arr)?.v ?: throw IllegalArgumentException("Expected JSON array")
    private fun str(v: Value?): String = (v as? Str)?.v ?: throw IllegalArgumentException("Expected JSON string")
    private fun nullableString(v: Value?): String? = when (v) {
        Null -> null
        is Str -> v.v
        else -> throw IllegalArgumentException("Expected nullable JSON string")
    }
    private fun bool(v: Value?): Boolean = (v as? Bool)?.v ?: throw IllegalArgumentException("Expected JSON boolean")
    private fun integer(v: Value?): Long = (v as? Num)?.raw?.toLongOrNull()
        ?: throw IllegalArgumentException("Expected integer JSON number")

    private fun render(v: Value): String = when (v) {
        Null -> "null"
        is Bool -> v.v.toString()
        is Num -> v.raw
        is Str -> buildString {
            append('"')
            v.v.forEach { c ->
                when (c) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\b' -> append("\\b")
                    '\u000c' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
                }
            }
            append('"')
        }
        is Arr -> v.v.joinToString(prefix = "[", postfix = "]") { render(it) }
        is Obj -> v.v.entries.joinToString(prefix = "{", postfix = "}") { (k, x) -> render(Str(k)) + ":" + render(x) }
    }

    private sealed interface Value
    private data object Null : Value
    private data class Bool(val v: Boolean) : Value
    private data class Num(val raw: String) : Value
    private data class Str(val v: String) : Value
    private data class Arr(val v: List<Value>) : Value
    private data class Obj(val v: LinkedHashMap<String, Value>) : Value

    private class Json(private val s: String) {
        private var p = 0
        fun parseObject(): Map<String, Value> {
            val root = value(0)
            ws()
            require(p == s.length) { "Trailing JSON content" }
            return (root as? Obj)?.v ?: throw IllegalArgumentException("Native response must be an object")
        }
        private fun value(depth: Int): Value {
            require(depth <= MAX_DEPTH) { "JSON nesting limit exceeded" }
            ws()
            require(p < s.length) { "Unexpected end of JSON" }
            return when (s[p]) {
                '{' -> objectValue(depth)
                '[' -> arrayValue(depth)
                '"' -> Str(string())
                't' -> literal("true", Bool(true))
                'f' -> literal("false", Bool(false))
                'n' -> literal("null", Null)
                '-', in '0'..'9' -> number()
                else -> throw IllegalArgumentException("Invalid JSON token")
            }
        }
        private fun objectValue(depth: Int): Obj {
            p++; ws()
            val map = linkedMapOf<String, Value>()
            if (take('}')) return Obj(map)
            while (true) {
                require(map.size < MAX_FIELDS) { "JSON object field limit exceeded" }
                ws()
                require(p < s.length && s[p] == '"') { "Expected object key" }
                val key = string()
                require(!map.containsKey(key)) { "Duplicate JSON key" }
                ws(); expect(':')
                map[key] = value(depth + 1)
                ws()
                if (take('}')) return Obj(map)
                expect(',')
            }
        }
        private fun arrayValue(depth: Int): Arr {
            p++; ws()
            val list = mutableListOf<Value>()
            if (take(']')) return Arr(list)
            while (true) {
                require(list.size < MAX_ELEMENTS) { "JSON array element limit exceeded" }
                list += value(depth + 1)
                ws()
                if (take(']')) return Arr(list)
                expect(',')
            }
        }
        private fun string(): String {
            expect('"')
            val out = StringBuilder()
            while (p < s.length) {
                val c = s[p++]
                when {
                    c == '"' -> return out.toString()
                    c == '\\' -> {
                        require(p < s.length) { "Invalid JSON escape" }
                        when (val e = s[p++]) {
                            '"', '\\', '/' -> out.append(e)
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000c')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> appendUnicodeEscape(out, hexChar())
                            else -> throw IllegalArgumentException("Invalid JSON escape")
                        }
                    }
                    c.code < 0x20 -> throw IllegalArgumentException("Unescaped control character")
                    c.isHighSurrogate() -> {
                        require(p < s.length && s[p].isLowSurrogate()) { "Unpaired high surrogate" }
                        out.append(c).append(s[p++])
                    }
                    c.isLowSurrogate() -> throw IllegalArgumentException("Unpaired low surrogate")
                    else -> out.append(c)
                }
            }
            throw IllegalArgumentException("Unterminated JSON string")
        }
        private fun appendUnicodeEscape(out: StringBuilder, first: Char) {
            when {
                first.isHighSurrogate() -> {
                    require(p + 6 <= s.length && s[p] == '\\' && s[p + 1] == 'u') { "High surrogate requires low-surrogate escape" }
                    p += 2
                    val second = hexChar()
                    require(second.isLowSurrogate()) { "Invalid surrogate pair" }
                    out.append(first).append(second)
                }
                first.isLowSurrogate() -> throw IllegalArgumentException("Unpaired low surrogate")
                else -> out.append(first)
            }
        }
        private fun hexChar(): Char {
            require(p + 4 <= s.length) { "Invalid Unicode escape" }
            val h = s.substring(p, p + 4)
            require(h.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) { "Invalid Unicode escape" }
            p += 4
            return h.toInt(16).toChar()
        }
        private fun number(): Num {
            val start = p
            if (take('-')) require(p < s.length) { "Invalid number" }
            if (take('0')) {
                require(p >= s.length || s[p] !in '0'..'9') { "Leading zero in number" }
            } else {
                require(p < s.length && s[p] in '1'..'9') { "Invalid number" }
                while (p < s.length && s[p] in '0'..'9') p++
            }
            if (take('.')) {
                require(p < s.length && s[p] in '0'..'9') { "Invalid fraction" }
                while (p < s.length && s[p] in '0'..'9') p++
            }
            if (p < s.length && (s[p] == 'e' || s[p] == 'E')) {
                p++
                if (p < s.length && (s[p] == '+' || s[p] == '-')) p++
                require(p < s.length && s[p] in '0'..'9') { "Invalid exponent" }
                while (p < s.length && s[p] in '0'..'9') p++
            }
            return Num(s.substring(start, p))
        }
        private fun <T : Value> literal(token: String, result: T): T {
            require(s.regionMatches(p, token, 0, token.length)) { "Invalid JSON literal" }
            p += token.length
            return result
        }
        private fun ws() { while (p < s.length && (s[p] == ' ' || s[p] == '\t' || s[p] == '\r' || s[p] == '\n')) p++ }
        private fun take(c: Char): Boolean { if (p < s.length && s[p] == c) { p++; return true }; return false }
        private fun expect(c: Char) { require(take(c)) { "Expected '$c'" } }
    }
}