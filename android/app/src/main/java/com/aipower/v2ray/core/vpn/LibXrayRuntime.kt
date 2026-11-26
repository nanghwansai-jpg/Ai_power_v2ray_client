package com.aipower.v2ray.core.vpn

import android.net.VpnService
import java.lang.reflect.Proxy
import org.json.JSONObject
import java.util.UUID

/**
 * Narrow boundary around the official XTLS/libXray gomobile AAR.
 *
 * Reflection keeps the app source compilable before the native AAR is produced.
 * Runtime startup fails closed when the pinned libXray artifact/API is unavailable.
 */
internal class LibXrayRuntime(
    private val vpnService: VpnService? = null
) {
    private val bindingClassName = "libXray.LibXray"
    private var socketController: Any? = null

    fun registerSocketProtector(): Boolean {
        val binding = bindingClass()
        val register = binding.methods.firstOrNull {
            it.name.equals("registerDialerController", ignoreCase = true) &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0].isInterface
        } ?: return false

        val controllerType = register.parameterTypes[0]
        val controller = Proxy.newProxyInstance(
            controllerType.classLoader,
            arrayOf(controllerType)
        ) { proxy, method, args ->
            when (method.name.lowercase()) {
                "protectfd" -> {
                    val fd = (args?.firstOrNull() as? Number)?.toInt() ?: -1
                    fd >= 0 && (vpnService?.protect(fd) ?: false)
                }
                "tostring" -> "AI Power VPN socket protector"
                "hashcode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> defaultValue(method.returnType)
            }
        }
        register.invoke(null, controller)
        socketController = controller
        return true
    }

    fun clearSocketProtector() {
        val binding = bindingClass()
        val register = binding.methods.firstOrNull {
            it.name.equals("registerDialerController", ignoreCase = true) &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0].isInterface
        } ?: return

        // libXray wraps this callback and does not accept null safely. Replace it
        // with a detached fail-closed controller instead of retaining the Service.
        val controllerType = register.parameterTypes[0]
        val disabledController = Proxy.newProxyInstance(
            controllerType.classLoader,
            arrayOf(controllerType)
        ) { proxy, method, args ->
            when (method.name.lowercase()) {
                "protectfd" -> false
                "tostring" -> "Inactive AI Power VPN socket protector"
                "hashcode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> defaultValue(method.returnType)
            }
        }
        register.invoke(null, disabledController)
        socketController = null
    }

    /**
     * libXray requires its Android DNS resolver to be configured before runXray
     * so the Go resolver does not recurse through the active VPN interface.
     */
    fun configureDns(server: String): Boolean {
        val controller = socketController ?: return false
        val binding = bindingClass()
        val setDns = binding.methods.firstOrNull {
            it.name.equals("setDNS", ignoreCase = true) &&
                it.parameterTypes.size == 2 &&
                it.parameterTypes[0].isInstance(controller) &&
                it.parameterTypes[1] == String::class.java
        } ?: return false
        setDns.invoke(null, controller, server)
        return true
    }

    fun resetDns() {
        val reset = bindingClass().methods.firstOrNull {
            it.name.equals("resetDNS", ignoreCase = true) &&
                it.parameterTypes.isEmpty()
        } ?: return
        reset.invoke(null)
        socketController = null
    }

    fun invoke(method: String, payload: JSONObject = JSONObject()): JSONObject {
        require(method in ALLOWED_METHODS) { "Unsupported libXray operation" }
        val payloadText = payload.toString()
        require(payloadText.length <= MAX_PAYLOAD_CHARS) {
            "libXray request payload is too large"
        }

        val binding = bindingClass()
        val invoke = binding.methods.firstOrNull {
            it.name.equals("invoke", ignoreCase = true) &&
                it.parameterTypes.contentEquals(arrayOf(String::class.java))
        } ?: error("libXray Invoke(String) entry point was not found")

        val request = JSONObject()
            .put("apiVersion", LIBXRAY_API_VERSION)
            .put("method", method)
            .put("payload", JSONObject(payloadText))
            .toString()

        val raw = invoke.invoke(null, request) as? String
            ?: error("libXray returned an empty response")
        require(raw.length <= MAX_RESPONSE_CHARS) {
            "libXray response is too large"
        }
        val response = JSONObject(raw)
        if (!response.optBoolean("success", false)) {
            // Do not expose native error text that may contain profile or endpoint data.
            error("Xray operation failed")
        }
        return response
    }

    /**
     * App-owned V4 entry point. This is intentionally additive: invoke() above
     * remains the unchanged V3 compatibility path until service migration gates pass.
     */
    fun invokeV4(method: String, payload: JSONObject = JSONObject()): NativeV4Result {
        require(method in V4_ALLOWED_METHODS) { "Unsupported libXray V4 operation" }
        val payloadText = payload.toString()
        val payloadBytes = payloadText.toByteArray(Charsets.UTF_8).size
        val maxPayload = if (method == "convertShareLinksToXrayJson") {
            MAX_V4_PROFILE_BYTES
        } else {
            MAX_V4_CONTROL_BYTES
        }
        require(payloadBytes <= maxPayload) { "libXray V4 request payload is too large" }

        val requestId = UUID.randomUUID().toString()
        val request = JSONObject()
            .put("apiVersion", V4_API_VERSION)
            .put("requestId", requestId)
            .put("method", method)
            .put("payload", JSONObject(payloadText))
            .toString()
        require(request.toByteArray(Charsets.UTF_8).size <= MAX_V4_ABSOLUTE_BYTES) {
            "libXray V4 request is too large"
        }

        val binding = bindingClass()
        val invokeV4 = binding.methods.firstOrNull {
            it.name.equals("invokeV4", ignoreCase = true) &&
                it.parameterTypes.contentEquals(arrayOf(String::class.java))
        } ?: error("libXray V4 binding is unavailable")

        val raw = try {
            invokeV4.invoke(null, request) as? String
                ?: error("libXray V4 returned an empty response")
        } catch (_: ReflectiveOperationException) {
            throw IllegalStateException("libXray V4 invocation failed")
        }
        return NativeV4ResponseParser.parse(raw, requestId, method)
    }

    fun convertShareLinksToXrayJsonV4(text: String, ageSecretKey: String? = null): String {
        val payload = JSONObject().put("text", text)
        if (ageSecretKey != null) payload.put("age", JSONObject().put("secretKey", ageSecretKey))
        return (invokeV4("convertShareLinksToXrayJson", payload) as? NativeV4Result.ConvertedProfile
            ?: error("Unexpected libXray V4 conversion result")).configJson
    }

    fun testXrayV4(xrayJson: String) {
        check(invokeV4("testXray", JSONObject().put("xrayJson", xrayJson)) === NativeV4Result.ConfigValid) {
            "Unexpected libXray V4 test result"
        }
    }

    fun runXrayV4(xrayJson: String) {
        check(invokeV4("runXray", JSONObject().put("xrayJson", xrayJson)) === NativeV4Result.Started) {
            "Unexpected libXray V4 start result"
        }
    }

    fun stopXrayV4() {
        check(invokeV4("stopXray") === NativeV4Result.Stopped) {
            "Unexpected libXray V4 stop result"
        }
    }

    private fun bindingClass(): Class<*> =
        Class.forName(bindingClassName)

    private fun defaultValue(type: Class<*>): Any? = when (type) {
        java.lang.Boolean.TYPE -> false
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        java.lang.Short.TYPE -> 0.toShort()
        java.lang.Byte.TYPE -> 0.toByte()
        java.lang.Character.TYPE -> '\u0000'
        else -> null
    }

    companion object {
        // Must match LibXrayAPIVersion in the pinned XTLS/libXray v26.11.9 source.
        private const val LIBXRAY_API_VERSION = 3
        private val ALLOWED_METHODS = setOf(
            "convertShareLinksToXrayJson",
            "testXray",
            "runXray",
            "stopXray"
        )
        private const val MAX_PAYLOAD_CHARS = 2_000_000
        private const val MAX_RESPONSE_CHARS = 2_000_000
        private const val V4_API_VERSION = 4
        private const val MAX_V4_CONTROL_BYTES = 64 * 1024
        private const val MAX_V4_PROFILE_BYTES = 2 * 1024 * 1024
        private const val MAX_V4_ABSOLUTE_BYTES = 4 * 1024 * 1024
        private val V4_ALLOWED_METHODS = setOf(
            "convertShareLinksToXrayJson",
            "testXray",
            "runXray",
            "stopXray"
        )
    }
}
