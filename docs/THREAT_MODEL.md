# Prana — Security Architecture & Threat Model

> **Compliance Target:** India Digital Personal Data Protection (DPDP) Act, 2023  
> **Classification:** Edge-Exclusive Non-Diagnostic Wellness Telemetry

---

## 1. Asset Inventory

| Asset | Description | Sensitivity | Location | Protection |
|---|---|---|---|---|
| **Live Camera Video** | Raw RGB frames of user fingertip/face | High | Ephemeral RAM only | Immediate in-memory discard after rPPG extraction; never saved |
| **Live Voice Audio** | 16kHz microphone PCM speech buffers | High | Ephemeral RAM only | Immediate in-memory discard after feature extraction; never saved |
| **Biometric Telemetry** | Heart rate (BPM), HRV (RMSSD ms), vocal stress | Medium | Local SQLite DB | Encrypted with SQLCipher (AES-256) |
| **Database Encryption Key** | 256-bit random passphrase for SQLCipher | Critical | Keystore / App Vault | Protected by Android Keystore hardware-backed AES-256-GCM master key |
| **Exported Reports** | Longitudinal wellness summary PDF | Medium | Cache / FileProvider | Encrypted with user-specified passphrase using AES-256-GCM envelope |

---

## 2. Threat Actors & Mitigations

### Threat 1: Malicious App on Same Device
- **Risk:** Malicious application attempting to read local health telemetry or database files.
- **Mitigation:** Android app sandboxing + SQLCipher database encryption. Without the hardware-isolated Keystore key, raw database contents read as pseudorandom ciphertext.

### Threat 2: Lost or Stolen Unlocked Phone
- **Risk:** Bystander browsing device history.
- **Mitigation:** Zero cloud syncing, right-to-erasure one-tap wipe button, and passcode-gated PDF exports.

### Threat 3: Supply-Chain Dependency Leakage
- **Risk:** Third-party analytics SDK transmitting health telemetry to external servers.
- **Mitigation:** ZERO external networking dependencies. The release `AndroidManifest.xml` omits `android.permission.INTERNET`, making outbound socket connections physically impossible at the OS level. CI Privacy Gate strictly validates this on every commit.

---

## 3. Alignment with DPDP Act, 2023 Principles

1. **Purpose Limitation (§4 DPDP Act):** Biometric and vocal signals are processed solely to compute user-requested wellness awareness scores.
2. **Data Minimization (§6 DPDP Act):** No raw video or audio recordings are retained on disk; only distilled mathematical summaries are preserved.
3. **Right to Erasure (§12 DPDP Act):** The "Delete All My Data" button in Settings invokes an immediate purge of all entries and cryptographic keys.
