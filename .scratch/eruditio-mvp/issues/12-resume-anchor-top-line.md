# 12: Resume anchor = top-of-screen line, saved on every exit

**What to build:** Wherever the user is, the top visible line *is* their place. Scrolling saves it, leaving the app or changing screens flushes it immediately, and check-in anchors to it instead of the bottom-of-screen item. Reopening a text always shows the top line they left at, at the top of the screen.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] Reopening any text shows the last top-of-screen line at the top
- [ ] Leaving the app mid-read (no check-in) restores that exact line
- [ ] Check-in changes neither the resume point nor the visible top line
- [ ] Existing seam tests still green
