# 12: Resume anchor = top-of-screen line, saved on every exit

**What to build:** Wherever the user is, the top visible line *is* their place. Scrolling saves it, leaving the app or changing screens flushes it immediately, and check-in anchors to it instead of the bottom-of-screen item. Reopening a text always shows the top line they left at, at the top of the screen.

**Blocked by:** None (can start immediately).

**Status:** ready-for-human

- [ ] Reopening any text shows the last top-of-screen line at the top
- [ ] Leaving the app mid-read (no check-in) restores that exact line
- [ ] Check-in changes neither the resume point nor the visible top line
- [ ] Existing seam tests still green

## Comments

**2026-09-24 — agent**: Shipped on `t12-resume-anchor` (commit 90d6672), merged to `eruditio-mvp-2`. Anchor = FIRST visible item: the 600ms debounced saver already persisted the top line; check-in now anchors the resume point to the top visible line (was last-visible); `ReaderActivity.onPause` flushes the top line immediately via a DisposableEffect-registered callback, so leaving mid-read persists the exact line without waiting for the debounce. Position store contract unchanged. Seam tests green (SessionControllerTest, StreakTest). Human step: verify reopen/exit/check-in behavior on device.
