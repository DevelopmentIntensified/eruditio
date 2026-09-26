# 24: Watcher always on

**What to build:** The interception watcher must survive everything the OS does to it, so opening a target app always meets the pause screen. Today the service is only started from `MainActivity.onCreate` — after a reboot nothing restarts it, so interception is dead until the user opens Eruditio by hand.

**Blocked by:** 04 (Interception friction loop).

**Status:** ready-for-human

- [x] `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` receiver starts the watcher (documented FGS-from-background exemption)
- [x] 15-minute WorkManager watchdog restarts the watcher when its heartbeat has gone stale
- [x] A refused background start retries with backoff instead of crashing the worker
- [x] The service beats a heartbeat every ~15 s; a restarted service seeds foreground state instead of missing the app already open
- [x] Settings shows one honest watcher state with fix actions (usage access, battery optimisation, start now)
- [x] `WatcherHealth` decision is a pure, unit-tested predicate
- [x] `gradlew.bat :app:testDebugUnitTest` green — 102 tests, 0 failures

**Limits (documented, not fixed):**

- A background `startForegroundService` from the watchdog is refused unless the OS is in an allowing state (battery optimisation off, exact alarm, or the app foreground). The watchdog retries; the Settings card is the escape hatch. This is a platform rule, not a bug to engineer around.
- Worst case after a mid-session kill: up to ~17 minutes without interception (2-minute stale threshold + 15-minute watchdog cadence).
- Detection stays `UsageStatsManager`-based; no `AccessibilityService` (ADR-0001).
- Battery-optimisation exemption is offered via the Settings list. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is not declared — Play restricts the direct request dialog.

**Files:** `intercept/WatcherHealth.kt` (pure decision), `intercept/WatcherRuntime.kt` (heartbeat, OS probes, arming), `intercept/WatcherWatchdog.kt` (WorkManager), `intercept/WatcherBootReceiver.kt`, `intercept/UsageStatsWatcherService.kt` (beats, watchdog, foreground seed, notification → app), `ui/WatcherCard.kt`, `test/WatcherHealthTest.kt`.

## Comments

- **2026-09-26 — agent**: Root cause of the reported symptom: `UsageStatsWatcherService.ensureRunning()` had exactly one caller, `MainActivity.onCreate`, so a reboot left the watcher dead until the user opened Eruditio by hand. Now three arming paths (boot/update receiver, app open, service self-arm) plus a 15-minute watchdog, and Settings says plainly whether the pause screen is armed. `MainActivity.hasUsageAccess` now delegates to `WatcherRuntime` so the AppOps probe exists once. The ongoing notification is tappable (opens the app) so "is it on?" is answerable from the shade. Human step: reboot the device, open a target app, confirm the pause screen — then force-stop Eruditio and confirm the Settings watcher card flips to *Not watching* and recovers.
