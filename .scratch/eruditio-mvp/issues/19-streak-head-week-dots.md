# 19: Streak head + week dots on Progress (prototype verdict B)

**What to build:** Progress shows the streak head the owner picked from the visual-layer prototype (2026-09-24): the day-count as the primary element with a compact 7-dot strip beside it — filled dot = day with at least one completed session, gold ring = today pending. Dots derive from real completed-session records in the session log, from the honest streak rule. Streak stays habit, not mastery; mastery metrics stay displayed and stored separately.

**Blocked by:** 15 (Togglable margin rail).

**Status:** ready-for-agent

- [ ] Week strip reflects actual completed-session days from the session log
- [ ] Today renders pending (gold ring), never falsely filled
- [ ] Streak head is primary; dots secondary; habit/mastery separation preserved
