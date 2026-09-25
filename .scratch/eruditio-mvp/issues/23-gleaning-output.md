# 23: Gleaning output

**What to build:** Every meaningful reading completion enters a source-linked gleaning flow: the reader writes one takeaway in their own words, then compares it with the original passage and any locally bundled curated context. The reader may explicitly discard the reflection; saved gleanings are collected for later browsing, never graded, and never enter the spaced-repetition ledger.

**Blocked by:** Reader flow and content/source identity (03, 05, 06).

**Status:** in-progress

## Needs doing

- [x] Pick the owner-approved Gleaning interaction prototype — Variant C, “three movements” (Notice → Compare → Keep)
- [x] Define the local gleaning record and source anchor
- [x] Add the reflection step to meaningful completion
- [x] Add a browsable Gleanings surface
- [x] Demote or hide arbitrary segment Review items until memorizable content exists
- [ ] Verify offline, private, no-data-egress behavior — static + unit verified; on-device airplane-mode smoke outstanding (no ADB device attached)

## Done

- [x] Product boundary recorded in ADR-0006
- [x] Owner chose: own takeaway + curated context together

## Comments

- **2026-09-25 — agent**: Current Review shows passive passage excerpts and self-grade buttons. Owner clarified the product goal is gleaning value from books, not memorizing them. Prototype: `prototype/gleaning.html?variant=A|B|C`.
- **2026-09-25 — owner**: **C — three movements**. Implement Notice → Compare → Keep; no grade or memorization mechanic.
- **2026-09-25 — implementation**: Added local Room-backed Gleanings (schema v6), explicit pending/saved/discarded status, hand-off written *before* completion, a one-query recovery join for the crash window, exact source anchors, an offline curated catalog (`assets/gleaning_context.json`) with no fabricated fallbacks, Notice → Compare → Keep persistence, ViewModel-owned saved drafts, full local browsing, safe exits, and no automatic passive-segment review enrollment.
- **2026-09-25 — review fixes**: A review pass found and fixed two real defects — (1) an explicitly discarded session was resurrectable, because the recovery scan treated a `discarded` tombstone as unhandled; `isHandled` now covers every status and a discarded session is terminal by test. (2) the v5→v6 migration defaulted in-flight hand-offs to `saved`, which would have published empty rows into the collection; the backfill now derives status from the old blank-takeaway encoding. Also: hand-off restored ahead of completion so "completed" always implies "a reflection is owed" (the result is a three-state `GleaningHandoff`, not a Boolean); catalog anchors are whitespace-insensitive so a hard-wrapped corpus line still matches — a bug the corpus-anchored test caught on its first run.
- **2026-09-25 — verification**: 89 unit/seam tests and the debug build pass. Gleaning path contains no network, HTTP, socket, or logging calls; all persistence is Room, all curated content is a bundled asset. Real-device migration (v5→v6) and airplane-mode smoke still pending — no ADB device connected, and `exportSchema = false` means the migration has no automated schema check, so the upgrade path must be exercised on device.
