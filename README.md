# AI Power V2Ray Client

Android VPN client project using Kotlin, Jetpack Compose, and a modular Xray integration architecture.

## Current status
- Repository foundation and architecture documentation initialized.
- Android application foundation and VPN permission preflight added.
- Android VPNService lifecycle and a pinned XTLS/libXray v26.11.9 runtime adapter added.
- CI is configured to build the native libXray AAR before the Android app.
- Basic local share-link/Xray JSON import and Connect/Disconnect controls are added.
- A single-profile local vault now supports explicit save/load/delete using AES-256-GCM and a non-exportable Android Keystore key; ciphertext stays in app-private no-backup storage.
- QR/subscription import, multi-profile Room management, verified end-to-end TUN traffic forwarding, Cloudflare backend, and device-level leak tests remain incomplete.

## Requirements
- Android Studio with a compatible JDK.
- Android SDK 36.
- Gradle wrapper must be generated or added before command-line builds can be run.

## Build
Open the repository in Android Studio and sync Gradle after configuring the required Android SDK. Build verification and device validation are still pending; do not treat the current source as a production-ready VPN or distribute an APK until CI and real-device tests pass.

## Project documentation
- [Architecture](docs/ARCHITECTURE.md)
- [Delivery roadmap](docs/ROADMAP.md)
- [Security policy](docs/SECURITY.md)
- [Build and release guide](docs/BUILD_RELEASE.md)
- [Xray integration](docs/XRAY_INTEGRATION.md)
