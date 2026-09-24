# 07: Completion feeds the engine

**What to build:** The connection that makes the product's promise real: a completed reading session (from either entry path) creates review items from the concepts in that segment, the review engine schedules them, and the home screen stops being a placeholder — it shows today's due reviews and recent sessions. After this ticket, tomorrow's due set is changed by today's reading.

**Blocked by:** 02 (Review scheduler and mastery ledger), 06 (Replace opens a real session).

**Status:** ready-for-human

- [ ] Seam test: completing a session changes the due-review set
- [ ] Review items derive from completed reading segments
- [ ] Home screen lists today's due reviews and recent sessions
- [ ] Manual smoke on device: open target app → replace → read → check in → review appears on home

**2026-09-24 — agent**: Seam test "completing a session changes the due-review set" passes; review items derive from completed segments (SegmentConcepts windows); Home lists today's due reviews and recent sessions. Device smoke (open → replace → read → check in → review) pending (human).
