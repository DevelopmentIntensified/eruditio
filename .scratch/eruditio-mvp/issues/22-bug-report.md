# 22: In-app bug report (privacy-first)

**What to build:** The user can report a bug from inside the app. "Report a bug" in Settings opens a small composer where the user writes the problem in their own words; sending hands the report to the device's mail client with app version + Android version pre-filled. Nothing is collected, logged, or sent automatically — nothing leaves the device until the user chooses to send. The privacy posture is the architecture (spec), so this ships with zero new dependencies or SDKs: no trackers, no analytics, no crash-reporting service.

**Owner preference (AGENTS.md):** prototype-first — the report surface is a UI decision, so prototype it and let the owner pick before production implementation (see tickets 16→17 for the pattern).

**Blocked by:** None (can start immediately).

**Status:** ready-for-human

- [x] Prototype of the report surface exists; owner picks from rendered variants
- [x] "Report a bug" reachable in Settings; opens a composer with version info pre-filled
- [x] Sending opens the user's mail client with the report (user-written text + versions only)
- [x] Composing works offline; no data collected or sent automatically
- [x] Zero new dependencies/SDKs; no automatic capture (no logs/dumps attached without user action)

## Comments

**2026-09-24 — agent**: Prototype built: `prototype/bug-report.html` (variants A sheet / B quiet page / C inline card; floating variant bar, ✓ pick, URL state, dark toggle; smoke-tested in browser — all three flows render, zero console errors).
**2026-09-24 — owner (via implement session)**: "with a" — **variant A, bottom sheet over Settings**. Verdict recorded; implementing A on this branch.
**2026-09-24 — agent**: Shipped on `t22-bug-report` (commit 3326062), merged to `eruditio-mvp-2` (724be12), seam tests green post-merge (36 tests, 0 failures). Only file: `App.kt` (+114). About card in `SettingsScreen` ("Report a bug" in tertiary gold opens the sheet; "Version" row shows `Eruditio ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})`); `BugReportSheet`: `ModalBottomSheet` (tap-outside dismiss), multiline `TextField` (prototype placeholder), read-only version line incl. `Android ${Build.VERSION.RELEASE} (API ${SDK_INT})`, Discard drops the draft, Send disabled when blank. Send = `ACTION_SENDTO mailto:bugs@eruditio.app` + subject + body (user text + version lines) via `createChooser` from the activity context; `ActivityNotFoundException` → Toast, no crash. Privacy: no new deps, no network, no logging, no automatic capture — the only egress is the user-tapped Send. Offline composing is trivially satisfied (TextField + Intent only). Human step: device glance (sheet opens from Settings, Send hands off to mail client).
