# Eruditio

An Android app that intercepts habitual scrolling and repays the moment with real reading and on-device spaced-repetition review — privately, with no ads, no accounts, and no monthly billing.

## Language

### Interception

**Interception**:
The moment a target app reaches the foreground and Eruditio shows the pause screen over it.
_Avoid_: Block (hard-blocking is prohibited by design)

**Target app**:
A user-chosen scroll app that gets intercepted; sensible defaults are present on first run.
_Avoid_: Blocked app, banned app

**Pause screen**:
The friction surface shown over a target app: a short delay, a breathing mark, then an explicit choice. It never hard-blocks.
_Avoid_: Friction screen, block screen, overlay

**Continue**:
The outcome of going straight into the target app from the pause screen. Always recorded.
_Avoid_: Skip, dismiss

**Replace**:
The outcome of taking a learning session instead of the target app. Always recorded.
_Avoid_: Substitute, swap

### Reading

**Session**:
One short reading engagement in a bundled text, spanning from a resume position to a completion point.

**Gleaning**:
The learning output from a session: the reader's own takeaway paired with source context, with curated interpretation added when the content provides it.
_Avoid_: Review item, bare summary, bare highlight

**Segment**:
A fixed-length reading window of a text; sessions complete within segments, and gleanings derive from completed segment ranges. Review items require curated memorizable content.
_Avoid_: Chapter, section

**Checkpoint**:
A marked pause point within a text where a session may be completed; the resume position is the checkpoint line itself.
_Avoid_: Bookmark, save point

**Resume anchor**:
Where a text reopens after being left: the first visible line IS the place — no interpretation or adjustment.
_Avoid_: Scroll position, last-read position

**Margin rail**:
The togglable reading-position rail beside the text; a habit aid, off by default.
_Avoid_: Progress bar, scrubber

**Plate**:
A sourced map frontispiece prepended to ancient-track texts; a single image item, not part of the text flow.
_Avoid_: Cover, illustration

**Content group**:
The one grouping of bundled texts (ancient, christian, biography) driving the library segment bar, plates, and suggested next steps.
_Avoid_: Category, track, genre

### Review

**Review item**:
A deliberately memorizable idea drawn from curated content and placed on the spaced-repetition ledger. Passive reading segments are not review items.
_Avoid_: Arbitrary passage excerpt, bare cue, self-grade-only card

**Mastery**:
The three-level progress of a review item: familiar → proficient → mastered.
_Avoid_: XP, level, score

**Decay**:
Mastery level dropping at query time when a concept is neglected past its interval; re-grading well restores it.

**Mastery challenge**:
The resurfacing of learned-but-aging material when nothing is due, so mastery stays honest.
_Avoid_: Bonus round, quiz

### Progress

**Completion**:
A reading session finished at a segment boundary — the moment progress registers.
_Avoid_: Session end, finish

**Check-in**:
The user's prompt ("I read to here") that records a completion.
_Avoid_: Attendance, log entry

**Streak**:
Consecutive days with at least one completion — a habit metric, stored and displayed separately from mastery.
_Avoid_: Score, rating

**Trial**:
The free-session allowance before the one-time purchase; the core loop gates at its end.
_Avoid_: Free tier, demo
