# 04: Interception friction loop

**What to build:** The verified friction mechanic, end to end on a real device. The user grants usage access; when a chosen target app opens, a pause screen appears over it — short delay, breathing moment, and an explicit option to continue straight into the app (never a hard block). The user can also tap "replace" (stub action at this stage). Every interception records its outcome: *continued* or *replaced*. Target-app list is user-selectable with sensible defaults (top social apps).

**Blocked by:** 01 (Walking skeleton).

**Status:** ready-for-human

- [ ] Opening a target app shows the pause screen over it, after the delay
- [ ] "Continue" opens the target app; the choice is recorded as *continued*
- [ ] "Replace" records *replaced* (action itself may be a stub)
- [ ] User can add/remove target apps; defaults present on first run
- [ ] Built on UsageStatsManager-based detection; AccessibilityService is not used
- [ ] Manual smoke on device passes for both paths

**2026-09-24 — agent**: Pause screen (10 s breathing delay, explicit continue), target list with defaults, UsageStatsManager polling service (no AccessibilityService), outcomes recorded at the seam. Per user direction, "Read a page instead" no longer waits out the delay; "Continue" still does. Manual smoke on device pending (human).

## Comments

- **2026-09-24 — review fix**: Dismissing the pause screen with Back now records the outcome as *continued* (the user proceeds to the app beneath the overlay) before finishing — story 4's "choice recorded" and ticket 09's weekly count no longer under-report. Limitation: the system Home button is not interceptable by an Activity, so a Home-press dismissal stays unrecorded. The recording is asynchronous via the app-scope coroutine but is not coupled to the watcher's re-intercept cooldown (cooldown keys on `SystemClock.elapsedRealtime`, recording on epoch day) — the record lands regardless of when the target app is foregrounded again.
