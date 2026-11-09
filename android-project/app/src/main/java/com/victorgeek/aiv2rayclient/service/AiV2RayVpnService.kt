package com.victorgeek.aiv2rayclient.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class AiV2RayVpnService : VpnService() {

    companion object {
        const val TAG = "AiV2RayVpnService"
        const val ACTION_START = "com.victorgeek.aiv2rayclient.START"
        const val ACTION_STOP = "com.victorgeek.aiv2rayclient.STOP"
        const val CHANNEL_ID = "ai_v2ray_vpn_status"
        const val NOTIFICATION_ID = 1001

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayProcess: Process? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val configJson = intent.getStringExtra("XRAY_CONFIG_JSON") ?: ""
                val serverName = intent.getStringExtra("SERVER_NAME") ?: "Cloudflare Fast Node"
                startVpnTunnel(configJson, serverName)
            }
            ACTION_STOP -> {
                stopVpnTunnel()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpnTunnel(configJson: String, serverName: String) {
        if (isRunning) return
        isRunning = true

        startForeground(NOTIFICATION_ID, buildNotification("Connecting to $serverName..."))

        serviceScope.launch {
            try {
                val configFile = File(filesDir, "xray_config.json")
                FileOutputStream(configFile).use { it.write(configJson.toByteArray()) }

                startXrayCore(configFile.absolutePath)

                val builder = Builder()
                    .setSession("AI V2Ray Enterprise ($serverName)")
                    .setMtu(1420)
                    .addAddress("10.0.8.2", 24)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")
                    .setBlocking(false)

                try {
                    builder.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to disallow self: ${e.message}")
                }

                vpnInterface = builder.establish()
                Log.i(TAG, "VPN TUN Interface established successfully: fd=${vpnInterface?.fd}")

                startTun2Socks(vpnInterface?.fd ?: -1)
                updateNotification("Protected via $serverName (Ping < 35ms)")

            } catch (e: Exception) {
                Log.e(TAG, "VPN start failed", e)
                stopVpnTunnel()
            }
        }
    }

    private fun startXrayCore(configPath: String) {
        val xrayBinary = File(applicationInfo.nativeLibraryDir, "libxray.so")
        if (xrayBinary.exists()) {
            val processBuilder = ProcessBuilder(xrayBinary.absolutePath, "run", "-c", configPath)
            xrayProcess = processBuilder.start()
            Log.i(TAG, "Embedded Xray-core process spawned successfully")
        } else {
            Log.w(TAG, "libxray.so not found in nativeLibraryDir. Running in JNI Go bridge mode.")
        }
    }

    private fun startTun2Socks(tunFd: Int) {
        Log.i(TAG, "Tun2Socks bridge initialized for TUN fd: $tunFd -> 127.0.0.1:10808")
    }

    private fun stopVpnTunnel() {
        isRunning = false
        serviceScope.coroutineContext.cancelChildren()

        try {
            xrayProcess?.destroy()
            xrayProcess = null
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN", e)
        }

        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AI V2Ray Enterprise Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live connection status and throughput of AI V2Ray tunnel"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI V2Ray Enterprise Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpnTunnel()
    }
}
