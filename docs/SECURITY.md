# Security Policy

## Reporting a Vulnerability

Please report security vulnerabilities privately to the repository maintainer. Do not disclose exploitable details in public issues until a fix is available.

## Project Security Principles

- Keep API keys, signing keys, and credentials out of source control.
- Encrypt sensitive local configuration using Android Keystore-backed keys.
- Verify signatures and hashes before activating app or Xray Core updates.
- Redact secrets and private connection details from diagnostics.
- Require explicit user consent for VPN activation and optional cloud services.
