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
- Vault reads are bounded to 2 MiB while streaming; a second size check after reading rejects oversized or truncated content.
- New ciphertext is written to a unique temporary file, flushed, assigned owner-only permissions, and synced before replacement.
- Replacement keeps a recovery backup until the new primary file is installed. Recovery authenticates the primary AES-GCM ciphertext before discarding the backup; if the primary is incomplete, it restores the last known-good backup. If Keystore access or recovery I/O fails, both files are preserved for a later retry.
- Decryption requires the existing Android Keystore key. Missing or invalidated keys are reported as a recoverable key-availability condition; the vault never silently generates a replacement key while loading.
- **Load saved profile** decrypts locally. **Delete saved profile** removes the ciphertext and deletes the corresponding Keystore key.
- There is no cloud upload, telemetry, or subscription URL storage in this vault.

This is an initial single-profile vault, not yet the planned Room-based multi-profile store. Secure deletion cannot guarantee physical flash-cell erasure; deleting the Keystore key provides cryptographic erasure for the encrypted file, subject to Android Keystore/device implementation guarantees.
