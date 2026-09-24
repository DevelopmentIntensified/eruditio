# Release build & smoke (ticket 11)

## Build

Debug (development; billing gate bypassed):

    ./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/app-debug.apk

> Gradle 8.14's embedded Kotlin script compiler rejects Java 25. If your machine's
> only JDK is 25+, install a JDK 21 and point `org.gradle.java.home` at it in
> `~/.gradle/gradle.properties` (user-level, not committed).

Release (signed; keystore/eruditio-release.jks + keystore.properties, both gitignored):

    ./gradlew :app:assembleRelease        # app/build/outputs/apk/release/app-release.apk

Unit tests (both seams):

    ./gradlew :app:testDebugUnitTest

## Manual smoke on a real device (per release, cannot be unit-tested)

1. Install the release APK.
2. Home → Interception card → grant **usage access** and **overlay** permissions.
3. Open Instagram (or any chosen target app): the pause screen appears over it.
4. Wait out the 10 s breathing delay.
5. **Continue** → target app opens; outcome recorded as continued.
6. Open target again → **Read a page instead** → reader opens at the last read position.
7. Read, then **Check in — I read to here** → completion recorded; review cards arrive next day; handoff suggestion appears.
8. Tomorrow: Home shows due reviews; grade one.
9. Progress: texts read, concepts by mastery, weekly intercepted count matches.

## Zero-tracker claim

The release build contains only androidx + kotlinx + Play Billing. Verify (expect no hits):

    ./gradlew :app:dependencies --configuration releaseRuntimeClasspath \
      | grep -iE "analytics|crashlytics|admob|facebook|firebase|branch|mixpanel|amplitude|segment|sentry|appsflyer" \
      | grep -viE "firebase-encoders"

`com.google.firebase:firebase-encoders` (and `-json`, `-proto`) is not a tracker: it is
Play Billing's own serialization codegen dependency and bundles no Firebase services. Billing's own telemetry stays within Google Play services (documented in the store listing).

## Publish checklist

- [ ] USPTO trademark check for "Eruditio" recorded (human task)
- [ ] Package id finalized in app/build.gradle.kts (human decision; `app.eruditio` is a placeholder)
- [ ] Signed release APK installed from Play internal track
- [ ] Final manual smoke passes (above)
- [ ] Store listing finalized (docs/store/listing.md)
- [ ] F-Droid metadata reviewed (app/fastlane/metadata/android/)
