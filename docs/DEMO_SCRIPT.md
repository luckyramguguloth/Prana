# Prana — Live 4-Minute Jury Presentation Script

**Presenter:** Gugulothu Laxman (Luckyram) · Team PaaruDev  
**Track:** HealthTech · iQOO Hackathon 2026, Hyderabad Battle  

---

### [0:00 – 0:30] Problem & Hook
*"Good afternoon, judges. Today, professionals spend 10+ hours in front of screens, accumulating silent stress that leads to burnout and cardiovascular strain. Existing solutions fail in two ways: wearables cost thousands, while AI health apps send your most intimate biometric data to third-party cloud servers.*

*We built **Prana** — a 90-second on-device multimodal wellness check-in that measures your heart rate from your camera and your stress from your voice, running 100% on this iQOO phone with zero data ever leaving the device."*

---

### [0:30 – 1:30] Live Hero Demo: Camera rPPG Pulse
*(Show phone mirrored via Office Kit)*  
*"I tap 'Start Check-in' and place my finger over the rear camera. Notice the real-time pulsing waveform on screen — that is not a canned animation; that is the actual optical chrominance signal extracting blood volume changes from my capillaries at 30 frames per second. Our on-device bandpass filter and FFT are calculating my resting pulse: 72 BPM with an HRV of 52 ms."*

---

### [1:30 – 2:15] Live Demo: Voice Prosody & Speech Stress
*"Now the app transitions to step two: voice prosody. I speak the prompt: 'I had a productive morning prepping the demo, but feeling a bit excited.'*  
*Our distilled CNN-BiLSTM processes the 16kHz audio buffer locally, evaluating pitch stability, jitter, and spectral centroid. In under 300 milliseconds on the Snapdragon NPU, it computes a vocal stress index of 3.8 out of 10."*

---

### [2:15 – 3:00] The Insight & On-Device SLM
*"Our multimodal fusion engine synthesizes these signals against my 7-day baseline into a unified score of 81/100 — 'Calm & Balanced'.*  
*Below, an on-device Small Language Model generates personalized, non-diagnostic guidance in plain language. Notice the badge: 'Guardrail Verified'. Our built-in safety filter ensures zero medical claims or diagnoses are ever generated."*

---

### [3:00 – 3:30] 7-Day Trend & Security Architecture
*"Here on the 7-day trend screen, Room with SQLCipher shows longitudinal recovery patterns. All data is encrypted with an Android Keystore hardware-backed AES-256 key.*  
*If I want to share this with a doctor, one tap generates a clinical PDF encrypted with a custom passcode via AES-256-GCM. And under Settings, we provide a one-tap data wipe fulfilling India's DPDP Act 2023 right-to-erasure."*

---

### [3:30 – 4:00] Closing & Technical Discipline
*"Best of all, our release manifest has ZERO internet permissions. Our automated CI privacy gate proves not a single byte escapes this device.*  
*Thank you — Prana brings clinical-grade multimodal wellness awareness directly to your pocket."*
