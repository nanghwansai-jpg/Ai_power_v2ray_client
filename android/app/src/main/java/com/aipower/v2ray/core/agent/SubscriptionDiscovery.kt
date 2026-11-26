package com.aipower.v2ray.core.agent

import android.util.Base64
import android.net.Network
import com.aipower.v2ray.core.vpn.ImportedXrayProfile
import com.aipower.v2ray.core.vpn.XrayProfileParser
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class ServerCandidate(
    val profile: ImportedXrayProfile,
    val host: String,
    val port: Int,
    val latencyMs: Long?,
    val failure: String?
)

/**
 * Fetches only a user-supplied HTTPS subscription. Candidate latency is a TCP
 * connect measurement to the advertised endpoint, not a promise of proxy
 * handshake, throughput, censorship circumvention, or Internet access.
 */
class SubscriptionDiscovery(
    private val underlyingNetwork: Network,
    private val parser: XrayProfileParser = XrayProfileParser()
) {
    fun discoverAndBenchmark(subscriptionUrl: String, limit: Int = 50): List<ServerCandidate> {
        require(limit in 1..50) { "Benchmark limit must be between 1 and 50." }
        val body = fetchHttps(subscriptionUrl)
        val decoded = decodeSubscription(body)
        val entries = extractEntries(decoded).take(500)
        require(entries.isNotEmpty()) { "Subscription contained no supported server profiles." }

        val candidates = entries.mapNotNull { entry ->
            runCatching {
                val profile = parser.parse(entry)
                val endpoint = endpoint(profile.configJson) ?: return@runCatching null
                ServerCandidate(profile, endpoint.first, endpoint.second, null, null)
            }.getOrNull()
        }.distinctBy { "${it.host.lowercase()}: ${it.port}|${it.profile.configJson.hashCode()}" }
            .take(limit)
        require(candidates.isNotEmpty()) { "No supported public server endpoints were found." }

        val pool = Executors.newFixedThreadPool(8)
        return try {
            val tasks = candidates.map { candidate ->
                pool.submit(Callable { probe(candidate) })
            }
            tasks.map { it.get(4, TimeUnit.SECONDS) }
                .sortedWith(compareBy<ServerCandidate> { it.latencyMs == null }.thenBy { it.latencyMs ?: Long.MAX_VALUE })
        } finally {
            pool.shutdownNow()
        }
    }

    private fun fetchHttps(raw: String): String {
        var current = URL(raw)
        require(current.protocol.equals("https", true)) { "Only HTTPS subscription URLs are accepted." }
        repeat(4) { redirect ->
            val connection = (current.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "text/plain, application/json, */*")
                setRequestProperty("User-Agent", "AIPowerV2RayClient/0.1")
            }
            try {
                val code = connection.responseCode
                if (code in 300..399) {
                    require(redirect < 3) { "Subscription redirected too many times." }
                    val location = connection.getHeaderField("Location")
                        ?: error("Subscription redirect omitted its destination.")
                    current = URL(current, location)
                    require(current.protocol.equals("https", true)) { "Subscription redirected to an insecure URL." }
                } else {
                    require(code in 200..299) { "Subscription server returned HTTP $code." }
                    val output = ByteArrayOutputStream()
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= MAX_BYTES) { "Subscription exceeds the 2 MiB limit." }
                            output.write(buffer, 0, count)
                        }
                    }
                    return output.toString(Charsets.UTF_8.name())
                }
            } finally {
                connection.disconnect()
            }
        }
        error("Subscription redirect processing failed.")
    }

    private fun decodeSubscription(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") || trimmed.contains("://")) return trimmed
        val compact = trimmed.filterNot(Char::isWhitespace)
        val bytes = runCatching { Base64.decode(compact, Base64.DEFAULT) }.getOrNull()
            ?: runCatching { Base64.decode(compact, Base64.URL_SAFE or Base64.NO_WRAP) }.getOrNull()
        val decoded = bytes?.toString(Charsets.UTF_8)?.takeIf { it.contains("://") || it.trimStart().startsWith("{") }
        return decoded ?: trimmed
    }

    private fun extractEntries(body: String): List<String> {
        val trimmed = body.trim()
        if (trimmed.startsWith("{")) return listOf(trimmed)
        return trimmed.lineSequence().map { it.substringBefore('#').trim() }
            .filter { it.startsWith("vless://", true) || it.startsWith("vmess://", true) ||
                it.startsWith("trojan://", true) || it.startsWith("ss://", true) ||
                it.startsWith("socks://", true) || it.startsWith("hy2://", true) ||
                it.startsWith("hysteria2://", true) }
            .distinct().toList()
    }

    private fun endpoint(configText: String): Pair<String, Int>? {
        val root = JSONObject(configText)
        val outbounds = root.optJSONArray("outbounds") ?: return null
        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            val settings = outbound.optJSONObject("settings")
            val vnext = settings?.optJSONArray("vnext")?.optJSONObject(0)
            val servers = settings?.optJSONArray("servers")?.optJSONObject(0)
            val host = vnext?.optString("address")?.takeIf { it.isNotBlank() }
                ?: servers?.optString("address")?.takeIf { it.isNotBlank() }
                ?: outbound.optString("server").takeIf { it.isNotBlank() }
                ?: outbound.optString("address").takeIf { it.isNotBlank() }
                ?: continue
            val port = vnext?.optInt("port", 0)?.takeIf { it > 0 }
                ?: servers?.optInt("port", 0)?.takeIf { it > 0 }
                ?: outbound.optInt("port", 0).takeIf { it > 0 }
                ?: continue
            if (host.length > 253 || host.any { it.isWhitespace() || it == '/' || it == '@' }) continue
            val address = runCatching { underlyingNetwork.getAllByName(host).firstOrNull { isPublicAddress(it) } }.getOrNull()
                ?: continue
            return address.hostAddress to port
        }
        return null
    }

    private fun probe(candidate: ServerCandidate): ServerCandidate {
        return try {
            val start = System.nanoTime()
            Socket().use { socket ->
                underlyingNetwork.bindSocket(socket)
                socket.connect(InetSocketAddress(candidate.host, candidate.port), CONNECT_TIMEOUT_MS)
            }
            candidate.copy(latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start))
        } catch (_: Exception) {
            candidate.copy(failure = "TCP connect failed or timed out")
        }
    }

    private fun isPublicAddress(address: InetAddress): Boolean =
        !address.isAnyLocalAddress && !address.isLoopbackAddress &&
            !address.isLinkLocalAddress && !address.isSiteLocalAddress &&
            !address.isMulticastAddress

    companion object {
        private const val MAX_BYTES = 2 * 1024 * 1024
        private const val CONNECT_TIMEOUT_MS = 1800
    }
}
