# NEXORA — Ultimate Universal Media Downloader

> **"Your Media. Your Quality."**  
> **Designed & Built by Gaurav** (`@avkaxofficial` • `@gxr.avka`)

NEXORA is a futuristic JARVIS-inspired media workstation built natively for Android (Kotlin + Jetpack Compose) with an accompanying self-hostable Node.js + Express + FFmpeg backend (`/backend`). It is engineered around **100% watermark-free exports**, strict **SSRF & HTTPS security**, and **modular provider compliance**.

---

## 1. Core Capabilities

1. **AMOLED Futuristic JARVIS Interface (`#05070B`)**:
   - Glassmorphic panels (`#0C111B`, `#111927`), electric cyan (`#00E5FF`) and ice blue (`#6EA8FF`) accents, custom hexagonal NEXORA emblem, and bundled `Orbitron`, `Space Grotesk`, and `JetBrains Mono` typography.
2. **Real 4-Stage URL Analysis & SSRF Guard**:
   - Enforces `https://` transport.
   - Blocks `localhost`, `127.0.0.0/8`, RFC 1918 private networks (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`), link-local (`169.254.0.0/16`), and cloud metadata endpoints (`169.254.169.254`, `metadata.google.internal`).
   - Sanitizes output filenames against path traversal (`..`, `/`, `\`).
3. **Watermark-Free Media Engine**:
   - **Zero Added Branding**: NEXORA never inserts watermarks, logos, intros, outros, or overlays into downloaded video or audio files.
   - **Direct HTTPS Streaming**: Downloads direct `.mp4`, `.webm`, `.mp3`, `.m4a`, and `.wav` URLs with real byte progress and SHA-256 verification.
   - **User-Owned Local Video Clean Export**: Uses the zero-permission Android Photo Picker (`PickVisualMedia`) + `MediaMetadataRetriever` + `MediaExtractor` + `MediaMuxer` to remux user-owned videos or extract audio tracks without re-encoding or watermarks.
   - **Authorized Open Media Studio (Clearly Labeled Sample Mode)**: Includes CC0 multi-resolution (`2160p 4K` to `360p`) video and lossless `WAV` / `M4A` / `MP3` audio presets for immediate on-device testing.
4. **Smart Quality & Data Saver**:
   - Automatically recommends optimal streams based on source formats, user preference, file size estimates, and metered connection state.

---

## 2. Project Structure

- `/app/src/main/java/com/example/`
  - `domain/security/UrlSecurityValidator.kt` — HTTPS & SSRF validation + filename sanitization.
  - `domain/provider/ProviderAdapter.kt` — Modular adapters for YouTube (official oEmbed), Instagram, Direct HTTPS streams, and Authorized Open Media.
  - `domain/provider/NexoraAnalysisPipeline.kt` — 4-stage analysis pipeline & Smart Quality recommender.
  - `domain/media/WatermarkFreeMediaEngine.kt` — OkHttp direct downloader, Android hardware `MediaMuxer` clean remuxer, and playable PCM WAV / ISO MP4 synthesizer with SHA-256 verification.
  - `data/local/` — Room database (`NexoraDatabase`, `RecentDownloadDao`, `RecentDownloadEntity`) for local download history & expiration tracking.
  - `data/preferences/` — Jetpack DataStore (`NexoraPreferencesRepository`) for settings.
  - `ui/` — Jetpack Compose screens (`NexoraAppScreen`, `NexoraViewModel`) and modular components.
- `/backend/` — Complete Node.js + Express + TypeScript + FFmpeg server implementing:
  - `POST /api/analyze`
  - `POST /api/download`
  - `GET /api/download/status/:jobId`
  - `DELETE /api/download/:jobId`
  - `GET /api/health`

---

## 3. Running & Deploying the Backend (`/backend`)

Because the AI Studio Android runtime executes the Android APK directly inside a container, external FFmpeg server processes are provided in `/backend` for optional cloud deployment (e.g., Google Cloud Run, Render, Fly.io, or Docker) and can be linked in-app via the **Backend API (`</>`)** button in the top header.

1. Install Node.js 20+ and `ffmpeg` on your host or container.
2. Copy `/backend/.env.example` to `/backend/.env`.
3. Install dependencies and start:
   ```bash
   npm --prefix backend install
   npm --prefix backend run build
   npm --prefix backend start
   ```
4. Open the **NEXORA Android App**, tap the **`</>` (Backend API)** icon in the top header, and enter your HTTPS backend URL.

---

## 4. Running Automated Tests

Run the local JVM unit and Robolectric test suite via Gradle:
```bash
gradle :app:testDebugUnitTest
```
