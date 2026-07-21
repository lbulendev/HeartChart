//
//  ErrorBanner.swift
//  HeartChart
//

import SwiftUI

/// Maps sensor errors to the localized copy users see — the same pattern
/// as TheMovieDBSwift's error banner.
extension HeartRateMonitor.SensorError {
    var message: String {
        switch self {
        case .connectionFailed:
            String(localized: "error_connection_failed", defaultValue: "Couldn't connect to the sensor.")
        case .connectionLost:
            String(localized: "error_connection_lost", defaultValue: "Lost contact with the sensor.")
        }
    }
}

/// The single error surface: a red banner with the friendly message and a
/// Retry button that re-attempts the connection. Persists until the sensor
/// reconnects.
struct ErrorBanner: View {
    let error: HeartRateMonitor.SensorError
    let retry: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Label(error.message, systemImage: "exclamationmark.triangle.fill")
                .font(.subheadline.weight(.semibold))
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(String(localized: "retry", defaultValue: "Retry"), action: retry)
                .font(.subheadline.weight(.bold))
                .buttonStyle(.plain)
        }
        .foregroundStyle(.white)
        .padding(.vertical, 10)
        .padding(.horizontal, 16)
        .background(.red, in: RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
        .accessibilityElement(children: .contain)
    }
}

#if DEBUG
#Preview("Lost contact") {
    ErrorBanner(error: .connectionLost) {}
}

#Preview("Connect failed") {
    ErrorBanner(error: .connectionFailed) {}
}
#endif
