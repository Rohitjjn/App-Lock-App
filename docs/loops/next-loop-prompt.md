Current loop number: 3
Previous loop state: Loop 2 completed as a bounded implementation; Android build, instrumentation compilation, 23 JVM tests and lint passed; device integration, storage/recovery migration and exhaustive audit remain open.

# MASTER PROTOCOL — AUTONOMOUS CODEBASE EVOLUTION LOOP (v1)





## 1. MISSION

Take the connected codebase and make it better in every dimension: correctness, security, performance, UI/UX polish, features, and code quality. Work in a loop. In each loop you (a) assemble/reuse an expert team, (b) audit the whole code, (c) implement a set of small, safe, high-value improvements, (d) verify them, and (e) have the team read the updated code and brainstorm what to improve next, then (f) write a complete prompt for the next loop, which I will paste into a new session. Every loop must leave the app better than it found it, and must never leave it broken.



## 2. STEP 1 — ASSEMBLE THE EXPERT TEAM

Create a team of senior expert agents. Each agent has:

- a unique human name,

- exactly ONE field of expertise (50+ years of experience in that field),

- a one-line "signature lens" describing what they look for.



Minimum roster (add more if the codebase needs it):

1. Architecture &amp; Code Audit Lead

2. Bug Hunter / QA &amp; Reliability Engineer

3. UI Design Director (visual polish, premium look and feel, design system)

4. UX &amp; Interaction Specialist (flows, onboarding, micro-interactions, empty/error states)

5. Product &amp; Feature Strategist (high-value features relevant to an app lock app)

6. Security Architect (authentication, PIN/pattern/biometric handling, secure storage, crypto, brute-force protection, bypass/tamper resistance, permissions, secrets, dependency risk)

7. Performance Engineer (startup time, memory, battery, rendering, I/O, background work)

8. Accessibility, Localization &amp; Privacy Specialist

9. Testing, CI/CD &amp; DevOps Engineer

10. Code Quality, Refactoring &amp; Documentation Lead



If a roster exists in the LOOP STATE section below, reuse the same names and fields. Do not rename them.

Each agent speaks and reports under their own name. Present the roster at the top of your report.



## 3. STEP 2 — FULL AUDIT (read before you write)

1. Map the repository: structure, entry points, dependencies, build/config files, existing tests, existing docs.

2. Read the code thoroughly, file by file. Do not guess about code you have not read.

3. Each agent audits from their own field and lists findings as: ID, severity (Critical/High/Medium/Low), file:line, what is wrong, why it matters, proposed fix.

4. Produce one consolidated, de-duplicated findings list ranked by severity and impact.

5. Only report real findings backed by code you actually read. If a field has nothing meaningful to improve, say so. Never invent problems or fake improvements to look busy.



## 4. STEP 3 — IMPLEMENT (the improvement pass)

Order of work: bugs/crashes → security → performance → UI/UX → features → code quality/tests/docs.

- In EACH category, make at least 1–2 meaningful changes (more if safe and clearly valuable). Prefer many small, safe, high-impact changes over one giant risky rewrite.

- Every change must be: atomic, minimal in blast radius, backward compatible (preserve user data, settings, and existing locks), and consistent with the existing code style and architecture.

- One commit per logical change with a clear message: `[category] short description (agent name)`.

- UI/UX changes must raise the app to a premium feel: consistent spacing/typography/color system, smooth animations, polished states (loading, empty, error), dark mode if missing, good touch targets.

- Security changes are top priority for an app lock: never weaken any existing protection, never log secrets/PINs, never store credentials in plaintext, and never add network calls or telemetry without a clear reason and an explicit note in the report.

- Do not add heavy dependencies unless the benefit is clear; if you add one, justify it.

- Do not delete user-facing functionality. Do not make breaking changes to data formats without a migration.



## 5. STEP 4 — VERIFY (do not skip)

After implementing, in the sandbox as far as the stack allows:

- install dependencies, build/compile, run linters, run existing tests, add tests for the bugs you fixed and critical logic you touched;

- re-read your own diffs and check for regressions, edge cases, and side effects;

- fix anything that fails before moving on.

Report ONLY what you actually ran, with real results. If something could not be run (e.g. no emulator available), say exactly that and list it under "Unverified". Never claim a test passed if you did not run it.



## 6. STEP 5 — SAVE THE WORK

Push the branch to GitHub and open a pull request (or, if write access fails, output the full diffs/patch and the list of changed files so I can apply them manually). Do not leave work only in the sandbox.



## 7. STEP 6 — TEAM BRAINSTORM FOR THE NEXT LOOP

After all changes are in, have the whole team re-read the UPDATED code. Each agent proposes their top 3 next improvements with: evidence (file:line), expected impact, effort (S/M/L), risk. Then the Audit Lead merges them into a ranked backlog and picks the best set for the next loop. Include ideas that were deferred this loop and why.



## 8. REQUIRED OUTPUT FORMAT (in this exact order)

A. TEAM ROSTER (names + fields)

B. AUDIT SUMMARY (top findings, counts by severity)

C. CHANGES MADE (per category: what, files, commit, why)

D. VERIFICATION RESULTS (what was run + real results + Unverified list)

E. GITHUB STATUS (branch, PR link or patch)

F. NEXT-LOOP BACKLOG (ranked, with evidence)

G. NEXT LOOP PROMPT — a complete, ready-to-paste prompt inside ONE code block (rules in section 9)



## 9. RULES FOR WRITING THE NEXT LOOP PROMPT (self-replication)

The next prompt must be fully self-contained, because I will paste it into a fresh session with no memory of this one. Build it as follows:

1. Copy this entire MASTER PROTOCOL (sections 0–10) VERBATIM, only changing the "Current loop number" to N+1 and the "Previous loop state" line.

2. Then append an updated "LOOP STATE" section containing:

   - Loop number just completed

   - Team roster (same names and fields)

   - Short app summary: architecture, tech stack, key files/modules (so the next loop can orient quickly)

   - Cumulative changelog (one line per change across all loops, newest last)

   - Open bugs/known issues

   - Ranked backlog for the next loop with file:line evidence and the team's reasoning

   - Constraints/decisions learned (what NOT to touch, things that failed, conventions to follow)

   - Verification gaps still open

3. Then append "THIS LOOP'S PRIORITY TASKS": the specific items the next loop should implement first, in addition to the normal full audit and per-category improvements.

4. Keep LOOP STATE compact (summarize old loops, do not let it grow endlessly) but never drop information that the next loop needs.

5. The next prompt must itself instruct the next agent to produce the following prompt, so the loop continues indefinitely.



## 10. GUARDRAILS

- Never commit secrets, keys, or personal data. If you find any in the repo, flag it as Critical and remove it from the code (and note that it must be rotated).

- No destructive git operations (no force-push, no history rewrite, no deleting branches you did not create).

- If the codebase is already strong in an area, say so and move on; diminishing returns are acceptable, fabrication is not.

- If a requirement is ambiguous, choose the safest reasonable assumption, state it in the report, and continue. Do not stop to ask unless you are completely blocked.

- Be honest about uncertainty, limitations, and anything you could not verify.

## LOOP STATE (after Loop 2)

- Completed loop: 2, on 2026-10-14. Repository Rohitjjn/App-Lock-App. Loop 1 PR #2 was merged; Loop 2 baseline ea2c3bc4cd8b4d34556dd068af116d49c2a1efd3. Loop 2 branch arena/37dbba83-app-lock-app; draft PR https://github.com/Rohitjjn/App-Lock-App/pull/3. Work ONLY on the next session's assigned branch; inspect PR inclusion/checks before duplicating changes. Do not merge/release without authorization.
- Team (same single fields): Adrian Cole — Architecture & Code Audit; Daniel Mercer — QA & Reliability; Elena Voss — UI Design; Sofia Reyes — UX & Interaction; Marcus Reed — Product Strategy; Maya Chen — Security Architecture; Owen Brooks — Performance Engineering; Leila Haddad — Inclusive Privacy Design; Priya Nair — Test Automation & Delivery; Victor Hale — Code Quality & Documentation. Fictional 50+ year senior review lenses, NOT independently executed agents or real human experience claims.
- Architecture: root :app is native Kotlin/Compose Material3, manual Application DI, AndroidViewModel/Flow/Room, SQLCipher, EncryptedSharedPreferences. assets/.aistudio is an old unbuilt snapshot. minSdk28, target35, compile36.1; AGP9.1.1, Kotlin2.2.10, Gradle9.3.1. MainActivity hosts onboarding/settings; AppDetectorService polls UsageStats every100ms; LockActivity/LockScreen authenticate targets; LockSessionManager owns temporary unlocks. AuthAttemptLimiter is application-owned through EncryptedPrefs.authAttempts. INTERNET already supports AutoUpdater GitHub checks; no telemetry/new network destination added.
- Reports: docs/loops/loop-1-report.md (24 findings); docs/loops/loop-2-report.md (18 reviewed findings: 0 new Critical,8 High,9 Medium,1 Low; 30 reviewer proposals); docs/loops/storage-recovery-design.md (design and required fixture matrix, NOT implemented migration).
- Audit remains bounded. Loop2 fully read detector, LockActivity/session and biometric/camera/updater helpers, OnboardingScreens and UserGuideDialog; reviewed authentication/storage-selection/app-list/MainActivity/ViewModel paths and all new code. Settings top-level permission/admin callbacks, credential dialogs and gallery reviewed. Decorative/customization Settings sections, parts of LockScreen/ViewModel, archive, binary resources and historical docs NOT exhaustively reread. Never claim a full independent line-by-line audit.

### Cumulative changelog (oldest first)

1. 74dd60e: baseline tracking; ignore generated backup/signing artifacts.
2. c0fef94: null/malformed credentials fail closed, constant-time comparison, legacy PBKDF2 format retained; duplicate password-submit guard/tests.
3. 0a8c00a: atomic immutable lock cache, exact application-ID exemption, monotonic temporary sessions/tests.
4. 1ecbe53: detector scope cancellation, screen-off startup state, pending-launch cleanup on error.
5. c648558: secure activity windows, camera opt-out/permission checks, preparation-generation/disposal cleanup.
6. d174163: app refresh/retry, honest count/empty states, saved filter, accessible single row toggle, bounded lazy IO icons/test.
7. 61641a9: updater timeouts/cancellation/stream cleanup/staging, retry dialog, numeric version/APK selection and progress deduplication.
8. a004df8: PR verification, standard debug signing, existing release-key secrets and version-tag validation; no random signing identity.
9. 881859a: effective light/dark app theme reaches lock controls.
10. 347fd04: recovery failures enter cooldown; persisted deadline recheck/ceil countdown/tests.
11. Loop1 docs: actual architecture/privacy/release instructions, audit/backlog and handoff.
12. c9c089d: hosted Android SDK replaces failed SDK action.
13. a2a6d99: snapshot async icon state before null checking.
14. cb6df3f/e14d5cc: bounded CI failure annotations for blocked log-host access.
15. 203b3c4: lifecycle secure-back dispatcher and XML CI result reporter.
16. 5488791: SDK-gated pre34 tile call, narrowly scoped lint exception; Python-cache ignore.
17. Final Loop1 docs: record successful Android verification and remaining gaps.
18. adf3956: await initial detector cache, retain/recheck stable foreground, resolve launcher once per polling session; target tests.
19. d192643: singleTask new-intent rebinding, generation-keyed UI/callback rejection, cancellable biometrics/tests.
20. f744818: shared persisted three-failure/30-second credential limiter for Settings/LockScreen/recovery; atomic existing-key edit/tests.
21. 9b96769: MainActivity main executor and stop/disposal prompt cleanup instead of unowned thread.
22. 6504919: live localized Material3 settings cooldown notices and Compose test.
23. af9a8e4: Lock apps again menu revokes temporary sessions, preserves saved locks, explains shield-off state; session test.
24. 47c4c70: instrumentation APK compilation in CI; host core checks expanded to11 policy groups.
25. d6970cb: correct/localize unsupported in-app instant-protection, camera, KDF and session claims.
26. d7ea306: fix CI-exposed Settings Boolean→Any callback inference after returning cancellable prompt; explicit Unit contract.
27. eb3582d: synthetic legacy preference fixtures preserve cooldown/credential/DB-key values across wrapper recreation.
28. Loop2 docs: audit/backlog, non-destructive storage/recovery plan, tracking records and complete Loop3 prompt.

### Ranked open backlog and reasoning

Paths below are relative to app/src/main/java/com/example unless prefixed otherwise.

1. **Verification gate** — .github/workflows/verify.yml:36; service/AppDetectorService.kt:99,124–157; ui/LockActivity.kt:158,369. Runtime-test Room readiness/failure, stable-target timeout/cache changes, suppressed launch retry, rapid A→B→A, repeated singleTask intent, screen-off and stale biometrics on API28/35. Pure policies are tested; service/activity integration is not. Impact High; M; Medium/High risk.
2. **Sensitive-action auth** — ui/SettingsScreen.kt:244–307 has unbound admin biometrics and direct executeAction when no Activity; MainActivity.kt:208–212 remembers optional unlock; SettingsScreen.kt:434 toggles shield without mandatory reauth. Add generation/lifecycle/cooldown gating and fail-closed fallback first; then short-lived grants for sensitive operations. Shared credential throttling is NOT complete settings authorization. High; M/L; High risk.
3. **Storage and recovery** — data/local/EncryptedPrefs.kt:18–88 silently falls back to plaintext/device-derived XOR DB key; ui/LockScreen.kt:966–971 recovery clears only PIN/pattern. Follow docs/loops/storage-recovery-design.md: generate BOTH real-store fixtures, test owner ambiguity/Keystore recovery/firmware changes/process death, never regenerate key/delete prefs/DB, stage confirmed replacement before modifying credentials. High; L; High risk. New Robolectric fixtures are not real Keystore/SQLCipher migration tests.
4. **Safe companion UX/privacy** — ui/OnboardingScreens.kt:500,518 unguarded OEM settings intents; UserGuideDialog.kt:120 narrow one-line tabs; OnboardingScreens.kt:221 non-scrollable intro; SettingsScreen.kt:2823,2920 deletion without confirmation/error handling; res/xml/data_extraction_rules.xml:2 lacks explicit exclusions. Guard intents, adaptive layouts, photo confirmation/errors, supported backup/transfer exclusions. Medium; S/M; Low/Medium risk. Preserve photos.
5. **Privacy/performance** — util/IntruderCameraHelper.kt:96–105,125–130 stale capture callbacks/delayed legacy capture; SettingsScreen.kt:2881,2969 and LockScreen.kt:1248 decode full-size on main. Bind camera ownership, sample/decode off-main, benchmark detector steady checks and KDF before optimization. Medium; M; Medium risk. Photos remain private but unencrypted; no retention migration yet.
6. **Updater/supply chain/release** — util/AutoUpdater.kt:80–127 needs cancellation/install/error tests and pre-install package/version/signer checks. .github/workflows/build-and-release.yml:45–65 needs original-key upgrade test/owner secrets; actions need SHA pinning/runtime updates and wrapper checksum. Prior public passwords must be rotated securely, NOT signing certificate replaced. CryptoObject (BiometricHelper.kt:104) and versioned KDF (HashUtil.kt:11) need device/recovery benchmarks. M/L; Medium/High risk.
7. **Finish audit/schema/docs/localization** — unread Settings customization/archive; data/local/AppDatabase.kt:13,31 needs v1 export/explicit migrations before any bump. Remaining hardcoded strings and historical safety claims require review. New AuthCooldownNotice and relock UI need TalkBack, forced-theme/large-font tests. M; Low/Medium risk.

### Constraints and verification

- Preserve application ID, DB schema/lock rows, hash encoding (10000-iteration saltBase64:hashBase64), setting keys, existing credentials, fallback-store installations and original signing identity. No new production dependency/telemetry/network destination in either loop.
- Never restore null-hash success, broad com.example.* exemptions, non-atomic cache replacement, screenshot exposure, ignored camera opt-out or random release keys. Do not rewrite polling blindly or infer hardware safety from pure-policy tests.
- Shared limiter retains wall-clock deadlines and SharedPreferences.apply; clock tampering/power-loss persistence remain unresolved. Master shield/admin biometric/sensitive-action flows are not all covered by the new credential policy. Recovery semantics deliberately unchanged pending fixtures.
- Read Rules.md and project docs; historical “impenetrable”, “offline-only”, instant or passed claims are not authoritative. Git archive can create a separate baseline; never delete repository root/.git. Local cache/tool artifacts are not committed/persisted as deliverables.
- Host JDK21.0.8.2 via PyPI jdk4py and Kotlin2.2.10 via npm supported tools/run-core-checks.sh: **11 regression groups passed**. Local ./gradlew --version failed distribution download SSL handshake (services.gradle.org outside allowlist); no local SDK/emulator.13 active XML and2 workflow YAML files parsed; diff checks passed. PyYAML had to be installed host-only.
- Loop2 Android verification **PASSED at eb3582d**, run https://github.com/Rohitjjn/App-Lock-App/actions/runs/38517565482 : testDebugUnitTest **23 tests,9 suites,0 failed/errors/skipped**; lintDebug **0errors/187other findings**; assembleDebug and assembleDebugAndroidTest succeeded. Earlier run38517153107 failed callback type inference, fixed d7ea306; superseded runs cancelled. Actions runtime deprecation warnings remain. After eb3582d only documentation changes; inspect actual next-session head checks.
- Still UNVERIFIED: instrumentation execution/emulator/hardware, real service/activity lifecycle and biometrics, TalkBack/visual output, OEM restrictions, signed release/original-key upgrade, Keystore/SQLCipher recovery/migration, update integration, camera/CPU/memory/battery benchmarks, exhaustive audit.

## THIS LOOP'S PRIORITY TASKS

1. Inspect current baseline/PR inclusion and head CI; preserve the passing JVM/lint/debug/instrumentation compilation gate. Add API28/35 device execution and integration tests before broad auth/detector changes.
2. Finish the unreviewed file-by-file audit with explicit coverage; reuse the same honest review personas and evidence-backed severity findings.
3. Close the admin biometric stale-callback/null-activity/cooldown path, then design/test mandatory sensitive-action grants. Do not claim shared credential throttling already covers all settings actions.
4. Execute storage/recovery fixture plan before changing keys, formats or reset behavior. Stage replacement credentials non-destructively; never clear existing credentials before replacement confirmation.
5. Add small safe companion improvements from backlog #4, sampled image/camera ownership work only with tests. Keep per-category changes meaningful, atomic and backward compatible.
6. Commit logical changes, push ONLY the assigned branch, open/update a PR; deliver A–G, all30 reviewer proposals and a complete self-contained Loop4 prompt by copying the master protocol and updating compact cumulative state. Continue the protocol indefinitely; do not omit future prompt generation.
