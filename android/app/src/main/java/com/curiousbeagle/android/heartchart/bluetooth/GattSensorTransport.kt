package com.curiousbeagle.android.heartchart.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import java.util.UUID

/**
 * Standard-GATT transport: scans for the Heart Rate service (0x180D) and
 * subscribes to Heart Rate Measurement (0x2A37) notifications — works with
 * the Polar H9 and any other standard strap.
 *
 * Permission checks happen in the UI before scanning/connecting; calls
 * without permission fail silently at the OS level rather than crashing.
 */
@SuppressLint("MissingPermission")
class GattSensorTransport(private val context: Context) : SensorTransport {

    override var listener: SensorTransport.Listener? = null

    private val bluetoothManager =
        context.getSystemService(BluetoothManager::class.java)
    private var gatt: BluetoothGatt? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            listener?.onSensorFound(result.device.address, result.device.name)
        }

        override fun onScanFailed(errorCode: Int) {
            listener?.onBluetoothUnavailable()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when {
                newState == BluetoothProfile.STATE_CONNECTED -> {
                    listener?.onConnected(gatt.device.name)
                    gatt.discoverServices()
                }

                newState == BluetoothProfile.STATE_DISCONNECTED &&
                    status != BluetoothGatt.GATT_SUCCESS -> {
                    listener?.onConnectionFailed()
                }

                newState == BluetoothProfile.STATE_DISCONNECTED -> {
                    listener?.onDisconnected()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val characteristic = gatt.getService(HEART_RATE_SERVICE)
                ?.getCharacteristic(HEART_RATE_MEASUREMENT) ?: return
            gatt.setCharacteristicNotification(characteristic, true)
            characteristic.getDescriptor(CLIENT_CONFIG)?.let { descriptor ->
                gatt.writeDescriptor(
                    descriptor,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                )
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT) {
                listener?.onMeasurement(value)
            }
        }
    }

    override fun startScan() {
        val scanner = bluetoothManager.adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
        if (scanner == null) {
            listener?.onBluetoothUnavailable()
            return
        }
        scanner.startScan(
            listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(HEART_RATE_SERVICE))
                    .build()
            ),
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build(),
            scanCallback,
        )
    }

    override fun stopScan() {
        bluetoothManager.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    override fun connect(address: String) {
        val adapter = bluetoothManager.adapter?.takeIf { it.isEnabled }
        if (adapter == null) {
            listener?.onBluetoothUnavailable()
            return
        }
        gatt?.close()
        // autoConnect: the system keeps the connect pending and completes
        // it whenever the strap comes in range — the reconnect behavior.
        gatt = adapter.getRemoteDevice(address)
            .connectGatt(context, true, gattCallback)
    }

    override fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    companion object {
        val HEART_RATE_SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CLIENT_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
