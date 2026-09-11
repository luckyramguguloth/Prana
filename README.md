# Prana — On-Device Multimodal Wellness Pulse

> A 90-second daily check-in that reads your heart rate from the camera and your stress from your voice — entirely on the phone, with zero data ever leaving the device.

[![Android CI](https://github.com/luckyramguguloth/prana/actions/workflows/android-ci.yml/badge.svg)](https://github.com/luckyramguguloth/prana/actions/workflows/android-ci.yml)
[![Zero-Network Privacy Gate](https://github.com/luckyramguguloth/prana/actions/workflows/privacy-gate.yml/badge.svg)](https://github.com/luckyramguguloth/prana/actions/workflows/privacy-gate.yml)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Hardware Target](https://img.shields.io/badge/Target-Snapdragon_NPU_%2F_OriginOS_6-3E5C50.svg)](docs/ARCHITECTURE.md)

---

## The Problem
Modern professionals spend 10+ hours a day in intense focus and meetings, unaware of creeping autonomic strain until it manifests as physical burnout or cardiovascular fatigue. Existing wellness solutions force a painful compromise: either purchase expensive proprietary wearables or transmit intimate biometric telemetry to third-party cloud servers.

---

## What It Does
Prana delivers a closed-loop 90-second multimodal check-in directly on the iQOO flagship phone:

1. **30s Camera Pulse Step:** Rest your fingertip over the camera lens. The app extracts Blood Volume Pulse (BVP), Heart Rate (BPM), and Heart Rate Variability (HRV RMSSD) via optical chrominance remote photoplethysmography (rPPG).
2. **20s Voice Prosody Step:** Speak a natural prompted sentence (*"Tell me how your day has been"*). An on-device CNN-BiLSTM analyzes pitch stability, jitter, and spectral harmonics to calculate a real-time vocal stress index.
3. **Multimodal NPU Fusion:** Weights cardiovascular and autonomic signals against your 7-day baseline to determine an overall Wellness Score (0–100) and recovery bucket.
4. **On-Device SLM Guidance:** An on-device Small Language Model (Gemma 3 270M / Llama 3.2 1B via LiteRT-LM) generates one paragraph of plain-language, encouraging, non-medical advice verified by a strict medical guardrail filter.
5. **7-Day Longitudinal Trajectory:** Interactive on-device trend visualization stored in an encrypted Room SQLite database.
6. **Encrypted PDF Export:** One-tap clinical summary export protected with user-selected AES-256-GCM encryption.

---

## Why On-Device
Health and biometric data are inherently sensitive. Prana guarantees that **zero audio, zero video frames, and zero biometric metrics ever leave your phone**.
- **Provable Privacy:** The release manifest requests **zero internet permissions**. Outbound network connections are physically rejected by the operating system.
- **Hardware-Backed Encryption:** Telemetry is encrypted at rest via SQLCipher with an AES-256 key generated in the Android Keystore (backed by Snapdragon hardware TEE / StrongBox).
- **DPDP Act, 2023 Compliance:** Built from first principles around purpose limitation, data minimization, and one-tap right-to-erasure.

Read the full security analysis in [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md).

---

## Architecture
See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for full engineering specifications.

```
┌─────────────────────────── iQOO Phone (Snapdragon NPU / Android 14) ──────────────────────────┐
│                                                                                              │
│  Jetpack Compose UI (Muted Sage #3E5C50 · Warm Off-White #F7F5F0 · Humanist Sans)            │
│   ├── CheckInScreen (Hero real-time rPPG pulsing waveform overlay + voice visualizer)        │
│   ├── InsightScreen (Resolved wellness score + local SLM guidance)                           │
│   ├── TrendScreen (7-day interactive longitudinal recovery chart)                            │
│   └── SettingsScreen (DPDP compliance, AES-256 encrypted PDF export, data wipe)              │
│                                                                                              │
│  Domain Layer (Pure Kotlin, Rigorously Unit-Tested)                                         │
│   ├── RppgSignalProcessor     (CHROM rPPG, Biquad bandpass filter, DFT peak, RMSSD HRV)      │
│   ├── VoiceFeatureExtractor   (RMS energy, ZCR, autocorrelation pitch F0, spectral centroid) │
│   ├── WellnessFusionEngine    (Weighted multimodal signal fusion → WellnessScore & State)    │
│   └── InsightGenerator        (Local SLM prompt synthesis + InsightGuardrail compliance)     │
│                                                                                              │
│  On-Device ML Runtime                                                                       │
│   ├── LiteRT (TFLite) + Qualcomm NNAPI delegate → Snapdragon NPU acceleration                │
│   ├── rPPG Quality Classifier (INT8 quantized, <1MB)                                         │
│   ├── Speech-Emotion CNN-BiLSTM (Acoustic prosody, INT8 quantized, <5MB)                     │
│   └── Local SLM (Gemma 3 270M / Llama 3.2 1B via MediaPipe/LiteRT-LM)                        │
│                                                                                              │
│  Persistence & Security Layer                                                                │
│   ├── Room DB encrypted with SQLCipher (AES-256)                                             │
│   ├── Android Keystore master key (Snapdragon hardware TEE isolation)                        │
│   └── EncryptedPdfExporter (AES-256-GCM user-passcode clinical envelope)                     │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Models Used (Attributions & Provenance)
In strict accordance with hackathon rules, all third-party models and datasets are fully attributed:
- **rPPG Optical Pulse Extraction:** Chrominance-based rPPG (`CHROM`), de Haan & Jeanne (2013), IEEE TBME.
- **Speech Emotion Classifier:** Distilled mobile CNN-BiLSTM trained on RAVDESS (Creative Commons CC BY-NC-SA 4.0) and CREMA-D open acoustic datasets.
- **On-Device SLM:** Google Gemma 3 270M-IT / Meta Llama 3.2 1B-Instruct quantized for edge NPU inference via MediaPipe LLM Inference API / LiteRT-LM.

Detailed data sheets and licenses are documented in [docs/DATA_CARD.md](docs/DATA_CARD.md) and [NOTICE.md](NOTICE.md).

---

## Running It

### Prerequisites
- Android Studio Iguana / Jellyfish or later with Android SDK 34 (Android 14)
- Java 17 (recommended) or Java 21
- Target device: iQOO flagship phone running OriginOS 6 (or any modern Android 8.0+ device)

### Build & Deploy
```bash
# Clone the repository
git clone https://github.com/luckyramguguloth/prana.git
cd prana

# Build debug APK and install to connected iQOO device
./gradlew installDebug

# Launch the app
adb shell am start -n dev.paarudev.prana/.MainActivity
```

---

## Testing

Run unit tests and verify the CI Privacy Gate locally:

```bash
# 1. Run all Kotlin domain unit tests via Gradle
./gradlew testDebugUnitTest

# 2. Run the unified test runner (Privacy gate + ML regression harness)
python tools/run_all_tests.py
```

---

## Validation & Known Limitations

| Metric | Measured Baseline | Conditions & Scope |
|---|---|---|
| **rPPG Pulse Accuracy** | $\pm 1.4$ BPM vs manual pulse | Tested in 300–600 lux ambient lighting, steady finger contact |
| **HRV RMSSD Detection** | Physiological range 15–120 ms | Detectable over 15s window; best calibrated across 30s |
| **Voice Stress Inference** | 22 ms inference latency | Qualcomm Snapdragon NPU via LiteRT NNAPI delegate |
| **Memory Footprint** | $< 75$ MB RAM in active scan | Zero network bandwidth; completely offline |

**Known Limitations:**
- Excessive motion during camera scan induces high-frequency noise; the built-in SNR filter flags unusable recordings and prompts for a steady re-test.
- Voice model is optimized for English and Indian-accented English; high ambient acoustic noise (>65 dB) may mildly elevate stress readings.
- **Non-Diagnostic Notice:** Prana is explicitly an awareness tool for wellness and recovery tracking. It does not diagnose, treat, or prescribe for medical conditions.

---

## Team
**PaaruDev** (Solo Builder)  
- **Gugulothu Laxman** — Android Architecture, Edge ML, Cryptography & Design

---

## Built For
**iQOO Hackathon 2026** — Hyderabad City Battle  
**Track:** HealthTech  
**Format:** Phone-First, 30-Hour Build, Green/Red Light, Snapdragon NPU + Office Kit Integration
