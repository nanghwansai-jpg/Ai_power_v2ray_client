# Runtime and Device Security Validation

This document is the release-gate checklist for the Android runtime trust boundary.
CI success is not a substitute for these checks.

## 1. Physical-device functional matrix

Record device model, Android version, build SHA, APK SHA-256, network type, and test timestamp.
Use a non-production test profile with no user credentials.

| Case | Procedure | Required result |
|---|---|---|
| Import and validate | Import a known-good share link; validate before connect | Profile is parsed and validated; no plaintext profile is logged |
| Connect and traffic | Connect, resolve DNS, make HTTPS requests, verify traffic counters | Tunnel carries traffic; DNS follows configured policy; no cleartext fallback |
| Disconnect | Disconnect while traffic is active | Native core stops; TUN closes; foreground notification is removed |
| Repeated lifecycle | Repeat start/stop 20 times, including rapid double taps | No duplicate core, stale service start, leaked TUN, or stuck foreground state |
| Wi-Fi/mobile transition | Switch Wi-Fi to cellular and back while connected | Service follows documented network policy; no crash or stale descriptor |
| Permission revoke | Revoke VPN permission in system settings while connected | Service stops safely and does not silently restart |
| Reboot | Reboot with an imported profile | No automatic connection unless explicitly designed and authorized; profile remains encrypted and readable |

Capture logcat only with redaction enabled. Never attach profile URLs, UUIDs, keys, or decrypted config.

## 2. Native V4 response fault injection

The fault mutator is located only in src/test; it must not be referenced by production source.
Unit tests cover duplicate keys, wrong API/method, partial correlation, oversized and deeply nested payloads,
malformed UTF-16/Unicode, and contradictory success/error envelopes.

The release workflow inspects every DEX file in the signed release APK for the unique
AI_POWER_V4_FAULT_INJECTION_TEST_ONLY sentinel and fails if it is present.

For device-level JNI testing, run an instrumented test build with a test-only native shim that returns
the malformed envelopes. Do not add a runtime-configurable injection switch to production code.

## 3. Vault process-interruption matrix

The JVM unit tests simulate the recoverable filesystem states and injected interruption boundaries:
- primary absent / backup present;
- both present with authenticated or corrupt primary;
- failure while moving the backup;
- interruption after restore but before directory sync;
- unavailable Keystore validation, preserving both files.

On a physical device, repeat save/update with forced process termination at:
1. after temporary ciphertext write;
2. after file descriptor sync;
3. after primary-to-backup rename;
4. after temporary-to-primary rename;
5. before backup deletion.

After each restart, verify that either the last committed authenticated profile is readable or recovery fails closed
without deleting the only recoverable ciphertext. Record the outcome and inspect both vault paths through
a debuggable test harness; never export decrypted contents.

## 4. Release decision

All three groups above require recorded evidence on at least one physical Android device and the exact release
candidate SHA. Until then, keep the PR Draft/Open and production HOLD.
