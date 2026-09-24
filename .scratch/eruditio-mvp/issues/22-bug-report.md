# 22: In-app bug report (privacy-first)

**What to build:** The user can report a bug from inside the app. "Report a bug" in Settings opens a small composer where the user writes the problem in their own words; sending hands the report to the device's mail client with app version + Android version pre-filled. Nothing is collected, logged, or sent automatically — nothing leaves the device until the user chooses to send. The privacy posture is the architecture (spec), so this ships with zero new dependencies or SDKs: no trackers, no analytics, no crash-reporting service.

**Owner preference (AGENTS.md):** prototype-first — the report surface is a UI decision, so prototype it and let the owner pick before production implementation (see tickets 16→17 for the pattern).

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] Prototype of the report surface exists; owner picks from rendered variants
- [ ] "Report a bug" reachable in Settings; opens a composer with version info pre-filled
- [ ] Sending opens the user's mail client with the report (user-written text + versions only)
- [ ] Composing works offline; no data collected or sent automatically
- [ ] Zero new dependencies/SDKs; no automatic capture (no logs/dumps attached without user action)
