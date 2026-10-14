# Loop 1 — 2026-10-14

## A. TEAM ROSTER

These are fictional senior review personas, each modeled with the requested 50+ year
expert lens in exactly one discipline. They are not independently executed agents or
claims of real human experience. One coding agent performed the work.

| Name | Single field | Signature lens |
|---|---|---|
| Adrian Cole | Architecture & Code Audit | Follow trust and state ownership across module boundaries. |
| Daniel Mercer | QA & Reliability | Exercise missing, failed, repeated and late events. |
| Elena Voss | UI Design | Make visual state consistent with the chosen design system. |
| Sofia Reyes | UX & Interaction | Every loading, empty and error state needs a clear next action. |
| Marcus Reed | Product Strategy | Favor useful protection controls over decorative features. |
| Maya Chen | Security Architecture | Fail closed without erasing existing credentials or locks. |
| Owen Brooks | Performance Engineering | Bound background work and tie resources to their owners. |
| Leila Haddad | Inclusive Privacy Design | Respect consent and expose understandable accessible controls. |
| Priya Nair | Test Automation & Delivery | Make checks reproducible and preserve signing identity. |
| Victor Hale | Code Quality & Documentation | Distinguish executable behavior from aspirational claims. |

## B. AUDIT SUMMARY

### Repository map and coverage

Baseline: `709eba714697c3acc43ff2a95fbe70124d63de82`, 139 tracked files.
Single Gradle Android `:app`; Kotlin/Compose/Material3, manual Application dependency
container, AndroidViewModel + Flow + Room repository, SQLCipher and encrypted prefs.
Entry points: MainActivity, non-exported LockActivity, BootReceiver, detector and
Quick Settings services. Dependencies: catalog + app build script; release-only CI
at baseline. There were no tests in the active module; template tests live only in
the archived `assets/.aistudio` tree. Existing docs describe older accessibility and
offline behavior that does not match the manifest/detector/updater.

**Coverage limitation:** mapped the whole tracked repository, reviewed active data,
service, utility, theme, app-list and credential paths, and inspected security-relevant
settings sections. Large decorative sections of SettingsScreen/Onboarding/UserGuide,
archived duplicate source, binary resources and all historical documentation were
not exhaustively read line-by-line. This is a risk-focused first pass, **not a completed
whole-repository or independent multi-agent audit**. No claims are made about unread
code. A full audit remains a next-loop obligation.

### Consolidated findings

Evidence below is at the **baseline commit**, unless marked updated. Counts are
**2 Critical, 11 High, 9 Medium, 2 Low (24 total)**. “Fixed” means patched in source,
not device verified. Gradle verification subsequently passed (section D). Combined rows deduplicate overlapping persona findings.

| ID | Severity / reviewer | Evidence (`app/src/main/java/com/example/` unless noted) | Problem / impact / proposed fix | Disposition |
|---|---|---|---|---|
| F01 | Critical — Maya | ui/PinPad.kt:200; PasswordEntryPad.kt:110; PatternView.kt:79 | A null hash authorizes any credential for that selected method. Require successful verification, including missing/malformed rejection. | Fixed |
| F02 | Critical — Priya | .github/workflows/build-and-release.yml:29–44 | Public release-key passwords and a new key every run; exposure and incompatible installed updates. Restore original key from secrets, replace exposed passwords without changing certificate. | Workflow fixed; owner key/password action needed |
| F03 | High — Maya | util/LockSessionManager.kt:144 | Any `com.example.*` package is exempt from locks. Exempt only exact application ID. | Fixed |
| F04 | High — Adrian | util/LockSessionManager.kt:28–30 | Cache clear/addAll publishes an empty/partial lock set. Publish defensive immutable snapshots atomically. | Fixed |
| F05 | High — Maya | data/local/EncryptedPrefs.kt:18–88 | Keystore exception silently switches to private plaintext preferences with device-derived XOR DB-key obfuscation. A safe migration/error-state design is required. | Deferred: preserve fallback users/data |
| F06 | High — Maya | ui/LockScreen.kt:964–974 | Recovery guesses never count toward cooldown; countdown floors the final partial second and callbacks do not re-check deadline. Use persistent checks and count failures. | Fixed for LockScreen; central settings policy still F11 |
| F07 | High — Leila | ui/LockActivity.kt:72–77; MainActivity.kt:58 | Secure-window protection removed/absent on credential and gallery windows. Restore FLAG_SECURE. | Fixed |
| F08 | High — Leila | ui/LockScreen.kt:133–147; util/IntruderCameraHelper.kt:23–60 | Capture ignores saved opt-out and late camera preparation can bind after departure. Guard permission + preference, invalidate pending preparation, release on disposal. | Fixed; lifecycle/device tests pending |
| F09 | High — Daniel | service/AppDetectorService.kt:88–91, 114–142 | Initial polling races cache hydration; only package transitions are checked, so a lock-list/timeout change or suppressed launch can be missed while the package remains foreground. Coordinate readiness and reevaluation. | Deferred: detector redesign needs device trace |
| F10 | High — Daniel | AndroidManifest.xml:55; ui/LockActivity.kt:68–111 | singleTask activity reads package only in onCreate; no onNewIntent implementation. Rebind/cancel stale auth on target change. | Deferred: lifecycle tests first |
| F11 | High — Maya | ui/SettingsScreen.kt:1181,1613,1698,2727 | Settings credential checks do not share LockScreen attempt policy; repeated guesses remain possible. Centralize persisted throttling. | Deferred: cross-flow auth policy |
| F12 | High — Marcus | MainActivity.kt:190–195; data/local/EncryptedPrefs.kt:406 | App/settings gate is optional and remembered unlock is not lifecycle-reset. Someone with app access may alter protections. Design reauthentication for sensitive actions. | Deferred: preserve configured flows |
| F13 | High — Daniel | util/AutoUpdater.kt:66–110; MainActivity.kt:144–152 | Download errors are swallowed, dialog remains busy, resources/timeouts/cancellation unmanaged. Return outcome, always clear busy, close streams, stage partial files. | Fixed |
| F14 | Medium — Maya | util/HashUtil.kt:35–51 | Empty salt reaches invalid PBEKeySpec; hashes compared by early-return array equality. Validate sizes/use constant-time comparison. | Fixed; legacy encoding/work factor retained |
| F15 | Medium — Owen | service/AppDetectorService.kt:51,88,247 | Service scope outlives onDestroy; starts polling even if screen already off. Cancel entire scope and initialize screen state. | Fixed |
| F16 | Medium — Owen | ui/MainViewModel.kt:196–217; AppListScreen.kt:569 | Prewarm all icons with unbounded jobs and retain every icon forever before first display. Load visible icons on IO into bounded cache. | Fixed |
| F17 | Medium — Sofia | ui/AppListScreen.kt:63,300–314; MainViewModel.kt:131–218 | Search changes apparent total; empty/error states misleading, no refresh/retry. Separate count from filtering, add refresh and honest states. | Fixed |
| F18 | Medium — Elena | ui/LockScreen.kt:156; PinPad.kt:92; PasswordEntryPad.kt:92; PatternView.kt:132 | Controls follow OS mode even when user selects a different app theme. Propagate effective theme. | Fixed |
| F19 | Medium — Leila | ui/AppListScreen.kt:413,474 | Clickable row and separate switch fragment semantics; redundant icon labels. Expose one merged toggle role/state and decorative icon. | Fixed; TalkBack pending |
| F20 | Medium — Priya | .github/workflows/build-and-release.yml:3; app/build.gradle.kts:35–40 | No PR verification; debug key path refers to absent ignored file. Add verification and standard debug signing. | Fixed in configuration; CI pending |
| F21 | Medium — Maya | util/BiometricHelper.kt:103–104 | Key helper exists but authenticate explicitly omits CryptoObject. Design enrollment invalidation/fallback before claiming crypto-bound authentication. | Deferred: device/hardware tests |
| F22 | Medium — Marcus | util/AutoUpdater.kt:38–53 | Any unequal tag offered, first asset may not be an APK. Compare numeric stable versions and select APKs. | Fixed; archive identity verification deferred |
| F23 | Low — Victor | README.md:18–21; project-blueprint.md:24–26 | API-key setup and network-free/accessibility/zero-latency claims do not match active implementation. Document actual behavior and limits. | README corrected; historical docs annotated |
| F24 | Low — Adrian | data/local/AppDatabase.kt:13,31 | Destructive migration fallback, no exported schema, though database is still v1. Before any schema bump use explicit tested migrations/schema exports. | Deferred; no schema change in loop |

Verification additionally exposed two baseline lint blockers: the legacy back override
was replaced with a lifecycle-aware secure-home callback (`203b3c4`), and the already
SDK-gated pre-34 tile API was isolated behind a runtime check with a narrowly scoped
lint exception (`5488791`). No global lint baseline/error suppression was added.

No actual private signing key or live API token was found in inspected active files.
The release workflow's public passwords were removed. Never rotate the signing
certificate casually: installed updates must retain that identity. The old PBKDF2
10,000-iteration format is deliberately retained until a versioned migration and
on-device performance budget are available. No production dependency was added;
no telemetry/new network destination was added.

## C. CHANGES MADE

| Category | Commit | Files / change / why |
|---|---|---|
| Docs/security hygiene | 74dd60e | .gitignore, backup-manager.md: tracked baseline archive location; ignore backup and key artifacts. |
| Bugs + security | c0fef94 | HashUtil + three entry pads + tests/tools: fail closed, malformed hash rejection, timing-safe equality, preserve legacy hashes, suppress duplicate password submits during error feedback. |
| Security | 0a8c00a | LockSessionManager, LockedPackageCache + tests: atomic immutable cache; exact self exemption; monotonic in-memory session timing. |
| Performance/reliability | 1ecbe53 | AppDetectorService: structured scope cleanup, screen-state initialization, clear pending launches on exceptions. |
| Privacy/security | c648558 | MainActivity, LockActivity, LockScreen, IntruderCameraHelper: secure windows and consent-aware/lifecycle-aware capture. |
| Feature + UX + performance | d174163 | AppListScreen, MainViewModel, strings.xml, AppRowSemanticsTest: explicit refresh/retry, correct total count and empty copy, saved filter, one switch target; lazy bounded 128-icon cache. |
| Bugfix/update UX | 61641a9 | AutoUpdater, ReleaseVersion, MainActivity, strings/tests: timeouts, cancellation, stream cleanup, .part staging, retryable UI, numeric update ordering/APK filtering, progress deduplication. |
| Tests/CI | a004df8 | Two workflows + app build: PR verification, standard debug key, existing release secrets, pre-sign checks and manual-tag validation. |
| UI polish | 881859a | Theme + four lock controls: honor selected light/dark mode consistently. |
| Security + tests | 347fd04 | LockScreen, CooldownPolicy/tests: round remaining seconds up, persisted callback checks, throttle recovery attempts. |
| CI follow-up | c9c089d, cb6df3f, e14d5cc | Use hosted SDK after setup action failure; surface bounded failure diagnostics through check annotations because log-download hosts are blocked. |
| Bugfix review | a2a6d99 | Snapshot asynchronous icon state for a stable Kotlin null check. |
| Reliability/CI | 203b3c4, 5488791 | Lifecycle back dispatcher retains secure-home behavior; pre-34 tile helper documents the API compatibility lint exception; XML CI diagnostics. |
| Docs | af723e9 + final report update | README and loop/tracking records: current architecture, release-key recovery constraints, honest audit gaps and next prompt. |

No DB version, credential encoding, setting keys, app ID or persisted lock rows changed.
Null credentials now deny instead of bypassing; damaged configurations may require
recovery/settings rather than silently opening a protected app. Existing optional
biometric behavior is retained. Screenshots are intentionally blocked on sensitive
activities. No benchmark improvement claim is made without measurements.

## D. VERIFICATION RESULTS

### Local checks (sandbox)

Executed locally:
- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`: initially
  failed before build because no Java was installed.
- Installed host-only `jdk4py==21.0.8.2` via PyPI and Kotlin compiler `2.2.10` from
  npm under ignored `/home/user/.cache/safelock-tools`. They are not app dependencies.
  GitHub compiler download failed on blocked `release-assets.githubusercontent.com`.
- Re-ran Gradle with Java: failed downloading Gradle from `services.gradle.org`
  (SSL handshake terminated by network restrictions). SDK/Maven access is also not
  available in this sandbox. Dependency resolution/Android compilation did not run.
- `tools/run-core-checks.sh`: **8 regression groups passed**, compiling actual Kotlin
  implementations: null/malformed hashes; independent legacy PBKDF2 vector;
  round-trip/random salts; immutable snapshots; 4 concurrent readers (80,000 reads)
  and 10,000 cache replacements; version ordering; unknown versions; cooldown boundary.
  Repeated as changes were introduced. This is not a Gradle/JUnit execution.
- Kotlin compiler PSI parser: all active Kotlin/test/build scripts parsed with no
  syntax errors (51 files at final local verification). **Not type checking.**
- Python XML parser: 13 active Android XML files parsed.
- PyYAML 6.0.2: both workflow YAML files parsed (not GitHub Actions semantic execution).
- `git diff --check`: passed; changed source/diffs reread for regressions.

### GitHub-hosted Android verification

[Successful run 38515638482](https://github.com/Rohitjjn/App-Lock-App/actions/runs/38515638482) at source/workflow commit
`5488791`: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
**PASSED** (5m24s). Actual XML diagnostics: **10 tests in 5 suites, 0 failures,
0 errors, 0 skipped; lint 0 errors and 187 other findings**. Debug APK assembly
passed. This includes the two Robolectric session tests, not the Compose device test.
Subsequent final edits are documentation only; check the PR for the automatically
triggered documentation-head rerun rather than assuming it has completed.

Earlier failed runs were investigated and addressed:
- `38513427069`: SDK setup action failed before app tasks. Replaced with the hosted
  ubuntu-24.04 SDK. Detailed log-host access was blocked; no invented root cause.
- `38513538013`/`38513849193`: build/lint failures; added bounded log annotations
  because direct `gh run view --log-failed` redirects to a blocked host.
- `38514405154`: lint's MissingSuperCall on the secure back override; replaced with
  OnBackPressedDispatcher, not an unsafe superclass fallback.
- `38515009145`: 10 JVM tests passed; only pre-34 TileService API lint remained.
  Scoped exception is guarded by an SDK check; newer OS still uses PendingIntent.
- KSP background AWT cleanup exceptions appeared in some earlier failed-run logs;
  not present in the final run's annotations. Investigate recurrence rather than
  claiming a KSP upgrade fixed them. Action runtime deprecation warnings remain.

### Unverified / release blockers
- Compose row instrumentation test (including AndroidTest APK compilation).
- Signed release build and original-signature update compatibility.
- Physical/emulated API 28/34/35/36 behavior: interception, screen off, boot, new intents,
  biometric enrollment, camera opt-out/lifecycle, recovery/cooldown, forced themes,
  TalkBack/large-font layout, app refresh and updater retry/install/signatures.
- Existing-data migration from encrypted/fallback prefs and upgrade using original key.
- Startup/memory/battery measurements; HTTP/installer integration tests.
- Exhaustive unread file/dependency vulnerability audit.

## E. GITHUB STATUS

Branch: `arena/a4df26fa-app-lock-app`, pushed to origin. Draft PR: https://github.com/Rohitjjn/App-Lock-App/pull/2. Live check status is recorded in the final user report and PR. Changes are intentionally not merged or released
without device/security regression verification. Android build/unit/lint checks passed as above. No production secrets were configured in this session.

## F. NEXT-LOOP BACKLOG

### Each review persona's three proposals after rereading changed paths

Each proposal lists updated evidence → impact; effort S/M/L; risk Low/Medium/High.
“Reread” refers to the single agent using these lenses, not independent execution.

| Reviewer | Top three proposals |
|---|---|
| Adrian Cole | 1. Detector hydration/reevaluation (`service/AppDetectorService.kt:92,137`) → eliminate missed locks; L/High. 2. SingleTask target ownership (`ui/LockActivity.kt:108`, manifest:55) → stop stale-target auth; M/High. 3. Explicit migrations/schema export (`data/local/AppDatabase.kt:13,31`) → preserve future data; M/Medium. |
| Daniel Mercer | 1. Reproduce startup/resume races (`AppDetectorService.kt:118–142`) → reliable detector tests; M/Medium. 2. Updater fake HTTP/installer tests (`util/AutoUpdater.kt:63–118`) → verify error/cancel cleanup; M/Low. 3. Camera delayed-callback tests (`util/IntruderCameraHelper.kt:39–49`) → prevent post-disposal capture; M/Medium. |
| Elena Voss | 1. Device screenshots for both forced modes (`ui/theme/Theme.kt:58`, `ui/LockScreen.kt:165`) → prove consistency; S/Low. 2. Small-screen/large-font lock layouts (`ui/LockScreen.kt:561`, `ui/PatternView.kt:98`) → avoid clipped authentication; M/Medium. 3. Theme-derived text contrast in empty states (`ui/AppListScreen.kt:373`) → readable states; S/Low. |
| Sofia Reyes | 1. Recovery must establish a new credential rather than partially clearing (`ui/LockScreen.kt:986–990`) → coherent usable recovery; L/High. 2. Settings verification for DEFAULT/multi-lock (`ui/SettingsScreen.kt:1101–1170`) → avoid dead-end fallback; M/High. 3. Refresh progress/result tests (`ui/MainViewModel.kt:135–205`) → preserve locks and retry affordance; M/Low. |
| Marcus Reed | 1. Reauthenticate protection-changing actions (`MainActivity.kt:208–212`, `SettingsScreen.kt:421`) → protect the protector; L/High. 2. Visible permission/protection-health status (`MainActivity.kt:347`, `AppDetectorService.kt:137`) → honest protection state; M/Medium. 3. Intruder retention/confirmation controls (`ui/SettingsScreen.kt:2777–2835`) → bounded private photo history; M/Medium. |
| Maya Chen | 1. Keystore-failure migration and non-destructive error mode (`EncryptedPrefs.kt:18–88`) → remove weak fallback; L/High. 2. Shared persisted auth policy for settings/recovery (`SettingsScreen.kt:1181,1613,1698,2727`; `LockScreen.kt:984`) → close remaining brute-force paths; L/High. 3. Biometric enrollment-bound auth (`BiometricHelper.kt:103`) → cryptographic assurance with recovery; L/High. |
| Owen Brooks | 1. Reuse main executor instead of leaked remembered thread (`MainActivity.kt:232`) → no auth thread leak; S/Low. 2. Off-main sampled wallpaper/gallery decoding (`LockScreen.kt:1261`, `SettingsScreen.kt:2876`) → avoid jank/OOM; M/Medium. 3. Measure 100ms polling/initial one-hour scan (`AppDetectorService.kt:119,147`) → battery/startup budget before tuning; M/Medium. |
| Leila Haddad | 1. Photo encryption/retention plus backup exclusions (`IntruderCameraHelper.kt:75–90`, res/xml/data_extraction_rules.xml:2) → reduce photo exposure; L/High. 2. TalkBack, fonts/RTL and pattern alternative (`PatternView.kt:184`, `AppListScreen.kt:437`) → usable accessible auth; M/Medium. 3. Finish string extraction in unchanged screens (`ui/OnboardingScreens.kt`, `ui/SettingsScreen.kt`) → localizable flows; L/Low (needs full read). |
| Priya Nair | 1. Keep Android CI green and run device tests (`.github/workflows/verify.yml:29`) → merge confidence; M/Low. 2. Restore original signing key/test upgrade (`build-and-release.yml:45–65`) → non-destructive installs; M/High, owner required. 3. Pin Actions SHA/check wrapper checksum (`.github/workflows/*.yml`, gradle/wrapper/gradle-wrapper.properties:3) → supply-chain hardening; S/Low. |
| Victor Hale | 1. Finish full audit of unread UI/archive (`ui/SettingsScreen.kt`, assets/.aistudio/) → coverage honesty; M/Low. 2. Reconcile historical security/architecture docs (`project-blueprint.md:24`, security-protocol.md:16) → accurate promises; M/Low. 3. Extract auth coordinator from presentation (`ui/LockScreen.kt:57–1010`) → testable lifecycle rules without giant rewrite; L/High. |

### Adrian Cole — ranked next-loop selection

1. **Verification gate:** keep Android CI green; add device matrix and original-signature update trial.
   No new security architecture should ship without it (Priya #1/#2).
2. **Detector/target correctness:** test cache hydration, stable foreground rechecks,
   suppressed launches, singleTask new intents and stale biometric callbacks
   (Adrian #1/#2, Daniel #1). Separate atomic patches, no wider polling refactor.
3. **Shared authentication/recovery policy:** extend persisted throttling into settings;
   test mandatory sensitive-action auth and non-destructive reset setup. This loop only
   repaired LockScreen recovery; timezone/clock tampering remains to design.
4. **Fallback storage migration design and fixtures:** highest unresolved storage risk;
   do not simply delete fallback, regenerate DB keys or enable destructive migration.
5. **Small safe wins:** main executor ownership, updater integration tests, forced-theme
   and accessible-list instrumentation. These are preferred alongside #2/#3 next loop.
6. **Privacy/resources:** camera late callbacks, photo retention/encryption/backup and
   downsampled image I/O. Preserve existing images and explicitly confirm deletion.
7. **Crypto/update supply chain:** benchmark versioned KDF, biometric invalidation,
   inspect downloaded APK package/version/signature before opening installer.
8. **Finish audit/docs/localization:** remaining UI/archive/read coverage, documentation
   corrections, schema exports and action/checksum pinning.

High-risk items were deferred for device/recovery compatibility, not dismissed.
Dark mode already existed; it was corrected rather than falsely reported as a new
feature. No heavy dependency or cosmetic rewrite was needed.

## G. NEXT LOOP PROMPT

See `next-loop-prompt.md` for the self-contained verbatim supplied protocol plus
updated loop state. The supplied protocol has sections 1–10, not section 0, and no
“Current loop number”/“Previous loop state” fields; these are added as metadata,
not silently invented protocol sections. The final response includes one prompt block.
