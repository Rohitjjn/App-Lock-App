## 2026-10-14 — Authoritative build versions
The checked-in configuration currently specifies AGP9.1.1, Kotlin2.2.10, Gradle9.3.1, compileSdk36.1, targetSdk35, minSdk28. See gradle/libs.versions.toml for actual dependency versions; historical examples below are not authoritative. No production library added. Host-only JDK/Kotlin tooling was provisioned outside Git to run core regressions; full Android SDK/Gradle downloads were blocked in the sandbox.

# Technical Specifications — App Lock

## 1. System SDK Boundaries

| Environment Metric | Configuration Targets |
| :--- | :--- |
| **Minimum Android SDK** | 28 (Android 9.0 Pie) |
| **Target Android SDK** | 35 (Android 15) |
| **Compile Android SDK** | 36 (Android 16 Developer Preview) |
| **Kotlin Version** | 2.1.0 |
| **Gradle Version** | 9.3+ |
| **Android Gradle Plugin (AGP)** | 8.8.0 |

---

## 2. Platform Permissions

The App Shield requires system-level administrative permissions to secure background lifecycles and catch launches.

| Permission Identifier | Protection Level | Purpose |
| :--- | :--- | :--- |
| `android.permission.FOREGROUND_SERVICE` | Normal | Core KeepAlive Foreground service tracking |
| `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` | Normal (FGS type) | Persistent FGS Category with Special Use metadata |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Normal | Auto-reboot startup listener registration |
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | Signature / Privileged | Sub-10ms App launch interception through TYPE_WINDOW_STATE_CHANGED |
| `android.permission.BIND_DEVICE_ADMIN` | Signature / Privileged | Device Administration permissions blocking app uninstallation |
| `android.permission.USE_BIOMETRIC` | Normal | Biometric gate unlocking and settings verification |
| `android.permission.CAMERA` | Dangerous | Front camera silent snap configuration for intruders |

---

## 3. Libraries & Dependencies

All dependencies are centrally configured within Version Catalogs (`libs.versions.toml`).

```toml
# Modern Library Stack
room-runtime = "2.6.1"
room-compiler = "2.6.1"
room-ktx = "2.6.1"
security-crypto = "1.1.0-alpha06"
biometric = "1.2.0-alpha05"
camerax = "1.4.0"
sqlcipher = "4.6.0"
compose-bom = "2024.12.01"
```

---

## 4. Performance & Memory Thresholds

- **Startup Latency**: <12ms execution path during splash loading
- **Interception Delay**: <10ms window identification via TYPE_WINDOW_STATE_CHANGED
- **Active Memory Footprint**: <42MB standby RAM consumption
- **Intruder Photo Size**: Cached capture capped at 80KB (240x320 silent resolution)
