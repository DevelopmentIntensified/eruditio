# Status — eruditio-mvp

Pull the next task from the queue below. Do **not** re-read all ticket files to find work; this file is the source. Update it (queue + statuses) whenever a task completes.

## Queue (next up, in order)

(none — agent work complete; branch ready)

## Recently completed

- **Deepening review round + fix pass** — final review over `f6f99f1...HEAD` (Standards + Spec axes, both read-only scouts; spec verified statically, std confirmed post-state): six promises held; fix pass merged as `13cf617`: dead `paragraphIndex` column dropped (Room schema v2→v3, non-destructive migration), named `isStarted`/`isReadThrough` predicates (LibrarySelection rule enforced), shared `AppContainer.progressById()`, `sessions.resumeTextId()` (PauseActivity's last positions reach absorbed — promise 3 now fully held), fake de-dup, `weekDots` uses the TrailingWeek value, ADR-0002 amendment names pure predicate modules under `ui/`. Suppressed finding: the three one-line SessionLog port forwards are the owner-directed reach-closing interface, not Middle Men.
- **Deepening candidates 1–5** — per-candidate worktrees, owner-approved one by one, merged `df0649a` → `7a0310d`.
- **Pre-deepening review round** — merged as `f6f99f1` (zone-drift fix + seam test, challenge fallback, back-press records continued, saveJob fix, Groups constants, CONTEXT.md + ADRs 0001–0005).

## State of the branch

- `eruditio-mvp-2` @ final merge of `deepening-review-fixes` — the MVP spec (tickets 01–22) plus two review rounds plus a five-candidate deepening round, all merged. 70 seam/unit tests green.
- No git remote configured → no hosted draft PR exists; the branch itself is the deliverable. Marking "ready" = this state.
- Human steps remain per ticket (all recorded on the ticket files, statuses ready-for-human): device smoke per ticket, Play internal testing (10), USPTO + package id (11).
- Pre-existing (out of delta, flagged, not fixed): `AppContainer.textLength` first hit loads the asset synchronously on the caller's dispatcher — jank candidate on first open; the ×30 percent recompute per refresh/check-in predates and was owner-approved at the C4 gate.

## Bookkeeping pending

(none)

## Agent work done, awaiting human device steps (do NOT re-implement)

01, 02, 03, 04, 05, 06, 07, 09, 10, 11, 12, 13, 14, 15, 18, 19, 20, 21, **22** — all `ready-for-human`; each ticket file records its remaining human step (device smoke, Play internal testing, USPTO/package-id checklist).

## Parked / deferred

- **08 — handoff shelf**: `needs-triage`; owner deferred the redo post-MVP. Interim in-app "Read next" ships as-is. Not this branch's work.
- **16, 17 — visual layer decision/prototype**: `resolved`.

## State of the branch

- PR branch = `eruditio-mvp-2` (no git remote configured, so no hosted draft PR exists). All `tNN-*` ticket branches merge here.
- A code-review round already landed on this branch (`b6082de` spec-axis fixes, `984b606` standards cleanups). After ticket 22 lands, run /code-review once more over `main..eruditio-mvp-2`, fix findings in a single implementer pass.
- Untracked in-tree: `prototype/visual-layer.html` (intentionally untracked per ticket 17; leave).
