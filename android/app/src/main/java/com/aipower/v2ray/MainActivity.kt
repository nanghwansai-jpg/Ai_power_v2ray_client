package com.aipower.v2ray

import android.app.Activity
import android.content.Intent
import android.net.VpnService
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var permissionStatus by remember {
                mutableStateOf("VPN permission has not been checked")
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                permissionStatus = if (result.resultCode == Activity.RESULT_OK) {
                    "VPN permission granted. A server profile is still required."
                } else {
                    "VPN permission was not granted."
                }
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "AI Power V2Ray Client",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Xray Core integration foundation")
                        Spacer(Modifier.height(20.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("VPN engine")
                                Text("Android VpnService + XTLS/libXray adapter")
                                Spacer(Modifier.height(8.dp))
                                Text("Engine version: libXray v26.11.9")
                                Spacer(Modifier.height(8.dp))
                                Text("Connection: Not connected")
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val prepareIntent = VpnService.prepare(this@MainActivity)
                                if (prepareIntent == null) {
                                    permissionStatus =
                                        "VPN permission already granted. A server profile is still required."
                                } else {
                                    permissionLauncher.launch(prepareIntent)
                                }
                            }
                        ) {
                            Text("Check VPN permission")
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = permissionStatus,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "Import and validate a server profile before connecting. " +
                                "The app will not create a tunnel without a valid Xray configuration.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
