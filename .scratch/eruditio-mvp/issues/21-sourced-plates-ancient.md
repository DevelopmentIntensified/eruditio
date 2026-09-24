# 21: Accurate sourced plates — map + timeline — for the ancient track (prototype verdict A)

**What to build:** Opening an ancient-track text shows a chapter plate in the reading flow (owner verdict A, 2026-09-24): a real map plate at the front of the text like a book frontispiece, tap to open a full-screen plate viewer carrying the map and a typeset timeline of the library's span. Owner directive (2026-09-24): plates must be **accurate** — verified each time, or embedded from other sources. So: the map ships as a licensed-verified public-domain scan (e.g. William R. Shepherd's 1911 Historical Atlas plates, on Wikimedia Commons, public domain due to age), never a schematic hand-drawing; the timeline is typeset in-app from dates verified against authoritative sources. Every plate asset ships with recorded verification: source file + license tag + a content accuracy check.

**Blocked by:** 15 (Togglable margin rail).

**Status:** ready-for-human

- [ ] Ancient-track texts open onto a sourced map plate in the reading flow; tap opens the full-screen viewer
- [ ] Map asset: public-domain scan with source page + license tag recorded on this ticket before ready-for-human
- [ ] Timeline plate typesets dates verified against sources, each date recorded
- [ ] Reading-flow insertion keeps the resume-anchor contract exact (first visible item IS the place, checkpoints every 12 items, rail ticks agree) — record how the plate item composes with item indexes
- [ ] App size impact noted (ticket 11 cares)

## Comments

**2026-09-24 — agent**: Shipped on `t21-plates` (fe9aa9c + 8ab8aa5), merged and verified on `eruditio-mvp-2` (`ece9cc1` — conflict resolution combined ticket 20's breathing painter with the offset-corrected checkpoint condition).

**Map plate verification (owner accuracy directive):**
- Source: https://commons.wikimedia.org/wiki/File:Hellas_431bc.jpg — William R. Shepherd, *Historical Atlas*, 1926 edition. License verified via Commons imageinfo extmetadata: **Public domain (PD-old; no restrictions; Shepherd d. 1934)**.
- Era fit: "GREECE at the Beginning of the Peloponnesian War (431 B.C.)" — exactly the Plato-era world (Plato b. 428/427 during the war; Socrates' Athens).
- Content check: on-scan legend (Delian Confederacy / Sparta and allies / Macedonia / neutral states, scale 1:6,000,000); labels verified off-scan: Athens (Attica) with Piraeus/Salamis/Aegina/Megara, Sparta (Laconia) with Gythium, Corinth/Argos/Sicyon/Epidaurus/Troezen, Mantinea/Tegea, Pylos/Methone/Taenarum/Cythera — placements cross-checked against standard classical geography (publisher of record: Perry-Castañeda Library, UT Austin).
- Asset: `res/drawable-nodpi/plate_greece_431bc.webp`, 1120×1100 native scan resolution, WebP q92, **446 KB (~+0.44 MB APK — ticket 11 impact noted)**.

**Timeline plate verified dates (all Britannica or corroborated):** Plato b. 428/427 BC · Socrates d. 399 BC · Marcus Aurelius 121–180 · Augustine 354–430 · KJV 1611 · Spurgeon 1834–1892. Typeset in-app on Canvas (`TimelinePlate`), palette-derived colors, caption states the factual span only.

**Index composition:** one header-offset constant (`PLATE_ITEM_COUNT = 1` iff ancient). All persisted state stays in TEXT-item space; offset applied only at scroll boundaries — reads subtract it (flush, debounced saver, visible-checkpoint, check-in), writes add it (restore scroll). Checkpoint rhythm counts text items only; the plate counts as "the beginning"; rail draws raw list space with ticks shifted by the offset. First visible item IS the place, including when it is the plate.

Human step: device check — open an ancient text (plate frontispiece), tap to viewer (map + timeline), confirm resume/checkpoints/rail still exact.
