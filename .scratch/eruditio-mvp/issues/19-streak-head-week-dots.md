# 19: Streak head + week dots on Progress (prototype verdict B)

**What to build:** Progress shows the streak head the owner picked from the visual-layer prototype (2026-09-24): the day-count as the primary element with a compact 7-dot strip beside it — filled dot = day with at least one completed session, gold ring = today pending. Dots derive from real completed-session records in the session log, from the honest streak rule. Streak stays habit, not mastery; mastery metrics stay displayed and stored separately.

**Blocked by:** 15 (Togglable margin rail).

**Status:** ready-for-human

- [ ] Week strip reflects actual completed-session days from the session log
- [ ] Today renders pending (gold ring), never falsely filled
- [ ] Streak head is primary; dots secondary; habit/mastery separation preserved

## Comments

**2026-09-24 — agent**: Shipped on `t19-week-dots` (commit 1afb4f1), merged and verified on `eruditio-mvp-2`. Streak head card: headlineLarge day-count + "day streak" unit + compact 7-dot strip (filled = completed-session day from real `SessionLog.completedEpochDays()`, gold ring = today pending, weekday initials). Dots data flows through the seam: `SessionController.weekDots()` (core seam, no DAO change), VM fills `ProgressState.weekDots`. Today never pre-filled — seam-tested on both sides of midnight. "Habit, not mastery" caption kept; mastery card untouched; old text-only streak line + 🔥 dropped. Colors all palette-derived. Seam tests green (18 tests incl. new week-dots case). Human step: device glance.
