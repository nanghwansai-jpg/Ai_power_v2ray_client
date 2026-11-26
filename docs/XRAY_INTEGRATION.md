# Xray Core and Android VPN Integration

## Selected engine
The Android client uses the official XTLS/libXray gomobile wrapper, pinned to **v26.11.9**. That wrapper embeds the matching Xray-core release and exposes a structured JSON entry point:
- `testXray` validates an Xray JSON configuration.
- `runXray` starts the managed core.
- `stopXray` stops it.
- Android socket protection is registered through the Android wrapper controller.
- Android DNS is configured through the wrapper before the core starts.

Upstream references:
- [XTLS/libXray](https://github.com/XTLS/libXray)
- [libXray v26.11.9 release](https://github.com/XTLS/libXray/releases/tag/v26.11.9)
- [Android VPN guide](https://developer.android.com/develop/connectivity/vpn)

The upstream wrapper warns that its API is not guaranteed stable and is intended to match the latest Xray-core release. Keep the wrapper pinned and update it only with an explicit compatibility review.

## Native library build
The app does not commit generated native binaries. CI builds the Android AAR from the pinned upstream source using its recommended build script:
```bash
LIBXRAY_VERSION=v26.11.9 bash scripts/build-libxray-android.sh
```
Prerequisites: Git, Go, Python 3, Android SDK/NDK, and the environment variables expected by gomobile. The script writes the generated AAR to `android/app/libs/libXray.aar`.

The Gradle app module consumes AAR files in `android/app/libs`. If the AAR is absent, the Kotlin sources still compile because the adapter uses reflection, but runtime VPN startup fails closed with an engine-unavailable error.

## Runtime sequence
1. The user grants Android VPN consent through `VpnService.prepare()`.
2. The app supplies a validated Xray JSON profile to `XrayVpnService`.
3. The service registers the Xray socket-protection callback. Tunnel sockets must be protected to avoid routing loops.
4. The service configures libXray's Android DNS resolver before starting the core.
5. The service validates the supplied configuration with `testXray`.
6. Android creates a TUN interface; its file descriptor is inserted at `env.xray.tun.fd` as required by the upstream wrapper.
7. The service invokes `runXray`, maintains a foreground notification, and calls `stopXray` / resets DNS on shutdown or VPN revocation.

## Current limitations
- The UI now accepts supported share-link text or Xray JSON, parses it locally, and exposes Connect/Disconnect controls. Encrypted profile persistence, QR scanning, and subscription URL fetching are not implemented.
- The service expects a valid, complete Xray JSON profile in its start intent; the UI converts supported share links to that JSON before starting it. Subscription URL fetching and QR import remain future work.
- IPv4 and IPv6 default routes are configured. IPv6 reachability, DNS leak behavior, socket protection, and traffic forwarding still require device-level tests.
- The runtime adapter discovers gomobile-generated Java methods reflectively. The generated class/method names and native callback behavior still need runtime verification; a successful Gradle compile alone does not prove compatibility.
- The repository has not yet demonstrated a successful Android build, Xray startup, or real traffic tunnel. Do not describe the client as production-ready until those checks pass.
- Do not load another independently built Go/cgo/gomobile runtime into this Android process. Upstream libXray documents that multiple Go runtimes in one process can cause build, load, or runtime failures.

## Safety behavior
- The app does not establish a TUN interface without a supplied profile.
- Missing native library, missing socket-protection API, missing DNS-protection API, invalid configuration, or failed core startup stops the service instead of reporting a connected state. On teardown, the socket callback is replaced with a detached fail-closed callback to avoid retaining the VPN Service.
- VPN consent remains controlled by Android; the app cannot bypass the system dialog.
