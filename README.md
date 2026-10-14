# SafeLock — Android App Lock

Native Kotlin/Jetpack Compose app locking with PIN, pattern, password and biometric
verification, encrypted Room/SQLCipher storage, optional intruder photos, and a
Quick Settings shield tile. Only the root `app/` module is built; `assets/.aistudio/`
is an older source snapshot, not a second maintained application.

## Build and test

Use Android Studio with JDK 17+, Android SDK **36.1**, build tools **36.0.0**, and
accepted SDK licenses. The checked-in Gradle wrapper is 9.3.1; dependency versions
are in `gradle/libs.versions.toml`. Minimum Android version is 9 (API 28), target 35.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest  # requires a device/emulator
```

Debug signing uses the standard automatically generated local Android debug key.
No Gemini/API key is required by the active app. Do not put production secrets in
`.env`: the existing Secrets plugin can expose values in generated app code.

With JDK 17+ and Kotlin 2.2.10+ on PATH, pure Kotlin checks can run without the SDK:

```sh
./tools/run-core-checks.sh
```

These check hashing compatibility, atomic cache snapshots, release comparisons and
cooldown boundaries. They do **not** replace Android compilation or device tests.

## Protection and privacy boundaries

- Detection uses `UsageStatsManager` polling, not an AccessibilityService. Enable
  Usage Access and Display Over Other Apps. Android/OEM background restrictions
  can delay or suppress the lock activity; this is not an OS security boundary.
- Intruder capture requires both the in-app toggle and camera permission. Images
  are private app files but currently **not separately encrypted**. Delete them
  through Settings → Intruder Selfies Log.
- Sensitive activity windows block screenshots and screen recording.
- The app makes an existing automatic GitHub release check at startup and downloads
  an APK only on request. It is not network-free. This loop adds no telemetry or
  new network destination. Android's installer retains the final installation step.
- Keystore-failure fallback storage and settings authentication/rate limiting still
  require hardening. See `docs/loops/loop-1-report.md`; do not treat documentation's
  historical “impenetrable”, “offline-only” or latency claims as verified guarantees.

## Releasing without breaking installed updates

The release workflow requires an **existing** `vMAJOR.MINOR.PATCH` tag and a stable
signing identity. Configure these GitHub Actions secrets through repository settings:

- `RELEASE_KEYSTORE_BASE64`: base64 of the **original** signing keystore.
- `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_PASSWORD`.
- `RELEASE_KEY_ALIAS` (optional; defaults to `upload`).

Never paste keys/passwords into an issue, PR, or chat. The workflow deliberately
fails if secrets are absent, rather than generating a replacement key. The former
workflow generated a new key per build with public passwords; preserve the original
certificate and change exposed keystore passwords securely. If the original key is
lost, a normal in-place update for those installations cannot be guaranteed. Do not
recommend uninstalling SafeLock as a workaround: that can lose user locks/data.

Update `versionCode` and `versionName` before a release. Test an in-place upgrade on
an existing installation. Manual workflow dispatch takes the existing release tag;
it no longer publishes a release named after a branch.

## Loop records

- [Loop 1 audit, changes and next backlog](docs/loops/loop-1-report.md)
- [Ready-to-paste Loop 2 prompt](docs/loops/next-loop-prompt.md)

Android build/device verification must pass before merging or distributing changes.
