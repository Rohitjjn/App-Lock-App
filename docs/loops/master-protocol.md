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
