# 02: Review scheduler and mastery ledger

**What to build:** The retention engine as pure logic at the review-scheduler seam, with no Android dependencies. Completion events (concept + recall grade) in; due-review set and mastery transitions out. Concepts move through familiar → proficient → mastered, decay when neglected, and a mastery-challenge mode resurfaces learned-but-aging concepts. A debug screen shows today's due items for a synthetic ledger so the logic is verifiable without UI.

**Blocked by:** 01 (Walking skeleton).

**Status:** ready-for-human

- [ ] Table-driven tests cover mastery transitions, decay, and challenge selection
- [ ] Boundary cases pass: empty ledger, missed days, re-mastering after decay
- [ ] Due set ordering is deterministic
- [ ] Debug screen displays the due set for a synthetic ledger

**2026-09-24 — agent**: Agent work complete. 13 seam tests pass (mastery table, hard/again/easy, decay, re-master, missed days, deterministic ordering, limits, challenge selection). Debug synthetic-ledger surface now registers only in debug builds (`app/src/debug/AndroidManifest.xml`). Remaining human step: glance at Progress → "Debug: synthetic ledger due set" on device.

## Comments

- **2026-09-24 — review fix**: Mastery challenge now reaches the user (spec story 14): when the due set is empty and the challenge set is non-empty, the Review tab serves challenge cards. Placement: VM-side fallback with a mode flag (`EruditioViewModel.servedReviewCards()` + `ReviewState.isChallenge`) — the review state machine (`current`/`index`/`total`) lives in the VM, so the rule sits there with one home; grading still flows through the seam's existing grade path. Caption stays claim-disciplined: "Mastery challenge · learned material resurfacing".
