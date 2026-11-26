package com.aipower.v2ray.core.vpn

import android.net.VpnService
import java.lang.reflect.Proxy
import org.json.JSONObject

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
        ) { _, method, args ->
            when (method.name.lowercase()) {
                "protectfd" -> {
                    val fd = (args?.firstOrNull() as? Number)?.toInt() ?: -1
                    fd >= 0 && (vpnService?.protect(fd) ?: false)
                }
                "tostring" -> "AI Power VPN socket protector"
                "hashcode" -> System.identityHashCode(this)
                "equals" -> false
                else -> defaultValue(method.returnType)
            }
        }
        register.invoke(null, controller)
        socketController = controller
        return true
    }

    fun unregisterSocketProtector() {
        val binding = bindingClass()
        val register = binding.methods.firstOrNull {
            it.name.equals("registerDialerController", ignoreCase = true) &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0].isInterface
        } ?: return
        register.invoke(null, *arrayOf<Any?>(null))
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
        val binding = bindingClass()
        val invoke = binding.methods.firstOrNull {
            it.name.equals("invoke", ignoreCase = true) &&
                it.parameterTypes.contentEquals(arrayOf(String::class.java))
        } ?: error("libXray Invoke(String) entry point was not found")

        val request = JSONObject()
            .put("apiVersion", 2)
            .put("method", method)
            .put("payload", payload)
            .toString()

        val raw = invoke.invoke(null, request) as? String
            ?: error("libXray returned an empty response")
        val response = JSONObject(raw)
        if (!response.optBoolean("success", false)) {
            error(response.optString("error").ifBlank { "Xray operation failed" })
        }
        return response
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
}
