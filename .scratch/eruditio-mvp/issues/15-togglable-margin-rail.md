# 15: Togglable margin rail

**What to build:** The rail replaces the scrollbar: drag or tap anywhere on it to jump into the book; it tracks position while scrolling normally; a Settings toggle turns it on/off, persisted on-device. Checkpoint ticks are added to it by ticket 14 if the rail ships first.

**Blocked by:** 12 (Resume anchor = top-of-screen line — rail position derives from the same anchor).

**Status:** ready-for-human

- [x] Rail visible in the reader; drag or tap jumps to that part of the text
- [x] Marker tracks scroll position during normal scrolling
- [x] Settings has a "Reading rail" toggle, persisted across restarts
- [x] Off state hides the rail entirely

## Comments

**2026-09-24 — agent**: Shipped on `t15-rail` (commit 2758c51), merged to `eruditio-mvp-2` and verified (compile + seam tests green post-merge). Rail: 6dp column in a 22dp gesture strip at the reading column's right edge, between title header and check-in button; checkpoint ticks at `index % 12 == 11` (at-or-before first visible = stronger primary color); marker = first visible item / items.count — same anchor as resume. Tap jumps, plain vertical drag scrubs continuously (no long-press); during scrub the debounced saver persists the first visible item (correct: resume follows what you looked at). Settings card "Reading rail" switch, persisted via SharedPreferences (`ReadingSettings`). Off state: composable early-returns — no space reserved. Default on (groundwork commit 6eb72b7 choice; "togglable" shipped on-by-default). Seam tests green. Human step: device check (drag/tap jump, toggle off/on, persistence).
**2026-09-24 — bookkeeping**: Status corrected ready-for-agent → ready-for-human; checkboxes ticked — the work shipped and merged (code verified on `eruditio-mvp-2`: `ReadingSettings`, Settings switch in `App.kt`, rail wired in `ReaderActivity.kt`); the only remaining step is the owner's device check, already recorded above.
