# HeartChart

Live heart-rate charting for Bluetooth LE chest straps (built against the Polar H9, works with any standard strap). Originally written in 2018 as a pair of sample apps around a vendor Obj-C library; rewritten in 2026 as modern native apps to demonstrate current platform practices. The repo hosts two apps sharing one design:

- **`iOS/`** — Swift 6 / SwiftUI / Swift Charts / CoreBluetooth
- **`android/`** — Kotlin / Jetpack Compose / Android BLE, mirroring the iOS architecture and test strategy (the 2018 Java app it replaced lives on in git history)

## Features

- **Live heart rate** — big BPM readout plus a chart of the last 3 minutes, streaming from the sensor about once a second
- **Age-based zone lines** — enter your age (prompted on first launch, editable any time) and the chart draws two solid red lines at the target zone bounds and a dashed red line at estimated max, using the standard formula: max = 220 − age, zone = 50–85% of max (45y → 88–149, max 175)
- **Pair / unpair** — scan for nearby heart-rate sensors, tap to pair, and the app remembers the sensor across launches; unpair forgets it. Reconnection is automatic when the strap comes back in range
- **Honest failure UX** — a lost connection or failed connect raises a red banner with a friendly message and a Retry button; the banner persists until reconnection succeeds. Raw error text never reaches the screen
- **Localization scaffolding** — every user-facing string is externalized under a snake_case keyspace shared by both platforms, with placeholder locales for en, en-US, es-US, en-CA, and fr-CA

## Architecture

Both apps follow the same shape:

- **Standard GATT, no vendor SDK** — the sensor layer speaks the Bluetooth Heart Rate service (0x180D) and Heart Rate Measurement characteristic (0x2A37) directly, so any brand of strap works. The measurement parser handles both 8-bit and 16-bit formats, tolerates RR-interval and energy-expended fields, and returns nil/null on truncated payloads instead of crashing
- **A testable monitor** — connection state, pairing, the sample window, and banner rules live in a state machine unit-tested without any Bluetooth. iOS exposes internal lifecycle handlers on the `@MainActor @Observable` monitor; Android puts GATT behind a `SensorTransport` interface with a fake for tests
- **First-party charting** — Swift Charts on iOS; a Compose `Canvas` on Android. No third-party chart dependencies
- **Right-sized persistence** — samples are in-memory only (rolling window); age and the paired sensor id persist via `@AppStorage`/UserDefaults on iOS and Preferences DataStore on Android
- **Platform-current stacks** — iOS: Swift 6 language mode with default MainActor isolation, `@Observable` + `@Environment`, `@preconcurrency` CoreBluetooth delegate conformance, synchronized folder groups. Android: Kotlin DSL + version catalog, Compose + Material 3, StateFlow, app-scoped monitor so the BLE connection survives rotation, runtime Bluetooth permissions

```
iOS/HeartChart/HeartChart/          android/app/src/main/java/...
├── App/                            ├── HeartChartApplication, MainActivity
├── Bluetooth/  HeartRateMonitor    ├── bluetooth/  SensorTransport, Gatt impl, monitor
├── Models/     HeartRateZones      ├── data/       HeartRateZones, SettingsStore
└── Views/      chart, age, pairing └── ui/         chart, age, pairing, banner
```

## Requirements

**Both platforms**
- A Bluetooth LE heart-rate strap (Polar H9/H10, Garmin, Wahoo, …) and a physical device — neither the iOS simulator nor the Android emulator has Bluetooth

**iOS (`iOS/`) — Xcode / Swift**
- Xcode 27, iOS 27 deployment target

**Android (`android/`) — Android Studio / Kotlin**
- Android Studio (AGP 9.3, Kotlin 2.2, Gradle 9.5 via the wrapper), JDK 21 (the Studio-bundled runtime works)
- Android SDK platform 37 (minSdk 26 / target 36)

No API keys or accounts — the sensor is the only data source.

## Running it

Build to a phone, wear the strap (moisten the electrodes — the Polar H9 only advertises while worn), tap **Pair**, and select the sensor. Enter your age when prompted and the zone lines appear. iOS asks for Bluetooth permission on first connect; Android requests the runtime Bluetooth permissions when the pairing sheet opens.

## Tests

Both apps carry the same tagged taxonomy — **smoke** (critical path), **sanity** (contracts/scaffolding), **regression** (pinned edge cases, each documenting the failure it prevents) — with area tags (`zones`, `parsing`, `pairing`) for cross-cutting filters.

- **iOS** — Swift Testing suites plus committed test plans: `FullTests` (default, with coverage) and `SmokeTests` (smoke tag). Coverage includes the published zone table, both measurement formats, truncated payloads, pairing persistence, banner raise/clear rules, and window trimming
- **Android** — JUnit 5 mirrors of the same tests through the fake transport seam (run `./gradlew :app:testDebugUnitTest`, or `-PincludeTags=smoke`; see `android/TestPlans.md`), plus one Android-only pin: 8-bit readings above 127 must decode unsigned past Kotlin's signed `Byte`

## Future work

- **R-R intervals / HRV** — the H9 already sends beat-to-beat intervals in every notification (flag bit 4); the parsers tolerate and discard them today. Surfacing them is the natural next feature
- **Workout sessions** — record/stop with SwiftData (iOS) and Room (Android) for session history, time-in-zone stats, and re-charting past workouts
- **Polar H10 extras** — raw ECG and accelerometer streams via Polar's PMD service; the official PolarBleSdk package is already resolved in the iOS project, intentionally unused until an H10 is in hand
