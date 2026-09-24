# 02: Review scheduler and mastery ledger

**What to build:** The retention engine as pure logic at the review-scheduler seam, with no Android dependencies. Completion events (concept + recall grade) in; due-review set and mastery transitions out. Concepts move through familiar → proficient → mastered, decay when neglected, and a mastery-challenge mode resurfaces learned-but-aging concepts. A debug screen shows today's due items for a synthetic ledger so the logic is verifiable without UI.

**Blocked by:** 01 (Walking skeleton).

**Status:** ready-for-agent

- [ ] Table-driven tests cover mastery transitions, decay, and challenge selection
- [ ] Boundary cases pass: empty ledger, missed days, re-mastering after decay
- [ ] Due set ordering is deterministic
- [ ] Debug screen displays the due set for a synthetic ledger
