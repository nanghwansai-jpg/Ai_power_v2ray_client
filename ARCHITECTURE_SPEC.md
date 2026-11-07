# Enterprise AI V2Ray / Xray System Architecture Specification
**Author:** AI Engineering & Core Architecture Team (Victor Geek)  
**Classification:** Enterprise Production-Ready Specification  
**Version:** 3.0.0-ENTERPRISE  
**Target:** Linux / Docker / Cloud & Native Android Mobile

---

## 1. Executive Summary & Architecture Overview

The **Enterprise AI V2Ray Client** is an industrial-grade proxy management and censorship circumvention system designed to deliver sub-100ms ultra-low latency routing, zero-downtime auto-failover, and deep packet inspection (DPI) evasion.

The system integrates four tightly coupled architectural layers:
1. **Presentation & Monitoring Layer:** Industrial Precision UI (React + Vite + Tailwind CSS + Lucide Icons) & Android Jetpack Compose Native Interface.
2. **Enterprise Orchestration & Backend Layer:** High-concurrency Node.js/TypeScript Express server with SSE/WebSocket real-time telemetry streaming and local persistent database.
3. **Autonomous Background & Probing Engine:** Multi-threaded/asynchronous background worker managing real TCP socket latency probing, packet loss metrics, periodic node scraping, and sub-second auto-failover.
4. **Hybrid AI Engine (Local Edge + Cloud AI):**
   - **Local Edge AI:** Zero-latency autonomous node scoring, censorship bypass rule matcher, anomaly detection, and local LLM (Ollama) integration.
   - **Cloud AI:** Gemini AI deep web discovery, DPI evasion parameter synthesizer, and multi-protocol subscription parsing.
5. **Core Execution Layer (Xray-Core Runtime):** Native Xray-Core 64-bit daemon managing dual local inbounds (SOCKS5 `:10808`, HTTP `:10809`), gRPC Statistics API (`:10085`), and outbound routing for VLESS (Reality/Vision), VMess, Trojan, Shadowsocks, and Hysteria2.

---

## 2. Complete System Component Diagram

```
+-----------------------------------------------------------------------------------+
|                        ENTERPRISE CLIENT USER INTERFACES                          |
|  [Web UI (Burnished Copper)]          [Android Native UI (Jetpack Compose)]       |
|  - Real-time Latency Radar            - VpnService Tunnel Controller              |
|  - In-Band Throughput Graphs          - Material3 Industrial Aesthetic            |
|  - One-Click Instant Switch           - Sub-100ms Filter & Sort                   |
+------------------------------------------+----------------------------------------+
                                           | HTTP / REST & SSE / IPC
                                           v
+-----------------------------------------------------------------------------------+
|                           ENTERPRISE BACKEND SERVICE                              |
|  [Express 4.x / Node.js Engine]                                                   |
|  * REST Endpoints (/api/nodes, /api/tunnel/connect, /api/metrics, /api/ai/intent) |
|  * Server-Sent Events (/api/tunnel/stream) for live telemetry                     |
|  * Persistent Store (SQLite / JSON Atomic Store) for nodes & history              |
+------------------------------------------+----------------------------------------+
                                           |
         +---------------------------------+--------------------------------+
         |                                                                  |
         v                                                                  v
+-----------------------------------------------+  +-----------------------------------------------+
|      AUTONOMOUS BACKGROUND ENGINE             |  |           HYBRID AI ENGINE                    |
|  * Real TCP Latency & Jitter Prober           |  |  [Local Edge Engine]                          |
|  * Sub-100ms Strict Filter & Ascending Sorter |  |  - Autonomous Node Health & Score Evaluator   |
|  * Failover Watchdog (Heartbeat every 15s)    |  |  - Anomaly & Packet Loss Auto-Mitigator       |
|  * Live Bandwidth Meter (Tx/Rx bps via gRPC)  |  |  - Local LLM Support (Ollama / Local API)     |
|  * Periodic Subscription Auto-Sync            |  |  [Cloud Intelligence Engine]                  |
+-----------------------+-----------------------+  |  - Gemini 2.5 Flash Deep Researcher           |
                        |                          |  - Dynamic Reality SNI & Camouflage Tuner    |
                        v                          +-----------------------------------------------+
+-----------------------------------------------+
|             XRAY CORE DAEMON                  |
|  * Native Executable Process (PID management) |
|  * Dual Inbounds: SOCKS5 (:10808), HTTP(:10809)
|  * Stats gRPC API (:10085)                    |
|  * Outbounds: VLESS+Reality, VMess, Trojan,   |
|               Shadowsocks, Hysteria2          |
+-----------------------------------------------+
```

---

## 3. Core Protocols & Port Specifications

| Protocol / Component | Inbound / Outbound | Default Port | Security / Mode | Purpose |
|---|---|---|---|---|
| **SOCKS5 Inbound** | Local Inbound | `10808` | No auth (Localhost) | Local browser / app proxying |
| **HTTP Inbound** | Local Inbound | `10809` | No auth (Localhost) | CLI / HTTP proxy clients |
| **gRPC Stats API** | Internal | `10085` | Dokodemo-door | Traffic metrics & session counts |
| **VLESS** | Outbound | Variable (443/8443) | Reality / TLS (Vision) | DPI-resistant stealth tunnel |
| **Trojan** | Outbound | 443 | TLS / WebSockets | Low-latency gaming & streaming |
| **VMess** | Outbound | 443 | WebSocket + TLS | High-concurrency content access |
| **Hysteria2** | Outbound | UDP variable | QUIC / Salamander | High packet-loss UDP acceleration |

---

## 4. Background Probing & Sub-100ms Sorting Logic

1. **Active Probing Mechanism:**
   - Every node is probed via high-speed asynchronous TCP socket connect to `host:port` with a timeout of 1500ms.
   - For Reality/TLS nodes, a TLS ClientHello probe measures total TCP + TLS handshake round-trip time (RTT).
2. **Strict Quality Gates:**
   - Strict condition: Latency <= 100 ms.
   - Nodes with Latency > 100 ms or connection timeouts are flagged and excluded from the recommended tier.
   - Sorting order: Ascending order by latency (P0 <= P1 <= P2 ... <= Pn).
3. **Auto-Failover Logic:**
   - The Background Watchdog polls the currently active node every 15 seconds.
   - If 2 consecutive heartbeat probes fail or latency degrades severely, the Local Decision Engine automatically selects the top-ranked healthy node (P0), regenerates the Xray config, gracefully reloads the Xray daemon, and emits an event to the UI without user intervention.

---

## 5. Android Enterprise Native Architecture

For mobile Android deployment:
- **VpnService Architecture:** `AiV2RayVpnService.kt` creates a virtual network interface (`tun0`) intercepting all device IP traffic.
- **Tun2Socks Pipeline:** Packets from `tun0` are routed through `tun2socks` into the local SOCKS5 inbound port (`127.0.0.1:10808`).
- **Core Engine:** Precompiled Go binary (`libv2ray.so` / `libxray.aar`) controlled via JNI or native subprocess.
- **UI:** Jetpack Compose industrial interface matching the exact burnished copper and machined gunmetal design specs.

