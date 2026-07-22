//
//  HeartRateMonitor.swift
//  HeartChart
//

import CoreBluetooth
import Foundation
import Observation

/// Connects to any sensor exposing the standard Bluetooth Heart Rate
/// service (the Polar H9 does), streams measurements, and remembers the
/// paired sensor across launches.
///
/// The central runs on the main queue, so delegate callbacks (which arrive
/// nonisolated under Swift 6) can safely `MainActor.assumeIsolated` back
/// into this main-actor type.
@MainActor
@Observable
final class HeartRateMonitor: NSObject {

    enum ConnectionState: Equatable {
        /// Bluetooth is off or unauthorized.
        case bluetoothUnavailable
        /// No sensor paired yet.
        case idle
        case scanning
        case connecting(name: String)
        case connected(name: String)
        /// Paired but out of reach — a connect request is pending and
        /// completes automatically when the sensor comes in range.
        case reconnecting
    }

    struct DiscoveredSensor: Identifiable, Equatable {
        let id: UUID
        let name: String
    }

    /// A connection problem worth a banner. The UI maps each case to a
    /// localized message; persists until a reconnect succeeds.
    enum SensorError: Equatable {
        /// A connect attempt failed outright.
        case connectionFailed
        /// An established connection dropped (strap off, out of range).
        case connectionLost
    }

    private(set) var state: ConnectionState = .idle
    private(set) var discovered: [DiscoveredSensor] = []
    private(set) var currentHeartRate: Int?
    /// Rolling window of recent samples for the chart.
    private(set) var samples: [HeartRateSample] = []
    private(set) var pairedSensorID: UUID?

    /// The failure currently shown as a red banner; nil when healthy.
    private(set) var connectionError: SensorError?

    var isPaired: Bool { pairedSensorID != nil }

    static let heartRateService = CBUUID(string: "180D")
    static let heartRateMeasurement = CBUUID(string: "2A37")

    /// How much history the chart keeps.
    static let sampleWindow: TimeInterval = 3 * 60

    private var central: CBCentralManager!
    private var sensor: CBPeripheral?
    private var discoveredPeripherals: [UUID: CBPeripheral] = [:]
    private let defaults: UserDefaults
    private static let pairedIDKey = "pairedSensorID"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        super.init()
        pairedSensorID = defaults.string(forKey: Self.pairedIDKey).flatMap(UUID.init)
        central = CBCentralManager(delegate: self, queue: .main)
    }

    // MARK: Pairing

    func startScanning() {
        guard central.state == .poweredOn else { return }
        discovered = []
        state = .scanning
        central.scanForPeripherals(withServices: [Self.heartRateService])
    }

    func stopScanning() {
        central.stopScan()
        if state == .scanning {
            state = isPaired ? .reconnecting : .idle
        }
    }

    func pair(_ sensorInfo: DiscoveredSensor) {
        guard let peripheral = discoveredPeripherals[sensorInfo.id] else { return }
        central.stopScan()
        pairedSensorID = sensorInfo.id
        defaults.set(sensorInfo.id.uuidString, forKey: Self.pairedIDKey)
        connectionError = nil
        connect(peripheral)
    }

    func unpair() {
        if let sensor {
            central.cancelPeripheralConnection(sensor)
        }
        sensor = nil
        pairedSensorID = nil
        defaults.removeObject(forKey: Self.pairedIDKey)
        currentHeartRate = nil
        connectionError = nil
        state = central.state == .poweredOn ? .idle : .bluetoothUnavailable
    }

    /// The banner's Retry: re-issue the pending connect (or rescan if the
    /// system no longer knows the peripheral).
    func retryConnection() {
        guard isPaired else { return }
        if let sensor {
            connect(sensor)
        } else {
            reconnectIfPaired()
        }
    }

    private func connect(_ peripheral: CBPeripheral) {
        sensor = peripheral
        peripheral.delegate = self
        state = .connecting(name: peripheral.name ?? "Heart Rate Sensor")
        central.connect(peripheral)
    }

    /// On launch (or Bluetooth power-on), reconnect to the remembered
    /// sensor: directly when the system still knows it, via a scan when not.
    private func reconnectIfPaired() {
        guard let id = pairedSensorID else {
            state = .idle
            return
        }
        if let known = central.retrievePeripherals(withIdentifiers: [id]).first {
            connect(known)
        } else {
            state = .reconnecting
            central.scanForPeripherals(withServices: [Self.heartRateService])
        }
    }

    // MARK: Measurements

    /// Parses a standard Heart Rate Measurement (0x2A37) payload: flags
    /// byte, then UInt8 or UInt16 BPM depending on flag bit 0.
    /// Nonisolated: a pure function callable from any context (and tests).
    nonisolated static func heartRate(from data: Data) -> Int? {
        let bytes = [UInt8](data)
        guard let flags = bytes.first else { return nil }
        if flags & 0x01 != 0 {
            guard bytes.count >= 3 else { return nil }
            return Int(bytes[1]) | (Int(bytes[2]) << 8)
        }
        guard bytes.count >= 2 else { return nil }
        return Int(bytes[1])
    }

    // MARK: Connection lifecycle (internal so tests can drive them
    // without constructing CoreBluetooth objects)

    func handleConnected(name: String) {
        state = .connected(name: name)
        connectionError = nil
    }

    func handleDisconnection() {
        currentHeartRate = nil
        guard isPaired else { return }
        state = .reconnecting
        connectionError = .connectionLost
    }

    func handleConnectionFailure() {
        guard isPaired else { return }
        state = .reconnecting
        connectionError = .connectionFailed
    }

    // Internal (not private) so tests can drive samples without Bluetooth.
    func record(_ bpm: Int) {
        // The sensor reports 0 when it loses skin contact — that's an
        // outage, not a reading. No sample means the chart shows a gap.
        guard bpm > 0 else {
            currentHeartRate = nil
            return
        }
        currentHeartRate = bpm
        samples.append(HeartRateSample(date: .now, bpm: bpm))
        trimSamples(now: .now)
    }

    func trimSamples(now: Date) {
        let cutoff = now.addingTimeInterval(-Self.sampleWindow)
        samples.removeAll { $0.date < cutoff }
    }
}

// MARK: - CBCentralManagerDelegate

// @preconcurrency conformance keeps these methods MainActor-isolated (the
// central dispatches to the main queue, so the inserted runtime check holds)
// without region-isolation errors on the non-Sendable CB types.
extension HeartRateMonitor: @preconcurrency CBCentralManagerDelegate {
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .poweredOn:
            reconnectIfPaired()
        default:
            state = .bluetoothUnavailable
        }
    }

    func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        discoveredPeripherals[peripheral.identifier] = peripheral
        // Reconnect-by-scan: the remembered sensor reappeared.
        if peripheral.identifier == pairedSensorID {
            central.stopScan()
            connect(peripheral)
            return
        }
        let entry = DiscoveredSensor(
            id: peripheral.identifier,
            name: peripheral.name ?? "Heart Rate Sensor"
        )
        if !discovered.contains(where: { $0.id == entry.id }) {
            discovered.append(entry)
        }
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        handleConnected(name: peripheral.name ?? "Heart Rate Sensor")
        peripheral.discoverServices([Self.heartRateService])
    }

    func centralManager(
        _ central: CBCentralManager,
        didDisconnectPeripheral peripheral: CBPeripheral,
        error: (any Error)?
    ) {
        handleDisconnection()
        guard isPaired else { return }
        // Re-issue the connect; it stays pending until back in range.
        central.connect(peripheral)
    }

    func centralManager(
        _ central: CBCentralManager,
        didFailToConnect peripheral: CBPeripheral,
        error: (any Error)?
    ) {
        handleConnectionFailure()
    }
}

// MARK: - CBPeripheralDelegate

extension HeartRateMonitor: @preconcurrency CBPeripheralDelegate {
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: (any Error)?) {
        let service = peripheral.services?.first { $0.uuid == Self.heartRateService }
        guard let service else { return }
        peripheral.discoverCharacteristics([Self.heartRateMeasurement], for: service)
    }

    func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverCharacteristicsFor service: CBService,
        error: (any Error)?
    ) {
        let characteristic = service.characteristics?.first { $0.uuid == Self.heartRateMeasurement }
        guard let characteristic else { return }
        peripheral.setNotifyValue(true, for: characteristic)
    }

    func peripheral(
        _ peripheral: CBPeripheral,
        didUpdateValueFor characteristic: CBCharacteristic,
        error: (any Error)?
    ) {
        guard characteristic.uuid == Self.heartRateMeasurement,
              let data = characteristic.value,
              let bpm = Self.heartRate(from: data) else { return }
        record(bpm)
    }
}
