# Test Plans

Mirrors the iOS test taxonomy (`iOS/HeartChart/TestPlans/*.xctestplan`) and
the TheMovieDBSwift Android setup. Every unit test carries one **purpose**
tag (via its suite class) and one **area** tag (via its nested class).

## Tags

| Purpose tag  | Meaning                                                     |
|--------------|-------------------------------------------------------------|
| `smoke`      | Critical-path checks. If any fail, stop and fix first.      |
| `sanity`     | Contract checks on helpers, defaults, and persistence.      |
| `regression` | Pinned edge cases (16-bit/unsigned parsing, banner rules, window trimming) that must never come back. |

Area tags: `zones`, `parsing`, `pairing`.

## Plans

```sh
./gradlew :app:testDebugUnitTest                      # FullTests
./gradlew :app:testDebugUnitTest -PincludeTags=smoke  # SmokeTests
./gradlew :app:testDebugUnitTest -PincludeTags=parsing  # by area
```

Unit tests drive `HeartRateMonitor` through the `SensorTransport` seam with
a fake — no Bluetooth, no emulator. On-device behavior (real GATT, runtime
permissions) requires a phone plus a worn strap.
