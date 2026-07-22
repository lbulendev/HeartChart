package com.curiousbeagle.android.heartchart

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.curiousbeagle.android.heartchart.bluetooth.HeartRateMonitor
import com.curiousbeagle.android.heartchart.bluetooth.SensorTransport
import com.curiousbeagle.android.heartchart.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A fake at the transport seam: tests drive connection events and
 * measurements without any Bluetooth — the analog of the iOS internal
 * handler seam plus TheMovieDBSwift's FakeTmdbApi.
 */
class FakeSensorTransport : SensorTransport {
    override var listener: SensorTransport.Listener? = null

    var scanning = false
        private set
    val connectRequests = mutableListOf<String>()
    var disconnected = false
        private set

    override fun startScan() {
        scanning = true
    }

    override fun stopScan() {
        scanning = false
    }

    override fun connect(address: String) {
        connectRequests += address
    }

    override fun disconnect() {
        disconnected = true
    }
}

/** DataStore backed by a StateFlow — isolated persistence per test. */
class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

fun makeSettings(): SettingsStore = SettingsStore(InMemoryPreferencesDataStore())

fun makeMonitor(
    scope: CoroutineScope,
    transport: FakeSensorTransport = FakeSensorTransport(),
    settings: SettingsStore = makeSettings(),
): HeartRateMonitor = HeartRateMonitor(transport, settings, scope)
