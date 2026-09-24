# 09: Progress view and streak layer

**What to build:** The honest answer to "am I getting anywhere." A progress screen shows: texts read, concepts by mastery level, reviews due, and the weekly count of interceptions. A streak for completing sessions exists as a habit layer — visually secondary, rewarded for completing sessions (never for merely opening the app), and kept strictly separate from mastery metrics.

**Blocked by:** 07 (Completion feeds the engine).

**Status:** ready-for-human

- [ ] Progress screen shows texts read, concepts by mastery level, reviews due — all matching the ledger state
- [ ] Weekly intercepted count equals recorded continued + replaced outcomes
- [ ] Streak increments only on days with at least one completed session and resets per its stated rule
- [ ] Streak and mastery are displayed and stored separately

**2026-09-24 — agent**: Progress screen shows concepts by mastery (effective, decay-aware), reviews due, texts started vs read through (ticket wording), weekly intercepted + replaced. Streak counts only completed-session days (StreakTest: 7 cases); stored separately from the ledger; displayed as "habit, not mastery". Device verification pending (human).
