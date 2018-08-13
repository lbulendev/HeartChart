//
//  MainViewController.swift
//  HeartChart-iOS
//
//  Created by Larry Bulen on 8/11/18.
//  Copyright © 2018 Larry Bulen. All rights reserved.
//

import Foundation
import CoreBluetooth
import FBSDKCoreKit
import FBSDKLoginKit
import GoogleSignIn

class MainViewController: UIViewController, FBSDKLoginButtonDelegate, GIDSignInDelegate, GIDSignInUIDelegate {
    
    @IBOutlet weak var googleSignInButton: GIDSignInButton!
    @IBOutlet weak var facebookSignInButton: FBSDKLoginButton!

    @IBOutlet weak var oauthLabel: UILabel!
    let persistentStore = PersistentStore()
    var centralManager: CBCentralManager!
    var heartRatePeripheral: CBPeripheral!
    @IBOutlet weak var bodySensorLocationLabel: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()
        GIDSignIn.sharedInstance().uiDelegate = self
        GIDSignIn.sharedInstance().delegate = self
        facebookSignInButton.delegate = self

        centralManager = CBCentralManager(delegate: self, queue: nil)

        if (persistentStore.getGoogleUserAuth()) {
            googleSignInButton.isEnabled = false
            facebookSignInButton.isEnabled = false
            oauthLabel.text = persistentStore.getGoogleUserToken()
        } else if (persistentStore.getFacebookUserAuth()) {
            googleSignInButton.isEnabled = false
            facebookSignInButton.isEnabled = false
            oauthLabel.text = persistentStore.getFacebookUserToken()
        }
    }

    @IBAction func didTapSignOut(_ sender: AnyObject) {
        GIDSignIn.sharedInstance().signOut()
    }

    // GIDSignInDelegate - delegate method
    func sign(_ signIn: GIDSignIn!, didSignInFor user: GIDGoogleUser!, withError error: Error!) {
        print("Signed in!")
        print(user.authentication.idToken!)
        let idToken: String = user.authentication.idToken!
        if (idToken != persistentStore.getGoogleUserToken()) {
            persistentStore.setGoogleUserToken(token: user.authentication.idToken!)
            persistentStore.setGoogleUserAuth(auth: true)
            print(persistentStore.getGoogleUserToken())
        }
        oauthLabel.text = "Google token: " + persistentStore.getGoogleUserToken()
    }

    // FBSDKLoginButtonDelegate - delegate methods
    func loginButton(_ loginButton: FBSDKLoginButton!, didCompleteWith result: FBSDKLoginManagerLoginResult!, error: Error!) {
        if (error == nil) {
            persistentStore.setFacebookUserAuth(auth: true)
            let fbToken: String = FBSDKAccessToken.current().tokenString!
            persistentStore.setFacebookUserToken(token: fbToken)
            oauthLabel.text = "FB token: " + fbToken
        }
    }
    
    func loginButtonDidLogOut(_ loginButton: FBSDKLoginButton!) {
        print("FB Logout!")
    }
}

extension MainViewController: CBCentralManagerDelegate {
    // CBCentralManagerDelegate - delegate methods
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .unknown:
            print ("central.state = Unknown")
        case .resetting:
            print ("central.state = resetting")
        case .unsupported:
            print ("central.state = unsupported")
        case .unauthorized:
            print ("central.state = unauthorize")
        case .poweredOff:
            print ("central.state = poweredOff")
        case .poweredOn:
            print ("central.state = poweredOn")
            centralManager.scanForPeripherals(withServices: [Constants.BLUService.heartRateServiceCBUUID])
        }
    }

    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        print(peripheral)
        heartRatePeripheral = peripheral
        heartRatePeripheral.delegate = self

        centralManager.stopScan()
        centralManager.connect(heartRatePeripheral)
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        print("CB Connected!")
        heartRatePeripheral.discoverServices([Constants.BLUService.heartRateServiceCBUUID])
    }
}

extension MainViewController: CBPeripheralDelegate {
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let services = peripheral.services else { return }
        
        for service in services {
            peripheral.discoverCharacteristics(nil, for: service)
            print(service)
            print(service.characteristics ?? "characteristics are nil")
        }
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard let characteristics = service.characteristics else { return }
        
        for characteristic in characteristics {
            print(characteristic)
            if characteristic.properties.contains(.read) {
                print("\(characteristic.uuid): properties contains .read")
                peripheral.readValue(for: characteristic)
            }
            if characteristic.properties.contains(.notify) {
                print("\(characteristic.uuid): properties contains .notify")
                peripheral.setNotifyValue(true, for: characteristic)
            }
        }
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        switch characteristic.uuid {
        case Constants.BLUService.bodySensorLocationCharacteristicCBUUID:
            let bodySensorLocation = bodyLocation(from: characteristic)
           // bodySensorLocationLabel.text = bodySensorLocation
            print (bodySensorLocation)
        case Constants.BLUService.heartRateMeasurementCharacteristicCBUUID:
            let bpm = heartRate(from: characteristic)
            onHeartRateRceived(bpm)
        default:
            print("Unhandled Characteristic UUID: \(characteristic.uuid)")
        }
    }

    private func bodyLocation(from characteristic: CBCharacteristic) -> String {
        guard let characteristicData = characteristic.value,
            let byte = characteristicData.first else { return "Error" }
        
        switch byte {
        case 0: return "Other"
        case 1: return "Chest"
        case 2: return "Wrist"
        case 3: return "Finger"
        case 4: return "Hand"
        case 5: return "Ear Lobe"
        case 6: return "Foot"
        default:
            return "Reserved for future use"
        }
    }

    private func heartRate(from characteristic: CBCharacteristic) -> Int {
        guard let characteristicData = characteristic.value else { return -1 }
        let byteArray = [UInt8](characteristicData)
        
        let firstBitValue = byteArray[0] & 0x01
        if firstBitValue == 0 {
            // Heart Rate Value Format is in the 2nd byte
            return Int(byteArray[1])
        } else {
            // Heart Rate Value Format is in the 2nd and 3rd bytes
            return (Int(byteArray[1]) << 8) + Int(byteArray[2])
        }
    }

    private func onHeartRateRceived(_ bpm: Int) -> Void {
        print("heart rate: " + String(bpm))
    }
}
