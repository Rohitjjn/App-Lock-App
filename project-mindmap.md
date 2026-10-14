## 2026-10-14 — Current module note
No production dependency added in Loop 1. Root :app now includes pure HashUtil/LockedPackageCache/ReleaseVersion/CooldownPolicy regression checks plus Android test scaffolding. Actual detector uses UsageStats; historical Accessibility descriptions below are not the current runtime. See README for setup and loop report for gaps.

# Project Mind Map — App Lock

## 1. Feature Hierarchy Tree

```
APP LOCK
├── SECURITY CORE
│   ├── Lock Types
│   │   ├── PIN (4-6 digits, custom layout PinPad)
│   │   ├── Pattern (Custom Canvas 3x3 drawing grid)
│   │   ├── Password (Secure alphanumeric keyboard entry)
│   │   └── Biometric (Fingerprint/Face biometric prompts)
│   ├── Validation
│   │   ├── Encrypted Storage (Jetpack Security crypto prefs)
│   │   ├── PBKDF2 with Salt (High-security hashing)
│   │   └── Attempt Limiting (5 wrong entries -> 30s lockout)
│   └── Lock Screen UI
│       ├── Full Screen Overlay (LockActivity with NEW_TASK flags)
│       ├── Active App Icon & Name Display
│       └── Back & System Multi-Task Buttons Blocked
│
├── APP MANAGEMENT
│   ├── Installed Apps Scanner (Filter system/third-party apps)
│   ├── App Toggle lock switch (Real-time DB updates)
│   ├── Locked Apps Database (Encrypted Room SQLCipher)
│   └── App Categories (System Settings, Social, Banking, etc.)
│
├── STEALTH ACCESS
│   ├── Hide App Icon
│   │   ├── Activity-Alias Toggle (com.example.MainActivityLauncherAlias)
│   │   ├── Launcher Grid Refresh (Without rebooting)
│   │   └── Dialer Code Fallback (*#*#1234#*#*)
│   ├── Notification Control
│   │   ├── Persistent Foreground KeepAliveService
│   │   ├── Action Intents (Lock toggle actions)
│   │   └── Quick Settings Tile (SafeLockTileService)
│   └── Dialer Launch
│       ├── *#*#1234#*#* dial code
│       ├── SecretCodeReceiver (Broadcast intercept)
│       └── Biometric Gate before display
│
└── SYSTEM PROTECTION
    ├── Uninstall Block
    │   ├── Device Policy Admin (AdminReceiver)
    │   ├── Settings Interceptor (Accessibility blocker)
    │   └── Biometric confirmation gate
    ├── Background Persistence
    │   ├── Start Sticky Foreground Service
    │   ├── Boot Complete Receiver (Auto-trigger after reboot)
    │   └── OEM Battery Whitelist guides (RealmeOemHelper, etc.)
    └── Data Security
        ├── SQLCipher db encryption
        ├── MasterKey (Keystore AES-256-GCM)
        ├── Intruder Capture (Silent CameraX selfie on fail)
        └── Auto-lock on Screen Off (BroadcastReceiver)
```

---

## 2. Navigation Flow Tree

```
SPLASH SCREEN (Check permissions status & theme)
└── ONBOARDING (First Launch tutorial & instructions)
    └── SET LOCK TYPE (Choose initial PIN, Pattern, or Password)
        └── SET SECURITY QUESTION (Recover lost access)
            └── HOME SCREEN (Main app view containing tabs)
                ├── LOCKED APPS LIST
                │   ├── Search apps (Filter by keywords)
                │   ├── Filter list (System, Locked, Unlocked)
                │   └── Click Toggle (Updates LockedAppDao synchronously)
                ├── SETTINGS SCREENS
                │   ├── Lock Type Change (Reverify old credential)
                │   ├── Stealth (Hide App Icon, Dialer code config)
                │   ├── Cooldown Period (Wrong tries limit, Lock duration)
                │   ├── Intruder Selfie (Toggle CameraX snapshot on fail)
                │   ├── Uninstall Protection (Toggle administrator state)
                │   └── Theme/Accent settings (Dark, light, dynamic tinting)
                └── APP LOCK OVERLAY (Triggered via Window State events)
                    ├── PIN / Pattern / Password Grid UI
                    ├── Biometric Prompt dialog
                    └── Unlock Success -> Resume launcher app
```

---

## 3. Service Dependency Tree

```
FOREGROUND SERVICE (KeepAliveService)
├── NOTIFICATION MANAGER
│   ├── Custom RemoteViews notification body
│   ├── PendingIntent (Toggle service state)
│   └── PendingIntent (Launch MainActivity Router)
├── BROADCAST RECEIVER (BootReceiver)
│   └── Auto-restart on Boot Completed
└── LIFECYCLE
    ├── START_STICKY
    ├── Notification ID 8812 (FGS Special Use category)
    └── OnDestroy -> Attempt system-restart broadcast callback

ACCESSIBILITY SERVICE (AppLockAccessibilityService)
├── WINDOW STATE DETECTOR
│   ├── TYPE_WINDOW_STATE_CHANGED
│   ├── Query current package in foreground
│   └── System launchers filtering list
├── APP LAUNCH INTERCEPTOR
│   ├── Start LockActivity (FLAG_ACTIVITY_NEW_TASK)
│   ├── Exclude from Recents view (FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
│   └── Throttle redundant intents via LockSessionManager
└── UNINSTALL BLOCKER
    ├── Intercept "com.android.settings" package activity detail screens
    ├── Intercept "App info / Uninstall" click triggers
    └── Show Biometric /PIN overlay lock screen instantly

DEVICE ADMIN RECEIVER (AdminReceiver)
├── DEVICE POLICY MANAGER
│   └── Verify admin active setting
└── ADMIN STATUS LISTENER
    ├── onEnabled -> Persist protection
    └── onDisabled -> Warning warning shown/Log event
```

---

## 4. Data Layer Dependency Tree

```
UI LAYER (Jetpack Compose ViewState)
└── VIEWMODEL (MainViewModel - StateFlow objects manager)
    └── REPOSITORY (AppLockRepository)
        ├── ROOM DATABASE (AppDatabase via SQLCipher helper)
        │   ├── DAO interface (LockedAppDao)
        │   ├── Entity (LockedApp table with package fields)
        │   └── Entity (SecurityQuestion table for passwords recovery)
        ├── ENCRYPTED PREFERENCES (EncryptedPrefs)
        │   ├── MasterKey (Hardware-backed AES-256)
        │   ├── Stored Password Hashes (Salted and PBKDF2 hashed)
        │   └── Settings Flags (Vibration, timer count, cooldown, themes)
        └── DATA UTILITIES
            ├── HashUtil (Salt generate, SHA-256 PBKDF2)
            └── Converters (Serializer lists mapping types)
```

---

## 5. Security Flow Tree

```
USER CREDENTIAL LEVEL (PIN, Pattern, Password)
└── INPUT INTERFACES (PinPad, PatternView, PasswordPad)
    ├── Check input integrity (Null, min-length limits)
    └── Dispatch to verification helper
        └── IF BIOMETRIC -> BiometricPrompt API -> System Keystore
        └── IF MANUAL -> Hash comparison
            ├── Jetpack EncryptedPrefs (Read target hash)
            ├── SHA-256 Hash algorithm
            ├── Salt recovery from storage
            └── Comparative Match check
                ├── MATCH? -> Clear incorrect counter -> UNLOCK SUCCESS
                │             ├── Update LockSessionManager (grants grace window)
                │             └── finishAndRemoveTask() screen overlay
                └── FAIL?  -> Increment incorrect limit
                              ├── IF LIMIT > Threshold -> Silent photo (CameraX)
                              └── IF LIMIT = MAX -> Cooldown period (30s)
                                                    └─ Lock interface block
```

---

## 6. State Management Tree

```
APP SECURITY STATE
├── LOCKED APPS STATE (StateFlow<List<LockedApp>>)
│   ├── Loading (Gathering installed packages database)
│   ├── Success (Lists of locked apps available)
│   └── Error (Permission or manifest reading failures)
├── LOCK OVERLAY STATE (StateFlow<UnlockState>)
│   ├── Idle (Underlying check passing)
│   ├── Prompting (Display secure authentication window)
│   ├── Validating (Checking hash matching)
│   ├── Unlocked (Success trigger, destroy activity wrapper)
│   └── LockedOut (Cooldown active, show countdown overlay)
└── GENERAL SETTINGS SYSTEM (StateFlow<SettingsState>)
    ├── IconHidden: Boolean (Launch alias status)
    ├── DeviceAdminActive: Boolean
    ├── IntruderSelfieOn: Boolean
    ├── CooldownTimerSec: Int
    └── SelectedAccentHex: String
```
