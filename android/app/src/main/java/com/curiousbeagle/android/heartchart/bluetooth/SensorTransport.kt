package com.curiousbeagle.android.heartchart.bluetooth

/**
 * The seam between HeartRateMonitor's state machine and Android's BLE
 * stack — the analog of TheMovieDBSwift's API interface: production uses
 * GattSensorTransport; unit tests drive a fake without any Bluetooth.
 */
interface SensorTransport {

    interface Listener {
        fun onBluetoothUnavailable()
        fun onSensorFound(address: String, name: String?)
        fun onConnected(name: String?)
        fun onConnectionFailed()
        fun onDisconnected()
        /** Raw Heart Rate Measurement (0x2A37) payload. */
        fun onMeasurement(data: ByteArray)
    }

    var listener: Listener?

    /** Scan for peripherals advertising the Heart Rate service. */
    fun startScan()
    fun stopScan()

    /** Connect (or re-issue a pending connect) to the given device address. */
    fun connect(address: String)
    fun disconnect()
}
