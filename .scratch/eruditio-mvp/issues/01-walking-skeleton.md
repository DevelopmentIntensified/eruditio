# 01: Walking skeleton

**What to build:** The app installs, opens, and works as a project: Android app shell (native Kotlin), minimal home screen, local database that boots, and the first seam test passing so the test conventions exist. Nothing user-facing beyond a home placeholder. This is the ticket every other ticket stands on.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] Debug build installs and launches on a device or emulator
- [ ] Home screen displays (placeholder content acceptable)
- [ ] Local database creates its schema on first run
- [ ] A first unit test at the review-scheduler seam runs and passes (empty ledger → empty due set)
- [ ] No interception permission requested yet; app is otherwise inert

## Comments

**2026-09-24 — agent**: Implemented on main: Gradle 8.14.3 + AGP 8.13.2 + Kotlin 2.2.21 + Compose BOM, Room schema (positions/sessions/interceptions/ledger/entitlement), `EruditioApp` shell with bottom nav (Today/Library/Review/Progress), first seam test at the `ReviewScheduler` seam (empty ledger → empty due set) passing via `:app:testDebugUnitTest`. Interception permissions intentionally not requested yet (per ticket). Device/emulator install pending — smoke noted for release ticket.
