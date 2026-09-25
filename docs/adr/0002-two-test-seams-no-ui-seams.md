# Two test seams hold the entire test surface; none inside UI

The testable logic concentrates in two pure seams — `SessionController` (OS event/decision/session lifecycle: friction decisions, outcome recording, replace→session, check-ins) and `ReviewScheduler` (the mastery ledger and due/challenge logic). Both are Android-free and the OS watcher, overlay, and UI are replaceable details behind them. No test seams exist inside UI: Compose screens and activities are verified by manual device smoke, not by seam fakes.

Amendment (deepening 1): the two seams remain the external contract consumed by the OS and UI layers. Pure core modules and their store ports (e.g. `PositionStore`) count as the core's test surface with internal seams — faked at the core boundary without dragging Android types in. Pure predicate modules living under `ui/` but carrying no Android/Compose imports (e.g. `LibrarySelection`) count as the core's JVM-tested surface too: the "no test seams inside UI" rule targets UI surfaces, not pure files.

Amendment (gleaning, ticket 23): a use-case coordinator is a third kind of internal seam. `GleaningCoordinator` composes existing ports (`SessionController`, `GleaningStore`, text assets) to implement the durable hand-off rule from ADR-0006 — it adds no Android or Compose type, is injected through `AppContainer`, and is exercised through those same ports. The test surface therefore widens by composing existing fakes, not by adding a new externally-consumed contract; `SessionController` and `ReviewScheduler` remain the only seams the OS and UI layers bind to.

Status: accepted

## Considered Options

- Per-screen ViewModel/UI tests: rejected — they would pin incidental composition behavior and duplicate coverage the seams already own, at the cost of dragging Android types into the test surface.
- A full `androidTest` suite as primary verification: rejected — slow, flaky, and it tests plumbing rather than decisions; manual smoke on a real device covers the interception plumbing that cannot be unit-tested.
