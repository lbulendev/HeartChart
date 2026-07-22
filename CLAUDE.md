# HeartChart

Live heart-rate charting for BLE chest straps (Polar H9), two apps sharing
one design: `iOS/HeartChart/` (Swift 6, SwiftUI, Swift Charts,
CoreBluetooth) and `android/` (Kotlin/Compose port; replaced the legacy
2018 Java app, which lives on in git history). Global mobile conventions
in ~/.claude/CLAUDE.md apply; specifics below.

## Build & test

iOS — deployment target is iOS 27, so tests need the iOS 27.0 simulator:

```sh
cd iOS/HeartChart && xcodebuild test -project HeartChart.xcodeproj \
  -scheme HeartChart -destination "id=556B0C84-C791-4E30-8810-61AF9AEBB917" \
  -only-testing:HeartChartTests
# Smoke only (CLI): -only-testing:HeartChartTests/SmokeTests
```

Android (no system Java — use the Studio JBR; compileSdk 37 because the
template's androidx versions require it, minSdk 26):

```sh
cd android && JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:testDebugUnitTest          # FullTests
  # -PincludeTags=smoke                     # SmokeTests plan (TestPlans.md)
```

## Secrets — NO publicly available API keys (iOS and Android)

- The current apps need NO API keys — BLE is the only data source. Keep it
  that way unless a feature genuinely requires one.
- INCIDENT (July 2026): Google flagged a publicly accessible API key for
  GCP project heartchart-ios, hardcoded as `GoogleOauthKey` in the legacy
  app's `ios-Swift/HeartChart-iOS/.../Misc/Constants.swift` — deleted from
  the working tree but permanently in the public repo's git history. That
  key must stay revoked in Google Cloud Console; never re-add it or any
  hardcoded key.
- If a key ever becomes necessary, follow the TheMovieDBSwift pattern
  exactly: the key enters as an environment variable — iOS via git-ignored
  `Secrets.xcconfig` (`SETTING = $(ENV_VAR)`) → Info.plist → Bundle;
  Android via git-ignored `local.properties` with an env-var fallback →
  BuildConfig — and the app fails fast at launch when it's missing.

## Project notes

- Modern pbxproj with synchronized folder groups — moving/adding files on
  disk is enough, no project edits. Template sets
  `SWIFT_DEFAULT_ACTOR_ISOLATION = MainActor`; pure models/parsers must be
  explicitly `nonisolated` or the test target can't call them.
- BLE is standard GATT (service 180D, characteristic 2A37) — works with any
  brand strap. The resolved PolarBleSdk package is intentionally UNUSED; it
  only earns its place for H10-exclusive features (raw ECG, ACC — the H9 has
  none). H9 R-R intervals arrive in 2A37 (flag bit 4, 1/1024s units) and are
  currently discarded — the parser tolerates them; an HRV feature would
  parse them.
- Zone values come from the published 12-row age table: youth RANGES
  (6-12→70-110/220, 13-19→60-100/220) plus single-age adult rows
  (20→100-170/200 … 70→75-128/150). Lookup: a containing range wins;
  otherwise nearest bracket by distance to its range — a 27-year-old gets
  the 30-year row; ties snap to the younger bracket; ages outside the
  table clamp to the end rows. No formula, no interpolation. The full
  table, boundaries, and snapping are pinned by regression tests on BOTH
  platforms — keep the two `brackets` tables identical. validAges is
  6...100.
- Connection lifecycle goes through internal `handleConnected` /
  `handleDisconnection` / `handleConnectionFailure` on HeartRateMonitor so
  banner behavior is testable without CoreBluetooth objects. Unpair and
  fresh pairing must never raise the error banner.
- Samples are in-memory only (3-min rolling window); age + paired sensor id
  in UserDefaults. SwiftData is the planned vehicle for workout-session
  history (record/stop → batch insert), not for live samples.
- Simulator has no Bluetooth: on-device testing needs a real iPhone + worn
  strap (H9 only advertises while worn, electrodes moistened). Same for the
  Android emulator — a real phone is required for live data.
- Android mirrors the iOS design: `SensorTransport` is the testable seam
  (GattSensorTransport = production, FakeSensorTransport = tests) with the
  same internal handleConnected/handleDisconnection/handleConnectionFailure
  contract; the chart is Compose Canvas (no 3rd-party chart lib); settings
  (age + paired address) live in Preferences DataStore; monitor is
  app-scoped in HeartChartApplication so BLE survives rotation. Parser
  regression suite includes the Kotlin-specific signed-Byte case (readings
  above 127 must decode unsigned).
