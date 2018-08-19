//
//  MainViewController.swift
//  HeartChart-iOS
//
//  Created by Larry Bulen on 8/11/18.
//  Copyright © 2018 Larry Bulen. All rights reserved.
//

import Foundation
import FBSDKCoreKit
import FBSDKLoginKit
import GoogleSignIn
import Charts

class MainViewController: UIViewController, FBSDKLoginButtonDelegate, GIDSignInDelegate, GIDSignInUIDelegate, BBBLEDiscoveryManagerDelegate, BBBLEHRMDeviceDelegate, ChartViewDelegate, UITextFieldDelegate {
    
    var discoveryManager : BBBLEHRMDiscoveryManager?
    var hrmDevice : BBBLEHRMDevice?
    let persistentStore = PersistentStore()
    var heartRatePeripheral: CBPeripheral!
    var values : [ChartDataEntry] = [ChartDataEntry()]  // used to store the recorded 'heart rate'
    var set1 : LineChartDataSet? = nil                  // used to pass the data into the 'chart'

    @IBOutlet weak var googleSignInButton: GIDSignInButton!
    @IBOutlet weak var facebookSignInButton: FBSDKLoginButton!
    @IBOutlet weak var oauthLabel: UILabel!
    @IBOutlet var chartView : HeartChart?               // used for the 'heart rate' chart/graph
    @IBOutlet weak var ageTextField: UITextField!       // used to allow the user to enter an age

    required init?(coder aDecoder: NSCoder) {
        self.discoveryManager = nil
        self.hrmDevice = nil
        super.init(coder: aDecoder)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        self.discoveryManager = BBBLEHRMDiscoveryManager.init(delegate: self)
        self.discoveryManager?.discover()
        self.chartView?.delegate = self;
        self.ageTextField.delegate = self

        GIDSignIn.sharedInstance().uiDelegate = self
        GIDSignIn.sharedInstance().delegate = self
        facebookSignInButton.delegate = self

        if (persistentStore.getGoogleUserAuth()) {
            googleSignInButton.isEnabled = false
            facebookSignInButton.isEnabled = false
            oauthLabel.text = persistentStore.getGoogleUserToken()
        } else if (persistentStore.getFacebookUserAuth()) {
            googleSignInButton.isEnabled = false
            facebookSignInButton.isEnabled = false
            oauthLabel.text = persistentStore.getFacebookUserToken()
        }
        
        setupLeftAxisAndLimits(age: 0)
        
        chartView?.rightAxis.enabled = false;
        
        self.set1 = nil
        if ((chartView?.data != nil) && ((chartView?.data?.dataSetCount)! > 0))
        {
            set1 = chartView?.data?.dataSets[0] as! LineChartDataSet?
            set1?.values = values
            chartView?.data?.notifyDataChanged()
            chartView?.notifyDataSetChanged()
        }
        else
        {
            set1 = LineChartDataSet.init(values: values, label: "Heart Rate")
            
            set1?.drawCirclesEnabled = false
            
            set1?.lineDashLengths = [5.0, 2.5];
            set1?.highlightLineDashLengths = [5.0, 2.5];
            set1?.setColor(UIColor.black)
            set1?.setCircleColor(UIColor.black)
            set1?.lineWidth = 1.0;
            set1?.circleRadius = 3.0;
            set1?.drawCircleHoleEnabled = false;
            set1?.valueFont = UIFont(name: "HelveticaNeue-Light", size: 9)!
            set1?.formLineDashLengths = [5.0, 2.5];
            set1?.formLineWidth = 1.0;
            set1?.formSize = 15.0;
            
            let yellowColorLowAlpha = UIColor.init(red: 1, green: 1, blue: 0, alpha: 0.35).cgColor
            let gradientColors : NSArray = [yellowColorLowAlpha]
            let gradient : CGGradient = CGGradient.init(colorsSpace: nil, colors: gradientColors, locations: [0, 0, 1])!
            set1?.fillAlpha = 0.5;
            set1?.fill = Fill.fillWithLinearGradient(gradient, angle: 90)
            set1?.drawFilledEnabled = true;
            
            var dataSets : [IChartDataSet]? = [IChartDataSet]()
            dataSets?.append(set1!)
            
            let data : LineChartData = LineChartData.init(dataSets: dataSets)
            
            chartView?.data = data;
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

    // FBSDKLoginButtonDelegate - delegate methods
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        setupLeftAxisAndLimits(age: Double(textField.text!)!)
        updateValues()
        return true
    }

    // BBBLEDiscoveryManagerDelegate - methods
    func bbbleDiscoveryManagerStarted(_ manager: BBBLEDiscoveryManager!) {
        updateValues()
    }
    
    func bbbleDiscoveryManagerFailed(_ manager: BBBLEDiscoveryManager!) {
        updateValues()
    }
    
    func bbbleDiscoveryManagerCancelled(_ manager: BBBLEDiscoveryManager!) {
        updateValues()
    }
    
    func bbbleDiscoveryManagerDeviceDiscovered(_ manager: BBBLEDiscoveryManager!, deviceData: BBBLEDiscoveryData!) {
        // If a BBLE device is found and connected, then init the HRMDEVICE and connect, and stop discovering other devices
        if deviceData != nil {
            self.hrmDevice = BBBLEHRMDevice.init(uuid: deviceData.uuid, restorationIdentifier: "HRM")
            self.hrmDevice?.delegate = self
            self.hrmDevice?.debugLogging = true
            self.discoveryManager?.stopDevicesDiscovery()
            self.hrmDevice?.connect()
        }
    }
    
    func bbbleDiscoveryManagerCompleted(_ manager: BBBLEDiscoveryManager!, devicesData: [Any]!) {
        updateValues()
    }
    
    func bbbleDiscoveryManagerProgressUpdated(_ manager: BBBLEDiscoveryManager!) {
        updateValues()
    }
    
    func bbbleDiscoveryManagerStatusUpdated(_ manager: BBBLEDiscoveryManager!) {
        updateValues()
    }
    
    // BBBLEHRMDeviceDelegate - methods
    func bbblehrmDeviceStatusUpdated(_ device: BBBLEHRMDevice!) {
        updateValues()
    }
    
    func bbblehrmDeviceBodyContactStatusUpdated(_ device: BBBLEHRMDevice!) {
        updateValues()
    }
    
    func bbblehrmDeviceDataUpdated(_ device: BBBLEHRMDevice!) {
        updateValues()
    }
    
    func bbblehrmDeviceCharacteristicUpdated(_ device: BBBLEHRMDevice!, characteristic: BBBLEDeviceHRMCharacteristic) {
        updateValues()
    }

    func setupLeftAxisAndLimits(age : Double) {
        let limits = findHealthyHeartRange(age: age)
        let upperLimit : ChartLimitLine = ChartLimitLine.init(limit: Double(limits.upperLimit), label: "Upper Limit")
        upperLimit.lineWidth = 4.0;
        upperLimit.lineDashLengths = [5.0, 5.0];
        upperLimit.labelPosition = .rightTop
        upperLimit.valueFont = UIFont(name: "HelveticaNeue-Light", size: 10)!
        
        let lowerLimit : ChartLimitLine = ChartLimitLine.init(limit: Double(limits.lowerLimit), label: "Lower Limit")
        lowerLimit.lineWidth = 4.0;
        lowerLimit.lineDashLengths = [5.0, 5.0];
        lowerLimit.labelPosition = .rightBottom
        lowerLimit.valueFont = UIFont(name: "HelveticaNeue-Light", size: 10)!
        
        let leftAxis : YAxis = (chartView?.leftAxis)!
        leftAxis.removeAllLimitLines()
        leftAxis.addLimitLine(upperLimit)
        leftAxis.addLimitLine(lowerLimit)
        leftAxis.axisMaximum = 200.0;
        leftAxis.axisMinimum = 0.0;
        leftAxis.gridLineDashLengths = [5.0, 5.0];
        leftAxis.drawZeroLineEnabled = false
        leftAxis.drawLimitLinesBehindDataEnabled = true
    }

    func updateValues() {
        // If the HRMDEVICE exists; capture the current heart rage and add to the list of VALUES.
        // Now, that we have a value, update the 'chart'.
        if (self.hrmDevice != nil) {
            let icon : UIImage = UIImage.init(named: "icon")!
            let dataEntryItem : ChartDataEntry = ChartDataEntry.init(x: Double((values.count) + 1), y: Double((self.hrmDevice?.heartRate)!), data: icon)
            values.append(dataEntryItem)
            set1 = chartView?.data?.dataSets[0] as! LineChartDataSet?
            set1?.values = values
            chartView?.data?.notifyDataChanged()
            chartView?.notifyDataSetChanged()
        }
        
        // Test to see if the DISCOVERYMANAGER is NIL or not.
        // I put in PRINT statements to help see the cycle the bluetooth discovery manager is in.
        if (self.discoveryManager != nil) {
            switch self.discoveryManager!.state {
            case BBBLEDiscoveryManagerStateUnknown:                // Status is unkown (not determined yet)
                print("BBBLEDiscoveryManagerStateUnknown")
                break
            case BBBLEDiscoveryManagerStateBLEIsNotSupported:    // BLE is not supported by the device hardware
                print("BBBLEDiscoveryManagerStateBLEIsNotSupported")
                break
            case BBBLEDiscoveryManagerStateUnauthorized:        // App is not authorized to use Bluetooth
                print("BBBLEDiscoveryManagerStateUnauthorized")
                break
            case BBBLEDiscoveryManagerStateBLEIsPoweredOff:        // Bluetooth is powered off (Airplane mode?)
                print("BBBLEDiscoveryManagerStateBLEIsPoweredOff")
                break
            case BBBLEDiscoveryManagerStateResetting:            // Bluetooth is resetting
                print("BBBLEDiscoveryManagerStateResetting")
                break
            case BBBLEDiscoveryManagerStateDiscoveryIdle:        // Does not discovering
                print("BBBLEDiscoveryManagerStateDiscoveryIdle")
                break
            case BBBLEDiscoveryManagerStateDiscoveryInProgress:    // Discovering in progress
                print("BBBLEDiscoveryManagerStateDiscoveryInProgress")
                break
            default:
                print("default - self.discoveryManager!.state")
                break
            }
        }
        
        // Test to see if the HRMDEVICE is NIL or not.
        // I put in PRINT statements to help see the cycle the HRMDEVICE is in.
        if (self.hrmDevice != nil) {
            switch self.hrmDevice!.status {
            case BBBLEHRMDeviceStatusUnknown:                    // Status Unknown (not determined yet)
                print("BBBLEHRMDeviceStatusUnknown")
                break
            case BBBLEHRMDeviceStatusBLEIsNotSupported:            // BLE is not supported by device hardware
                print("BBBLEHRMDeviceStatusBLEIsNotSupported")
                break
            case BBBLEHRMDeviceStatusBLEIsPoweredOff:            // Bluetooth is powered off (Airplane mode?)
                print("BBBLEHRMDeviceStatusBLEIsPoweredOff")
                break
            case BBBLEHRMDeviceStatusBLEIsUnauthorized:            // Application is not authorized to use Bluetooth (In privacy settings?)
                print("BBBLEHRMDeviceStatusBLEIsUnauthorized")
                break
            case BBBLEHRMDeviceStatusBLEIsPending:                // Bluetooth in pending state (unkown or resetting)
                print("BBBLEHRMDeviceStatusBLEIsPending")
                break
            case BBBLEHRMDeviceStatusSearching:                    // Searching for device
                print("BBBLEHRMDeviceStatusSearching")
                break
            case BBBLEHRMDeviceStatusNotFound:                    // Device not found
                print("BBBLEHRMDeviceStatusNotFound")
                break
            case BBBLEHRMDeviceStatusNotConnected:                // Device is not connected
                print("BBBLEHRMDeviceStatusNotConnected")
                break
            case BBBLEHRMDeviceStatusConnecting:                // Connecting and reading device data
                print("BBBLEHRMDeviceStatusConnecting")
                break
            case BBBLEHRMDeviceStatusConnectedAndCalibrating:    // Connected and waiting for the first data sampling
                print("BBBLEHRMDeviceStatusConnectedAndCalibrating")
                break
            case BBBLEHRMDeviceStatusNoBodyContact:                // No sensor to body contact (possible only of contact detection is supported)
                print("BBBLEHRMDeviceStatusNoBodyContact")
                break
            case BBBLEHRMDeviceStatusConnected:                    // Connected and sampling data
                print("BBBLEHRMDeviceStatusConnected")
                break
            case BBBLEHRMDeviceStatusReconnecting:                // Auto reconnecting
                print("BBBLEHRMDeviceStatusReconnecting")
                break
            default:
                print("default - self.hrmDevice!.status")
                break
            }
        }
    }

    func findHealthyHeartRange (age: Double) -> (lowerLimit: Int, upperLimit: Int) {
        var returnLimits = (30, 150)  // Set default values
        switch age {
            // Found the following values on:
        // http://www.newhealthadvisor.com/Normal-Heart-Rate-Chart.html
        case 0..<1:
            returnLimits = (100, 160)
            break
        case 1..<2:
            returnLimits = (90, 150)
            break
        case 2...5:
            returnLimits = (80, 140)
            break
        case 6..<12:
            returnLimits = (70, 120)
            break
        case 12..<20:
            returnLimits = (60, 100)
            break
            // Found the following values on:
        // http://www.heart.org/HEARTORG/HealthyLiving/PhysicalActivity/FitnessBasics/Target-Heart-Rates_UCM_434341_Article.jsp#.WK-jmRjMxp8
        case 20..<30:
            returnLimits = (100, 170)
            break
        case 30..<35:
            returnLimits = (95, 162)
            break
        case 35..<40:
            returnLimits = (93, 157)
            break
        case 40..<45:
            returnLimits = (90, 153)
            break
        case 45..<50:
            returnLimits = (88, 149)
            break
        case 50..<55:
            returnLimits = (85, 145)
            break
        case 55..<60:
            returnLimits = (83, 140)
            break
        case 60..<65:
            returnLimits = (80, 136)
            break
        case 65..<70:
            returnLimits = (78, 132)
            break
        case 70..<200:
            returnLimits = (75, 128)
            break
        default:
            break
        }
        return returnLimits
    }
}
