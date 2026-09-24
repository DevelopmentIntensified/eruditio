# Two test seams hold the entire test surface; none inside UI

The testable logic concentrates in two pure seams — `SessionController` (OS event/decision/session lifecycle: friction decisions, outcome recording, replace→session, check-ins) and `ReviewScheduler` (the mastery ledger and due/challenge logic). Both are Android-free and the OS watcher, overlay, and UI are replaceable details behind them. No test seams exist inside UI: Compose screens and activities are verified by manual device smoke, not by seam fakes.

Status: accepted

## Considered Options

- Per-screen ViewModel/UI tests: rejected — they would pin incidental composition behavior and duplicate coverage the seams already own, at the cost of dragging Android types into the test surface.
- A full `androidTest` suite as primary verification: rejected — slow, flaky, and it tests plumbing rather than decisions; manual smoke on a real device covers the interception plumbing that cannot be unit-tested.
