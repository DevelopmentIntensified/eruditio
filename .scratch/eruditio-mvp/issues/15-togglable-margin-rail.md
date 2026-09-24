# 15: Togglable margin rail

**What to build:** The rail replaces the scrollbar: drag or tap anywhere on it to jump into the book; it tracks position while scrolling normally; a Settings toggle turns it on/off, persisted on-device. Checkpoint ticks are added to it by ticket 14 if the rail ships first.

**Blocked by:** 12 (Resume anchor = top-of-screen line — rail position derives from the same anchor).

**Status:** ready-for-agent

- [ ] Rail visible in the reader; drag or tap jumps to that part of the text
- [ ] Marker tracks scroll position during normal scrolling
- [ ] Settings has a "Reading rail" toggle, persisted across restarts
- [ ] Off state hides the rail entirely
