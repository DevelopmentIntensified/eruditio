# 05: Complete the 30-text library

**What to build:** The full MVP reading library. The remaining texts beyond ticket 03's first batch get bundled into the app: Spurgeon sermons, Augustine, KJV selections, missionary biographies — public-domain sources only, with Project Gutenberg branding stripped (per PG's developer guidance for commercial redistribution) and Standard Ebooks preferred where CC0 editions exist. No reading or scheduling changes — pure content expansion of the existing reader.

**Blocked by:** 03 (Reader with first texts).

**Status:** ready-for-human

- [ ] All 30 texts appear in the library list and open correctly in the reader
- [ ] Source audit: every text public domain (US), no third-party-branded boilerplate inside bundled files
- [ ] App size remains acceptable on device

## Comments

**2026-09-24 — user**: PG (ratings) is not the goal. Old texts may have PG-13-level content or worse; avoid obscene content, but do not avoid every non-PG thing. Source audit criterion reads "public domain (US)" — not "family friendly throughout".

**2026-09-24 — agent**: All 30 texts bundled (`app/src/main/assets/texts/`, 11.0 MB). Corrections made during the source audit: Imitation of Christ → PG#1653 (was #4783, wrong work), Meno → Jowett edition PG#1643 (an earlier note said #1539, which is Shakespeare's Winter's Tale — script's #1643 is what ships), Crito → PG#1657, Practice of the Presence of God → PG#5657. PG#1901-1903 are not "Spurgeon's Sermons" volumes; they are three real Spurgeon sermon-collections — Talks to Farmers, The Art of Illustration, Gleanings among the Sheaves — retitled accordingly. Branding audit: `grep -il "project gutenberg" assets/texts/*.txt` → none; every file cut between PG's START/END markers or truncated at old-style footers. Content-rating guidance recorded above (classics ship as written).

**2026-09-24 — agent**: Follow-ups committed: crito head trimmed of residual old-style PG footer (`Ver.04.29.93*END*` + transcriber credit) — the only boilerplate residue found across the 30 files. Meno note corrected: PG#1539 is The Winter's Tale; the Jowett Meno that ships is PG#1643.
