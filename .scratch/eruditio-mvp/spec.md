# Eruditio — MVP Spec

Status: ready-for-agent
Feature: eruditio-mvp
Sources: `research/replace-scrolling.md`, `research/open-content-sources.md` (cited facts below trace to these)

## Problem Statement

The user reaches for their phone out of habit, opens a feed, and loses 20 minutes to doomscrolling. Their trained attention window is short (~47 s average before self-interruption), their app sessions produce nothing they can point to, and the existing alternatives fail them in one of two ways: blockers only subtract (friction with no payoff), while "learn something" apps are ad-funded data harvesters or feeds of summaries that replicate the slot-machine mechanics they claim to replace. There is no product that intercepts the scroll moment *and* pays the user back with verifiable learning progress — privately, with no ads, no accounts, and no monthly billing.

## Solution

**Eruditio** is an Android app that stands between the user and their most-opened scroll apps. When a target app opens, Eruditio shows a brief friction screen — a short delay, a breathing animation, and an explicit choice: continue into the app, or take a learning session instead. Choosing the session opens a quiet, offline reading surface (public-domain classics bundled in the app), and every completed session feeds an on-device spaced-repetition review schedule with Khan-style mastery levels. Progress is real and visible: what you've read, what you've mastered, what's due for review. The app costs once; new content tracks arrive later as optional packs. No trackers, no accounts, no data leaves the device — the privacy posture is the architecture, not a policy page.

## User Stories

1. As a distracted user, I want my phone to show a brief pause screen when I open a chosen scroll app, so that my autopilot opening is interrupted.
2. As a distracted user, I want the pause screen to offer a breathing moment and an explicit "continue" option, so that I can still reach the app when I genuinely intend to — never a hard block.
3. As a distracted user, I want the pause screen to offer a learning session as the alternative, so that the interruption converts into something valuable instead of guilt.
4. As a user who slipped and opened the feed anyway, I want that choice recorded, so that my progress reflects reality rather than a flattering fiction.
5. As a learner, I want to choose which apps get intercepted, so that the friction applies to my actual time-sinks, not everything.
6. As a new user, I want sensible defaults for the target-app list (top social apps), so that the app works usefully on day one without configuration effort.
7. As a learner, I want a bundled library of classic texts available offline, so that my reading session never depends on network or external services.
8. As a reader, I want a quiet, distraction-free reading surface with no feed adjacency, so that deep reading survives on my phone.
9. As a reader, I want to pick up where I left off in any text, so that a book spans many short sessions instead of one marathon.
10. As a learner, I want a session to be short by default (one reading segment or one review set), so that it fits the attention window I actually have.
11. As a learner, I want reading sessions to generate review items, so that what I read gets tested rather than merely seen.
12. As a learner, I want spaced-repetition reviews scheduled on-device, so that my retention grows without any server knowing about it.
13. As a learner, I want concepts to move through mastery levels (familiar → proficient → mastered), so that I can see real progress rather than activity metrics.
14. As a learner, I want previously-learned material to resurface in review challenges and decay if neglected, so that mastery stays honest.
15. As a habit-builder, I want a daily streak for completing sessions, so that returning daily becomes automatic — while the streak stays clearly separate from mastery.
16. As a habit-builder, I want rewards tied to completing sessions rather than opening the app, so that the app never replicates the reward-on-arrival mechanics I'm escaping.
17. As a learner, I want a check-in prompt after a session, so that progress registers as something I did, not time I spent.
18. As a learner, I want a progress view showing texts read, concepts mastered, and reviews due today, so that "am I actually getting anywhere" has an honest answer.
19. As a learner, I want the app to reward a session with a suggested next step (a handoff link to a Khan course or Exercism track), so that momentum continues beyond the session.
20. As a curious learner, I want handoff links to the best existing tools (Khan, Exercism), so that Eruditio amplifies them instead of duplicating them badly.
21. As a privacy-conscious user, I want no account requirement, so that using the app creates no identity trail.
22. As a privacy-conscious user, I want all progress stored on-device, so that no one — including the app's developer — can profile my habits or learning.
23. As a privacy-conscious user, I want the app to contain zero third-party trackers or ad SDKs, so that "privacy-focused" is verifiable, not a claim.
24. As a buyer, I want to pay once and own the core app, so that I'm never asked monthly for friction that already benefits me.
25. As a buyer, I want future content tracks (explainers, languages, more classics) offered as optional packs, so that I pay exactly for what I choose to add.
26. As an offline-first user, I want everything except optional purchases to work in airplane mode, so that the app never assumes connectivity.
27. As a returning user, I want the app to surface today's due reviews on open, so that "what should I do now" is always answered in one tap.
28. As a parent-of-my-own-habits, I want my weekly openings-intercepted count visible, so that I can see the friction working in my own numbers.
29. As a skeptical user, I want the app to claim only what the evidence supports (friction reduces openings; review improves retention), so that I'm never marketed an inflated promise.
30. As a user who sometimes needs the escape hatch, I want the interception to include an accessibility-respectful, non-punishing tone, so that the app feels like a doorstop, not a warden.

## Implementation Decisions

- **Native Android, Kotlin** — the interception mechanic requires OS hooks; iOS waits on the Family Controls (Distribution) entitlement and is a separate future effort.
- **Interception via UsageStatsManager-based foreground detection; AccessibilityService is prohibited** by design (Google Play policy restricts accessibility-based automation; a safe architecture avoids it entirely).
- **Friction pattern (verified, PNAS 2023):** ~10 s delay + breathing animation + explicit "Continue to app" option on interception. Every interception records an outcome: *continued* or *replaced*. The user can always go straight to the app; hard-blocking is prohibited by design.
- **Two test seams:** `SessionController` (OS event/decision/session lifecycle — friction, check-ins, handoff shelf hang here; the UsageStats listener and overlay are replaceable details behind it) and `ReviewScheduler` (pure logic holding the mastery ledger; no Android dependencies). No test seams inside UI.
- **On-device SRS:** SM-2-family scheduler in the `ReviewScheduler` seam; mastery states (familiar/proficient/mastered) with decay and a mastery-challenge mode surfacing learned material; all state in a local database.
- **Session shapes start short** (one reading segment or one review card set) and grow only through user pacing — no autoplayed adjacency.
- **Content:** 30 public-domain texts bundled in the app package (Plato translations, Marcus Aurelius, KJV selections, Spurgeon sermons, Augustine, missionary biographies; sources: Project Gutenberg — bundle copies, strip PG branding per its own developer guidance — and Standard Ebooks CC0). Reader renders plain text with position memory and per-text progress.
- **Streak mechanics exist but are demoted:** they reward completing sessions, are visually secondary, and never masquerade as mastery. Engagement metrics and mastery metrics are stored and displayed separately.
- **Handoff shelf:** a static registry of stable deep links (Khan course URLs, Exercism tracks) shown post-session. If it grows beyond a trivial list of links, it is cut from MVP.
- **Monetization:** Google Play Billing one-time products — a core unlock plus optional future content packs. No subscriptions, no ads, no attribution/analytics SDKs of any kind (the Exodus census of incumbents shows 6–13 trackers; this app ships zero).
- **No accounts, no sync** in MVP; all progress local. (Backup/export is a future consideration, out of scope here.)
- **Package id** is finalized at first Play Store publish; display name **Eruditio** (USPTO check pending — human task before publishing).
- **Copy claims discipline:** effectiveness copy limited to RCT-supported statements (friction reduces openings; review-based progress improves retention). Lumosity's FTC settlement is the standing warning.
- **Standing design-review question** for every future surface: *does any surface here replicate the variable-ratio mechanics we're replacing?*

## Testing Decisions

- A good test asserts **external behavior through the two seams**, never implementation details: what a consumer (the UI, the OS) observes — decisions, recorded outcomes, due schedules, mastery transitions.
- **`ReviewScheduler` (deepest tests, pure logic):** sequences of completion events → due-review sets; mastery level transitions and decay over time; mastery-challenge selection (learned-but-aging concepts first); boundary cases (empty ledger, missed days, re-mastering after decay). Deterministic, table-driven.
- **`SessionController` (decision logic):** target-app opening with friction enabled → interception decision; user chooses continue → outcome recorded as *continued*; user chooses replacement → session starts and completion is recorded; friction disabled → pass-through; handoff shelf appears only post-session. OS watcher and overlay are fakes at the seam.
- **Manual smoke on a real device** for the full loop: open a real social app → friction screen appears → both paths work → outcome recorded. The interception plumbing (UsageStats permission grant, overlay over other apps) cannot be unit-tested and must be smoke-verified per release.
- No prior art exists (greenfield); the seam tests above become the codebase's first test conventions.

## Out of Scope

- Everything beyond the classics track: explainers, languages (Russian/German/Spanish/Hebrew), communication drills, coding principles, course-sequencing layer — roadmap in that order, as future content packs/features.
- Schoolwork-first mode and any LMS/Canvas integration (deferred; revival design: Canvas ICS calendar-feed import first, then an admin-issued OAuth developer key — facts in `research/open-content-sources.md` and the session record).
- iOS version (blocked on Apple entitlement review).
- Accounts, cloud sync, backup/export.
- Community features, leaderboards, social accountability.
- Any claim of clinical or cognitive-training efficacy.

## Further Notes

- The one-sec PNAS study is the strongest causal evidence in the category (−57% target-app openings over 6 weeks, N=280); the friction mechanic is a measured decision, not a taste choice.
- "1% better each day" is motivational framing (Clear's compounding arithmetic), not an effect size; the product's honest engine is progress monitoring + spaced practice, both meta-analytically supported.
- Hebrew is the thinnest of the four roadmap languages (no FSI/Peace Corps course; DLI GLOSS unverified) — flagged now so the future language pack plans for it.
- Project Gutenberg permits commercial redistribution of its texts but charges royalties if trading on the PG name — bundled texts ship with PG branding stripped; Standard Ebooks (CC0) is the preferred source where editions exist.
- Podcast RSS playback is an established pattern, not settled law — only relevant post-MVP (audio track), and always from the publisher's own feed.
