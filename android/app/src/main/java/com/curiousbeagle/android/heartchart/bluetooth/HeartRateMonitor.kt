package com.curiousbeagle.android.heartchart.bluetooth

import com.curiousbeagle.android.heartchart.data.HeartRateSample
import com.curiousbeagle.android.heartchart.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The port of the iOS HeartRateMonitor: connects to any standard Heart Rate
 * sensor via the transport seam, streams measurements, and remembers the
 * paired sensor. Pure JVM state machine — all Bluetooth lives behind
 * SensorTransport, so unit tests drive it with a fake.
 */
class HeartRateMonitor(
    private val transport: SensorTransport,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) : SensorTransport.Listener {

    sealed interface ConnectionState {
        /** Bluetooth is off or unauthorized. */
        data object BluetoothUnavailable : ConnectionState
        /** No sensor paired yet. */
        data object Idle : ConnectionState
        data object Scanning : ConnectionState
        data class Connecting(val name: String?) : ConnectionState
        data class Connected(val name: String?) : ConnectionState
        /** Paired but out of reach — the connect completes when in range. */
        data object Reconnecting : ConnectionState
    }

    /**
     * A connection problem worth a banner. The UI maps each case to a
     * localized message; persists until a reconnect succeeds.
     */
    enum class SensorError {
        /** A connect attempt failed outright. */
        CONNECTION_FAILED,
        /** An established connection dropped (strap off, out of range). */
        CONNECTION_LOST,
    }

    data class DiscoveredSensor(val address: String, val name: String?)

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _discovered = MutableStateFlow<List<DiscoveredSensor>>(emptyList())
    val discovered: StateFlow<List<DiscoveredSensor>> = _discovered.asStateFlow()

    private val _currentHeartRate = MutableStateFlow<Int?>(null)
    val currentHeartRate: StateFlow<Int?> = _currentHeartRate.asStateFlow()

    /** Rolling window of recent samples for the chart. */
    private val _samples = MutableStateFlow<List<HeartRateSample>>(emptyList())
    val samples: StateFlow<List<HeartRateSample>> = _samples.asStateFlow()

    private val _pairedSensorAddress = MutableStateFlow<String?>(null)
    val pairedSensorAddress: StateFlow<String?> = _pairedSensorAddress.asStateFlow()

    /** The failure currently shown as a red banner; null when healthy. */
    private val _connectionError = MutableStateFlow<SensorError?>(null)
    val connectionError: StateFlow<SensorError?> = _connectionError.asStateFlow()

    val isPaired: Boolean get() = _pairedSensorAddress.value != null

    init {
        transport.listener = this
        scope.launch {
            val stored = settings.pairedSensorAddress()
            _pairedSensorAddress.value = stored
            if (stored != null) {
                _state.value = ConnectionState.Reconnecting
                transport.connect(stored)
            }
        }
    }

    // MARK: Pairing

    fun startScanning() {
        _discovered.value = emptyList()
        _state.value = ConnectionState.Scanning
        transport.startScan()
    }

    fun stopScanning() {
        transport.stopScan()
        if (_state.value == ConnectionState.Scanning) {
            _state.value = if (isPaired) ConnectionState.Reconnecting else ConnectionState.Idle
        }
    }

    fun pair(sensor: DiscoveredSensor) {
        transport.stopScan()
        _pairedSensorAddress.value = sensor.address
        _connectionError.value = null
        _state.value = ConnectionState.Connecting(sensor.name)
        scope.launch { settings.setPairedSensorAddress(sensor.address) }
        transport.connect(sensor.address)
    }

    fun unpair() {
        transport.disconnect()
        _pairedSensorAddress.value = null
        _currentHeartRate.value = null
        _connectionError.value = null
        _state.value = ConnectionState.Idle
        scope.launch { settings.setPairedSensorAddress(null) }
    }

    /** The banner's Retry: re-issue the pending connect. */
    fun retryConnection() {
        val address = _pairedSensorAddress.value ?: return
        _state.value = ConnectionState.Connecting(null)
        transport.connect(address)
    }

    // MARK: Connection lifecycle (internal handlers, testable without BLE)

    fun handleConnected(name: String?) {
        _state.value = ConnectionState.Connected(name)
        _connectionError.value = null
    }

    fun handleDisconnection() {
        _currentHeartRate.value = null
        if (!isPaired) return
        _state.value = ConnectionState.Reconnecting
        _connectionError.value = SensorError.CONNECTION_LOST
    }

    fun handleConnectionFailure() {
        if (!isPaired) return
        _state.value = ConnectionState.Reconnecting
        _connectionError.value = SensorError.CONNECTION_FAILED
    }

    // MARK: Measurements

    fun record(bpm: Int, now: Long = System.currentTimeMillis()) {
        // The sensor reports 0 when it loses skin contact — that's an
        // outage, not a reading. No sample means the chart shows a gap.
        if (bpm <= 0) {
            _currentHeartRate.value = null
            return
        }
        _currentHeartRate.value = bpm
        _samples.value = _samples.value + HeartRateSample(date = now, bpm = bpm)
        trimSamples(now)
    }

    fun trimSamples(now: Long) {
        val cutoff = now - SAMPLE_WINDOW_MILLIS
        _samples.value = _samples.value.filter { it.date >= cutoff }
    }

    // MARK: SensorTransport.Listener

    override fun onBluetoothUnavailable() {
        _state.value = ConnectionState.BluetoothUnavailable
    }

    override fun onSensorFound(address: String, name: String?) {
        // Reconnect-by-scan: the remembered sensor reappeared.
        if (address == _pairedSensorAddress.value) {
            transport.stopScan()
            _state.value = ConnectionState.Connecting(name)
            transport.connect(address)
            return
        }
        if (_discovered.value.none { it.address == address }) {
            _discovered.value = _discovered.value + DiscoveredSensor(address, name)
        }
    }

    override fun onConnected(name: String?) = handleConnected(name)

    override fun onConnectionFailed() = handleConnectionFailure()

    override fun onDisconnected() = handleDisconnection()

    override fun onMeasurement(data: ByteArray) {
        heartRate(data)?.let { record(it) }
    }

    companion object {
        /** How much history the chart keeps. */
        const val SAMPLE_WINDOW_MILLIS: Long = 3 * 60 * 1000

        /**
         * Parses a standard Heart Rate Measurement (0x2A37) payload: flags
         * byte, then UInt8 or UInt16 BPM depending on flag bit 0.
         */
        fun heartRate(data: ByteArray): Int? {
            val flags = data.firstOrNull()?.toInt() ?: return null
            return if (flags and 0x01 != 0) {
                if (data.size < 3) return null
                (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
            } else {
                if (data.size < 2) return null
                data[1].toInt() and 0xFF
            }
        }
    }
}
