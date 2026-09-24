# Interception via UsageStatsManager; AccessibilityService prohibited

Interception needs a signal for "a chosen app just reached the foreground" plus the ability to show a screen over it. AccessibilityService can deliver both, but Google Play policy restricts accessibility-based automation and rejects apps whose accessibility use is incidental to their main purpose. Eruditio uses UsageStatsManager foreground-event detection (a polling service over recent UsageEvents) with an overlay activity, and AccessibilityService is prohibited by design — no safe variant exists, and the Play-policy risk compounds the privacy posture the product already commits to.

Status: accepted

## Consequences

- Requires the usage-access permission (a settings hand-off, never silent) and the draw-over-apps permission for the pause screen.
- Foreground detection is poll-based (1 Hz), not event-pushed — a momentary race is accepted rather than reach for accessibility.
