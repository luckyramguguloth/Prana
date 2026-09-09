# Prana — System Architecture & Technical Specification

> **Track:** HealthTech · iQOO Hackathon 2026 (Hyderabad City Battle)  
> **Team:** PaaruDev (Luckyram / Gugulothu Laxman)  
> **Core Guarantee:** 100% On-Device Multimodal Wellness Analysis · Zero Cloud Dependency

---

## 1. System Architecture Diagram

```
┌───────────────────────────── iQOO Phone (OriginOS 6 / Android 14) ───────────────────────────┐
│                                                                                                │
│  Jetpack Compose UI                                                                           │
│   ├─ CheckInScreen (live Camera rPPG waveform + mic prosody capture)                         │
│   ├─ InsightScreen (smooth score resolution + on-device SLM guidance)                        │
│   ├─ TrendScreen (7-day interactive longitudinal recovery chart)                             │
│   └─ SettingsScreen (DPDP compliance, AES-256 PDF export, one-tap data wipe)                 │
│                                                                                                │
│  Domain Layer (Pure Kotlin, Rigorously Unit-Tested)                                           │
│   ├─ RppgSignalProcessor      → CHROM rPPG + 0.7-4Hz Biquad filter + DFT peak + RMSSD HRV     │
│   ├─ VoiceFeatureExtractor    → 16kHz PCM → RMS, ZCR, F0 pitch tracking, spectral centroid    │
│   ├─ WellnessFusionEngine     → Weighted multimodal fusion → WellnessScore (0-100) & State    │
│   └─ InsightGenerator         → Local SLM prompt synthesis + InsightGuardrail compliance      │
│                                                                                                │
│  On-Device ML Runtime                                                                         │
│   ├─ LiteRT (TensorFlow Lite) + Qualcomm NNAPI delegate → Snapdragon NPU acceleration        │
│   ├─ rPPG Quality Classifier (INT8 quantized, <1MB)                                           │
│   ├─ Speech-Emotion CNN-BiLSTM (distilled, INT8 quantized, <5MB)                             │
│   └─ Local SLM Inference (Gemma 3 270M-IT / Llama 3.2 1B via MediaPipe/LiteRT-LM)            │
│                                                                                                │
│  Local Persistence & Security Layer                                                           │
│   ├─ Room Database encrypted via SQLCipher (`prana_wellness_secure.db`)                       │
│   ├─ Android Keystore AES-256-GCM hardware-backed master key (TEE / StrongBox)                │
│   ├─ EncryptedPdfExporter (Clinical wellness export protected by AES-256-GCM envelope)        │
│   └─ Zero Network Verification (No INTERNET permission; CI Privacy Gate enforced)             │
│                                                                                                │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Signal Processing Pipeline

### 2.1 Optical Remote Photoplethysmography (rPPG)
1. **Frame Capture:** 30 FPS video frames from rear camera with flash illumination (or front camera ambient).
2. **Chrominance Extraction (CHROM):**
   $$X_s = 3R - 2G$$
   $$Y_s = 1.5R + G - 1.5B$$
   $$S = X_s - \alpha \cdot Y_s \quad \text{where } \alpha = \frac{\sigma(X_s)}{\sigma(Y_s)}$$
3. **Bandpass Filtering:** 2nd-order IIR Biquad filter passing 0.7 Hz to 4.0 Hz (42 – 240 BPM).
4. **Spectral Analysis:** Hann-windowed Discrete Fourier Transform (DFT) with 300 frequency bins to extract dominant peak pulse rate.
5. **Heart Rate Variability (HRV):** Refractory-bounded peak detection to extract inter-beat intervals (IBI) and calculate Root Mean Square of Successive Differences (RMSSD):
   $$\text{RMSSD} = \sqrt{\frac{1}{N-1}\sum_{i=1}^{N-1} (IBI_{i+1} - IBI_i)^2}$$

### 2.2 Voice Prosody & Acoustic Stress Extraction
1. **Audio Capture:** 16kHz 16-bit PCM mono buffer.
2. **Feature Extraction:**
   - Root-Mean-Square (RMS) energy & temporal variance
   - Zero-Crossing Rate (ZCR) for vocal glottal tension
   - Pitch / Fundamental Frequency ($F_0$) via normalized autocorrelation across 75 Hz – 400 Hz
   - Spectral Centroid (spectral brightness / high-frequency tension)
3. **ML Inference:** 16-dimensional acoustic tensor evaluated by quantized CNN-BiLSTM model running on Qualcomm NPU via LiteRT NNAPI delegate.

---

## 3. Multimodal Fusion & Local SLM Reasoning

- **Weighted Fusion Equation:**
  $$\text{Composite Score} = 0.40 \times \text{HRV}_{\text{score}} + 0.30 \times \text{HR}_{\text{score}} + 0.30 \times \text{Vocal}_{\text{score}}$$
- **Categorical States:** `OPTIMAL_RECOVERY`, `CALM_BALANCED`, `MILD_STRAIN`, `HIGH_STRESS`, `FATIGUED`.
- **Insight Generation:** Contextualized against 7-day user baseline and passed through `InsightGuardrail` to strictly eliminate medical/diagnostic tokens.
