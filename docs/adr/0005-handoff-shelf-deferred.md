# Handoff shelf deferred post-MVP; interim in-app "Read next" ships

The spec's handoff shelf (static registry of stable deep links — Khan courses, Exercism tracks — shown post-session only) was implemented, then the owner directed it off the completed screen: the external redirect felt like being pushed out of the learning path. The redesign is deferred post-MVP. Interim: the post-session step is an in-app "Read next" suggestion (same content group, unread texts first). The `HandoffShelf` object and its seam tests remain intentionally — the redesign builds on them.

Status: accepted

## Consequences

Future architecture reviews must NOT re-suggest rendering the shelf or re-adding external handoff links to the completed session screen; the removal is an owner decision, not dead code to cut. The ticket stays needs-triage until a later design pass.
