# AI Power V2Ray Client — Product Architecture

## Product intent
An Android-first, local-first VPN client in which an on-device agent manages the Xray/libXray engine and recommends the best verified connection for the user's stated purpose. Gemini is an optional reasoning and explanation service; the VPN must remain usable without Gemini or backend connectivity.

## Non-negotiable principles
- Android `VpnService` owns TUN creation, foreground lifecycle, user consent, and tunnel teardown.
- Pinned XTLS/libXray owns protocol execution. The app uses the typed Native V4 contract; upstream V3 compatibility remains intact.
- The local agent is deterministic and policy constrained. Gemini may rank or explain bounded candidate actions, but never receives arbitrary shell execution, unrestricted device control, raw profile secrets, or permission to silently change Android security settings.
- User intent, OS permission, and explicit confirmation govern connection, per-app routing, and any behavior that changes device traffic.
- No fabricated server, ping, availability, or connection-success claims. Measurements must include method, timestamp, network, and result.
- Public nodes and subscriptions are untrusted input. Accept HTTPS subscription sources, cap response sizes, validate each profile locally, and never treat a public share node as trusted.
- No traffic interception, credential harvesting, unauthorized access, carrier billing circumvention, or exploit-based “bug host” behavior. SNI/TLS settings are only for legitimate server interoperability and must not impersonate unrelated services or bypass access controls.
- Cloudflare is an optional control plane. Local VPN operation, profile vault, diagnostics, and deterministic recovery continue offline.

## Runtime architecture

### Android client
- **Presentation:** Jetpack Compose dark UI; Home, Servers, Logs, AI Agent, Network, Settings.
- **VPN engine:** Android `VpnService` + pinned XTLS/libXray v26.11.9 + Native V4 request/response validation.
- **Local agent:** observes service lifecycle, network changes, profile state, failures, and health checks; evaluates deterministic recovery policies; stores a bounded redacted event history.
- **Profile vault:** AES-GCM encrypted profile persistence backed by a non-exportable Android Keystore key, with crash-safe replacement/recovery.
- **Network policy:** DNS selection, global/split routing, per-app allow/block lists, IPv4/IPv6 handling, MTU, and network-specific tuning. Changes must be applied through the VPN builder and tested against real device traffic.
- **Server intelligence:** imports user-authorized HTTPS subscriptions and public share sources; parses locally; validates endpoints; benchmarks candidates on the underlying physical Wi-Fi/cellular network; ranks measured results; user chooses or explicitly enables a policy-based auto-select rule.
- **AI gateway:** Gemini API through a bounded client. Send only minimized diagnostics and structured, redacted facts. API keys remain encrypted locally. No key or raw config in logs.
- **Offline mode:** local policies, saved profiles, cached candidate metadata, diagnostics, and reconnection state remain available. Gemini explanations and remote source refresh pause until connectivity returns.

### Optional Cloudflare control plane
- **Workers:** authenticated API gateway, source registry, update metadata, report intake.
- **D1:** account/device consent metadata, source catalog, release metadata, aggregated non-sensitive health statistics.
- **KV:** short-lived config/policy cache and rate-limit state.
- **R2:** signed manifests, public metadata snapshots, crash bundles only after redaction and user consent.
- **Queues:** asynchronous crash/health report processing and daily source refresh.
- **Security:** per-device scoped tokens, rate limits, request signatures, replay protection, retention limits, audit trail, signed release manifests. Backend outage must not block local VPN use.

## AI and local-agent control contract
1. At app launch, ask the user's usage goal (general browsing, messaging/social, streaming, gaming/low-latency, or work/private access). Do not start a tunnel before Android VPN consent.
2. Convert the goal into explicit measurable weights: endpoint reachability, TCP connect latency, proxy handshake, packet loss, DNS success, throughput, stability, region preference, and data cost.
3. Discover only configured/authorized sources. Refresh source metadata on a daily schedule when online; enforce source allowlists, HTTPS, size/time limits, deduplication, and provenance.
4. Parse and validate candidates locally. Reject malformed, unsupported, expired, unsafe, or policy-incompatible configurations.
5. Probe no more than 50 candidates per batch by default. A TCP connect measurement is only a preliminary reachability metric; final ranking requires a real proxy handshake and tunnel-level DNS/HTTPS/packet-loss checks.
6. Rank candidates using measured values, not a hard-coded “under 100 ms” claim. Maintain a short list and fallback order with per-network history.
7. Apply approved DNS/routing/MTU settings, connect through the Native V4 API, and verify actual tunnel traffic before reporting connected.
8. Observe network changes and failures. Retry with bounded exponential backoff, rotate only to a previously validated candidate, preserve user-selected exclusions, and stop retrying after a bounded failure budget.
9. On crash or severe failure, write a redacted local diagnostic bundle. Upload to the developer endpoint and Gemini only with consent, using a report ID and app/core/device versions; never upload profiles, UUIDs, tokens, API keys, decrypted configs, or browsing history.
10. On loss of Internet/backend access, continue local recovery and show honest offline state. Never claim the agent can create Internet access where the underlying network has none.

## Server discovery and measurement semantics
- Inputs: user-entered HTTPS subscription URL, manually imported share links, or administrator-configured public source list.
- Candidate cap: 500 parsed entries per subscription; at most 50 network probes per batch.
- Subscription download cap: 2 MiB, HTTPS-only including redirects, bounded timeouts.
- Endpoint checks must use the underlying physical network, not recursively route through the VPN being benchmarked.
- Current implementation measures TCP connection establishment to the advertised public endpoint. It is not an Xray handshake, HTTP reachability, throughput, or end-to-end VPN latency measurement.
- Future complete score must include proxy handshake, tunnel establishment, DNS resolution, HTTPS request, packet loss, throughput, and stability over repeated samples.
- A candidate below 100 ms is not automatically “best”; user goal and other measurements matter.

## Self-healing policy
- **Recoverable:** temporary DNS failure, transient TCP failure, network handover, core stopped unexpectedly. Try bounded reconnect to last known-good candidate, then next ranked candidate.
- **Non-recoverable/security:** invalid signature, profile authentication failure, parser contract violation, vault authentication failure, revoked VPN permission, suspected tampering. Fail closed; do not silently downgrade or weaken security.
- Use jittered exponential backoff, max attempts per window, cooldown, circuit breaker, and a user-visible event trail.
- Repeated crashes produce a redacted report and disable automatic retry until a safe recovery threshold is met.

## Release and update architecture
- App releases: signed APK/AAB with protected release key, signed manifest, version/rollback policy.
- Xray core: independently updateable only as a signed, hash-verified, ABI-compatible artifact; retain last-known-good core and automatic rollback.
- Backend policy/source metadata: signed and versioned; reject rollback and expired manifests.
- CI: pinned upstream libXray, native tests, JNI signature checks, Kotlin tests, debug/release builds, DEX test-hook exclusion, provenance and artifact hashes.
- Production release remains blocked until physical-device, native fault-injection, process-kill vault recovery, signing, and current-head CI gates pass.

## Current implementation status (2026-11-27)
- **Implemented:** Android Compose client shell and navigation; Xray/libXray V4 integration; encrypted profile vault; Gemini key encryption and basic Gemini request; local redacted diagnostic history; startup usage-goal prompt; user-supplied HTTPS subscription download; local parsing; up to 50 TCP endpoint probes bound to underlying Wi-Fi/cellular; latency-sorted candidate selection.
- **Partial:** Gemini is currently a direct request/response assistant, not an autonomous policy planner. Startup goal is stored but does not yet weight a complete scoring model. Core start/stop logs exist, but comprehensive crash capture and consented developer reporting do not. DNS resolver selection and per-app allowlist/blocklist are now applied when the next VPN tunnel is established; settings are not hot-applied to an already-running tunnel.
- **Not yet implemented:** daily scheduled public-source ingestion; signed source registry; true proxy-handshake and tunnel health benchmark; automatic best-node selection; adaptive network tuning; offline self-healing state machine; Cloudflare Workers/D1/KV/R2/Queues deployment; signed remote core update and rollback.
- **Release gates still open:** physical Android device matrix; JNI fault injection through the real boundary; OS process-kill vault recovery; protected production signing; exact candidate SHA release verification.
