//
//  TestSupport.swift
//  HeartChartTests
//

import Foundation
@testable import HeartChart

/// A fresh, isolated defaults suite per call so paired-sensor state never
/// leaks between tests or into the simulator's standard defaults.
func makeIsolatedDefaults() -> UserDefaults {
    let suiteName = "HeartChartTests-\(UUID().uuidString)"
    let defaults = UserDefaults(suiteName: suiteName)!
    defaults.removePersistentDomain(forName: suiteName)
    return defaults
}

@MainActor
func makeMonitor(defaults: UserDefaults? = nil) -> HeartRateMonitor {
    HeartRateMonitor(defaults: defaults ?? makeIsolatedDefaults())
}
