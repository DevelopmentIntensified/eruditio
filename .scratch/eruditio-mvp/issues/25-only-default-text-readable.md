# 25: Only the bundled default text is readable

**Report:** Owner (2026-10-31): "we need to fix the fact that you can't read any book but the default." Everything was confirmed pushed to the device earlier that day, so the bug is on the shipped build too.

**Ruled out statically (2026-10-31 agent):** library lists all 30 bundled texts (`assets/texts/index.json`); each row calls `vm.startReading(text.id)`; `SessionController.openSession` starts a session on exactly that textId at that text's own stored position; `ReaderActivity` loads `library.text(record.textId)` by id; reading positions are per-text rows (Room v3, PK `textId`). No global pin in the data path.

**Candidate causes remaining:** device-side runtime behavior not yet reproduced (crash / stuck "Loading…"; session started but reader navigates away via gleaning gating; restore offset beyond the new text's end; billing-gate edge). Repro on emulator-5554 is step one.

**Repro recipe:** current debug build → emulator-5554 → `app.eruditio` → Library tab → tap any row that is not the current/default text → observe reader.

**Status:** ready-for-agent

- [ ] Reproduced on emulator-5554 with the current build
- [ ] Root cause identified and fixed
- [ ] Non-default text: open → reader shows that text → resume at stored position → check-in works
- [ ] Default-text flow unregressed (unit suite green after change)

## Comments

**2026-10-31 — agent**: Filed from owner direction (session item 3). Agent claimed diagnosis in-flight; repro + fix evidence gets appended below.

**2026-10-31 — agent — device evidence (emulator medium_phone/5554, fresh `pm clear` + `install` per journey):**
- Static: data path is per-text everywhere (`index.json` has 30 unique id→file pairs; `TextLibrary.text/requireMeta`; `ReadingPositions` PK `textId`; `RoomSessionLog.start` inserts the row's own textId).
- Debug build @ HEAD (`9fa1d4b` + uncommitted watcher edits): Today shelf rows and Library rows open **the tapped text** and create the matching session in Room — verified crito→crito, euthyphro-band→crito (layout had shifted), meno→meno from Library, apology→apology. Reader header and body match the session's text every time.
- Release build @ HEAD (unsigned, debug-key signed, fresh install): full journey Start → Check in → Notice → Compare → Keep (three-movement gleaning) works.

**Findings — the two concrete "can't read" states that DO exist:**
1. **Trial-exhausted release install is a silent dead end at the tap points.** After 3 completed sessions (`TrialGate.FREE_SESSION_LIMIT`), every `startReading` hits `SessionStart.Locked` → `refresh()` (do-nothing). The trial card renders at the *bottom* of Today correctly (verified reactively), but the BEGIN/NEW SESSION card at the top still shows an active "Start reading →" and Library/shelf rows stay tappable — they simply don't react. That is exactly "you can't read any book" from the user's seat. Minimal no-design fix: under `gateLocked`, disable/idle the begin CTA and shelf rows (label stays, affordance dies), so the state is honest at the tap point rather than only at the bottom card.
2. **The pause screen ("Read a page instead") can only ever open the last-read text** (`resumeTextId() ?: texts.first()`; no picker). Every interception loop pass = the same book. Interim "handoff shelf" behavior per parked ticket 08, but it matches "you can't read any book but the default" through the core loop surface. Product decision needed (picker on pause vs. keep interim).

**Status:** needs-info — no defect found in the row→text path at HEAD on either build; owner please state WHERE the failure appeared (Today shelf / Library tab / pause screen "Read a page instead") and at which stage (fresh install, mid-trial, or after N sessions). Candidates above are ready to fix once the owner's flow is named.
