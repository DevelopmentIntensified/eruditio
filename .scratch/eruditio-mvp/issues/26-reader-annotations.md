# 26: Reader annotations — bookmark, highlight, note

**What to build:** While reading, the user can save a bookmark at the current line, highlight a selected range, and attach a short note, then revisit all of them later (list per text, tap to jump back). Nothing collected, logged, or sent — the ticket-22 privacy posture applies: no analytics, no export by default, data stays on device.

**Owner preference (AGENTS.md):** prototype-first — annotation UX is a UI design decision, so rendered variants come first and the owner picks (pattern: 16→17, 22).

**Data direction (implementation-light note for 27, not a locked design):** Room v4 non-destructive — one `annotations` table: `textId`, `kind` (bookmark | highlight | note), `startChar`, `endChar`, `noteText` nullable, `createdAtMillis`. Offsets in the same character space sessions/positions already use; a bookmark anchors at the first visible TEXT-item `startChar` per the ticket-21 checkpoint math so it survives restarts.

**Blocked by:** None (prototype immediately; owner verdict gates implementation).

**Status:** ready-for-human

- [ ] Prototype variants for bookmark / highlight / note rendered; owner picks one (verdict recorded here)
- [ ] Model + Room v4 migration lands non-destructively (v3 pattern)
- [ ] Bookmarks jump to the exact line across app restarts
- [ ] Highlights persist and render in the reader
- [ ] Notes editable; all marks listed per text and tappable

## Comments

**2026-10-31 — agent**: Filed from owner direction (session item 3: "need to be able to bookmark, highlight, note a book"). Implementation ticket created as 27, blocked by this one's verdict. Prototype: `prototype/annotations.html` (throwaway, untracked per convention). Variants: **A selection** (select → inline mark menu; hanging caret bookmarks the line), **B band** (bottom pill band: Bookmark/Highlight/Note/Marks); **C rail** (right-margin rail of existing marks, + opens the composer; rail doubles as the list). Shared marks drawer, jump-back + delete, note sheet (ticket-22 motif), dark toggle, URL persistence.

Smoke (browser, 2026-10-31): boot state renders; variant cycle A→B→C→A clean; A: programmatic selection → mark menu → Note → Save ("note saved · line 2"); drawer/jump/delete operate; ✓ pick persists in URL (`?page=marks&v=B+band&pick=B+band`); **zero console errors** across all flows.

**Owner:** open the file and pick A / B / C (floating bar "✓ pick" or ← → keys). Implementation (27) proceeds on the pick.
