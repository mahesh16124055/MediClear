# 🏥 MediClear – Real-Time Emergency Medical Orchestration

**MediClear** is a mission-critical, production-grade emergency medical orchestration system. It serves as a high-fidelity bridge between frontline emergency responders (EMS) and hospital trauma systems, automating pre-authorization, resource allocation, and clinical synthesis using advanced generative intelligence.

---

> [!IMPORTANT]
> **Production Status**: The core orchestration engine is now stabilized with a **429-resilient backend architecture**, ensuring uninterrupted diagnostic analysis even under high-frequency clinical intake.

## 🚀 Intelligent Features

- **Multi-Modal Synthesis**: Processes voice transcripts, high-res medical document scans, and handwritten clinical notes simultaneously.
- **Automated Triage (ESI)**: Predicts Emergency Severity Index (1-5) based on real-time physiological data and patient history.
- **Dynamic Resource Routing**: Automatically identifies required ICD-10/CPT codes and hospital resource requirements (e.g., ICU, Cath Lab).
- **Family Alert System**: Generates human-readable progress updates to keep families informed during critical transit windows.
- **Cloud-Native Resilience**: Built for the Google Cloud ecosystem with native integration for Vertex AI, Cloud Run, and Firestore.

## 🛠️ Architecture Stack

- **Frontend**: High-performance React 19 SPA powered by **Vite**, **Tailwind CSS v4**, and **Framer Motion** for a zero-latency diagnostic UI.
- **Backend**: Enterprise-grade **Java Spring Boot 3.4+** implementing a reactive `WebClient` orchestration layer with automatic backoff/retry.
- **AI Core**: Native **Vertex AI (Gemini 1.5 Flash)** integration via secure OAuth-scoped tokens.
- **Persistence**: Real-time synchronization with **Google Firebase** (Firestore) and comprehensive **Cloud Logging** forensics.

## 📁 Repository Blueprint

```text
/
├── mediclear-java-backend/  # ☕ Secure Java Spring Boot entry point
├── mediclear-react/         # ⚛️ High-fidelity React diagnostic dashboard
├── .gitignore               # Root-level safety excludes
└── README.md                # System documentation
```

## 🔐 Security & Governance

- **Zero-Key Policy**: This repository contains **NO hardcoded API keys**. All secrets must be injected as environment variables (`GEMINI_API_KEY`, etc.) in the Cloud Run control plane.
- **Error Transparency**: Implements a de-sanitized global exception handler for transparent diagnostic forensics on the frontend.

---
© 2026 MediClear Orchestration Systems. Restricted for medical evaluation purposes.
