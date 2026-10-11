# Glasses Scanner

Glasses Scanner is an advanced Android application engineered to protect personal privacy and physical space against unauthorized surveillance from smart glasses, camera eyewear, hidden pinhole lenses, and RTSP streaming cameras.

The application combines multi-protocol wireless signal detection (Bluetooth Low Energy and Wi-Fi Direct), sensor-assisted physical detection (CameraX AI frame analysis, Google ML Kit, and Magnetometer EMF meter), and automated geofencing perimeters.

---

## Technical Specifications

- Target SDK: 37 (Android 15+)
- Minimum SDK: 23 (Android 6.0 Marshmallow)
- Language: Kotlin
- UI Framework: Jetpack Compose (Material Design 3)
- Architecture: MVVM with Clean Architecture principles
- Asynchronous Engine: Kotlin Coroutines and StateFlow
- Local Data Persistence: AndroidX DataStore Preferences and Room
- Map Engine: OSMDroid (OpenStreetMap)
- Camera & Vision Engine: CameraX with Google ML Kit Face Detection
- Cloud Sync & Telemetry: Firebase Realtime Database, Authentication, and Analytics
- Monetization Engine: Google Play Billing Library and Google AdMob

---

## Core Features

### 1. Multi-Protocol Wireless Scanner
- Bluetooth Low Energy (BLE) scanning with low-latency signal filtering.
- Detection of manufacturer data signatures including Meta Platforms, Snap Inc., XREAL, Amazon Echo Frames, Bose, Apple, Lenovo, Huawei, Microsoft, Razer, TCL RayNeo, and OEM Spy Eyewear.
- Proximity categorization based on Received Signal Strength Indicator (RSSI) levels: IMMEDIATE (< 1m), NEAR (< 3m), and FAR (> 5m).

### 2. GATT Deep Inspect (BLE De-anonymization)
- Directly queries Bluetooth GATT services on unlabelled or randomized MAC address devices.
- Reads Device Information Service (UUID 0x180A) and Manufacturer Name String (UUID 0x2A29) to reveal true hardware identity.

### 3. AI Visual Scan and ML Kit Detection
- On-device real-time face detection using Google ML Kit.
- Low-light inspection with built-in torch toggle.
- Evidence photo capture stored directly to local gallery (Pictures/GlassesScanner).

### 4. Magnetic EMF Meter
- Uses the device magnetometer sensor (TYPE_MAGNETIC_FIELD) to measure electromagnetic field strength in microteslas (uT).
- Identifies hidden electronics, lenses, and microphones embedded behind two-way mirrors, walls, or fixtures.

### 5. Network Deep Scan (RTSP IP Camera Finder)
- Performs ARP sweeps and multi-threaded socket port checks (ports 554, 80, 8080) on connected Wi-Fi networks.
- Flags hidden streaming IP cameras and high-bandwidth wireless surveillance devices.

### 6. Auto-Zone Security (Geofencing)
- Configures custom 100-meter security perimeters on the map.
- Automatically escalates scanning frequency and triggers background notifications when entering designated perimeters.

### 7. Tactical Map and Community Threats
- Interactive map rendering using OSMDroid with zero dependency on proprietary map keys.
- Plots historical detection points, active real-time threats, and global crowdsourced threat markers.

### 8. Analytics Dashboard and PDF Export
- Summary metrics showing total scans, confirmed smart glasses counts, and peak threat hours.
- Formal A4 PDF Privacy Audit Report generation containing timestamped detection logs.

### 9. Eco-Scan Mode (Battery Saver)
- Dynamic power management that adjusts BLE scan mode (SCAN_MODE_LOW_POWER with batching) and increases Wi-Fi polling delays to conserve battery during background service execution.

### 10. Crowdsource Threat Reporting
- Allows users to report unlisted suspicious MAC addresses directly to Firebase Realtime Database for community signature updates.

---

## Architecture and Project Structure

```
com.glassesscan.app
├── data
│   ├── history          # Local history persistence (Room / DataStore)
│   ├── repository       # Repository implementations for scanner data
│   ├── scanner          # BLE, Wi-Fi, Network Deep Scan, and EMF helpers
│   └── settings         # User preferences and DataStore manager
├── domain
│   ├── model            # DetectedDevice and data domain models
│   └── repository       # Repository interfaces
├── service              # Foreground service, boot receiver, widget, tile
├── ui
│   ├── audit            # Privacy Audit Checklist screen
│   ├── auth             # Firebase Login and authentication
│   ├── history          # History list and Analytics Dashboard
│   ├── map              # OSMDroid Tactical Map screen
│   ├── navigation       # Compose Navigation route definitions
│   ├── onboarding       # Onboarding flow
│   ├── permissions      # Permission handler
│   ├── scanner          # Radar scanner screen and custom components
│   ├── settings         # App settings and preferences
│   ├── theme            # Color palettes, typography, and Material 3 theme
│   └── visual           # Visual AI camera and EMF meter screen
└── util                 # AdMob, Billing, Auth, and Realtime DB utilities
```

---

## Building and Running

### Prerequisites
- Android Studio Ladybug or newer.
- Android SDK 37 installed.
- JDK 17 configured in Android Studio.

### Build Steps

1. Clone the repository:
   git clone https://github.com/your-username/GlassesScan.git
   cd GlassesScan

2. Ensure `google-services.json` is located at `app/google-services.json`.

3. Build the Debug APK:
   ./gradlew assembleDebug

4. Build the Release App Bundle:
   ./gradlew bundleRelease

---

## Permissions Required

The application requests the following permissions for proper operation:
- `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`: To discover and inspect nearby Bluetooth Low Energy smart glasses.
- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`: Required by Android OS for BLE scanning and mapping coordinates.
- `CAMERA`: Required for AI Visual Scan and ML Kit face analysis.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_CONNECTED_DEVICE`: To maintain continuous background monitoring.
- `POST_NOTIFICATIONS`: To deliver top-screen heads-up banner alerts when threats are detected.

---

## Privacy Policy

Glasses Scanner operates under a strict Privacy-First model:
- All signal processing, RSSI calculations, and history logs remain on the local device.
- Location data is used strictly for local map rendering and perimeter calculation.
- Evidence photos captured via AI Visual Scan are saved locally to device storage and are never uploaded.

---

## License and Copyright

Copyright (c) 2026 Circle Mesh Limited. All rights reserved.
