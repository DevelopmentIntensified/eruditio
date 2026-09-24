# All progress on-device; no accounts, sync, or backup in MVP

Eruditio's promise is that no one — including its developer — can profile the user's habits or learning. That promise is only credible if it is structural: every interception record, session, and mastery ledger entry lives in a local database, with no account creation, no cloud sync, and no backup/export in the MVP. Privacy posture here is architecture, not a settings toggle, so removing it later would be a product repositioning rather than a cleanup.

Status: accepted

## Consequences

- Uninstalling erases all progress; this is accepted rather than papered over with silent sync.
- Backup/export is a future consideration and out of scope here.
- No analytics or attribution SDKs of any kind — zero third-party trackers.
