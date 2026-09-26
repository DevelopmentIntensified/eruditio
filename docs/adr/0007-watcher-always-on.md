# The watcher is always on: boot receiver, watchdog, honest liveness

Interception only works while the polling watcher is alive, and nothing in Android keeps a foreground service alive on its own. Starting it from the app's own UI (the original single call site, `MainActivity.onCreate`) means a reboot silently ends interception until the user happens to open Eruditio — the failure the owner actually hit. The watcher is therefore armed from every legal entry point, backed by a periodic self-heal, and its state is reported to the user rather than assumed.

Status: accepted

## Decisions

- **`BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` receiver arms the watcher.** Both are documented exemptions from the background foreground-service start restrictions, so the watcher can re-arm with the user nowhere in sight. No `QUICKBOOT_POWERON` or OEM-specific action is chased.
- **A 15-minute WorkManager watchdog revives a dead watcher** (unique periodic work, `KEEP` so arming is idempotent). A WorkManager job is *not* an exemption from those restrictions, so a refused start is expected: the worker retries with exponential backoff rather than crashing, and each periodic run keeps trying.
- **The service beats a heartbeat every ~15 s** into `SharedPreferences`; silence past 2 minutes means it is gone. `WatcherHealth` turns the beat plus the OS probes (usage access, battery optimisation, selected targets) into one `WatcherStatus` — a pure predicate, unit-tested, no Android imports.
- **Settings shows that state with the fix only the user can give** (usage access, battery-optimisation list, "start the watcher now"). A silent watcher is the worst outcome: the pause screen never appears and nothing says why.
- **Battery-optimisation exemption is offered, never requested.** `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is not declared; Play restricts the direct request dialog, so the card links to the Settings list instead.
- **A (re)started service seeds foreground state from the last 30 s of usage events** instead of starting blind, so an app already in the foreground is not missed — and is not intercepted on the restart either.

## Consequences

- Worst case after a mid-session kill: roughly 17 minutes without interception (2-minute stale threshold + 15-minute cadence), less when the OS allows the restart or the user exempts battery optimisation.
- The heartbeat is one `SharedPreferences.apply()` per 15 polls at 1 Hz — negligible, and it is the only honest liveness signal available.
- Detection remains poll-based and `AccessibilityService`-free (ADR-0001); this ADR changes only how long the watcher survives, not how it sees.
