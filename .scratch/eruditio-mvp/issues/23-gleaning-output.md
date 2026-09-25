# 23: Gleaning output

**What to build:** A meaningful reading completion leaves a source-linked gleaning: the reader writes one takeaway in their own words, then compares it with the original passage and any locally bundled curated context. Gleanings are collected for later browsing; they are not graded and do not enter the spaced-repetition ledger.

**Blocked by:** Reader flow and content/source identity (03, 05, 06).

**Status:** in-progress

## Needs doing

- [x] Pick the owner-approved Gleaning interaction prototype — Variant C, “three movements” (Notice → Compare → Keep)
- [x] Define the local gleaning record and source anchor
- [x] Add the reflection step to meaningful completion
- [x] Add a browsable Gleanings surface
- [x] Demote or hide arbitrary segment Review items until memorizable content exists
- [ ] Verify offline, private, no-data-egress behavior

## Done

- [x] Product boundary recorded in ADR-0006
- [x] Owner chose: own takeaway + curated context together

## Comments

- **2026-09-25 — agent**: Current Review shows passive passage excerpts and self-grade buttons. Owner clarified the product goal is gleaning value from books, not memorizing them. Prototype: `prototype/gleaning.html?variant=A|B|C`.
- **2026-09-25 — owner**: **C — three movements**. Implement Notice → Compare → Keep; no grade or memorization mechanic.
- **2026-09-25 — implementation**: Added local Room-backed Gleanings (schema v5), source anchors + bundled editorial lens, exact Notice → Compare → Keep persistence, ViewModel-owned state with saved drafts, accurate counts, safe exits, and no automatic passive-segment review enrollment. Unit tests and debug build pass. Real-device migration/flow smoke remains pending because no ADB device is currently connected.
