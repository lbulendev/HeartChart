package com.curiousbeagle.android.heartchart

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.curiousbeagle.android.heartchart.bluetooth.GattSensorTransport
import com.curiousbeagle.android.heartchart.bluetooth.HeartRateMonitor
import com.curiousbeagle.android.heartchart.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class HeartChartApplication : Application() {

    /** App-scoped dependencies; small enough that manual wiring beats a DI framework. */
    lateinit var settings: SettingsStore
        private set

    lateinit var monitor: HeartRateMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(settingsDataStore)
        // App-scoped so the BLE connection survives configuration changes.
        monitor = HeartRateMonitor(
            transport = GattSensorTransport(this),
            settings = settings,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        )
    }
}
