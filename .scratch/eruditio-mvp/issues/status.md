# Status — eruditio-mvp

Pull the next task from the queue below. Do **not** re-read all ticket files to find work; this file is the source. Update it (queue + statuses) whenever a task completes.

## Queue (next up, in order)

1. **Final /code-review round** over `main..eruditio-mvp-2` (per implement-spec: all tickets complete → review the PR branch; fix all findings in a single implementer pass). Previous round already landed (`b6082de` + `984b606`); this one covers ticket 22 too.
2. After review fixes: mark branch ready (no git remote configured — no hosted PR exists; see State below), clean up the `t22-bug-report` worktree.

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
