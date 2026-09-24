# 14: Checkpoints (variant A, spacing widened) with pause-to-Home

**What to build:** Unobtrusive checkpoint dots in the gutter every ~2 screens of reading. When a checkpoint is on screen, a quiet "Pause here" chip appears; pausing completes the session at that checkpoint, records the position, and lands the user on Home (Today) — no completion screen. The margin rail shows checkpoint ticks once this lands. Resume after a pause lands on the saved top line.

**Blocked by:** 12 (Resume anchor = top-of-screen line).

**Status:** ready-for-human

- [ ] Checkpoint dots appear in the gutter at the widened spacing (every 12 items; spacing decision recorded here, revisit only with evidence)
- [ ] Pause chip completes the session at that checkpoint and returns to Home
- [ ] Resume after pause lands on the saved top line
- [ ] Rail (if shipped) shows checkpoint ticks

## Comments

**2026-09-24 — agent**: Shipped on `t14-checkpoints` (commit ef0dc3f), merged to `eruditio-mvp-2` (7854365). Dots: every row gained a constant 18dp start gutter; `index % 12 == 11` rows draw a 9dp dot — spacing decision recorded: **12 items** (≙ ~2 screens at bodyLarge/30sp line height; revisit only with evidence). "Pause here" chip (quiet TextButton) appears whenever a checkpoint is visible; pause = save checkpoint item as resume point → `completeSession(sessionId, endCharAtNextCheckpoint)` → `finish()`, no completion screen. Race hardening: pending debounced saver cancelled and onPause flush callback nulled before finish, so nothing overwrites the checkpoint save. Pause-to-Home: `pauseToHome` flag on AppContainer consumed by the shell (reader is a separate activity; no activity-result contract exists — container-level flag was the minimal seam). Rail ticks deferred to ticket 15 (rail ships there). Seam tests green. Human step: device smoke (dots, chip, pause → Home, resume lands on checkpoint).
**2026-09-24 — review round (t22-review-fixes b6082de)**: S2 — interception-initiated pause-to-Home was unreachable (PauseActivity roots a NEW_TASK; no shell beneath; finish() dropped to launcher). Fix: `ReaderActivity.leaveAtCheckpoint()` — when the reader `isTaskRoot` (cold interception) it starts MainActivity with NEW_TASK|CLEAR_TOP and finishes onto it (flag consumed → Today); warm interception and in-app paths unchanged. Flush callback replaced with the `onRegisterFlush`/`onLeave` callback wiring; suppression of the pause flush is now synchronous at click time. Review findings T1/T5/T7/T8 (checkpoint predicate extraction, offset helpers, `saveJob` rename) landed in the same pass.
