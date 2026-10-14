Current loop number: 2
Previous loop state: Loop 1 completed as a bounded implementation; Android build/unit/lint checks passed; device verification and exhaustive audit remain open.

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

## LOOP STATE (after Loop 1)

- Loop number just completed: 1, on 2026-10-14.
- Repository: Rohitjjn/App-Lock-App. Baseline 709eba714697c3acc43ff2a95fbe70124d63de82. Loop 1 branch: arena/a4df26fa-app-lock-app; draft PR https://github.com/Rohitjjn/App-Lock-App/pull/2. Work on the branch assigned to the new session; inspect whether the prior PR is included before duplicating changes. Do not switch branches or merge without authorization.
- Team (reuse names and fields): Adrian Cole — Architecture & Code Audit; Daniel Mercer — QA & Reliability; Elena Voss — UI Design; Sofia Reyes — UX & Interaction; Marcus Reed — Product Strategy; Maya Chen — Security Architecture; Owen Brooks — Performance Engineering; Leila Haddad — Inclusive Privacy Design; Priya Nair — Test Automation & Delivery; Victor Hale — Code Quality & Documentation. These were fictional senior review lenses, not independently run agents; keep that disclosure honest.
- App: native Kotlin/Compose Material3, manual Application DI, AndroidViewModel/Flow/Room repository, SQLCipher, EncryptedSharedPreferences. Root :app is active; assets/.aistudio is an old unbuilt source snapshot. minSdk28, target35, compile36.1, AGP9.1.1, Kotlin2.2.10, Gradle9.3.1. MainActivity hosts onboarding/settings; AppDetectorService polls UsageStats every100ms; LockActivity/LockScreen handle locks; LockSessionManager owns temporary sessions. INTERNET is already used by AutoUpdater's GitHub startup check; not offline-only. No telemetry added.
- Audit: 24 consolidated findings (2 Critical,11 High,9 Medium,2 Low) in docs/loops/loop-1-report.md. Active data/service/util/theme/list/credential paths reviewed; only security-relevant Settings sections inspected. Decorative Settings/Onboarding/UserGuide sections, archive and historical docs NOT exhaustively read. Complete that audit; never claim Loop1 did a full line-by-line review.

Cumulative changelog, oldest first:
1. 74dd60e: source baseline recorded; generated backup and signing-key files ignored.
2. c0fef94: null/malformed credentials fail closed; constant-time hash comparison; preserve 10000-iteration saltBase64:hashBase64 format; password duplicate-submit guard; regression harness.
3. 0a8c00a: immutable atomic lock snapshots, exact app-ID exemption, monotonic in-memory unlock timing; cache/session tests.
4. 1ecbe53: cancel detector Room observer/scope on destroy, honor screen-off at startup, clear pending launch on exception.
5. c648558: FLAG_SECURE on both activities; honor intruder-camera opt-out and permission, invalidate late preparation, release on disposal.
6. d174163: app refresh/retry, correct unfiltered lock total, useful empty states, saved filter, single accessible row switch, on-demand IO icons bounded to128; Compose test.
7. 61641a9: update timeouts/cancellation/stream cleanup/.part staging, retryable dialog, numeric newer-version check and APK selection, deduplicated progress.
8. a004df8: PR build/lint/test workflow, normal debug signing, existing release-key secrets rather than random new keys, manual version-tag validation.
9. 881859a: effective app light/dark theme propagated to lock controls.
10. 347fd04: recovery failures count toward cooldown; persisted deadline checks for late successes; ceil countdown; pure tests.
11. Loop1 docs: README actual architecture/privacy/release instructions, audit and30-proposal backlog, tracking-file annotations, self-contained next-loop prompt.
12. c9c089d: use hosted Android SDK after SDK setup action failure.
13. a2a6d99: snapshot async icon state before null checking (self-review correction).
14. cb6df3f/e14d5cc: surface bounded failure diagnostics through check annotations when log-download hosts are blocked.
15. 203b3c4: replace legacy back override with lifecycle dispatcher retaining secure-home behavior; add XML CI result reporter.
16. 5488791: isolate pre-34 TileService API behind runtime guard and narrowly scoped lint exception; ignore Python cache artifacts.
17. Final docs-only update: record successful Android CI run and complete handoff.

Open issues / ranked backlog (paths relative to app/src/main/java/com/example unless noted):
1. Verification and signing gate: .github/workflows/verify.yml:29; build-and-release.yml:45–65. Keep Android build/lint/JUnit checks green, run device tests and validate upgrade with ORIGINAL certificate. Release secrets not configured in Loop1. Prior public keystore passwords must be changed securely; do not replace signing identity or uninstall users' data. Impact critical; effort M, risk High for signing.
2. Detector lifecycle: service/AppDetectorService.kt:92,118–142 can race cache hydration and misses stable-foreground lock/timeout updates or suppressed launches. ui/LockActivity.kt:108 reads target only in onCreate despite manifest singleTask:55; no onNewIntent. Test readiness, rechecks and stale auth before atomic fixes. Impact high; M/L, High risk.
3. Shared auth/recovery: ui/SettingsScreen.kt:1181,1613,1698,2727 remains unthrottled; MainActivity.kt:208–212 optional gate/session persists; LockScreen.kt:986–990 clears only PIN/pattern during recovery. Central persisted attempts and sensitive-action auth, coherent credential reset; wall-clock cooldown tampering still unresolved. Impact high; L, High risk.
4. Weak fallback: data/local/EncryptedPrefs.kt:18–88 silently falls back to unencrypted preferences and device-XOR DB key. Design migration with fixtures for both stores, non-destructive error UI and explicit recovery; never regenerate DB key/delete prefs on failure. Impact high; L, High risk.
5. Safe next wins: replace remembered auth executor (MainActivity.kt:232), test updater failure/cancel/install behavior (util/AutoUpdater.kt:63–118), forced themes (ui/theme/Theme.kt:58), row semantics (ui/AppListScreen.kt:437) and refresh states (MainViewModel.kt:135–205). M, Low/Medium risk.
6. Privacy/performance: camera delayed callbacks (util/IntruderCameraHelper.kt:39–49), private but unencrypted photos/retention (same file:75–90), gallery and wallpaper full-size main-thread decode (ui/SettingsScreen.kt:2876; LockScreen.kt:1261), backup exclusions (res/xml/data_extraction_rules.xml:2). M/L, Medium/High risk; preserve photos.
7. Crypto/supply chain: BiometricHelper.kt:103 omits CryptoObject; HashUtil.kt:11 retains 10000 iterations pending versioned KDF+benchmark; AutoUpdater.kt:121 lacks pre-install archive package/version/signer check. Pin Actions SHA and wrapper checksum. L, High risk for auth changes.
8. Finish audit/docs/localization; schema export/explicit migration before bump (data/local/AppDatabase.kt:13,31). Existing dark mode was corrected, not newly added. Full30 reviewer proposals and deferred reasoning are in docs/loops/loop-1-report.md.

Constraints / lessons:
- No schema, stored hash format, setting-key, app-ID or lock-row changes in Loop1. Preserve all existing data, including fallback-store installations.
- Never restore null-hash success, broad com.example.* exemption, cache clear/addAll, screenshot exposure, ignored camera opt-out, new per-build signing keys or credential logging.
- No heavy production dependency added. Host tools lived under /home/user/.cache, not Git.
- Updated UI is source-reviewed, not visually verified. Large auth/storage refactors deferred for device/recovery compatibility, not because safe already.
- Read Rules.md and project docs, but treat historical “impenetrable”, zero-latency, offline-only and past “passed” claims as unverified. Use current source as evidence. Base snapshot can be recreated via git archive into a separate folder; do not delete repository root/.git.
- Supplied master text contains sections1–10, no section0 and no current/previous metadata; preserve that text and update metadata/state rather than inventing missing original sections.

Verification:
- tools/run-core-checks.sh compiled/executed8 production-Kotlin regression groups: malformed/null hashes, independent legacy PBKDF2 vector, salts/roundtrips, defensive snapshots, concurrent snapshots(80000reads/10000writes), version ordering, invalid versions, cooldown boundaries.
- Kotlin PSI syntax parsing and XML/YAML parsing passed; git diff --check passed. These are NOT Android type checking or runtime tests.
- Initially no Java/SDK. Installed host-only jdk4py21.0.8.2 via PyPI and kotlin-compiler2.2.10 via npm. Gradle download from services.gradle.org failed SSL due sandbox allowlist. GitHub release-assets compiler download also blocked. GitHub-hosted verification subsequently PASSED at source/workflow commit5488791: https://github.com/Rohitjjn/App-Lock-App/actions/runs/38515638482. Debug assembly succeeded,10 Gradle JVM tests passed across5 suites(including2 Robolectric session tests),lint0errors/187other findings. Earlier SDK action failure was resolved using hosted ubuntu-24.04 SDK; earlier MissingSuperCall fixed with safe back dispatcher; remaining tile API lint was an already SDK-gated pre-34 path, now narrowly documented/guarded. KSP AWT cleanup errors appeared on earlier runs; no final-run annotation, investigate recurrence. Action runtime deprecation warnings remain. Compose instrumentation/AndroidTest compilation, emulator/hardware tests, signed release, original-key upgrades and benchmarks remain unverified. Final edits after5488791 are docs-only; inspect actual head checks rather than assuming the documentation rerun finished.

## THIS LOOP'S PRIORITY TASKS

1. Read prior report/diff and inspect GitHub checks; confirm Loop1 changes are present. Preserve the successful Android verification and address remaining lint warnings incrementally. No merge/release claim without evidence; note any existing build problems separately.
2. Complete remaining file-by-file audit, reusing roster and reporting only read evidence. Use review personas honestly if no independent-agent tool exists.
3. Add lifecycle regression coverage and make minimal fixes for detector cache readiness/stable-target reevaluation and LockActivity new-intent/stale-biometric handling. Do not rewrite detection blindly.
4. Extend persistent auth throttling to settings and design non-destructive recovery with tests. Plan fallback-storage migration with compatibility fixtures before touching stored keys.
5. Prefer small companion improvements: executor cleanup, updater integration tests, TalkBack/forced-theme/large-font/refresh verification. Keep normal per-category improvements safe and evidence-backed.
6. Commit logical changes, push only the session-assigned branch, open a PR, produce A–G and30 reviewer proposals, and generate the complete self-contained Loop3 prompt by preserving the supplied master protocol and updating compact cumulative state.

BEGIN LOOP 2 NOW.
