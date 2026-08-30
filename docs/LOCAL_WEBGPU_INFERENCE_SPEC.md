# ⚡ Local Offline AI Inference Engine Specification (WebGPU & SLMs)

This document details the on-device Small Language Model (SLM) inference architecture implemented in **MailGenie** for private, zero-cost, offline email drafting.

---

## 1. Architectural Architecture

```mermaid
graph TD
    User[User Prompt Context] --> Detect{Network / Mode}
    Detect -->|Online Mode| Cloud[Cloud LLMs (Groq / Gemini / OpenAI)]
    Detect -->|Offline / WebGPU| Local[LocalInferenceEngine]
    Local --> Hardware{WebGPU Available?}
    Hardware -->|Yes| GPU[WebGPU Shader Compute]
    Hardware -->|No| WASM[WASM SIMD CPU Fallback]
    GPU --> Model[Cached Model Weights (SmolLM2 / Qwen2.5)]
    WASM --> Model
```

---

## 2. Supported Quantized Model Catalogue

| Model Identifier | Parameters | Quantization | Size (MB) | Min. RAM | Target Device |
| :--- | :---: | :---: | :---: | :---: | :--- |
| `smollm2-360m` | 360 Million | q4f16 | 190 MB | 4 GB | Mobile, Laptops, Low-power GPUs |
| `qwen2.5-0.5b` | 500 Million | q4f16 | 350 MB | 6 GB | Modern Laptops, Apple Silicon |
| `phi3.5-mini` | 3.8 Billion | q4 | 2,200 MB | 8 GB | High-performance Desktops |

---

## 3. CacheStorage & Progressive Weight Delivery

Model weights are chunked and persisted inside `caches.open('mailgenie-offline-models-v1')`. Once downloaded, subsequent visits load model weights directly from the local disk cache without consuming internet bandwidth.
