# Storage and recovery compatibility gate (Loop 2 design, NOT implemented)

## Observed state

`EncryptedPrefs.kt:18–88` selects encrypted preferences, silently falls back on any
exception, and derives the fallback database-key obfuscation from `Build.ID` and
`Build.DEVICE`. Neither successful encrypted-store opening nor valid-looking key text
proves that it is the store which owns the installed SQLCipher database. A firmware
change can alter the derivation input. Never attempt to “fix” this by generating a new
key, clearing either preference file, deleting the database, or bumping the schema.

`LockScreen.kt` recovery currently clears only PIN and pattern hashes and then grants
an unlock. Password, PIN length, lock method and multi-lock sequence remain. This is
not a coherent credential reset. Loop 2 shares attempt throttling but deliberately
leaves reset semantics unchanged pending migration/device fixtures.

## Required fixture matrix BEFORE implementation

Use synthetic credentials and synthetic SQLCipher databases only. Keep real keys,
photos, device IDs, credentials and production database files out of source control.
Instrumented fixture generation should create stores through the shipping APIs;
handwriting encrypted preference XML does not test Android Keystore compatibility.

| Fixture | Required assertion |
|---|---|
| Encrypted-only populated store + existing DB | Preserve DB passphrase, all lock rows, credential hashes, settings and recovery question byte-for-byte. |
| Fallback-only populated store, Keystore unavailable | Existing locks remain readable; report degraded storage; do not fabricate an empty encrypted store. |
| Fallback-only store, Keystore available again | Prove the candidate key opens the EXISTING DB before adopting encrypted destination. |
| Both stores populated with conflicting keys/settings | Enter explicit non-destructive recovery; do not prefer newest/default automatically. |
| Existing DB with missing or malformed passphrase | Block initialization; no generated key, destructive migration, preference deletion, or “success” UI. |
| Empty first installation (no stores or DB) | Generate exactly one key and persist it before creating DB. |
| Changed Build.ID / Build.DEVICE, malformed fallback Base64 | Do not re-encode, guess, or replace the original. Preserve encrypted bytes and surface actionable failure. |
| Disk full, failed commit, killed process at every migration step | Original store remains authoritative until verified durable completion. Restart is idempotent. |
| Original-certificate upgrade API28 + API35, reboot/Keystore invalidation | Preserve data; provide a recoverable failure screen where keys are unavailable. |
| Existing photo directory | Neither migration nor recovery deletes files implicitly. |

## Proposed staged storage transition

1. Separate store discovery from store creation. Record presence without writing.
2. Introduce explicit `Ready`, `Degraded`, and `RecoveryRequired` startup states in
   the Application dependency container. No success/“shield active” claim while DB
   hydration has failed. Error UI must not require a working repository to render.
3. Open candidate keys against the original DB read-only. Do not log keys, hashes,
   raw preference maps or exception values that may contain sensitive data.
4. For an unambiguous fallback owner, copy a complete settings snapshot to a staging
   encrypted store, preserving the original DB passphrase (no SQLCipher rekey).
5. Verify destination readability, row counts and settings, then durably commit a
   versioned journal/owner marker. Inject power-loss failures around each write.
6. Restart and verify ownership. Retain the original store until a separately reviewed
   cleanup policy is safe; never use it as a silent writable fallback after migration.
7. Only then remove automatic fallback. No DB version bump is needed for this design.

## Proposed non-destructive credential recovery

- Verify recovery with the shared limiter and a generation-bound, short-lived grant.
- Keep existing credentials and lock rows until a replacement credential is entered
  and confirmed. Cancellation/process death must leave the old protection intact.
- Commit replacement hash, PIN length (if applicable), configured method and a valid
  multi-lock sequence together; no intermediate state with no configured credential.
- Clear temporary sessions after replacement, but do not delete the security question,
  photos or app locks unless explicitly requested in a separately authenticated flow.
- Device-test PIN-only, pattern-only, password-only, multi-lock, biometric-only, absent
  question, malformed hash, timeout during setup, rotation and interruption.
- Define one-use grant expiry and lifecycle rules before implementation. A settings
  cooldown alone is NOT mandatory authentication for all sensitive changes.

## Remaining threat-model decisions

The current limiter persists wall-clock deadlines with SharedPreferences.apply.
Same-process state is atomic, but this is not clock-tamper resistance or a power-loss
persistence guarantee. A future version needs an elapsedRealtime deadline during a
boot, a bounded reboot policy, and durability/IO tests. Do not change existing stored
keys or increase the KDF work factor without migration fixtures and device benchmarks.
