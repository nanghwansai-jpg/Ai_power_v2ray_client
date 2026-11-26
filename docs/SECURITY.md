# Security Policy

## Reporting a Vulnerability

Please report security vulnerabilities privately to the repository maintainer. Do not disclose exploitable details in public issues until a fix is available.

## Project Security Principles

- Keep API keys, signing keys, and credentials out of source control.
- Encrypt sensitive local configuration using Android Keystore-backed keys.
- Verify signatures and hashes before activating app or Xray Core updates.
- Redact secrets and private connection details from diagnostics.
- Require explicit user consent for VPN activation and optional cloud services.

## Local Profile Vault (implemented)

- The current UI stores one profile only when the user taps **Save securely**.
- Profile JSON is encrypted with AES-256-GCM using a non-exportable AES key generated in Android Keystore.
- A fresh 96-bit nonce is generated for each encryption; the 128-bit GCM authentication tag detects ciphertext tampering.
- Additional authenticated data binds the ciphertext to this app's profile-vault format.
- Ciphertext is stored under `noBackupFilesDir`, so Android Auto Backup does not copy it to cloud backup.
- **Load saved profile** decrypts locally. **Delete saved profile** removes the ciphertext and deletes the corresponding Keystore key.
- There is no cloud upload, telemetry, or subscription URL storage in this vault.

This is an initial single-profile vault, not yet the planned Room-based multi-profile store. Secure deletion cannot guarantee physical flash-cell erasure; deleting the Keystore key provides cryptographic erasure for the encrypted file, subject to Android Keystore/device implementation guarantees.
