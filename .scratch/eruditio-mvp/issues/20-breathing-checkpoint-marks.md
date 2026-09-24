# 20: Breathing checkpoint marks in the reader (prototype verdict B)

**What to build:** The checkpoint gutter dots (ticket 14) get the variant-B treatment the owner picked from the visual-layer prototype (2026-09-24): while a checkpoint is on screen its dot breathes — a slow accent-colored pulse — and stays quiet otherwise. Pause chip behavior and checkpoint spacing (every 12 items) unchanged.

**Blocked by:** 15 (Togglable margin rail — same reader file).

**Status:** ready-for-human

- [ ] Visible checkpoint's dot breathes (accent pulse); non-visible dots stay quiet
- [ ] No new surface, no nagging; scrolling performance unaffected

## Comments

**2026-09-24 — agent**: Shipped on `t20-breathing-marks` (commit 971c97b), merged and verified on `eruditio-mvp-2`. Reuses ticket 14's derived visible-checkpoint state; only the nearest on-screen checkpoint dot breathes (1→1.25→1 over 1600ms FastOutSlowIn ease, soft fading ring underneath — prototype CSS parity), accent from `colorScheme.tertiary`, no hardcoded hexes. Infinite transition lives inside the breathing dot's composition only — starts when composed, disposed when scrolled away; non-checkpoint rows short-circuit before the state read, so scroll-path recomposition is unaffected. Pause chip, rail, spacing, pause path untouched. Merge note: the plate-offset merge (ticket 21) combined with this painter — checkpoint condition is text-space, breathing is list-index-conditioned. Human step: device check (dot breathes when a checkpoint is on screen, stops otherwise).
