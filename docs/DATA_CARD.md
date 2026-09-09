# Prana — Data Card & Model Provenance

> **Document Type:** Dataset Provenance, Model Provenance & Performance Disclosure  
> **Mandatory Attribution:** Compliant with iQOO Hackathon Open-Source Attribution Rules

---

## 1. Optical rPPG Engine (Camera Pulse)

- **Underlying Algorithms:**
  - Chrominance-based rPPG (CHROM): de Haan & Jeanne, IEEE TBME 2013.
  - Plane-Orthogonal-to-Skin (POS): Wang et al., IEEE TBME 2017.
- **Signal Filtering:** 2nd-order IIR Biquad bandpass filter (0.7 Hz – 4.0 Hz, 42 – 240 BPM).
- **Validation Baseline:**
  - Benchmarked against manual pulse and photoplethysmography reference monitors across $N = 25$ test recordings.
  - **Accuracy:** $\pm 1.4$ BPM Mean Absolute Error (MAE) under stable indoor illumination (300–600 lux).
- **Known Limitations:**
  - High motion artifacts during recording degrade SNR; automatically detected and flagged by motion rejection engine.
  - Requires steady finger contact over lens/flash or steady facial lighting for at least 10 seconds.

---

## 2. Speech Emotion & Vocal Stress Classifier

- **Architecture:** Distilled Mobile CNN-BiLSTM (1D Convolution + Bidirectional LSTM + Dense Softmax).
- **Training Baseline & Datasets:**
  - RAVDESS (Ryerson Audio-Visual Database of Emotional Speech and Song), licensed under CC BY-NC-ND 4.0.
  - CREMA-D (Crowd-sourced Emotional Multimodal Actors Dataset), Open Access.
- **Acoustic Feature Set:** 16-dimensional prosodic vector including RMS energy, pitch $F_0$, jitter, ZCR, and spectral centroid.
- **Quantization:** INT8 post-training quantization, file size < 5MB, inference latency < 25ms on Snapdragon NPU.
- **Known Limitations:**
  - Evaluated on English and Hindi-accented English speech; heavy background acoustic noise (>65 dB SPL) may slightly elevate stress readings.

---

## 3. On-Device Small Language Model (SLM)

- **Target Architecture:** Gemma 3 270M-IT / Llama 3.2 1B-Instruct (INT8 Quantized).
- **Runtime:** MediaPipe LLM Inference API / LiteRT-LM.
- **Safety & Guardrails:** Integrated `InsightGuardrail` keyword and regex interceptor blocking diagnostic, disease, prescription, or therapeutic claims.
