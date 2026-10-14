# Loop 2 — 2026-10-14

## A. TEAM ROSTER

Fictional senior review personas, each using the requested 50+ years-of-experience
lens in exactly one field. These are **not independently executed agents or real
human experience claims**. One coding agent read and changed the repository.

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

### Baseline, map and coverage

Baseline `ea2c3bc` is the merge of Loop 1 PR #2. GitHub confirmed PR #2 merged and
main verification run [38516454828](https://github.com/Rohitjjn/App-Lock-App/actions/runs/38516454828)
succeeded. No Loop 1 changes were duplicated. A local untracked baseline archive was
made under `/home/user/.cache/safelock/loop-2-baseline.tar`; Git history remains the
persistent baseline. Current work stays on `arena/37dbba83-app-lock-app`.

Architecture is unchanged: one Android `:app`, Kotlin/Compose Material3, manual
Application DI, AndroidViewModel/Flow/Room, SQLCipher and EncryptedSharedPreferences.
Manifest entry points: MainActivity, non-exported LockActivity, detector/foreground/
tile services and boot/admin receivers. Gradle catalog, Android manifest, app build
script and verification/release workflows were inspected. `assets/.aistudio` is an
unbuilt historical source snapshot, not a second active app. Existing unit/core and
Compose instrumentation tests were inspected in the touched paths.

**Coverage is still bounded, not a complete file-by-file audit.** This loop fully
read detector, LockActivity, session manager, biometric/camera/updater helpers,
OnboardingScreens and UserGuideDialog; reread authentication, storage-selection,
app-list, MainActivity and ViewModel paths and all new policy/test code. Settings
review covered top-level permission/admin callbacks, authentication controls,
credential dialogs and gallery. Decorative/customization portions of the 3,000+
line SettingsScreen, portions of LockScreen and ViewModel, archive sources, binary
resources and historical docs were not exhaustively reread. Large command output
was sometimes truncated; unread tails are not counted as audited. Prior Loop 1
coverage is evidence for that loop, not a claim of fresh exhaustive review here.

### Consolidated findings

18 deduplicated findings: **0 new Critical, 8 High, 9 Medium, 1 Low**. This is the
reviewed set, not a count of every remaining repository defect. Evidence is relative
to `app/src/main/java/com/example/` unless noted. Fixed rows identify baseline lines
and/or updated implementation; open rows identify updated lines. A source patch is
not a device-verification claim. The prior exposed signing-password rotation and
original-certificate verification obligations remain open; no new secret was found
in the inspected changes.

| ID | Severity / reviewer | Evidence | Problem, impact and proposed fix | Disposition |
|---|---|---|---|---|
| L2-01 | High — Adrian Cole | service/AppDetectorService.kt baseline:92,118–142; updated:99,124–151 | Resume can be consumed before Room hydration; stable targets never rechecked after timeout/cache changes or suppressed launch. Await first emission, retain target and recheck. | Patched; OS traces open |
| L2-02 | High — Daniel Mercer | ui/LockActivity.kt baseline:108,157; updated:137–171,184–212 | singleTask new intent retains old target; late auth can act on another target/lifecycle. Rebind, key composition, cancel prompt and reject old generation. | Patched; device lifecycle open |
| L2-03 | High — Maya Chen / Sofia Reyes | ui/SettingsScreen.kt baseline:1142,1181,1613,1698,2727; updated:1144,1183,1616,1701,2732 | Existing-credential dialogs allow unbounded guesses and give no cooldown state. Use shared persisted policy and live notice. | Patched for credential checks, not all sensitive actions |
| L2-04 | High — Maya Chen | ui/LockScreen.kt baseline:78,140; updated:77,126,461 | Remembered failure counters in separate screens can overwrite shared count. Central synchronized counter/deadline update and success recheck. | Patched; wall-clock/durable-write threat model open |
| L2-05 | Medium — Owen Brooks | MainActivity.kt baseline:232; updated:232–253 | Remembered executor is never shut down; prompt callbacks outlive composition. Use main executor and cancellation/generation on stop/disposal. | Patched |
| L2-06 | Medium — Leila Haddad / Victor Hale | ui/OnboardingScreens.kt:187,193; ui/UserGuideDialog.kt:174,230–300 | Military-grade/instant/zero-leak/silent-capture claims misrepresent OS limitations, storage and consent. Correct user-visible explanations. | Patched selected claims, not all historic copy |
| L2-07 | High — Marcus Reed | MainActivity.kt:208–212; ui/SettingsScreen.kt:434 | Optional main gate remains unlocked in composition; master shield toggle is not independently authenticated. Design short-lived grants for sensitive actions. | Deferred; protection-flow/device work |
| L2-08 | High — Maya Chen | data/local/EncryptedPrefs.kt:18–88 | Silent plaintext fallback and device-derived DB-key obfuscation can select wrong store after recovery/update. Non-destructive ownership/migration fixtures first. | Design added; production unchanged |
| L2-09 | High — Adrian Cole / Maya Chen | ui/LockScreen.kt:966–971 | Recovery clears PIN/pattern only, leaving password/method/sequence inconsistent. Stage confirmed replacement and atomically update auth configuration. | Design added; no destructive change |
| L2-10 | High — Maya Chen | ui/SettingsScreen.kt:244–307 | Admin biometric action has no request-generation/lifecycle gate or shared cooldown; null-activity branch directly executes action. Extend sensitive-action coordinator; fail closed. | Open; normal screen has Activity, but fallback remains unsafe |
| L2-11 | Medium — Leila Haddad | util/IntruderCameraHelper.kt:96–105,125–130 | Old capture completion releases global provider; delayed legacy capture is not generation-bound. Bind capture ownership and delayed callbacks. | Deferred for camera/device tests |
| L2-12 | Medium — Owen Brooks | ui/SettingsScreen.kt:2881,2969; ui/LockScreen.kt:1248 | Full-size images decode in composition/main thread. Downsample off-main with explicit missing/corrupt states. | Deferred; preserve existing images |
| L2-13 | Medium — Leila Haddad | res/xml/data_extraction_rules.xml:2; AndroidManifest.xml:26 | Empty extraction rules do not explicitly exclude preferences/DB/photos from device transfer despite allowBackup=false intent. Add supported exclusions with platform tests. | Open; not claiming an observed data transfer |
| L2-14 | Medium — Daniel Mercer | ui/OnboardingScreens.kt:500,518 | Usage/overlay settings launches are unguarded; missing OEM handlers can crash onboarding. Add safe intent fallback and error state. | Open |
| L2-15 | Medium — Elena Voss | ui/UserGuideDialog.kt:120; ui/OnboardingScreens.kt:221 | Single-line guide tabs and non-scrollable intro content constrain large-font layouts. Test then use adaptive/scrollable content. | Open; visual behavior not measured |
| L2-16 | Low — Victor Hale | data/local/AppDatabase.kt:13,31 | No exported schema; destructive migration fallback remains. Export v1 and test explicit migrations before any bump. | Open; no schema change |
| L2-17 | Medium — Maya Chen | util/AutoUpdater.kt:109–127 | Installer opened without app-side archive package/version/signer validation. Add checks and failure/cancellation fixtures; Android installer remains a separate gate. | Open |
| L2-18 | Medium — Priya Nair | .github/workflows/verify.yml:36 | Existing instrumentation source not compiled in verification. Add assembleDebugAndroidTest; runtime device job still required. | Compile gate added |

There was no justification for a heavy dependency or a speculative storage/crypto
rewrite. No production dependencies, network destinations, telemetry, stored key
names, hash formats, database schemas, app IDs or lock rows were changed.

## C. CHANGES MADE

| Category | Commit | Files and reason |
|---|---|---|
| Bugs / architecture | `adf3956` | AppDetectorService, ForegroundTarget, ForegroundTargetTest: await initial cache, keep stable foreground target, retry/recheck policy; resolve launcher once per polling session rather than each poll. |
| Security / lifecycle | `d192643` | LockActivity, BiometricHelper, AuthRequestGeneration + tests: handle new intents, reset per-target composition, reject obsolete callbacks, cancel prompts; keep failed biometric scan non-terminal. |
| Security | `f744818` | AuthAttemptLimiter + tests, EncryptedPrefs, LockScreen, SettingsScreen, strings: shared three-failure/30-second persisted credential throttling using existing keys; atomic count/deadline edit. |
| Performance | `9b96769` | MainActivity: main executor, stop/disposal cancellation and callback token instead of leaked executor. Detector launcher resolution was also bounded in `adf3956`. |
| UI/UX | `6504919` | AuthCooldownNotice + Compose test, SettingsScreen, strings: rounded Material3 error-container panel, scalable typography, live localized retry duration in all four credential dialogs. Existing dark theme retained. |
| Features | `af9a8e4` | AppListScreen, MainViewModel, strings, LockSessionManagerTest: “Lock apps again” menu action clears temporary sessions only, with snackbar distinguishing shield-off state. No destructive confirmation is needed because no saved data is deleted. |
| Tests / delivery | `47c4c70` | verify.yml, core runner/script, LifecycleRegressionChecks: compile instrumentation APK and extend host checks from 8 to 11 policy groups. |
| In-app docs / privacy | `d6970cb` | OnboardingScreens, UserGuideDialog, strings: localizable, truthful protection/camera/session/KDF descriptions; document new relock action. |
| Compile regression fix | `d7ea306` | SettingsScreen: explicitly preserve `(Boolean) -> Unit` contract after BiometricHelper began returning a cancellable prompt. CI found the regression; it was not concealed. |
| Compatibility tests | `eb3582d` | EncryptedPrefsAttemptTest: synthetic legacy keys, existing credential/DB-key sentinels and persistence across wrapper recreation. Not a real Keystore/SQLCipher migration test. |
| UX follow-up | `170818c` | AppListScreen: read current persisted shield state for relock feedback, since Quick Settings can change it without refreshing ViewModel state. |
| Design / handoff | `24d87b1` plus verification handoff | storage-recovery-design.md, this report, next-loop-prompt.md, tracking docs: fixture matrix, non-destructive migration/recovery sequence, honest gaps and next-loop protocol. |

## D. VERIFICATION RESULTS

- Installed host-only JDK 21.0.8.2 from `jdk4py` and Kotlin compiler 2.2.10 into
  `/home/user/.cache/safelock`; no generated dependencies committed.
- `tools/run-core-checks.sh`: **PASS, 11 groups** using production Kotlin/JCA. Includes
  existing malformed/legacy hashes, random salts, snapshot concurrency, versions,
  cooldown boundaries plus stable target, generation rejection and shared attempts.
- Local `./gradlew --version`: **FAILED before Gradle execution**, distribution-host
  SSL handshake terminated. No local Android SDK/emulator was available. Local pure
  Kotlin checks are not an Android compilation or runtime substitute.
- Android CI attempt `38517153107`: **FAILED**, Settings callback inferred Boolean→Any
  after returning BiometricPrompt; fixed in `d7ea306`. Earlier superseded run cancelled.
- Final Android CI: **PASSED** at source commit `170818c` in
  [run 38518301076](https://github.com/Rohitjjn/App-Lock-App/actions/runs/38518301076).
  `:app:testDebugUnitTest`: **23 tests across 9 suites, 0 failures/errors/skipped**.
  `:app:lintDebug`: **0 errors / 187 other findings**. `:app:assembleDebug` and
  `:app:assembleDebugAndroidTest` both succeeded. Actions runtime deprecation warnings
  remain. Subsequent changes are documentation-only; no device tests were executed.
- Final XML/YAML parse and `git diff --check`: **PASS**: 13 active XML files,
  2 workflow YAML files, working-tree and baseline-to-HEAD whitespace checks. Initial
  YAML check lacked PyYAML; installed host-only PyYAML 6.0.3 and reran successfully.

- Documentation-head run [38518674395](https://github.com/Rohitjjn/App-Lock-App/actions/runs/38518674395)
  at `b83b492` also **succeeded** with 23 tests and the same lint/APK results, but
  emitted a failure-level annotation: `ksp.com.intellij...ApplicationManager.getApplication()`
  was null while resolving `Application.getService`. This resembles earlier Loop1
  KSP cleanup diagnostics; the cause is **not verified**. Full log download via
  `gh run view --log` failed at the blocked results-receiver host (EOF). Treat this
  as unresolved tooling debt, not a failed Android task or a warning-free run.
  A final documentation-only commit records this observation; its own rerun may be pending.

### Unverified

- Emulator/hardware execution of Compose tests, TalkBack, large fonts, forced-theme
  visual output, interaction with biometric system UI, and OEM background restrictions.
- Actual service Room hydration timing, silent background-launch suppression/retry,
  repeated singleTask target changes, screen-off, rapid app transitions and tile flows.
  New pure policy tests cover decisions, **not service/activity lifecycle integration**.
- Camera late capture ownership, photo backup/retention/encryption and image benchmarks.
- Real Android Keystore/fallback-to-encrypted migration and SQLCipher recovery. The new
  Robolectric fixtures are not evidence of encrypted-store migration safety.
- Signed release and upgrade using the original certificate; exposed old signing
  passwords still require owner rotation without changing signing identity.
- Clock-tamper resistance/power-loss durability, CryptoObject biometrics, updater
  integration/archive identity checks, and a complete repository audit.

## E. GITHUB STATUS

Branch: `arena/37dbba83-app-lock-app` (session-assigned; never switched).
Draft PR: https://github.com/Rohitjjn/App-Lock-App/pull/3
Code and handoff documents are included on the PR branch. No release,
merge, force push, history rewrite or signing identity change performed.

## F. NEXT-LOOP BACKLOG

### Updated-code review: three proposals per persona

Evidence below is updated source; `ui/`, `util/`, `data/`, and `service/` paths are
under `app/src/main/java/com/example/`. These are one agent's explicitly named review
lenses, not independent consensus. Effort S/M/L; risk is implementation risk.

| Reviewer | Three evidence-backed proposals (impact; effort; risk) |
|---|---|
| **Adrian Cole** | 1. Test service hydration/error/retry state (`service/AppDetectorService.kt:94–99,124`) and expose DB failure, not “Active” indefinitely — prevent silent non-monitoring; M/High. 2. Integrate target generation with actual singleTask tests (`ui/LockActivity.kt:158,369`) — prove ownership across stop/new intent; M/Medium. 3. Stage replacement recovery (`ui/LockScreen.kt:966–971`) — preserve valid credentials until confirmed replacement; L/High. |
| **Daniel Mercer** | 1. Replay launch suppression, same foreground timeout and rapid A→B→A traces (`service/AppDetectorService.kt:145–157`) — validate retry fix; M/Medium. 2. Guard OEM permission intents (`ui/OnboardingScreens.kt:500,518`) — prevent onboarding crashes; S/Low. 3. Test updater disconnect/cancel/unknown length (`util/AutoUpdater.kt:80–117`) — verify cleanup and honest progress; M/Medium. |
| **Elena Voss** | 1. Test light/dark/large-font cooldown notices (`ui/AuthCooldownNotice.kt:27–40`) — validate new surface and wrapping; S/Low. 2. Replace one-line narrow guide tabs after screenshot tests (`ui/UserGuideDialog.kt:120`) — readable navigation; S/Low. 3. Make intro pages scroll at large scale (`ui/OnboardingScreens.kt:221`) — avoid clipped explanatory text; S/Low. |
| **Sofia Reyes** | 1. Give recovery a replacement/confirm/cancel flow (`ui/LockScreen.kt:966`) — avoid misleading “security off” result; L/High. 2. Add protection-readiness state (`ui/OnboardingScreens.kt:435,615`; `service/AppDetectorService.kt:228`) — distinguish permission/DB/service issues; M/Medium. 3. Confirm photo deletion and show errors (`ui/SettingsScreen.kt:2821–2825,2920–2923`) — prevent accidental loss/silent failure; S/Low. |
| **Marcus Reed** | 1. Mandatory grants for master shield/settings changes (`ui/SettingsScreen.kt:434`; `MainActivity.kt:210`) — protect configuration, not only target apps; L/High. 2. Device-test “Lock apps again” with shield on/off and timeout (`ui/AppListScreen.kt:209–222`; `ui/MainViewModel.kt:41`) — confidence in useful protection control; S/Medium. 3. Readiness checklist driven by actual state (`ui/OnboardingScreens.kt:435`; `service/AppDetectorService.kt:228`) — actionable rather than absolute safety promises; M/Medium. |
| **Maya Chen** | 1. Implement storage fixture matrix before migration (`data/local/EncryptedPrefs.kt:18–88`; `docs/loops/storage-recovery-design.md`) — highest unresolved confidentiality/data-loss risk; L/High. 2. Extend generation/cooldown gating to admin biometric action and remove null-activity authorization (`ui/SettingsScreen.kt:244–307`) — close remaining auth path; M/High. 3. Add archive package/version/signer checks (`util/AutoUpdater.kt:109–127`) — reject unexpected updates before installer; M/Medium. |
| **Owen Brooks** | 1. Off-main sampled gallery/wallpaper decoding (`ui/SettingsScreen.kt:2881,2969`; `ui/LockScreen.kt:1248`) — reduce frame stalls/memory; M/Medium. 2. Benchmark added steady-target checks (`service/AppDetectorService.kt:145–157`) before changing 100ms cadence — bound CPU/battery without weakening latency; M/Medium. 3. Move KDF verification off-main with serialized results (`util/AuthAttemptLimiter.kt:39–44`; `util/HashUtil.kt:67`) — prevent UI stalls without stale-success races; M/High. |
| **Leila Haddad** | 1. Explicit backup/transfer exclusions (`res/xml/data_extraction_rules.xml:2`) — align consent with private data; S/Medium. 2. Generation-bound capture completion (`util/IntruderCameraHelper.kt:96–105,125–130`) — stale work cannot release new camera owner; M/Medium. 3. Complete localization/TalkBack pass (`ui/OnboardingScreens.kt:615,737`; `ui/SettingsScreen.kt:2931`) — understandable status and distinct image delete labels; M/Low. |
| **Priya Nair** | 1. Run instrumentation on API28/35 (`.github/workflows/verify.yml:36`; `app/src/androidTest/`) — move beyond APK compilation; M/Medium. 2. Validate original-key signed upgrade (`.github/workflows/build-and-release.yml:45–65`) — preserve installed user data; M/High, owner key required. 3. Investigate the recurring KSP cleanup annotation (run38518674395), pin actions and wrapper checksum (`.github/workflows/verify.yml:23–28`; `gradle/wrapper/gradle-wrapper.properties:3`) — supply-chain hardening; S/Low. |
| **Victor Hale** | 1. Finish unread Settings customization/archive audit (`ui/SettingsScreen.kt:1850–2630`; `assets/.aistudio`) — remove audit coverage gap; M/Low. 2. Export v1 schema and test migrations before any bump (`data/local/AppDatabase.kt:13,31`) — avoid future lock-row loss; M/Medium. 3. Reconcile remaining historical/in-app claims (`ui/SettingsScreen.kt:561,615`; `security-protocol.md`) — avoid overstated guarantees; M/Low. |

### Adrian Cole — ranked next-loop selection and deferrals

1. **Verification gate**: API28/35 instrumentation plus detector/new-intent/biometric
   lifecycle traces. Keep current JVM/build/lint/AndroidTest compilation green.
2. **Remaining sensitive-action auth**: admin biometric generation/cooldown/null-activity
   fallback first, then short-lived mandatory grants for shield/recovery changes.
   Shared credential throttling is implemented; it is not full settings authorization.
3. **Storage/recovery fixture gate**: execute the design matrix, expose non-destructive
   startup failure, then stage coherent credential replacement. Do not rush migration.
4. **Safe companion fixes**: OEM settings-intent fallback, confirmation/error handling
   for photo deletion, large-font guide/intro layouts and explicit backup exclusions.
5. **Privacy/performance**: generation-bound camera ownership, sampled off-main images,
   detector/prefs/KDF measurements before optimization. Preserve existing photos.
6. **Updater/supply chain/release**: failure/cancel/archive verification tests, actions
   pinning/checksum, original-key upgrade. CryptoObject/versioned KDF need device plans.
7. **Finish audit/schema/docs/localization**: no schema bump without exported v1 and
   explicit migrations. Preserve known limitations in subsequent reports.

High-risk migration, recovery and mandatory-auth changes were deferred because the
required device/compatibility fixtures are absent, not because current code is safe.
UI was source-reviewed, not visually verified. One meaningful feature and multiple
small improvements span every requested category; the exhaustive-audit and device
verification obligations remain explicitly incomplete.

## G. NEXT LOOP PROMPT

See `next-loop-prompt.md`, a self-contained Loop 3 handoff preserving the stored master
protocol, cumulative state, constraints, evidence and priority tasks. The supplied
protocol has sections 1–10 (no section 0). The final response supplies the prompt in
one code block; no missing protocol section is invented.
