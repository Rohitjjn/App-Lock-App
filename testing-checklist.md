# Testing Checklist — App Lock

## 1. Purpose
This document logs the testing strategy, validation matrices, and local checklist rules for the App Lock utility. It focuses on maintaining solid regressions and high test coverage utilizing standard JVM, Robolectric, and Roborazzi screenshot pipelines.

---

## 2. Testing Framework Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             TEST WORKFLOW                                   │
│                                                                             │
│               [ Local JVM Unit Tests ]  ───┐                                │
│                                             ├─▶  [ Gradle Verification ]    │
│               [ Robolectric OS Mocking ] ───┤            │                  │
│                                             │            ▼                  │
│               [ Roborazzi Visual Testing ] ─┘    [ Stable Compilation ]     │
└─────────────────────────────────────────────────────────────────────────────┘
```

The app's offline-only, system-intercept design is heavily dependent on specific system services. Thus, unit testing is layered:
1. **Local Unit Tests**: Validates helper utilities (`HashUtil`, state serialization) with zero context.
2. **Robolectric Tests**: Simulates full Android context lifecycle blocks (e.g., SQLite insertions, Preferences, and Lifecycle Broadcasts) on the local host.
3. **Roborazzi Screenshot Tests**: Automatically records visual renderings of LockActivity and Settings interfaces to prevent pixel drift.

---

## 3. Comprehensive Automation Testing Checklist

| Functional Area | Test Scenario | Expected Outcome | Testing Method | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Passcode Validation** | PIN input verification | Match returns True; unmatched increments wrong counter | Robolectric JVM | 🟢 Passed |
| **Pattern View** | Input gesture recognition | Connects 3x3 dots, generates correct hash representation | Robolectric JVM | 🟢 Passed |
| **Accessibility Intercept**| App launch window transition | Detects restricted package and fires LockActivity overlay | Robolectric JVM | 🟢 Passed |
| **Stealth Mode** | Hide launcher icon alias | Main launcher alias disabled without closing foreground services | Robolectric JVM | 🟢 Passed |
| **Local Persistence** | SQLCipher database stability | Encrypted write and read database values persist after lifecycle kills| Robolectric JVM | 🟢 Passed |
| **CameraX Capture** | Wrong prompt capture triggers | Triggers silent front-facing image snapshot on 2nd wrong guess| Integration / Manual| 🟢 Passed |
| **Uninstall Protection** | Lock settings and delete | Accessibility catches "uninstall/clear data", intercepts, passes action word | Robolectric JVM | 🟢 Passed |

---

## 4. Sample Test Codes

### Robolectric State Testing
```kotlin
package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.HashUtil
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CredentialHashingTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testGenerateAndVerifyHash() {
        val password = "securePass123"
        val salt = HashUtil.generateSalt()
        val hashed = HashUtil.pbkdf2(password, salt)
        val verified = HashUtil.verify(password, hashed, salt)
        assertTrue("Verification matches stored credentials hash", verified)
    }
}
```

---

## 5. Manual Verification Protocols
Before releasing build versions, perform the following verification steps on physical targets:
1. **Autostart Autotoggle**: Navigate to Settings and enable Autostart. Verify that background services persist.
2. **Notification Panel Access**: Hide the launcher icon from Home and confirm the icon vanishes. Verify the persistent 'App Lock' notification appears in the notification shade. Click 'Restore Icon' and verify the launcher icon successfully reappears. (Dialer code access is explicitly prohibited per Play Store policies).
3. **Device Admin Uninstallation Lock**: Admin Receiver permissions activated. Try uninstalling the application from Android settings and confirm that uninstallation is blocked.

---

## 6. FUTURE UPDATE RULES (MANDATORY)

┌─────────────────────────────────────────────────────────────────────────────┐
│  WHEN TO UPDATE THIS FILE:                                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  1. When adding a new app locking trigger, feature, or service:            │
│     • Add new rows to standard automation verification tables.              │
│     • Write sample test structures verifying key conditions.                 │
│                                                                              │
│  2. When fixing core accessibility/persistence bugs:                        │
│     • Reference the bug resolution test case within the checklist table.    │
│                                                                              │
│  3. NEVER:                                                                 │
│     • Release software updates with outstanding check failures.            │
│     • Disable runtime Robolectric validations without notifying leads.      │
└─────────────────────────────────────────────────────────────────────────────┘

=== FILE 4 COMPLETE ===
