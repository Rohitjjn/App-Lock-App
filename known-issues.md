# Known Issues — App Lock

## 1. Purpose
This file tracks all active, resolved, or ignored engineering issues, optimization limits, and environmental bugs discovered within the SafeLock App Lock application.

---

## 2. Active Issues Tracker Table

| ID | Date Found | Feature Area | Description | Severity | Workaround Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ISS-01** | 2026-06-14 | **Accessibility** | Window states do not track third-party split-screen launches in some OEM versions | **Medium** | Workaround: Implemented secondary polling checks in foreground monitor fallback |
| **ISS-02** | 2026-06-14 | **Stealth Mode** | Dialer launcher is blocked on some customized dialer implementations | **Low** | Workaround: Add persistent notification shortcut to quick panel |
| **ISS-03** | 2026-06-14 | **Intruder Photo** | Front camera shutter makes sound on specific device configurations | **Low** | Workaround: Capture image frame directly from camera stream without flash trigger |
| **ISS-04** | 2026-06-14 | **Performance** | RAM usage spikes by 25MB when binding CameraX instances first time | **Low** | Workaround: Lazy load and cache CameraX instances strictly on first wrong password entry |

---

## 3. High-Priority System Engineering Challenges

### 1. OEM Aggressive Background Killers (The "Don't Kill My App" challenge)
* **Problem**: Devices manufactured by Xiaomi, Huawei, and OnePlus close background persistent KeepAliveServices despite starting with `START_STICKY` flag and declaring Foreground status.
* **Remediation**: Integrate custom settings redirects inside `RealmeOemHelper.kt` to prompt the user to enable "App Auto-start", "Background Battery Optimization Whitelist", and "Lock Recent Task".

### 2. Android 14/15 Special Use Foreground Service Declaration
* **Problem**: Setting high-priority keepalive actions without correct manifests results in `SecurityException` runtime aborts on target Android 14 SDK systems.
* **Remediation**: Register `<service>` with `android:foregroundServiceType="specialUse"` attribute accompanied by a clear explain-tag description metadata block in `AndroidManifest.xml`.

---

## 4. Issue Log Submission Template
To submit or register a newly discovered bug file, use the following exact structure:
```
## [YYYY-MM-DD] — ISS-[XX] — [Short Title]
- **Found In**: App Version [e.g. v2.0.0]
- **Tested Device**: [e.g. Pixel 8 / Android 14]
- **Severity**: [High/Medium/Low]
- **Detailed Explanation**: [Describe the precise steps to reproduce the failure]
- **Proposed Workaround**: [Describe potential bypass approach]
```

---

## 5. FUTURE UPDATE RULES (MANDATORY)

┌─────────────────────────────────────────────────────────────────────────────┐
│  WHEN TO UPDATE THIS FILE:                                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  1. After discovering a new runtime error or bug:                          │
│     • Add new row to "Active Issues Tracker Table".                        │
│     • Fill out Issue Log Submission entries using exact template layout.    │
│                                                                              │
│  2. When an issue has been successfully resolved inside source code:        │
│     • Relocate entry status to "Resolved".                                  │
│     • Fill in exact commit versions or date of patch.                        │
│                                                                              │
│  3. NEVER:                                                                 │
│     • Hide critical regressions or architectural bypasses.                  │
│     • Remove unresolved rows from active issues list tracker.               │
└─────────────────────────────────────────────────────────────────────────────┘

=== FILE 5 COMPLETE ===
