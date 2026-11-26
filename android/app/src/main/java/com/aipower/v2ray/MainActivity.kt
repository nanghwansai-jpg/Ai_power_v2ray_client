package com.aipower.v2ray

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipower.v2ray.core.security.EncryptedProfileVault
import com.aipower.v2ray.core.vpn.ImportedXrayProfile
import com.aipower.v2ray.core.vpn.XrayProfileParser
import com.aipower.v2ray.vpn.XrayVpnService

class MainActivity : ComponentActivity() {
    private var pendingConfig: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var inputText by remember { mutableStateOf("") }
            var profile by remember { mutableStateOf<ImportedXrayProfile?>(null) }
            var status by remember { mutableStateOf("Paste a supported V2Ray/Xray link or JSON profile.") }
            var busy by remember { mutableStateOf(false) }
            var showDeleteConfirmation by remember { mutableStateOf(false) }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val config = pendingConfig
                pendingConfig = null
                if (result.resultCode == Activity.RESULT_OK && config != null) {
                    startVpn(config)
                    status = "VPN start requested. Check the persistent notification for connection status."
                } else {
                    status = "VPN permission was not granted."
                }
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = "AI Power V2Ray Client",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Xray Core • libXray v26.11.9")
                        Spacer(Modifier.height(16.dp))

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("VPN engine")
                                Text("Android VpnService + XTLS/libXray")
                                Text("State: service lifecycle integrated")
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Import server profile",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        TextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                profile = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 7,
                            placeholder = {
                                Text("VLESS, VMess AEAD, Trojan, Shadowsocks, SOCKS, Hysteria2, or Xray JSON")
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                busy = true
                                profile = null
                                status = "Parsing and validating profile locally..."
                                val raw = inputText
                                Thread {
                                    try {
                                        val imported = XrayProfileParser().parse(raw)
                                        runOnUiThread {
                                            profile = imported
                                            busy = false
                                            status = "Imported ${imported.displayName} • ${imported.outboundCount} outbound(s)"
                                        }
                                    } catch (error: Exception) {
                                        runOnUiThread {
                                            busy = false
                                            status = error.message ?: "Profile import failed."
                                        }
                                    }
                                }.start()
                            },
                            enabled = inputText.isNotBlank() && !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (busy) "Validating..." else "Import and validate")
                        }

                        OutlinedButton(
                            onClick = {
                                Thread {
                                    try {
                                        val saved = EncryptedProfileVault(this@MainActivity).load()
                                        runOnUiThread {
                                            if (saved == null) {
                                                status = "No saved profile was found in the encrypted local vault."
                                            } else {
                                                profile = saved
                                                inputText = ""
                                                status = "Loaded encrypted profile: ${saved.displayName}"
                                            }
                                        }
                                    } catch (error: Exception) {
                                        runOnUiThread {
                                            status = error.message ?: "Could not decrypt the saved profile."
                                        }
                                    }
                                }.start()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Load saved profile")
                        }

                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete saved profile")
                        }

                        if (showDeleteConfirmation) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirmation = false },
                                title = { Text("Delete saved profile?") },
                                text = { Text("This removes the encrypted profile file and its Android Keystore key. This action cannot be undone.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showDeleteConfirmation = false
                                            Thread {
                                                try {
                                                    EncryptedProfileVault(this@MainActivity).delete()
                                        runOnUiThread { status = "Saved encrypted profile deleted." }
                                                } catch (error: Exception) {
                                                    runOnUiThread {
                                                        status = error.message ?: "Could not delete the saved profile."
                                                    }
                                                }
                                            }.start()
                                        }
                                    ) {
                                        Text("Delete")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirmation = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        profile?.let { imported ->
                            Spacer(Modifier.height(8.dp))
                            Text("Selected: ${imported.displayName}")
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    Thread {
                                        try {
                                            EncryptedProfileVault(this@MainActivity).save(imported)
                                            runOnUiThread {
                                                status = "Profile encrypted and saved locally in Android Keystore vault."
                                            }
                                        } catch (error: Exception) {
                                            runOnUiThread {
                                                status = error.message ?: "Could not save encrypted profile."
                                            }
                                        }
                                    }.start()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save securely")
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val prepareIntent = VpnService.prepare(this@MainActivity)
                                    if (prepareIntent == null) {
                                        startVpn(imported.configJson)
                                        status = "VPN start requested. Check the persistent notification."
                                    } else {
                                        pendingConfig = imported.configJson
                                        permissionLauncher.launch(prepareIntent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Connect")
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                startService(
                                    Intent(this@MainActivity, XrayVpnService::class.java)
                                        .setAction(XrayVpnService.ACTION_STOP)
                                )
                                status = "VPN stop requested."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disconnect")
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Profiles are parsed locally. Saved profiles use AES-256-GCM with a non-exportable Android Keystore key and remain in app-private no-backup storage. QR scanning, subscription URL fetching, and live tunnel diagnostics are next-stage work.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    private fun startVpn(configJson: String) {
        val intent = Intent(this, XrayVpnService::class.java)
            .setAction(XrayVpnService.ACTION_START)
            .putExtra(XrayVpnService.EXTRA_XRAY_JSON, configJson)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
