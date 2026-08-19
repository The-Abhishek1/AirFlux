# AirFlux

**Fast, anonymous file sharing and downloads for Android — no accounts, no internet required for local transfers.**

Built by [XCloak](https://xcloak.tech).

---

## Overview

AirFlux is a native Android app (Kotlin + Jetpack Compose) that combines local file sharing, internet downloads, and offline messaging into one anonymous, no-login tool. Every core transfer — file sharing, Wi-Fi Chat, Bluetooth Chat — happens directly between devices. Nothing routes through a backend server we operate.

## Features

### File Sharing (Send / Receive)
- Send files over local Wi-Fi via an in-app HTTP server, no internet needed
- QR code pairing or manual address entry
- In-app native receiver (not browser-based)
- Session-based security: random per-session tokens, rate limiting, filename sanitization
- **Pro:** folder/batch transfer, unlimited simultaneous receivers, AES transfer encryption

### Download from Link
- Paste a direct file URL, download with automatic filename/type detection
- Background downloads via foreground service with live notifications
- Resume via HTTP Range requests
- **Pro:** pause/resume, batch URL input, scheduled downloads (WorkManager-backed), unlimited concurrent downloads, no speed throttling

> Downloads only support direct file URLs (CDNs, direct links, GitHub releases, etc.) — not platform video extraction (YouTube, Instagram, etc.) or torrents, which are out of scope by design for legal/ToS reasons.

### Wi-Fi Chat & Bluetooth Chat
- Host or join a chat with a nearby device — Wi-Fi Chat needs a shared network, Bluetooth Chat needs zero network at all
- QR pairing (Wi-Fi) or paired-device selection (Bluetooth)
- Persistent local chat history per channel
- **Pro:** photo, voice message, and video sharing (size-capped); unlimited chat history (free tier keeps the last 50 messages)

### Also included
- Local transfer/download history
- Settings screen with plan status and app info
- Onboarding flow explaining permissions on first launch
- Share-to-AirFlux: share files into the app directly from other apps (Gallery, Files, WhatsApp, etc.)
- AdMob banner/interstitial/rewarded ads on the free tier; free-tier download speed is throttled with a rewarded-ad "speed boost" option
- One-time lifetime Pro upgrade (no subscription)

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, Repository pattern |
| Local server | NanoHTTPD |
| Networking | OkHttp |
| Local database | Room |
| Background work | WorkManager, Foreground Service |
| QR | ZXing / zxing-android-embedded |
| Ads | Google AdMob |
| Async | Kotlin Coroutines & Flow |

Native Kotlin was chosen over Flutter/React Native since the app is Android-only and depends heavily on platform-specific APIs (Wi-Fi Direct-style local serving, Bluetooth RFCOMM, Storage Access Framework) that offer no real cross-platform benefit here.

## Project Structure

```
app/src/main/java/com/xcloak/airflux/
├── core/
│   ├── ads/            # AdMob banner/interstitial/rewarded + speed boost logic
│   ├── billing/         # Plan state, pricing display (real Play Billing pending)
│   ├── common/          # Shared utils: files, images, video, voice, network, storage
│   ├── designsystem/     # Glassmorphism UI components, colors, accessibility helpers
│   ├── network/          # Local HTTP server, chat sockets (Wi-Fi + Bluetooth), HTTP client
│   ├── notification/      # Download progress notifications
│   ├── security/         # Session tokens, rate limiting, AES transfer encryption
│   ├── service/          # Foreground service for background downloads
│   └── work/            # WorkManager worker for scheduled downloads
├── data/
│   ├── database/         # Room entities, DAOs, AppDatabase
│   └── repository/        # History and chat repositories
├── domain/model/         # Data classes shared across features
├── feature/
│   ├── sharing/          # Send / Receive screens + ViewModels
│   ├── downloader/        # URL downloader screen + ViewModel
│   ├── chat/             # Wi-Fi Chat
│   ├── btchat/           # Bluetooth Chat
│   ├── history/           # Transfer/download history
│   ├── settings/          # Settings + dev Pro toggle
│   ├── billing/           # Go Pro purchase screen
│   └── onboarding/        # First-run permission explainer
└── navigation/           # Compose Navigation graph
```

## Permissions Used

| Permission | Why |
|---|---|
| Camera | QR code scanning for device pairing only — never recorded or stored |
| Bluetooth (Scan/Connect/Advertise) | Bluetooth Chat device discovery and connection |
| Notifications | Download/transfer progress |
| Storage / Media | Selecting files to share, saving downloads and received files |
| Record Audio | Voice messages in chat (Pro) |
| Internet / Network State | Local server, downloads, connectivity checks |

## Security

- Every sharing session generates a fresh, cryptographically random token — no static credentials
- Local server rate-limits requests per IP and rejects invalid/missing tokens
- All filenames from remote sources are sanitized before being written to disk (no path traversal)
- Optional AES-CTR transfer encryption for sharing sessions (Pro)
- Release builds are minified/shrunk with ProGuard rules covering all third-party libraries

## Monetization

- Free tier: ads (banner/interstitial), single concurrent transfer, throttled download speed, limited chat history, no photo/voice/video in chat
- Pro tier (one-time purchase, region-priced): removes all limits and ads
- Currently gated behind a local dev toggle in Settings — real Google Play Billing integration is pending Play Console setup

## Status

Core features complete and functional. Remaining before Play Store submission:
- [ ] Real Google Play Billing (currently a local dev toggle)
- [ ] Play Console account, signed release build, closed testing period
- [ ] Real AdMob ad unit linking to live Play Store listing
- [ ] Final store listing screenshots and copy

## License

Proprietary — © XCloak. All rights reserved.