# Backup Manager — App Lock

## 1. Purpose
This file tracks ALL project backups. Every time code changes are made, a new backup MUST be created BEFORE editing.

## 2. Backup Rules (MANDATORY)

┌─────────────────────────────────────────────────────────────────────────────┐
│  WHEN TO CREATE BACKUP                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  • Before starting ANY new feature implementation                           │
│  • Before modifying existing working code                                    │
│  • Before refactoring architecture or moving files                           │
│  • Before updating dependencies or Gradle versions                           │
│  • When user says "try this" or "experiment with this"                     │
│  • At end of every successful day (daily snapshot)                           │
├─────────────────────────────────────────────────────────────────────────────┤
│  BACKUP NAMING FORMAT                                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│  v[MAJOR].[MINOR].[PATCH]-[YYYY-MM-DD]-[short-description].zip             │
│                                                                              │
│  Examples:                                                                   │
│  • v1.0.0-2026-06-14-initial-project-setup.zip                             │
│  • v1.1.0-2026-06-15-app-icon-hide-feature.zip                             │
│  • v1.1.1-2026-06-15-bugfix-notification-delay.zip                         │
│  • v1.2.0-2026-06-16-biometric-gate-implementation.zip                     │
│                                                                              │
│  VERSION RULES:                                                              │
│  • MAJOR (X.0.0): Architecture change, breaking change, major refactor       │
│  • MINOR (x.X.0): New feature added, new module created                    │
│  • PATCH (x.x.X): Bug fix, small tweak, optimization, documentation update   │
├─────────────────────────────────────────────────────────────────────────────┤
│  BACKUP LOCATION                                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│  /backup/  (folder in project root)                                        │
│  • NEVER delete old backups                                                 │
│  • Keep ALL versions for rollback safety                                    │
│  • If storage full, ask user before deleting oldest                         │
│                                                                              │
│  ZIP CONTENTS:                                                               │
│  • Entire project source code                                               │
│  • Exclude: /backup/ folder itself, /build/, /.gradle/, /captures/        │
│  • Include: All .kt, .xml, .md, .gradle, .properties files                 │
│                                                                              │
│  BACKUP DESCRIPTION FILE:                                                   │
│  Inside /backup/, create backup-log.txt with:                               │
│  [Version] | [Date] | [What was working] | [What was pending]                │
└─────────────────────────────────────────────────────────────────────────────┘

## 3. Backup Inventory Table

| Version | Date | Description | Status | File Count | Size | Working Features | Pending |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| v3.0.5 | 2026-07-10 | Fixed Timeout and Lock Behavior interaction | Stable | 54 | 206KB | Perfect combination of grace period timeouts and Until Screen Off lock tracking | None |
| v3.0.4 | 2026-07-10 | Optimized Polling via Slidng Window | Stable | 54 | 206KB | Perfect 100ms contiguous App Locking loop with zero event loss | Completed |
| v3.0.3 | 2026-07-10 | Fixed lock stalling and `isFirstRun` bypasses | Stable | 54 | 206KB | Flawless and instantaneous App Locking with proper lifecycle checks | Testing |
| v3.0.2 | 2026-07-10 | Added SYSTEM_ALERT_WINDOW integration for background activity launch bypass | Stable | 54 | 206KB | Full app locker using UsageStats, launching standard activities bypassing Android 10+ background blocks | Testing |
| v3.0.1 | 2026-07-09 | Restored UsageStats Locking Mechanism logic and fixed database flows | Stable | 54 | 206KB | Full app locker using UsageStats (200ms delay) | Testing |
| v3.0.0 | 2026-07-09 | Migrated to UsageStats Foreground Service polling, deprecated Accessibility | Stable | 54 | 205KB | Full app locker using UsageStats (200ms delay) with screen sleep optimisations | Testing |
| v2.9.1 | 2026-06-25 | Phase 2 banking app fix - stripped Accessibility screen reading flags dynamically | Stable | 54 | 200KB | Native AppLock functionality bypassing major banking security apps | Testing |
| v2.8.0 | 2026-06-19 | Configured Compass Disguise icon change, onboarding permission lifecycles, and stripped third-party uninstall systems | Stable | 54 | 200KB | Custom Compass Icon, Device Admin, Biometrics, Dynamic list-view | Testing |
| v2.7.0 | 2026-06-18 | Converted hide icon option into robust Activity-Alias Calculator Disguise | Stable | 53 | 198KB | Calculator Icon Disguise, Lock interfaces | Testing |
| v2.5.0 | 2026-06-18 | Added Uninstall Protection toggle feature catching App Info deletions/clear data contexts | Stable | 53 | 198KB | Full app protection suite & Uninstall Preventer | Testing |
| v2.4.0 | 2026-06-17 | Added uninstall protection, fast intruder selfies, single-lock fixes and UI toggle fixes | Stable | 53 | 198KB | Full app protection suite | Testing |
| v2.3.0 | 2026-06-16 | Fixed all 5 critical bugs reported by user regarding App Block, Biometrics, Blur intensity, Icon Sequence & Crash on Onboarding | Stable | 52 | 196KB | Full lock protection suite | Testing, Release |
| v2.2.6 | 2026-06-16 | Restored single-lock list order sequence back to natural order with Right-to-Left alignment | Stable | 52 | 196KB | Natural list order rendered Right-To-Left in single-lock mode | Testing, Release |
| v2.2.5 | 2026-06-16 | Added Right-to-Left alignment for bottom switcher icons in single-lock screen | Stable | 52 | 196KB | Dynamic Right-To-Left bottom icon bar switcher in both modes | Testing, Release |
| v2.2.2 | 2026-06-15 | Nullified DONT_KILL_APP to flush OEM UI ghost caching | Stable | 50 | 194KB | Full cache flushing | Testing, Release |
| v2.2.1 | 2026-06-15 | Enforced Device Admin precondition for hiding icon | Stable | 51 | 194KB | "Show App Icon in App List" with deep OS Device Admin tracking | Testing, Release |
| v2.2.0 | 2026-06-15 | Transitioned to "Show App Icon in App List" with DONT_KILL_APP flags | Stable | 51 | 194KB | Show App Icon in App List toggle with background process protection, default ON status | Testing, Release |
| v2.1.0 | 2026-06-15 | Dedicated launcher Activity and Biometric-gated Admin Uninstall protection | Stable | 51 | 192KB | Complete Stealth Mode Hiding on all Android versions, mandatory biometric settings toggling with passcode fallback | Testing, Release |
| v2.0.2 | 2026-06-15 | Enhanced secure lock validation cancel with instant Home redirects and processes termination | Stable | 50 | 188KB | Secure overlay cancel routing, direct Home redirects, background task processes termination | Testing, Release |
| v2.0.1 | 2026-06-14 | Fixed Lock Screen Dark Mode black elements visibility | Stable | 49 | 186KB | Core App intercept, Locks, Dark Theme Support | Testing, Release |
| v2.0.0 | 2026-06-14 | Fully functioning source tree migrated | Stable | 48 | 185KB | Core App intercept, Locks | Testing, Release |
| v0.0.0 | 2026-06-14 | Initial project structure created | Stable | 12 | 45KB | None | All features |

## 4. How to Create Backup (Step-by-Step)

```
STEP 1: Keep file and folder names strictly lowercase without spacing.
STEP 2: Determine new version number logic (Major/Minor/Patch adjustments).
STEP 3: Check last version entry registered in this file.
STEP 4: Compress source: exclude /build/, /.gradle/, /local.properties files.
STEP 5: Save compiled zip to project repository root /backup/.
STEP 6: Add log row to the Backup Inventory Table above.
STEP 7: Complete documentation logs in changelogs.md and update git commit.
```

## 5. How to Restore from Backup

```
STEP 1: Identify the last stable target version in the Backup Inventory Table.
STEP 2: Safely backup current work to temporary directory if restoring fresh.
STEP 3: Clear active project source files (keeping project metadata and /backup/).
STEP 4: Extract corresponding snapshot archive back into project workspace root.
STEP 5: Validate setup compiling capability via clean build task.
STEP 6: Log restoration event status info within changelogs.md.
```

## 6. FUTURE UPDATE RULES (MANDATORY)

┌─────────────────────────────────────────────────────────────────────────────┐
│  WHEN TO UPDATE THIS FILE:                                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  1. After EVERY backup creation:                                           │
│     • Add new row to "Backup Inventory Table"                               │
│     • Fill ALL columns (Version, Date, Description, Status, etc.)            │
│                                                                              │
│  2. When restoring backup:                                                 │
│     • Mark restored version Status as "Restored"                             │
│     • Mark current broken version Status as "Broken-RolledBack"              │
│     • Add note in "Pending" column explaining why rollback happened          │
│                                                                              │
│  3. When project phase completes:                                          │
│     • Update "Working Features" column for all recent backups               │
│     • Update "Pending" column to reflect current state                      │
│                                                                              │
│  4. NEVER:                                                                 │
│     • Delete old rows from Backup Inventory Table                             │
│     • Leave columns empty (use "N/A" or "-" if unknown)                     │
│     • Skip backup before making code changes                                │
│                                                                              │
│  UPDATE FORMAT:                                                            │
│  ```                                                                       │
│  ## [YYYY-MM-DD] — Backup v[X.X.X] Created                                 │
│  - Reason: [Why backup was made]                                             │
│  - Features Working: [List]                                                  │
│  - Features Pending: [List]                                                    │
│  - Notes: [Any special notes]                                                │
│  ```                                                                       │
└─────────────────────────────────────────────────────────────────────────────┘

=== FILE 1 COMPLETE ===
