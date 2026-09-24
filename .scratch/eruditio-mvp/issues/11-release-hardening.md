# 11: Release hardening and publish

**What to build:** Eruditio 1.0 reaching users. App icon, store listing, signed release build, a final manual smoke of the full interception loop on a real device, then publish to Google Play (internal track first) with F-Droid metadata prepared. Human prerequisites recorded before publishing: the USPTO trademark check for the name Eruditio, and finalizing the package id.

**Blocked by:** 05 (Complete the 30-text library), 08 (Handoff shelf), 09 (Progress view and streak layer), 10 (One-time billing).

**Status:** ready-for-human

- [ ] Signed release build installs from the Play internal track
- [ ] Final manual smoke of the full loop passes on a real device (pause → replace → read → review → progress)
- [ ] Store listing and icon complete; zero-tracker claim verified against the release build
- [ ] F-Droid metadata prepared
- [ ] USPTO trademark check for "Eruditio" recorded as done; package id finalized

**2026-09-24 — agent**: Signed release build assembles and verifies (apksigner: CN=Eruditio cert). Zero-tracker dependency check passes; doc refined to exclude firebase-encoders (Play Billing's own serialization dep, not a tracker). Store listing docs + fastlane metadata exist. Gradle now documents the JDK 21 requirement (Gradle 8.14 rejects Java 25; machine pin lives in user-level gradle.properties). Human checklist unchanged: USPTO check, package id finalization, Play internal track, final device smoke.
