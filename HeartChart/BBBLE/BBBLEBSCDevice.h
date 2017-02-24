//
//  BBBLEBSCDevice.h
//
//  Copyright (c) 2012 Beets BLU Electronics. All rights reserved.
//

#import "BBBLEDevice.h"

typedef enum
	{
	BBBLEBSCDeviceStatusUnknown,						// Status Unknown (not determined yet)
	BBBLEBSCDeviceStatusBLEIsNotSupported,			// BLE is not supported by device hardware
	BBBLEBSCDeviceStatusBLEIsPoweredOff,			// Bluetooth is powered off (Airplane mode?)
	BBBLEBSCDeviceStatusBLEIsUnauthorized,			// Application is not authorized to use Bluetooth (In privacy settings?)
	BBBLEBSCDeviceStatusBLEIsPending,				// Bluetooth in pending state (unkown or resetting)
	BBBLEBSCDeviceStatusSearching,					// Searching for device
	BBBLEBSCDeviceStatusNotFound,					// Device not found
	BBBLEBSCDeviceStatusNotConnected,				// Device is not connected
	BBBLEBSCDeviceStatusConnecting,					// Connecting and reading device data
	BBBLEBSCDeviceStatusConnected,					// Connected and sampling data
	BBBLEBSCDeviceStatusReconnecting				// Auto reconnecting
	}
	BBBLEBSCDeviceStatus;

typedef enum
	{
	BBBLESCDeviceLocationUnknown,		// Device location unknown or not supported
	BBBLESCDeviceLocationOther,			// 'Other'
	BBBLESCDeviceLocationTopOFShoe,		// Top of shoe
	BBBLESCDeviceLocationInShoe,		// In shoe
	BBBLESCDeviceLocationHip,			// Hip
	BBBLESCDeviceLocationFrontWheel,	// Front Wheel
	BBBLESCDeviceLocationLeftCrank,		// Left Crank
	BBBLESCDeviceLocationRightCrank,	// Right Crank
	BBBLESCDeviceLocationLeftPedal,		// Left Pedal
	BBBLESCDeviceLocationRightPedal,	// Right Pedal
	BBBLESCDeviceLocationFrontHub,		// Front Hub
	BBBLESCDeviceLocationRearDropout,	// Rear Dropout
	BBBLESCDeviceLocationChainstay,		// Chainstay
	BBBLESCDeviceLocationRearWheel,		// Rear Wheel
	BBBLESCDeviceLocationRearHub,		// Rear Hub
	BBBLESCDeviceLocationChest,			// Chest
	BBBLESCDeviceLocationReserved		// Reserved for future use
	}
	BBBLESCDeviceLocation;

typedef enum
	{
	BBBLEDeviceBSCWheelRevolutionsDataSuportUnknown,	// Not (yet) known
	BBBLEDeviceBSCWheelRevolutionsDataSuportSupported,
	BBBLEDeviceBSCWheelRevolutionsDataSuportNotSupported
	}
	BBBLEDeviceBSCWheelRevolutionsDataSuport;

typedef enum
	{
	BBBLEDeviceBSCCrankRevolutionsDataSuportUnknown,	// Not (yet) known
	BBBLEDeviceBSCCrankRevolutionsDataSuportSupported,
	BBBLEDeviceBSCCrankRevolutionsDataSuportNotSupported
	}
	BBBLEDeviceBSCCrankRevolutionsDataSuport;

typedef enum
	{
	BBBLEDeviceBSCMultipleLocationsSuportUnknown,		// Not (yet) known
	BBBLEDeviceBSCMultipleLocationsSuportSupported,
	BBBLEDeviceBSCMultipleLocationsSuportNotSupported
	}
	BBBLEDeviceBSCMultipleLocationsSuport;

typedef enum
	{
	BBBLEDeviceBSCCharacteristicDeviceName,
	BBBLEDeviceBSCCharacteristicSerial,
	BBBLEDeviceBSCCharacteristicModelName,
	BBBLEDeviceBSCCharacteristicManufacturerName,
	BBBLEDeviceBSCCharacteristicHardwareRevision,
	BBBLEDeviceBSCCharacteristicFirmwareRevision,
	BBBLEDeviceBSCCharacteristicSoftwareRevision,
	BBBLEDeviceBSCCharacteristicTxPower,
	BBBLEDeviceBSCCharacteristicBatteryLevel,
	BBBLEDeviceBSCCharacteristicFeature,
	BBBLEDeviceBSCCharacteristicMeasurement,
	BBBLEDeviceBSCCharacteristicSensorLocation
	}
	BBBLEDeviceBSCCharacteristic;

@class BBBLEBSCDevice;

@protocol BBBLEBSCDeviceDelegate <NSObject>

@required

@optional

// Device status changed
- (void) BBBLEBSCDeviceStatusUpdated:(BBBLEBSCDevice *)device;

// report distance. Requires wheelRadiusMeters to be set!
- (void) BBBLEBSCDeviceReportDistance:(BBBLEBSCDevice *)device distanceMeters:(float)distanceMeters timeSeconds:(float)timeSeconds;

// Current cadence
- (void) BBBLEBSCDeviceCadenceUpdated:(BBBLEBSCDevice *)device cadence:(float)cadence;

// Current pedal to wheel ratio
- (void) BBBLEBSCDevicePedalToWheelRatioUpdated:(BBBLEBSCDevice *)device ratio:(float)ratio;


// Advanced data ///////////////////////////////////////////////////////////////////

// New characteristic red from device. Useful with readDetailedData method
- (void) BBBLEBSCDeviceCharacteristicUpdated:(BBBLEBSCDevice *)device characteristic:(BBBLEDeviceBSCCharacteristic)characteristic;

// New wheel revolutions detected
- (void) BBBLEBSCDeviceReportWheelRevolutions:(BBBLEBSCDevice *)device revolutions:(unsigned int)revolutions timeSeconds:(float)timeSeconds;

// New crank revolutions detected
- (void) BBBLEBSCDeviceReportCrankRevolutions:(BBBLEBSCDevice *)device revolutions:(unsigned int)revolutions timeSeconds:(float)timeSeconds;

////////////////////////////////////////////////////////////////////////////////////

@end

@interface BBBLEBSCDevice : BBBLEDevice

@property (nonatomic) BBBLEBSCDeviceStatus status;							// Device status

// @property (nonatomic) NSUInteger heartRate;		// Current heart rate. Updated on sensor's demand when connected (usually every 1 second)

@property (nonatomic) BBBLESCDeviceLocation deviceLocation;				// Device placement
@property (nonatomic) BBBLEDeviceBSCWheelRevolutionsDataSuport wheelRevolutionsSupport;		// Wheel revolutions data support status
@property (nonatomic) BBBLEDeviceBSCCrankRevolutionsDataSuport crankRevolutionsSupport;		// Crank revolutions data support status
@property (nonatomic) BBBLEDeviceBSCMultipleLocationsSuport multipleLocationsSupport; 		// Multiple sensor locations support status
@property (nonatomic) float wheelRadiusMeters;			// Wheel radius in meters
@property (nonatomic) float currentSpeedMS;				// Current bike speed in meters per second (requires to set wheelRadiusMeters first, othersiwe - 0)
@property (nonatomic) float currentCadenceRPM;			// Current bike cadence in rounds per minute

// Device object with known UUID and given restoration UID (effective only on iOS7 and later). If restoration is not required - pass nil
+ (BBBLEBSCDevice *) deviceWithUUID:(NSString *)UUID restorationIdentifier:(NSString *)restorationIdentifier;

//- (float) currentEnergyExpedureForWieght:(float)weightKg age:(float)age isMale:(BOOL)isMale;		// Calculates current energy expedure (calories rate per 1 minute) for given congitions

- (void) readDetailedData;	// Requests more detailed data from connected sensor (Manufacturer name, Firmware version, etc...)

+ (NSArray *)connectedDevices;

@end

