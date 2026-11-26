# Android Build and Release

## Automated debug build
The `.github/workflows/android-ci.yml` workflow runs on pushes and pull requests targeting `main`, and can also be started manually. It builds the debug APK with JDK 17 and Gradle 8.13.

## Signed release build
The `.github/workflows/release.yml` workflow:
1. Runs when a version tag matching `v*` is pushed, or manually for an existing tag.
2. Requires the four signing secrets below.
3. Builds both the signed APK and Android App Bundle.
4. Generates SHA-256 checksums.
5. Uploads build artifacts and publishes a GitHub Release.

### Required GitHub Actions secrets
Open repository **Settings → Secrets and variables → Actions → New repository secret** and add:

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded Android release keystore file |
| `ANDROID_KEY_ALIAS` | Alias of the signing key |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_PASSWORD` | Key password |

Create and retain the keystore securely outside the repository. Back it up safely: losing the signing key can prevent updates to an already-published Android app. Never commit the keystore, passwords, or signing material.

To encode an existing keystore locally:
```bash
base64 -w 0 release-key.jks
```
Copy the output into `ANDROID_KEYSTORE_BASE64`. Do not paste it into chat or commit it.

### Publish a version
After the signing secrets are configured, create and push a version tag:
```bash
git tag v0.1.0
git push origin v0.1.0
```
The workflow then builds and publishes the release. A manual run requires the name of an existing tag.

## Current limitations
- This repository does not yet contain the Xray Core binaries or integration, a working VPNService data plane, protocol importers, or Cloudflare Worker implementation.
- Release workflow execution and APK/AAB output have not yet been verified. The workflow will stop if signing secrets are missing.
- No production-ready app should be distributed until VPN functionality, security, and device compatibility tests pass.
