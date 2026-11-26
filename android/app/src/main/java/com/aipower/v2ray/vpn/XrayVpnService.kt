package com.aipower.v2ray.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.content.pm.ServiceInfo
import com.aipower.v2ray.core.vpn.LibXrayRuntime
import org.json.JSONObject

class XrayVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private var runtime: LibXrayRuntime? = null
    private var coreStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTunnel()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startInForeground()
                val rawConfig = intent.getStringExtra(EXTRA_XRAY_JSON)
                if (rawConfig.isNullOrBlank()) {
                    stopWithError("No Xray configuration was supplied")
                    return START_NOT_STICKY
                }
                try {
                    startTunnel(rawConfig)
                } catch (error: Exception) {
                    stopWithError(error.message ?: "Unable to start VPN")
                }
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startTunnel(rawConfig: String) {
        val engine = LibXrayRuntime(this)
        check(engine.registerSocketProtector()) {
            "libXray Android socket-protection API is unavailable"
        }

        val config = JSONObject(rawConfig)
        engine.invoke("testXray", JSONObject().put("xrayJson", config.toString()))
        check(engine.configureDns("1.1.1.1:53")) {
            "libXray Android DNS protection API is unavailable"
        }

        val env = config.optJSONObject("env") ?: JSONObject().also {
            config.put("env", it)
        }
        val xrayEnv = env.optJSONObject("xray") ?: JSONObject().also {
            env.put("xray", it)
        }
        val tunEnv = xrayEnv.optJSONObject("tun") ?: JSONObject().also {
            xrayEnv.put("tun", it)
        }

        val descriptor = Builder()
            .setSession("AI Power V2Ray Client")
            .setMtu(1500)
            .addAddress("10.111.0.1", 30)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addAddress("fd00:1111:2222::1", 126)
            .addRoute("::", 0)
            .establish() ?: error("Android could not establish the VPN interface")

        tun = descriptor
        tunEnv.put("fd", descriptor.fd)

        try {
            engine.invoke(
                "runXray",
                JSONObject().put("xrayJson", config.toString())
            )
            runtime = engine
            coreStarted = true
            updateNotification("VPN connected")
        } catch (error: Exception) {
            runCatching { engine.resetDns() }
            descriptor.close()
            tun = null
            throw error
        }
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "VPN connection",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val notification = buildNotification("Starting secure VPN")
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun buildNotification(message: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("AI Power V2Ray Client")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(true)
            .build()
    }

    private fun stopWithError(message: String) {
        updateNotification("VPN stopped: $message")
        stopTunnel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopTunnel() {
        if (coreStarted) {
            runCatching { runtime?.invoke("stopXray") }
        }
        runCatching { runtime?.resetDns() }
        coreStarted = false
        runtime = null
        runCatching { tun?.close() }
        tun = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onRevoke() {
        stopTunnel()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.aipower.v2ray.vpn.START"
        const val ACTION_STOP = "com.aipower.v2ray.vpn.STOP"
        const val EXTRA_XRAY_JSON = "xray_json"
        private const val CHANNEL_ID = "vpn_connection"
        private const val NOTIFICATION_ID = 1001
    }
}
