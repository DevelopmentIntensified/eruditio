# 18: Quarters dial on library rows (prototype verdict A)

**What to build:** Library rows show the variant-A "quarters dial" the owner picked from the visual-layer prototype (2026-09-24): four tick marks, progress arc, gold position notch, label `unread` / `N%` / `read` — driven by the stored reading progress so dial and reader agree. No other library behavior changes.

**Blocked by:** 15 (Togglable margin rail).

**Status:** ready-for-human

- [ ] Rows show the quarters dial in place of the plain ring (unread / N% / read)
- [ ] Dial reflects the same stored progress the reader resumes from
- [ ] Library behavior otherwise unchanged (filters, rows, match count)

## Comments

**2026-09-24 — agent**: Shipped on `t18-dials` (commit 703fdff), merged and verified on `eruditio-mvp-2`. Canvas-based `QuartersDial` (outline circle, four quadrant ticks, progress arc round-cap from 12 o'clock, gold notch at the progress angle hidden at 0%/100%, label N% / read / unread). `Ring.kt` deleted (single call site migrated — clean cutover). Notch gold added once in the curated theme layer (`tertiary` in `Palettes.kt`, both light and dark branches) — no hardcoded hex in the composable. Data unchanged: same stored progress the reader resumes from. Human step: device glance (dial in Paper + a dark palette).
