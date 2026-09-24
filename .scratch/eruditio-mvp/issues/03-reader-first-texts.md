# 03: Reader with first texts

**What to build:** A real reading session, started by the user. Home screen shows a library list; the user opens a bundled classic (Plato, Marcus Aurelius among the first ~5 texts), reads on a quiet distraction-free surface, and the app remembers their position across restarts. Completing a session records a check-in. All offline.

**Blocked by:** 01 (Walking skeleton).

**Status:** ready-for-human

- [ ] Library list shows the bundled texts and opens the reader
- [ ] Reading position restores exactly after app restart
- [ ] Completing a session records a completion (visible in app records)
- [ ] Everything works in airplane mode

## Comments

**2026-09-24 — user (wireframe direction)**: Variant A (Paper) wins; bottom nav pinned always; percent rings from variant C reused in A; library compact like B. Theme picking added: curated named palettes (Paper/Ink/Sepia/Terminal, Omarchy/nvim-style) on the Progress screen; prototype set captured at branch `prototype/ui-wireframes`.

**2026-09-24 — agent**: Library list, reader, position restore, and check-in all wired; completion recorded through the session seam. Device bug fix: PG assets carry three line-ending vintages (crlf, old \r\r\n, bare \r) so the blank-line split produced one giant paragraph per text and any check-in marked 100% (crito/apology/meditations confirmed by user). TextLibrary now normalizes endings; reader splits into offset-carrying items with an oversized-block line fallback. Remaining human step: verify exact restore + airplane mode on device.
**2026-09-24 — user + agent**: Check-in now saves the last visible line itself, so reopen lands on the exact line checked in at (user report: resume overshot). Session read-time tracking added (schema v2 `readMillis`, accumulated from the reader lifecycle); Progress shows time reading this week/all-time, Today shows sessions this week + minutes today. Post-session external handoff removed by owner direction — see ticket 08. Direction captured on throwaway branch `prototype/library-checkpoints`: resume-checkpoint variants (gutter dots / breath beacon / checkpoint ruler) + library search/filter variants (chips / segmented / bottom-sheet) await owner verdict before implementation.
