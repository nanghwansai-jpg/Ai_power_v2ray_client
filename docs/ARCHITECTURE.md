# Architecture

## Product
AI Power V2Ray Client is an Android VPN client built with Kotlin, Jetpack Compose, and an embedded Xray engine.

## Architecture principles
- Clean Architecture with feature-oriented UI modules.
- Android VPNService owns the OS tunnel lifecycle; the XTLS/libXray gomobile adapter owns Xray-core lifecycle, TUN FD handoff, socket protection, and Android DNS resolver integration. The adapter is pinned to libXray v26.11.9.
- Room stores structured local data; DataStore stores preferences.
- Android Keystore protects encryption keys.
- Deterministic policy rules govern autonomous actions. AI may recommend actions but cannot execute arbitrary shell commands or code.
- Cloudflare services are optional and must not be required for core VPN operation.

## Planned modules
- `android/app`: application entry point and dependency composition.
- `android/core/domain`: domain models and use cases.
- `android/core/data`: repositories and persistence.
- `android/core/vpn`: VPNService and Xray adapter boundary.
- `android/core/security`: encryption and integrity verification.
- `android/core/network`: DNS and network state.
- `android/core/ai`: policy engine and AI gateway.
- `android/feature/*`: Compose UI features.
- `cloudflare/*`: optional backend services.
- `contracts/*`: shared API contracts.
