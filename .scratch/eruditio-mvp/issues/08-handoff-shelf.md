# 08: Handoff shelf

**What to build:** The post-session next step: after any completed session, the app suggests one relevant deep link — a Khan Academy course or an Exercism track — from a small static registry. The link opens the correct destination. It appears only after a completed session, nowhere else. If the registry starts growing beyond a trivial list of links, it gets cut rather than expanded.

**Blocked by:** 07 (Completion feeds the engine).

**Status:** needs-triage

- [ ] After any completed session, one relevant next-step link appears
- [ ] The link opens the correct external destination (Khan course or Exercism track)
- [ ] No link appears elsewhere in the app; the registry stays a static list

**2026-09-24 — agent**: Static registry (Khan world-history for ancient, Khan grammar for christian, Exercism Kotlin for biography); suggest() null for unknown groups; 4 seam tests pass. Handoff opens via ACTION_VIEW from the activity context (app-context ClassCastException fixed); appears only on the post-session screen. Device check pending (human).
**2026-09-24 — user/agent**: Owner questioned the external handoff and directed removal from the completed screen ("remove that and let's redo handoffs"); in-app "Read next" (same group, unread first) is the post-session step for now. `HandoffShelf` object + 4 seam tests remain so the redesign can build on them; the card no longer ships in the reader. Re-triaged needs-triage pending the redo.
**2026-09-24 — owner (via implement session)**: Redo deferred post-MVP. The interim in-app "Read next" step ships as-is; no external handoff work in this branch. Ticket stays needs-triage until a later design pass.
