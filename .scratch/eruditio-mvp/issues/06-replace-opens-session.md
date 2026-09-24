# 06: Replace opens a real session

**What to build:** The pause screen's "replace" action stops being a stub and starts a real reading session through the same entry the home screen uses: the reader opens at the user's last position, ready to read. The "continue" path is unchanged. Outcomes keep recording as before — the difference is that *replaced* now produces a real session.

**Blocked by:** 03 (Reader with first texts), 04 (Interception friction loop).

**Status:** ready-for-agent

- [ ] Choosing "replace" on the pause screen opens the reader at the last read position
- [ ] Completing a replacement-started session records a completion like any other session
- [ ] "Continue" path behaves exactly as in ticket 04
- [ ] Seam tests cover: replace → session starts; continue → recorded, no session
