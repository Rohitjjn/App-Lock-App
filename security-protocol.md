## 2026-10-14 — Source-backed security status
- Patched: null/malformed credential denial, MessageDigest.isEqual verification, atomic lock cache and exact app-ID exemption; FLAG_SECURE restored on both activities; intruder capture now checks opt-in/permission and invalidates late preparation; recovery attempts/callbacks check persisted cooldown.
- Compatibility: PBKDF2-HMAC-SHA256, 10000 iterations, 16-byte random salt, 32-byte hash, existing saltBase64:hashBase64 format retained. Higher work factor requires versioning/measurement, not an in-place constant change.
- Known gaps: weak Keystore-failure fallback remains pending non-destructive migration; biometric prompt not CryptoObject-bound; photos private but not separately encrypted; settings verification lacks shared throttling. Historical Argon2/zero-network/encrypted-photo/progressive-backoff claims below are NOT current guarantees.
- Release workflow public passwords removed; original key identity must be retained and exposed keystore passwords changed securely. Never paste secrets into chat/Git. No new network destination/telemetry.
- Validation: pure Kotlin regression groups passed; Android/device paths unverified. See loop report.

# Security Protocol — App Lock

## 1. Purpose
This document defines ALL security measures, encryption methods, and data protection rules.
Any code handling user credentials, biometric data, or app lock states MUST follow this protocol.

---

## 2. Security Architecture

┌─────────────────────────────────────────────────────────────────────────────┐
│  SECURITY LAYERS                                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  LAYER 1: UI / INPUT                                                         │
│  • No plaintext password display                                             │
│  • Auto-hide input after 3 seconds of inactivity                             │
│  • Screenshot blocked on lock screen via Window flag (FLAG_SECURE)           │
│                                                                              │
│  LAYER 2: VALIDATION                                                         │
│  • Hash comparison ONLY (never compare plaintext)                            │
│  • Rate limiting: 5 attempts -> 30 second lockout                            │
│  • Progressive delay: 5s, 10s, 20s, 30s, 60s after each failure              │
│                                                                              │
│  LAYER 3: STORAGE                                                            │
│  • AES-256-GCM encryption for ALL sensitive data                             │
│  • Android Keystore for key generation and storage                           │
│  • EncryptedSharedPreferences for settings and hashes                        │
│  • Room DB with SQLCipher for locked apps lists                              │
│                                                                              │
│  LAYER 4: BIOMETRIC                                                          │
│  • CryptoObject with setUserAuthenticationRequired(true)                     │
│  • Invalidated on new biometric enrollment (security requirement)            │
│  • Fallback to PIN/Pattern if biometric hardware fails                        │
│                                                                              │
│  LAYER 5: MEMORY                                                             │
│  • Clear password variables after validation (overwrite with zeros)          │
│  • No logging of sensitive data (even in debug builds)                       │
│  • ProGuard/R8 rules to obfuscate security classes                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

---

## 3. Encryption Specifications

| Component | Algorithm | Key Size | Storage | Library |
| :--- | :--- | :--- | :--- | :--- |
| **Master Key** | AES-256-GCM | 256-bit | Android Keystore | AndroidX Security |
| **Password Hash** | Argon2id / PBKDF2 | 256-bit | EncryptedSharedPrefs | BouncyCastle / Native |
| **Fallback Hash** | SHA-256 + Salt | 256-bit | EncryptedSharedPrefs | Native Java Crypto |
| **Biometric Key** | AES-256-CBC | 256-bit | Android Keystore | BiometricPrompt CryptoObject |
| **Settings Data** | AES-256-GCM | 256-bit | EncryptedSharedPrefs | AndroidX Security |
| **Database** | AES-256-CBC | 256-bit | Local SQLCipher Storage| SQLCipher for Room |

---

## 4. Coding & Configuration Guidelines

### Secure Window Flags
To block screenshot capabilities and app preview exposures inside system recents/multi-task lists, `LockActivity.kt` must always call `FLAG_SECURE` in its `onCreate` life-cycle block:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
    )
}
```

### Salted Hashing in Kotlin
Plaintext hashing is vulnerable to rainbow table attacks. Always generate a unique random salt for each biometric state setup or manual code configuration.
```kotlin
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

fun hashCredential(credential: String, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(credential.toCharArray(), salt, 20000, 256)
    val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    return skf.generateSecret(spec).encoded
}
```

---

## 5. Security Checklist
- [ ] FLAG_SECURE active on LockActivity.
- [ ] No log outputs containing raw passcode details.
- [ ] Keystore master keys stored in secure hardware enclave.
- [ ] Failure counter persistent through service restarts.
- [ ] Silent CameraX photo trigger active under wrong authorization conditions.

---

## 6. FUTURE UPDATE RULES (MANDATORY)

┌─────────────────────────────────────────────────────────────────────────────┐
│  WHEN TO UPDATE THIS FILE:                                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  1. When introducing any new authentication methods:                        │
│     • Update "Security Architecture" layer descriptions.                     │
│     • Append cryptographic rules to "Encryption Specifications" table.       │
│                                                                              │
│  2. When security audits reveal potential flaws:                            │
│     • Record vulnerability and resolution in a new remediation table below.  │
│                                                                              │
│  3. When upgrading cryptographic library dependencies:                       │
│     • Revalidate algorithm compatibility.                                    │
│     • Revise table columns listing version or engine providers.              │
│                                                                              │
│  4. NEVER:                                                                 │
│     • Hardcode raw secret tokens inside code commits.                        │
│     • Lower PBKDF2 iteration values below 10,000 passes.                     │
└─────────────────────────────────────────────────────────────────────────────┘

=== FILE 3 COMPLETE ===
