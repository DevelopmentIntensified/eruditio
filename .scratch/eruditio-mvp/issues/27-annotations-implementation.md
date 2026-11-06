# 27: Implement the owner-picked annotation UI (blocked by 26)

**What to build:** Production implementation of the bookmark/highlight/note variant the owner picks from ticket 26's prototype, on the Room v4 `annotations` table, wired into `ReaderActivity`/`ReaderText` checkpoint math and surfaced in the reader + library/reader entry points. Repo standards: seam-extract where a decision lives, pure predicates JVM-tested, existing unit suite stays green; new tests only for consumer-visible behavior (jump-exact, persistence, editing).

**Blocked by:** 26 (owner picks the variant first).

**Status:** ready-for-human

## Comments

**2026-10-31 — agent**: Created alongside 26 so the prototype gate and acceptance criteria are explicit up front.
