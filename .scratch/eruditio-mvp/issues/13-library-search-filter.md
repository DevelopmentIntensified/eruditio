# 13: Library search & filter (prototype variant B wins)

**What to build:** Library gets a segmented bar (All / Classics / Christian / Lives), a search icon revealing an input, and a progress pill cycling any / in progress / unread. Flat list, match count shown, percent rings kept. Section headers are replaced by the segment filter (prototype verdict: variant B).

**Blocked by:** None (can start immediately).

**Status:** ready-for-human

- [ ] Search by title/author from a hidden-until-tapped field
- [ ] Segment bar filters by content group
- [ ] Progress pill filters; the match count reflects all active filters
- [ ] Rows still open the reader at last position

## Comments

**2026-09-24 — agent**: Variant B shipped on `t13-library-filter` (commit 115d19a), merged to `eruditio-mvp-2`. Segment bar (All/Classics/Christian/Lives, single-select, replaces section headers — list always flat), hidden-until-tapped search (title OR author, case-insensitive, ✕ clears/hides), progress pill cycling any/in-progress/unread (100% shows "read", excluded from unread), "N of 30" match count composing all three filters, percent rings kept. Filters survive tab switches (rememberSaveable). Rows unchanged — same resume-at-last-position flow. No new deps, no seam changes. Human step: device glance.
