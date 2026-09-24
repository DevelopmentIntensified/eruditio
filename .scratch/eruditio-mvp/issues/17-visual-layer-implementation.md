# 17: Visual layer — prototype first, then implement (owner verdict: all four directions)

**What to build:** Owner picked **all four** sketched directions (2026-09-24), and directed
**prototyping first**. Phase 1 is a throwaway prototype; Phase 2 (implementation ticket)
spins out only after the owner's per-element verdicts.

**Prototype:** branch `prototype/visual-layer` → `prototype/visual-layer.html`
(also left untracked in-tree for review). Four questions, variant-switchable
(floating bar / ← → keys; `?page=…&variant=…` in URL; ✓ button records picks):

- **dials** — per-text reading dial on library rows: A quarters dial · B checkpoint dots · C spine fill · D plain ring (today)
- **week** — session-dot week strip on Progress: A quiet dots · B streak head · C volume bars · D linked path
- **signature** — checkpoint treatment as the visual signature: A quiet marks · B breathing marks · C instrument column · D diamond marks
- **plates** — ancient-track plates surfacing: A chapter plate in reader · B library badge gallery · C post-session artifact

**Blocked by:** owner verdicts per element (recorded here under Comments).

**Status:** resolved

- [x] Prototype branch exists with all four questions, state surfaced, pick buttons
- [x] Owner verdicts recorded per element
- [x] Implementation ticket spun out (18-visual-layer-build) with the verdict folded in

## Comments

**2026-09-24 — owner (via implement session)**: "16, prototype them first" — supersedes direct implementation; prototype the four directions before any Compose work.
**2026-09-24 — agent**: Prototype built and smoke-verified (browser: all pages, all variants, zero console errors; screenshots confirm rendering). Awaiting owner verdicts.
**2026-09-24 — owner (verdict URL)**: `picks=week:B, dials:A, signature:B, plates:A` — plus directive: plates must be accurate (verify each time or embed from other sources).
**2026-09-24 — agent**: Verdicts recorded; to-tickets quiz approved (four tickets, all gated on ticket 15; plates scope = map + timeline). Implementation spun out: `18-quarters-dial-library.md`, `19-streak-head-week-dots.md`, `20-breathing-checkpoint-marks.md`, `21-sourced-plates-ancient.md`. This ticket resolved; owner preference recorded in `AGENTS.md` (prototype-first).
