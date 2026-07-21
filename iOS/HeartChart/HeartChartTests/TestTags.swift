//
//  TestTags.swift
//  HeartChartTests
//

import Testing

// Tag taxonomy, mirroring TheMovieDBSwift: purpose tags say WHEN a test
// should run; area tags say WHAT it covers. Every test carries one purpose
// tag (via its suite) and one area tag, so test plans can filter either way.
extension Tag {
    // MARK: Purpose

    /// Critical-path checks: if any of these fail the build is not worth
    /// testing further. Fast, no edge cases.
    @Tag static var smoke: Self

    /// Contract checks on helpers and defaults: valid ranges, persistence
    /// keys, identity semantics. Failures usually mean scaffolding drift.
    @Tag static var sanity: Self

    /// Pinned edge cases that must never come back: 16-bit measurements,
    /// truncated payloads, unpair cleanup, window trimming.
    @Tag static var regression: Self

    // MARK: Area

    @Tag static var zones: Self
    @Tag static var parsing: Self
    @Tag static var pairing: Self
}
