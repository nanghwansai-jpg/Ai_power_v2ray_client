package com.aipower.v2ray

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.net.VpnService
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aipower.v2ray.core.agent.AgentLocalStore
import com.aipower.v2ray.core.agent.ServerCandidate
import com.aipower.v2ray.core.agent.SubscriptionDiscovery
import com.aipower.v2ray.core.security.EncryptedProfileVault
import com.aipower.v2ray.core.vpn.ImportedXrayProfile
import com.aipower.v2ray.core.vpn.XrayProfileParser
import com.aipower.v2ray.vpn.XrayVpnService
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private enum class Screen(val label: String) {
    HOME("Home"), SERVERS("Servers"), LOGS("Logs"), AGENT("AI Agent"), NETWORK("Network"), SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {
    private var pendingConfig: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = AgentLocalStore(this)
        setContent {
            var screen by remember { mutableStateOf(Screen.HOME) }
            var inputText by remember { mutableStateOf("") }
            var profile by remember { mutableStateOf<ImportedXrayProfile?>(null) }
            var status by remember { mutableStateOf("Ready · Local-first VPN control") }
            var busy by remember { mutableStateOf(false) }
            var connected by remember { mutableStateOf(false) }
            var apiKey by remember { mutableStateOf("") }
            var prompt by remember { mutableStateOf("") }
            var aiReply by remember { mutableStateOf("Gemini is not configured. Add your API key in Settings.") }
            var logs by remember { mutableStateOf(store.logs()) }
            var showDelete by remember { mutableStateOf(false) }
            var autoReconnect by remember { mutableStateOf(store.setting("auto_reconnect", "true") == "true") }
            var offlineMode by remember { mutableStateOf(store.setting("offline_mode", "true") == "true") }
            var startupAssistant by remember { mutableStateOf(store.setting("startup_assistant", "true") == "true") }
            var showStartupGoal by remember { mutableStateOf(store.setting("startup_goal_done", "false") != "true" && store.setting("startup_assistant", "true") == "true") }
            var dnsMode by remember { mutableStateOf(store.setting("dns_mode", "Cloudflare (1.1.1.1)") ) }
            var routingMode by remember { mutableStateOf(store.setting("routing_mode", "Global VPN")) }
            var routingPackages by remember { mutableStateOf(store.setting("routing_packages", "")) }
            var benchmarkLimit by remember { mutableStateOf(store.setting("benchmark_limit", "50")) }

            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val config = pendingConfig
                pendingConfig = null
                if (result.resultCode == Activity.RESULT_OK && config != null) {
                    startVpn(config)
                    connected = true
                    status = "VPN start requested. Confirm tunnel state from Android VPN indicator."
                    store.log("INFO", "VPN", "VPN start requested")
                    logs = store.logs()
                } else {
                    status = "VPN permission was not granted."
                    store.log("WARN", "VPN", status)
                    logs = store.logs()
                }
            }

            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFC8A7FF), secondary = Color(0xFFB9A2E8),
                background = Color(0xFF100E13), surface = Color(0xFF211E26),
                onSurface = Color(0xFFF1EDF5)
            )) {
                Scaffold(
                    containerColor = Color(0xFF100E13),
                    bottomBar = {
                        NavigationBar(containerColor = Color(0xFF1C1921)) {
                            Screen.values().forEach { item ->
                                NavigationBarItem(
                                    selected = screen == item,
                                    onClick = { screen = item },
                                    icon = { Text(when(item) {
                                        Screen.HOME -> "⌂"; Screen.SERVERS -> "◉"; Screen.LOGS -> "≋"
                                        Screen.AGENT -> "✧"; Screen.NETWORK -> "⇄"; Screen.SETTINGS -> "⚙"
                                    }) },
                                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("AI POWER", style = MaterialTheme.typography.labelMedium, color = Color(0xFFC8A7FF))
                                Text("V2Ray Client", style = MaterialTheme.typography.headlineSmall)
                            }
                            AssistChip(onClick = { screen = Screen.AGENT }, label = { Text("LOCAL AGENT") })
                        }
                        when (screen) {
                            Screen.HOME -> HomeScreen(
                                profile, status, connected, inputText, { inputText = it; profile = null },
                                { 
                                    val clip = getSystemService(ClipboardManager::class.java)?.primaryClip
                                    val pasted = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(this@MainActivity).toString() else ""
                                    if (pasted.isNotBlank()) { inputText = pasted; status = "Clipboard imported. Validate before connecting." }
                                    else status = "Clipboard is empty."
                                },
                                { 
                                    busy = true
                                    Thread {
                                        try {
                                            val parsed = XrayProfileParser().parse(inputText)
                                            runOnUiThread { profile = parsed; busy = false; status = "Validated: ${parsed.displayName} · ${parsed.outboundCount} outbound(s)"; store.log("INFO", "PROFILE", "Profile validated: ${parsed.displayName}"); logs = store.logs() }
                                        } catch (e: Exception) {
                                            runOnUiThread { busy = false; status = e.message ?: "Profile validation failed"; store.log("ERROR", "PROFILE", status); logs = store.logs() }
                                        }
                                    }.start()
                                }, busy,
                                { 
                                    val selected = profile
                                    if (selected != null) Thread {
                                        try { EncryptedProfileVault(this@MainActivity).save(selected); runOnUiThread { status = "Profile saved in encrypted vault."; store.log("INFO", "VAULT", "Encrypted profile saved"); logs = store.logs() } }
                                        catch (e: Exception) { runOnUiThread { status = e.message ?: "Vault save failed"; store.log("ERROR", "VAULT", status); logs = store.logs() } }
                                    }.start()
                                },
                                {
                                    Thread {
                                        try {
                                            val saved = EncryptedProfileVault(this@MainActivity).load()
                                            runOnUiThread { profile = saved; inputText = ""; status = if (saved == null) "No saved profile." else "Loaded: ${saved.displayName}" }
                                        } catch (e: Exception) { runOnUiThread { status = e.message ?: "Vault load failed" } }
                                    }.start()
                                },
                                { showDelete = true },
                                {
                                    val selected = profile
                                    if (selected == null) status = "Import and validate a profile first."
                                    else {
                                        val permission = VpnService.prepare(this@MainActivity)
                                        if (permission == null) { startVpn(selected.configJson); connected = true; status = "VPN start requested"; store.log("INFO", "VPN", "VPN start requested"); logs = store.logs() }
                                        else { pendingConfig = selected.configJson; permissionLauncher.launch(permission) }
                                    }
                                },
                                {
                                    startService(Intent(this@MainActivity, XrayVpnService::class.java).setAction(XrayVpnService.ACTION_STOP))
                                    connected = false; status = "Disconnect requested"; store.log("INFO", "VPN", "Disconnect requested"); logs = store.logs()
                                },
                                { screen = Screen.SETTINGS }, { screen = Screen.LOGS }
                            )
                            Screen.SERVERS -> ServersScreen(profile, { screen = Screen.HOME }, { selected -> profile = selected; screen = Screen.HOME; status = "Selected server: ${selected.displayName}"; store.log("INFO", "SERVER", "User selected benchmarked server: ${selected.displayName}"); logs = store.logs() }, benchmarkLimit)
                            Screen.LOGS -> LogsScreen(logs, { logs = store.logs() }, { store.clearLogs(); logs = emptyList() })
                            Screen.AGENT -> AgentScreen(offlineMode, autoReconnect, startupAssistant, aiReply, prompt, { prompt = it }, {
                                val question = prompt.trim()
                                val key = store.geminiKey()
                                if (key.isNullOrBlank()) { aiReply = "Gemini API key မသတ်မှတ်ရသေးပါ။ Settings မှာ ထည့်ပါ။" }
                                else if (question.isBlank()) { aiReply = "မေးခွန်းထည့်ပါ။" }
                                else {
                                    aiReply = "Gemini ကို မေးနေသည်…"
                                    Thread {
                                        val result = callGemini(key, question)
                                        runOnUiThread { aiReply = result; store.log(if (result.startsWith("Gemini error")) "ERROR" else "INFO", "GEMINI", if (result.startsWith("Gemini error")) "Gemini request failed" else "Gemini response received"); logs = store.logs() }
                                    }.start()
                                }
                            }, { screen = Screen.SETTINGS }, { store.log("INFO", "AGENT", "Local recovery diagnostics requested"); logs = store.logs(); aiReply = "Local Agent is active. No validated profile health probe is available yet; import a profile and run a connection test." })
                            Screen.NETWORK -> NetworkScreen(dnsMode, { dnsMode = it; store.putSetting("dns_mode", it); store.log("INFO", "NETWORK", "DNS preference set: $it"); logs = store.logs() }, routingMode, { routingMode = it; store.putSetting("routing_mode", it); store.log("INFO", "NETWORK", "Routing preference set: $it"); logs = store.logs() }, routingPackages, { routingPackages = it; store.putSetting("routing_packages", it) })
                            Screen.SETTINGS -> SettingsScreen(
                                apiKey, { apiKey = it }, { 
                                    runCatching { store.saveGeminiKey(apiKey.trim()); apiKey = ""; status = "Gemini key encrypted and saved in Android Keystore."; store.log("INFO", "SETTINGS", "Gemini API key updated") }
                                        .onFailure { status = "Could not save Gemini key securely."; store.log("ERROR", "SETTINGS", status) }
                                    logs = store.logs()
                                }, { store.deleteGeminiKey(); status = "Gemini key deleted."; store.log("INFO", "SETTINGS", "Gemini API key deleted"); logs = store.logs() },
                                store.hasGeminiKey(), autoReconnect, { autoReconnect = it; store.putSetting("auto_reconnect", it.toString()) },
                                offlineMode, { offlineMode = it; store.putSetting("offline_mode", it.toString()) },
                                startupAssistant, { startupAssistant = it; store.putSetting("startup_assistant", it.toString()) },
                                benchmarkLimit, { benchmarkLimit = it; store.putSetting("benchmark_limit", it) },
                                status
                            )
                        }
                    }
                }
            }
            if (showStartupGoal) {
                AlertDialog(
                    onDismissRequest = { showStartupGoal = false },
                    title = { Text("What will you use your VPN for?") },
                    text = { Text("Choose a goal. The local agent saves this preference and can use it when ranking verified profiles. It will not invent working servers.") },
                    confirmButton = {
                        Column {
                            listOf("General browsing", "Social media", "Video streaming", "Gaming · low latency", "Work · private access").forEach { goal ->
                                TextButton(onClick = {
                                    store.putSetting("startup_goal", goal)
                                    store.putSetting("startup_goal_done", "true")
                                    store.log("INFO", "AGENT", "Startup usage goal saved: $goal")
                                    logs = store.logs()
                                    status = "Usage goal saved: $goal"
                                    showStartupGoal = false
                                }) { Text(goal) }
                            }
                        }
                    },
                    dismissButton = { TextButton(onClick = { store.putSetting("startup_goal_done", "true"); showStartupGoal = false }) { Text("Skip") } }
                )
            }
            if (showDelete) {
                AlertDialog(onDismissRequest = { showDelete = false }, title = { Text("Delete saved profile?") },
                    text = { Text("This permanently removes the encrypted profile and its Keystore key.") },
                    confirmButton = { TextButton(onClick = {
                        showDelete = false
                        Thread { try { EncryptedProfileVault(this@MainActivity).delete(); runOnUiThread { status = "Encrypted profile deleted."; store.log("WARN", "VAULT", "Saved profile deleted"); logs = store.logs() } }
                            catch (e: Exception) { runOnUiThread { status = e.message ?: "Delete failed" } } }.start()
                    }) { Text("Delete") } },
                    dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } })
            }
        }
    }

    private fun startVpn(configJson: String) {
        val intent = Intent(this, XrayVpnService::class.java).setAction(XrayVpnService.ACTION_START)
            .putExtra(XrayVpnService.EXTRA_XRAY_JSON, configJson)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }
}

@Composable
private fun HomeScreen(
    profile: ImportedXrayProfile?, status: String, connected: Boolean, input: String, onInput: (String) -> Unit,
    paste: () -> Unit, validate: () -> Unit, busy: Boolean, save: () -> Unit, load: () -> Unit, delete: () -> Unit,
    connect: () -> Unit, disconnect: () -> Unit, settings: () -> Unit, logs: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF292530))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (connected) "● VPN START REQUESTED" else "○ VPN DISCONNECTED", color = if (connected) Color(0xFF80D6A5) else Color(0xFFC8A7FF), style = MaterialTheme.typography.titleMedium)
                Text(profile?.displayName ?: "No active profile", style = MaterialTheme.typography.headlineSmall)
                Text("Xray Core · libXray v26.11.9", color = Color.LightGray)
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = settings, modifier = Modifier.weight(1f)) { Text("VPN Settings") }
            OutlinedButton(onClick = logs, modifier = Modifier.weight(1f)) { Text("View Logs") }
        }
        Text("IMPORT SERVER PROFILE", color = Color(0xFFC8A7FF), style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(value = input, onValueChange = onInput, modifier = Modifier.fillMaxWidth(), minLines = 3,
            placeholder = { Text("VLESS / VMess / Trojan / Shadowsocks / Xray JSON") })
        OutlinedButton(onClick = paste, modifier = Modifier.fillMaxWidth()) { Text("Paste from clipboard") }
        Button(onClick = validate, enabled = input.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Validating…" else "Import & Validate") }
        if (profile != null) {
            Text("Validated: ${profile.displayName} · ${profile.outboundCount} outbound(s)", color = Color(0xFF80D6A5))
            OutlinedButton(onClick = save, modifier = Modifier.fillMaxWidth()) { Text("Save Encrypted Profile") }
            Button(onClick = connect, modifier = Modifier.fillMaxWidth()) { Text("Connect VPN") }
        }
        OutlinedButton(onClick = load, modifier = Modifier.fillMaxWidth()) { Text("Load Saved Profile") }
        OutlinedButton(onClick = delete, modifier = Modifier.fillMaxWidth()) { Text("Delete Saved Profile") }
        OutlinedButton(onClick = disconnect, modifier = Modifier.fillMaxWidth()) { Text("Disconnect VPN") }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ServersScreen(
    profile: ImportedXrayProfile?,
    goHome: () -> Unit,
    onSelect: (ImportedXrayProfile) -> Unit,
    limit: String
) {
    var subscriptionUrl by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<List<ServerCandidate>>(emptyList()) }
    var message by remember { mutableStateOf("Only scan subscription URLs you own or are authorized to use.") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Server Intelligence", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text("Current profile", style = MaterialTheme.typography.titleMedium)
            Text(profile?.displayName ?: "No selected profile")
            Text("Outbound count: ${profile?.outboundCount ?: 0}")
            Text("Benchmark limit: max 50 candidate endpoints")
        } }
        Text("HTTPS Subscription URL", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = subscriptionUrl,
            onValueChange = { subscriptionUrl = it.take(2048) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://provider.example/subscription") },
            singleLine = true
        )
        Text("The app fetches a bounded subscription, parses supported V2Ray links locally, and measures TCP connect latency to each public endpoint. TCP latency is not a full VPN speed test.")
        Button(
            enabled = !busy && subscriptionUrl.startsWith("https://"),
            onClick = {
                busy = true
                message = "Fetching subscription and benchmarking up to $limit endpoints…"
                Thread {
                    val outcome = runCatching {
                        run {
                            val connectivity = getSystemService(ConnectivityManager::class.java)
                            val physicalNetwork = connectivity.allNetworks.firstOrNull { network ->
                                val caps = connectivity.getNetworkCapabilities(network)
                                caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                    (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                            } ?: error("No underlying Wi-Fi or cellular network is available for safe direct benchmarking.")
                            SubscriptionDiscovery(physicalNetwork).discoverAndBenchmark(subscriptionUrl, limit.toIntOrNull()?.coerceIn(1, 50) ?: 50)
                        }
                    }
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        busy = false
                        outcome.onSuccess {
                            result = it
                            val reachable = it.count { candidate -> candidate.latencyMs != null }
                            message = "Tested ${it.size} candidates · ${reachable} TCP reachable · sorted by measured latency"
                        }.onFailure {
                            result = emptyList()
                            message = it.message ?: "Subscription discovery failed."
                        }
                    }
                }.start()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Testing servers…" else "Fetch & Benchmark Servers") }
        Text(message, style = MaterialTheme.typography.bodySmall)
        result.forEachIndexed { index, candidate ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("#${index + 1} · ${candidate.profile.displayName}", style = MaterialTheme.typography.titleMedium)
                    Text("${candidate.host}:${candidate.port}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        candidate.latencyMs?.let { "TCP latency: ${it} ms" } ?: (candidate.failure ?: "No measurement"),
                        color = if (candidate.latencyMs != null && candidate.latencyMs < 100) Color(0xFF80D6A5) else Color.LightGray
                    )
                    if (candidate.latencyMs != null) {
                        Button(onClick = { onSelect(candidate.profile) }, modifier = Modifier.fillMaxWidth()) { Text("Select this server") }
                    }
                }
            }
        }
        OutlinedButton(onClick = goHome, modifier = Modifier.fillMaxWidth()) { Text("Back to VPN") }
    }
}

@Composable
private fun LogsScreen(logs: List<String>, refresh: () -> Unit, clear: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Diagnostics & Logs", style = MaterialTheme.typography.headlineSmall)
            Text("${logs.size} events")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = refresh) { Text("Refresh") }
            OutlinedButton(onClick = clear) { Text("Clear") }
        }
        if (logs.isEmpty()) Text("No diagnostic events yet.", modifier = Modifier.padding(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(logs) { line ->
                Card(Modifier.fillMaxWidth()) { Text(line, Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun AgentScreen(
    offline: Boolean, reconnect: Boolean, startup: Boolean, reply: String, prompt: String, onPrompt: (String) -> Unit,
    ask: () -> Unit, settings: () -> Unit, diagnose: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Autonomous AI Agent", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("LOCAL AGENT · ON-DEVICE", color = Color(0xFF80D6A5))
            Text("Offline mode: ${if (offline) "Enabled" else "Disabled"}")
            Text("Auto reconnect preference: ${if (reconnect) "Enabled" else "Disabled"}")
            Text("Startup assistant: ${if (startup) "Enabled" else "Disabled"}")
            Text("Local rules remain available without Gemini. Gemini assistance requires internet access.")
        } }
        Text("Gemini Assistant", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = prompt, onValueChange = onPrompt, modifier = Modifier.fillMaxWidth(), minLines = 2, placeholder = { Text("Ask about VPN configuration or diagnostics…") })
        Button(onClick = ask, modifier = Modifier.fillMaxWidth()) { Text("Ask Gemini") }
        OutlinedButton(onClick = diagnose, modifier = Modifier.fillMaxWidth()) { Text("Run Local Diagnostics") }
        OutlinedButton(onClick = settings, modifier = Modifier.fillMaxWidth()) { Text("Gemini & Agent Settings") }
        Card(Modifier.fillMaxWidth()) { Text(reply, Modifier.padding(16.dp)) }
    }
}

@Composable
private fun NetworkScreen(
    dns: String, onDns: (String) -> Unit,
    routing: String, onRouting: (String) -> Unit,
    packages: String, onPackages: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Network Control", style = MaterialTheme.typography.headlineSmall)
        Text("DNS resolver · applied on next VPN connection")
        listOf("Cloudflare (1.1.1.1)", "Google (8.8.8.8)", "Quad9 (9.9.9.9)").forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = dns == item, onClick = { onDns(item) }); Text(item) }
        }
        Divider()
        Text("App routing · applied on next VPN connection")
        listOf("Global VPN", "Allowlist / Split tunnel", "Blocklist").forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = routing == item, onClick = { onRouting(item) }); Text(item) }
        }
        if (routing != "Global VPN") {
            OutlinedTextField(
                value = packages,
                onValueChange = onPackages,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("Android package names (comma separated)") },
                placeholder = { Text("com.android.chrome, org.telegram.messenger") }
            )
            Text(if (routing == "Allowlist / Split tunnel")
                "Only listed apps are routed into the VPN. An empty allowlist prevents tunnel startup."
                else "Listed apps are excluded from the VPN; all other apps use the tunnel.")
        }
        Text("Use package names from Android App info. Unknown package names cause VPN startup to fail safely.")
    }
}

@Composable
private fun SettingsScreen(
    apiKey: String, onApiKey: (String) -> Unit, saveKey: () -> Unit, deleteKey: () -> Unit, hasKey: Boolean,
    reconnect: Boolean, setReconnect: (Boolean) -> Unit, offline: Boolean, setOffline: (Boolean) -> Unit,
    startup: Boolean, setStartup: (Boolean) -> Unit, limit: String, setLimit: (String) -> Unit, status: String
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Text("Gemini API", style = MaterialTheme.typography.titleLarge)
        Text(if (hasKey) "API key is encrypted in Android Keystore storage." else "No Gemini API key saved.")
        OutlinedTextField(value = apiKey, onValueChange = onApiKey, modifier = Modifier.fillMaxWidth(),
            label = { Text("Gemini API key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        Button(onClick = saveKey, enabled = apiKey.length >= 10, modifier = Modifier.fillMaxWidth()) { Text("Encrypt & Save API Key") }
        OutlinedButton(onClick = deleteKey, enabled = hasKey, modifier = Modifier.fillMaxWidth()) { Text("Delete API Key") }
        Divider()
        Text("Agent behaviour", style = MaterialTheme.typography.titleLarge)
        SettingToggle("Auto reconnect preference", reconnect, setReconnect)
        SettingToggle("Offline recovery mode", offline, setOffline)
        SettingToggle("Ask usage goal on startup", startup, setStartup)
        Text("Node benchmark candidates")
        OutlinedTextField(value = limit, onValueChange = { value -> if (value.all(Char::isDigit) && value.length <= 3) setLimit(value) }, singleLine = true)
        Text("Allowed range: 5–100. Benchmarking remains disabled until a real network probe engine is integrated.")
        Divider()
        Text("VPN & Privacy", style = MaterialTheme.typography.titleLarge)
        Text("Profiles are stored in the encrypted Android Keystore vault. Diagnostic history is local and redacts common credentials.")
        Text(status, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun callGemini(apiKey: String, prompt: String): String {
    var connection: HttpURLConnection? = null
    return try {
        val endpoint = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
        connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 12000; readTimeout = 20000; doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", apiKey)
        }
        val body = JSONObject().put("contents", org.json.JSONArray().put(JSONObject()
            .put("parts", org.json.JSONArray().put(JSONObject().put("text", "You are a privacy-first Android VPN troubleshooting assistant. Never request credentials or recommend unauthorized network access bypass. User question: " + prompt.take(4000))))))
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText().take(16000) }
        if (connection.responseCode !in 200..299) "Gemini error (${connection.responseCode}): request failed. Check API key and network."
        else JSONObject(response).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")
            ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "Gemini returned no text."
    } catch (_: Exception) { "Gemini error: network request failed. Check internet access and API configuration." }
    finally { connection?.disconnect() }
}
