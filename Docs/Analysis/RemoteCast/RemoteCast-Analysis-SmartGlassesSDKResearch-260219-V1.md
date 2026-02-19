# Smart Glasses SDK Research: Vuzix, Mentra, XReal, Rokid, Epson
**Date:** 2026-02-19 | **Version:** V1

## Executive Summary

| Device | OS | Our App Runs On | SDK Required | Display | Connection | Direct TCP Stream? |
|--------|-----|-----------------|-------------|---------|-----------|-------------------|
| **Vuzix Z100** | Proprietary (no user apps) | Phone companion | YES — Ultralite SDK | 640x480 mono green | BLE to phone | NO — BLE only, must use SDK Canvas API |
| **Vuzix Blade 2** | Android (AOSP) | Glasses directly | Optional | Color binocular | WiFi + BLE | YES — standard Android, sideload APK |
| **Mentra Live** | MentraOS (Android-based) | Via MentraOS SDK | YES — TypeScript SDK | Color camera glasses | WiFi + BLE | PARTIAL — MentraOS mediates hardware access |
| **XReal Aura** | Android XR (2026) | Via Android XR SDK | YES — Android XR SDK | Color AR 70° FoV | Tethered USB-C puck | NO — tethered, uses Android XR framework |
| **Rokid Glasses** | Android (custom) | Glasses directly | Optional (UXR SDK) | Color dual-eye | WiFi + BLE | YES — standard Android with Rokid launcher |
| **Epson Moverio BT-45** | Android 9 (AOSP) | Glasses directly | Optional (Basic Function SDK) | Color binocular Si-OLED | WiFi + USB-C | YES — full Android, sideload APK |

---

## Detailed Analysis Per Device

### 1. Vuzix Z100

**Architecture:** The Z100 does NOT run Android apps on the glasses themselves. It's a BLE peripheral — a companion app on your Android/iOS phone communicates with the glasses via the Ultralite SDK over Bluetooth Low Energy.

**SDK:** [Vuzix Ultralite SDK (GitHub)](https://github.com/Vuzix/ultralite-sdk-android)
- Android library (JitPack dependency)
- Communication: BLE only (not WiFi/TCP)
- Display API: `Canvas` class for direct drawing, `Layout` for structured content, `Notification` for push
- Can send: text, images, SVG-like drawings to the 640x480 green display
- Cannot: run arbitrary apps on the glasses, stream video frames over BLE (too slow)

**Display:** 640x480 monochrome green microLED waveguide, right eye only, 30° FoV

**Impact on our architecture:**
- We CANNOT run GlassClient APK on the Z100 — it has no app runtime
- We CAN send rendered frames as images via Ultralite SDK from the phone companion app
- BLE bandwidth (~1 Mbps practical) limits us to ~2-5 FPS at 640x480 JPEG — not real-time video
- Better suited for: text overlays, command status, notification HUD — NOT screen mirroring
- The phone app would use Ultralite SDK to push VoiceOS HUD content (text commands, overlays, status)

**Verdict:** SDK REQUIRED. Not suitable for full screen cast. Best for HUD text/status overlay mode.

### 2. Vuzix Blade 2

**Architecture:** Runs full Android (AOSP). You can sideload APKs via ADB. Has its own WiFi, runs apps natively.

**SDK:** [Vuzix SDK for Android](https://support.vuzix.com/docs/sdk-for-android) — optional, provides access to proprietary hardware (trackpad, gesture sensor, camera)

**Display:** Color binocular, higher resolution than Z100

**Impact on our architecture:**
- We CAN run GlassClient APK directly on the Blade
- Standard Android TCP/WiFi — direct RemoteCast streaming works
- Vuzix SDK only needed for: trackpad input, camera access, gesture recognition
- Standard Android APIs sufficient for: display, audio, microphone, WiFi, BLE

**Verdict:** Our standard GlassClient APK works. Vuzix SDK optional (nice-to-have for trackpad).

### 3. Mentra Live / Mentra Mach 1

**Architecture:** Runs MentraOS — an open-source (MIT license) smart glasses platform built on Android. Apps are TypeScript "MiniApps" that run in the MentraOS runtime, not native Android APKs.

**SDK:** [MentraOS SDK (GitHub)](https://github.com/Mentra-Community/MentraOS)
- TypeScript SDK (not Kotlin/Java)
- Provides access to: display, microphone, camera, speakers
- Apps distributed via MentraOS MiniApp Store
- Cross-compatible: same app works on Mentra Live, Vuzix Z100 (via MentraOS), Even Realities G1

**Key insight — AugmentOS collaboration:** Vuzix and Mentra collaborated on AugmentOS (Feb 2025), a universal OS for smart glasses. MentraOS is effectively the runtime for Z100 as well.

**Display:** Color, with 12MP camera, 1080p video, stereo speakers

**Impact on our architecture:**
- We CANNOT directly sideload an Android APK as a native app
- We COULD write a MentraOS MiniApp in TypeScript that connects to our phone app
- The MiniApp would: receive cast frames via WebSocket/TCP, display them, capture voice, relay commands
- OR: since MentraOS is open-source and Android-based, we might be able to sideload our APK as a system app (needs investigation)

**Verdict:** TypeScript MiniApp required for official MentraOS distribution. Possible native APK sideload since it's Android-based. Research needed on native access.

### 4. XReal Aura (Project Aura, 2026)

**Architecture:** Runs Android XR — Google's new XR platform. Tethered to a compute puck via USB-C. The puck runs Android XR, glasses are a display + sensors.

**SDK:** [Android XR SDK](https://android-developers.googleblog.com/2025/12/build-for-ai-glasses-with-android-xr.html)
- Android XR SDK Developer Preview 3 (Dec 2025)
- XR Glasses emulator in Android Studio
- Built on standard Android framework with XR extensions
- Supports: spatial computing, hand tracking, 6DoF head tracking

**Display:** Color AR, 70° FoV, binocular

**Impact on our architecture:**
- Tethered design means the compute puck IS the "phone" — our app runs on the puck
- No remote streaming needed in tethered mode — the app runs where the display is
- For untethered use (if supported): Android XR SDK required
- Standard Android APIs work for TCP/WiFi since the puck runs Android

**Verdict:** Tethered = no RemoteCast needed (app runs on puck). Android XR SDK needed for spatial features. Our standard APK may work on the puck as a regular Android app.

### 5. Rokid Glasses (Rokid Air, Rokid Max)

**Architecture:** Runs custom Android. UXR SDK for Unity XR development. Standard Android APKs can be sideloaded.

**SDK:** [Rokid UXR SDK (GitHub)](https://github.com/RokidGlass/UXR-docs)
- UXR SDK for Unity apps (XR features)
- Standard Android development also supported
- Two modes: UXR Dock SDK (glasses + dock) and UXR Phone SDK (glasses + phone)

**Display:** Color, dual-eye AR display

**Impact on our architecture:**
- Standard Android APK sideload works
- WiFi available for TCP streaming
- UXR SDK only needed for: 3D/spatial features, not for 2D app display
- Our GlassClient APK would work as a standard Android app

**Verdict:** Standard Android APK works. UXR SDK optional (only for spatial/3D features).

### 6. Epson Moverio BT-45C/BT-45CS

**Architecture:** Runs Android 9 (AOSP). Full Android device with binocular Si-OLED displays. Can run any Android APK.

**SDK:** [Moverio Basic Function SDK](https://tech.moverio.epson.com/en/basic_function_sdk/)
- Provides access to: display control, camera, IMU sensors, brightness
- Standard Android development via Android Studio
- Optional MAXST AR SDK for augmented reality features

**Display:** Color binocular Si-OLED, HD resolution

**Impact on our architecture:**
- Full Android — our GlassClient APK runs natively
- WiFi + USB-C connectivity
- Moverio SDK needed for: display brightness control, camera access, sensor data
- Standard Android APIs sufficient for: WiFi, TCP, audio, microphone

**Verdict:** Standard Android APK works. Moverio SDK optional for hardware-specific features.

---

## Architecture Strategy: Three Tiers

### Tier 1: Full Android Glasses (Direct APK)
**Devices:** Vuzix Blade 2, Rokid Glasses, Epson Moverio BT-45
**Approach:** Sideload our `GlassClient` APK directly. Full RemoteCast streaming over WiFi TCP.
**SDK needed:** Optional (for device-specific features like trackpad, camera, sensors)
**This is our PRIMARY target — standard Android, no vendor lock-in.**

### Tier 2: Companion App Glasses (BLE/SDK)
**Devices:** Vuzix Z100
**Approach:** Phone companion app uses Vuzix Ultralite SDK to push HUD content (text, images) via BLE.
NOT full screen mirroring — limited to text overlays, command status, AVID labels.
**SDK needed:** YES — Ultralite SDK mandatory (BLE communication)
**Best for:** Lightweight HUD mode — show voice command status, notifications, AVID labels

### Tier 3: Platform-Mediated Glasses (MiniApp/XR)
**Devices:** Mentra Live (MentraOS TypeScript), XReal Aura (Android XR)
**Approach:** Write platform-specific app (TypeScript MiniApp for MentraOS, Android XR app for XReal)
**SDK needed:** YES — platform-specific SDK
**Deferred until Tier 1 is solid.**

---

## Can We Directly Transmit to These Devices?

| Device | Direct TCP Stream? | Why / Why Not |
|--------|-------------------|---------------|
| Vuzix Z100 | NO | BLE-only peripheral, no WiFi on glasses, max ~1 Mbps |
| Vuzix Blade 2 | YES | Full Android with WiFi, standard TCP sockets |
| Mentra Live | MAYBE | Android-based but MentraOS mediates; possible via native sideload |
| XReal Aura | N/A | Tethered — app runs on the puck, display is direct |
| Rokid Glasses | YES | Full Android with WiFi |
| Epson Moverio | YES | Full Android with WiFi |

---

## Display Specifications (Critical for UI/ColorMatrix)

| Device | Resolution | Color | Type | FoV |
|--------|-----------|-------|------|-----|
| Vuzix Z100 | 640x480 | Monochrome GREEN | microLED waveguide | 30° |
| Vuzix Blade 2 | 480x480 | Full color | waveguide | 20° |
| Mentra Live | TBD | Full color | camera-based (not AR overlay) | N/A |
| XReal Aura | TBD (high) | Full color | waveguide | 70° |
| Rokid Glasses | 1920x1080 | Full color | micro OLED | 50° |
| Epson BT-45 | 1920x1080 | Full color | Si-OLED binocular | 34° |

Only the Vuzix Z100 is monochrome green. All others are full color.
Our MONO_GREEN ColorMatrix is specifically for Z100 (and future mono displays).

---

## Recommended Implementation Priority

1. **Tier 1 first:** Target Epson Moverio + Rokid + Vuzix Blade — standard Android APK
2. **Tier 2 parallel:** Vuzix Z100 companion mode — Ultralite SDK for HUD text overlay
3. **Tier 3 deferred:** Mentra MiniApp + XReal Android XR — platform-specific development

---

## SDK Dependencies to Add

| SDK | Dependency | Purpose | Tier |
|-----|-----------|---------|------|
| Vuzix Ultralite | `com.vuzix:ultralite-sdk-android` (JitPack) | Z100 BLE display control | Tier 2 |
| Moverio Basic Function | Epson Maven repo | BT-45 sensor/display access | Tier 1 (optional) |
| Rokid UXR | GitHub releases | Spatial features (optional) | Tier 1 (optional) |
| MentraOS | npm/TypeScript | MiniApp development | Tier 3 |
| Android XR | Google Maven | XReal Aura spatial | Tier 3 |

---

## Open Questions After Research

- [ ] Can we sideload native APK on MentraOS? (It's Android-based, MIT licensed)
- [ ] Vuzix Z100: What's the max image size/frequency we can push via Ultralite SDK Canvas?
- [ ] Vuzix Z100 via MentraOS/AugmentOS: Does AugmentOS provide a different (higher bandwidth) display API?
- [ ] Rokid: Does the Rokid AR companion app allow third-party display streaming?
- [ ] Epson BT-45: Does the tethered USB-C mode offer lower-latency display than WiFi?

---

## Sources
- [Vuzix Ultralite SDK (GitHub)](https://github.com/Vuzix/ultralite-sdk-android)
- [Vuzix Z100 Product Page](https://www.vuzix.com/products/z100-smart-glasses)
- [Vuzix Developer Center](https://www.vuzix.com/pages/developer-center)
- [MentraOS (GitHub)](https://github.com/Mentra-Community/MentraOS)
- [MentraOS Platform](https://mentraglass.com/os)
- [Mentra Live Product Page](https://mentraglass.com/live)
- [Android XR SDK Developer Preview 3](https://android-developers.googleblog.com/2025/12/build-for-ai-glasses-with-android-xr.html)
- [XReal Aura (Android XR)](https://www.xreal.com/us/aura)
- [XREAL SDK Documentation](https://docs.xreal.com/)
- [Rokid UXR SDK (GitHub)](https://github.com/RokidGlass/UXR-docs)
- [Rokid AR Platform](https://ar.rokid.com/sdk?lang=en)
- [Epson Moverio Basic Function SDK](https://tech.moverio.epson.com/en/basic_function_sdk/)
- [Epson Moverio BT-45C](https://epson.com/For-Work/Wearables/Smart-Glasses/Moverio-BT-45C-AR-Smart-Glasses/p/V11H970020)
