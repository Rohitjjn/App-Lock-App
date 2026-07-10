# PROJECT RULES — App Lock Application (Android 14+)
# Version: 1.0
# Last Updated: 2026-06-14

================================================================================
                           SECTION 1: MANDATORY READ ORDER
================================================================================

Before making ANY changes to the project, you MUST read these files in this 
exact order:

┌─────────────────────────────────────────────────────────────────────────────┐
│  READ ORDER                    │  PURPOSE                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  1. project-blueprint.md       │  Overall architecture, tech stack, modules   │
│  2. rules.md                   │  This file — development rules & workflows   │
│  3. project-mindmap.md         │  Visual feature map, dependencies          │
│  4. project-chart.md           │  Module interaction flow, data flow        │
│  5. changelogs.md             │  What changed, when, why (date-wise log)     │
│  6. backup-manager.md         │  Backup locations, versions, restore guide   │
│  7. tech-specs.md             │  Android API levels, libraries, permissions  │
│  8. security-protocol.md       │  Encryption methods, biometric rules         │
│  9. testing-checklist.md      │  Test cases, device matrix, QA steps         │
│  10. known-issues.md           │  Current bugs, workarounds, pending fixes    │
└─────────────────────────────────────────────────────────────────────────────┘

RULE: NEVER modify code without reading at least files 1-6.
RULE: If any file is missing, create it with template content first.

================================================================================
                           SECTION 2: FILE UPDATE PROTOCOL
================================================================================

Whenever you modify ANY project file, update these tracking files:

┌─────────────────────────────────────────────────────────────────────────────┐
│  ACTION                        │  FILE TO UPDATE                            │
├─────────────────────────────────────────────────────────────────────────────┤
│  Add/Remove/Modify feature    │  changelogs.md (date + description)        │
│  Change architecture/logic    │  project-blueprint.md + project-chart.md   │
│  Add new dependency/library   │  tech-specs.md + project-mindmap.md        │
│  Fix bug                      │  known-issues.md (mark resolved)           │
│  Change security method       │  security-protocol.md + changelogs.md      │
│  Before ANY code change       │  backup-manager.md (new backup entry)      │
│  After testing complete       │  testing-checklist.md (mark passed)        │
└─────────────────────────────────────────────────────────────────────────────┘

UPDATE FORMAT for changelogs.md:
```

[YYYY-MM-DD] — [Feature/Bugfix/Refactor]
- Changed: [File path] — [Specific change]
- Reason: [Why this change was made]
- Tested: [Yes/No — if No, mark as PENDING]
- Related: [Issue # or Feature name]

```

================================================================================
                           SECTION 3: BACKUP PROTOCOL (CRITICAL)
================================================================================

Before EVERY coding session or feature implementation, create a backup.

┌─────────────────────────────────────────────────────────────────────────────┐
│  BACKUP FOLDER STRUCTURE                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  /backup/                                                                    │
│  ├── v1.0.0-2026-06-10-initial-setup.zip          ← Initial project setup  │
│  ├── v1.1.0-2026-06-11-icon-hide-feature.zip      ← After icon hide done  │
│  ├── v1.2.0-2026-06-12-biometric-gate.zip          ← After biometric done  │
│  ├── v1.2.1-2026-06-12-bugfix-notification.zip     ← Bug fix backup        │
│  └── [version]-[date]-[description].zip            ← NEVER delete old ones │
│                                                                              │
│  BACKUP NAMING CONVENTION:                                                   │
│  v[MAJOR].[MINOR].[PATCH]-[YYYY-MM-DD]-[short-description].zip              │
│                                                                              │
│  VERSION RULES:                                                              │
│  MAJOR: New architecture or breaking change (2.0.0)                        │
│  MINOR: New feature added (1.1.0, 1.2.0)                                     │
│  PATCH: Bug fix or small tweak (1.2.1, 1.2.2)                              │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

BACKUP CREATION STEPS:
1. Read backup-manager.md to see last version number
2. Increment version (patch for bugfix, minor for feature, major for restructure)
3. Create zip of ENTIRE project source (excluding /backup/ folder itself)
4. Save to /backup/ with proper naming
5. Update backup-manager.md with:
   - Version number
   - Date
   - Description of what was working at this point
   - File count / size
   - Any special notes

BACKUP-ENTRY FORMAT in backup-manager.md:
```markdown
| Version | Date | Description | Status | Restore Command |
|---------|------|-------------|--------|-----------------|
| v1.0.0 | 2026-06-10 | Initial project setup, basic UI | Stable | unzip v1.0.0-... |
| v1.1.0 | 2026-06-11 | App icon hide/show working | Stable | unzip v1.1.0-... |
| v1.2.0 | 2026-06-12 | Biometric gate + lock screen | Testing | unzip v1.2.0-... |
```

NEVER DELETE old backups. Keep all versions for rollback safety.

================================================================================
SECTION 4: FEATURE REQUEST WORKFLOW
================================================================================

When user requests a new feature or change:

┌─────────────────────────────────────────────────────────────────────────────┐
│  STEP-BY-STEP WORKFLOW                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. READ ALL MANDATORY FILES (Section 1)                                     │
│     → Understand current state, what exists, what doesn't                    │
│                                                                              │
│  2. CREATE BACKUP (Section 3)                                                │
│     → Save current working state before touching anything                    │
│                                                                              │
│  3. ANALYZE REQUEST                                                          │
│     → Break into small, implementable tasks                                  │
│     → Identify affected files and modules                                    │
│     → Check for conflicts with existing features                           │
│                                                                              │
│  4. IMPLEMENT CHANGES                                                        │
│     → One feature at a time                                                  │
│     → Follow architecture from project-blueprint.md                          │
│     → Add comments in code for complex logic                                 │
│                                                                              │
│  5. TEST THOROUGHLY                                                          │
│     → Check if feature works as expected                                     │
│     → Check if existing features still work (regression test)                │
│     → Test on different scenarios (Android 14, 15, different devices)          │
│                                                                              │
│  6. IF PROBLEM FOUND:                                                        │
│     → STOP, do not proceed to next feature                                   │
│     → Debug and fix the issue                                                │
│     → Test again until desired output is achieved                            │
│     → If cannot fix, restore from last backup and try different approach     │
│                                                                              │
│  7. IF SUCCESS:                                                              │
│     → Update all tracking files (Section 2)                                  │
│     → Create new backup with incremented version                             │
│     → Update backup-manager.md                                               │
│     → Present result to user with summary of changes                         │
│                                                                              │
│  8. ASK USER FOR CONFIRMATION                                                │
│     → "Feature implemented. Test results: [X] works, [Y] needs check"         │
│     → Wait for user feedback before starting next task                       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

================================================================================
SECTION 5: CODE QUALITY RULES
================================================================================

┌─────────────────────────────────────────────────────────────────────────────┐
│  RULE                    │  DESCRIPTION                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  Modular Architecture    │  One feature = One module/package                │
│  No Hardcoded Strings    │  All strings in strings.xml or constants         │
│  Comment in Hindi        │  Complex logic: Hindi comments for user clarity  │
│  Error Handling          │  Every try must have catch with fallback         │
│  Null Safety             │  Use ?.let {} ?: run {} pattern                │
│  Lifecycle Aware         │  Use DisposableEffect, LaunchedEffect properly   │
│  Memory Leak Check       │  Cancel coroutines, remove listeners in onDispose│
│  Permission Check        │  Runtime check before using system APIs          │
│  Android 14+ Compatible  │  Always test with API 34+ behavior changes       │
└─────────────────────────────────────────────────────────────────────────────┘

================================================================================
SECTION 6: PROJECT FILE TEMPLATES
================================================================================

If any mandatory file is missing, create it with this template:

--- project-blueprint.md template ---

```markdown
# Project Blueprint — App Lock
## Architecture: MVVM + Clean Architecture
## Tech Stack: Jetpack Compose, Room, Hilt, BiometricPrompt, AccessibilityService
## Modules: app, data, domain, presentation
## Current Phase: [1/2/3]
## Completed Features: [List]
## In Progress: [Feature name]
## Pending: [Feature names]
```

--- project-mindmap.md template ---

```markdown
# Project Mind Map
## Core Features
- [ ] App Icon Hide/Show
- [ ] Lock Screen (PIN/Pattern/Password/Biometric)
- [ ] Notification Panel Control
- [ ] Dialer Code Launch
- [ ] Uninstall Protection
## Dependencies
- Compose UI → ViewModel → UseCase → Repository → Room DB
## Security Flow
- User Input → Validation → EncryptedStorage → BiometricCheck → Unlock
```

--- changelogs.md template ---

```markdown
# Changelog
## [2026-06-14] — Initial Setup
- Created: project structure, rules.md, backup system
- Status: In Progress
```

--- backup-manager.md template ---

```markdown
# Backup Manager
| Version | Date | Description | Status | Notes |
|---------|------|-------------|--------|-------|
| v0.0.0 | 2026-06-14 | Template creation | Stable | Initial structure |
```

================================================================================
SECTION 7: USER COMMUNICATION RULES
================================================================================

┌─────────────────────────────────────────────────────────────────────────────┐
│  WHEN TO NOTIFY USER                                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ✓ Before starting work: "Reading project files, creating backup..."        │
│  ✓ After backup created: "Backup saved: v[X.X.X] — starting implementation"  │
│  ✓ Feature complete: "Feature done. Testing now..."                          │
│  ✓ Test results: "Tests: [Pass/Fail]. Issues: [None/List]"                  │
│  ✓ If stuck: "Issue found in [file]. Trying fix [1/2/3]..."                  │
│  ✓ Before next feature: "Confirm to proceed to [next feature]?"             │
│                                                                              │
│  NEVER:                                                                      │
│  ✗ Hide errors or silent failures                                            │
│  ✗ Skip backup before making changes                                         │
│  ✗ Delete old backups without user permission                                │
│  ✗ Start new feature without closing previous one                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

================================================================================
SECTION 8: EMERGENCY PROTOCOLS
================================================================================

┌─────────────────────────────────────────────────────────────────────────────┐
│  SCENARIO: Code broken, app crashes, user angry                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. STOP all current work                                                    │
│  2. Identify last stable backup from backup-manager.md                       │
│  3. Restore: Unzip backup to project root (overwrite)                        │
│  4. Verify: Build and run to confirm stable                                  │
│  5. Update: changelogs.md — "Rolled back to v[X.X.X] due to [reason]"       │
│  6. Report: "Restored stable version. Issue logged in known-issues.md"      │
│  7. Re-approach: Try different implementation strategy                       │
│                                                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│  SCENARIO: User wants to undo last change                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. Check backup-manager.md for version before last change                    │
│  2. Restore that version                                                     │
│  3. Update changelogs.md with rollback entry                                 │
│  4. Notify: "Restored to v[X.X.X] — [feature] removed"                      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

================================================================================
SECTION 9: AI BEHAVIOR REMINDERS
================================================================================

┌─────────────────────────────────────────────────────────────────────────────┐
│  REMINDERS FOR AI (Self-Correction)                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  • Always check if file exists before reading/writing                        │
│  • If user says "fix this" — check known-issues.md first                     │
│  • If user says "add feature" — check project-mindmap.md for dependencies    │
│  • If user says "it's slow" — check tech-specs.md for optimization notes     │
│  • Never assume — always verify with project files                            │
│  • If confused about user's intent, ask before implementing                  │
│  • Keep responses concise, use diagrams for complex flows                    │
│  • When giving code, include file path and line numbers reference            │
│                                                                              │
│  MISSING SOMETHING?                                                          │
│  If you (AI) notice user missed any important rule or file, TELL THEM:       │
│  "Note: I noticed [file/rule] is missing. Should I create it?"                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

================================================================================
