//
//  HeartChartApp.swift
//  HeartChart
//
//  Created by larrybulen on 7/21/26.
//

import SwiftUI

@main
struct HeartChartApp: App {
    @State private var monitor = HeartRateMonitor()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(monitor)
        }
    }
}
