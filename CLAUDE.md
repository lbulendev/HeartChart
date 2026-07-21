# HeartChart

Live heart-rate charting for BLE chest straps (Polar H9). Active app is
`iOS/HeartChart/` (Swift 6, SwiftUI, Swift Charts, CoreBluetooth); the
`android/` folder is a legacy 2018 Java app, untouched. Global mobile
conventions in ~/.claude/CLAUDE.md apply; specifics below.

## Build & test

Deployment target is iOS 27, so tests need the iOS 27.0 simulator:

```sh
cd iOS/HeartChart && xcodebuild test -project HeartChart.xcodeproj \
  -scheme HeartChart -destination "id=556B0C84-C791-4E30-8810-61AF9AEBB917" \
  -only-testing:HeartChartTests
# Smoke only (CLI): -only-testing:HeartChartTests/SmokeTests
```

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
- Zone math: max = 220 − age; target zone 50–85% of max, `.rounded()`
  (half-up). The published table (45→88/149/175, 50→85/145/170,
  55→83/140/165) is pinned by regression tests — don't change the formula
  without updating the table test.
- Connection lifecycle goes through internal `handleConnected` /
  `handleDisconnection` / `handleConnectionFailure` on HeartRateMonitor so
  banner behavior is testable without CoreBluetooth objects. Unpair and
  fresh pairing must never raise the error banner.
- Samples are in-memory only (3-min rolling window); age + paired sensor id
  in UserDefaults. SwiftData is the planned vehicle for workout-session
  history (record/stop → batch insert), not for live samples.
- Simulator has no Bluetooth: on-device testing needs a real iPhone + worn
  strap (H9 only advertises while worn, electrodes moistened).
