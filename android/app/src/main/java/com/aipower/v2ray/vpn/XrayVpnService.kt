package com.aipower.v2ray.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.aipower.v2ray.core.vpn.LibXrayRuntime
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

class XrayVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private var runtime: LibXrayRuntime? = null
    private var coreStarted = false
    private val destroyed = AtomicBoolean(false)
    private val foregroundLock = Any()
    @Volatile private var foregroundActive = false
    @Volatile private var latestStartId = 0
    private val worker = Executors.newSingleThreadExecutor()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (destroyed.get()) return START_NOT_STICKY
        synchronized(foregroundLock) {
            latestStartId = startId
            if (intent?.action == ACTION_START) startInForeground()
        }
        when (intent?.action) {
            ACTION_STOP -> executeOnWorker(startId) {
                stopTunnel(removeForeground = false)
                finishStopIfCurrent(startId)
            }

            ACTION_START -> {
                val rawConfig = intent.getStringExtra(EXTRA_XRAY_JSON)
                if (rawConfig.isNullOrBlank() || rawConfig.length > MAX_CONFIG_CHARS) {
                    executeOnWorker(startId) { stopWithError(startId) }
                    return START_NOT_STICKY
                }

                executeOnWorker(startId) {
                    try {
                        stopTunnel(removeForeground = false)
                        startTunnel(rawConfig)
                    } catch (_: Exception) {
                        stopWithError(startId)
                    }
                }
            }

            else -> executeOnWorker(startId) {
                stopTunnel(removeForeground = false)
                finishStopIfCurrent(startId)
            }
        }
        return START_NOT_STICKY
    }

    /**
     * All tunnel mutations are serialized on one executor. Never wait for this
     * executor from the main thread; service destruction queues final cleanup.
     */
    private fun executeOnWorker(startId: Int, block: () -> Unit) {
        if (destroyed.get()) {
            stopSelf(startId)
            return
        }
        try {
            worker.execute {
                if (!destroyed.get()) block()
            }
        } catch (_: RejectedExecutionException) {
            stopSelf(startId)
        }
    }

    @Synchronized
    private fun startTunnel(rawConfig: String) {
        val engine = LibXrayRuntime(this)
        runtime = engine
        check(engine.registerSocketProtector()) {
            "libXray Android socket-protection API is unavailable"
        }

        val config = JSONObject(rawConfig)
        engine.testXrayV4(config.toString())
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
        try {
            check(engine.configureDns("1.1.1.1:53")) {
                "libXray Android DNS protection API is unavailable"
            }
            tunEnv.put("fd", descriptor.fd)
            engine.runXrayV4(config.toString())
            runtime = engine
            coreStarted = true
            updateNotification("VPN connected")
        } catch (error: Exception) {
            runCatching { engine.stopXrayV4() }
            runCatching { engine.resetDns() }
            runCatching { engine.clearSocketProtector() }
            runCatching { descriptor.close() }
            tun = null
            runtime = null
            coreStarted = false
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
        val notification = buildNotification("Starting secure VPN", ongoing = true)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregroundActive = true
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(message, ongoing = true))
    }

    private fun postFailureNotification() {
        getSystemService(NotificationManager::class.java)
            .notify(
                NOTIFICATION_ID,
                buildNotification(
                    "VPN could not start. Check the profile and try again.",
                    ongoing = false
                )
            )
    }

    private fun buildNotification(message: String, ongoing: Boolean): Notification {
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
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .build()
    }

    private fun stopWithError(startId: Int) {
        stopTunnel(removeForeground = false)
        synchronized(foregroundLock) {
            // An older start failure must not remove the foreground notification
            // belonging to a newer start/stop request.
            if (latestStartId == startId) {
                removeForegroundNotification()
                // Android may suppress this when notification permission is denied.
                postFailureNotification()
                stopSelf(startId)
            }
        }
    }

    private fun finishStopIfCurrent(startId: Int) {
        synchronized(foregroundLock) {
            if (latestStartId == startId) {
                removeForegroundNotification()
                stopSelf(startId)
            }
        }
    }

    private fun removeForegroundNotification() {
        if (foregroundActive) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundActive = false
        }
    }

    @Synchronized
    private fun stopTunnel(removeForeground: Boolean = true) {
        if (coreStarted) {
            runCatching { runtime?.stopXrayV4() }
        }
        runCatching { runtime?.resetDns() }
        runCatching { runtime?.clearSocketProtector() }
        coreStarted = false
        runtime = null
        runCatching { tun?.close() }
        tun = null
        if (removeForeground) removeForegroundNotification()
    }

    private fun shutdownWorkerWithCleanup(stopServiceAfterCleanup: Boolean = false) {
        // Queue cleanup behind any in-flight operation. Queued user actions
        // check destroyed and are skipped; this final cleanup task still runs.
        try {
            worker.execute {
                stopTunnel()
                if (stopServiceAfterCleanup) stopSelf()
            }
        } catch (_: RejectedExecutionException) {
            // Best-effort fallback for an already-terminated executor.
            stopTunnel()
            if (stopServiceAfterCleanup) stopSelf()
        }
        worker.shutdown()
    }

    override fun onRevoke() {
        if (destroyed.compareAndSet(false, true)) {
            shutdownWorkerWithCleanup(stopServiceAfterCleanup = true)
        }
        super.onRevoke()
    }

    override fun onDestroy() {
        if (destroyed.compareAndSet(false, true)) {
            // Do not block Android's main thread waiting for native code.
            shutdownWorkerWithCleanup()
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.aipower.v2ray.vpn.START"
        const val ACTION_STOP = "com.aipower.v2ray.vpn.STOP"
        const val EXTRA_XRAY_JSON = "xray_json"
        private const val CHANNEL_ID = "vpn_connection"
        private const val NOTIFICATION_ID = 1001
        private const val MAX_CONFIG_CHARS = 1_000_000
    }
}
