//
//  BBBLEHRMDevice.h
//
//  Copyright (c) 2012 Beets BLU Electronics. All rights reserved.
//

#import "BBBLEDevice.h"

typedef enum
	{
	BBBLEHRMDeviceStatusUnknown,						// Status Unknown (not determined yet)
	BBBLEHRMDeviceStatusBLEIsNotSupported,			// BLE is not supported by device hardware
	BBBLEHRMDeviceStatusBLEIsPoweredOff,			// Bluetooth is powered off (Airplane mode?)
	BBBLEHRMDeviceStatusBLEIsUnauthorized,			// Application is not authorized to use Bluetooth (In privacy settings?)
	BBBLEHRMDeviceStatusBLEIsPending,				// Bluetooth in pending state (unkown or resetting)
	BBBLEHRMDeviceStatusSearching,					// Searching for device
	BBBLEHRMDeviceStatusNotFound,					// Device not found
	BBBLEHRMDeviceStatusNotConnected,				// Device is not connected
	BBBLEHRMDeviceStatusConnecting,					// Connecting and reading device data
	BBBLEHRMDeviceStatusConnectedAndCalibrating,	// Connected and waiting for the first data sampling
	BBBLEHRMDeviceStatusNoBodyContact,				// No sensor to body contact (possible only of contact detection is supported)
	BBBLEHRMDeviceStatusConnected,					// Connected and sampling data
	BBBLEHRMDeviceStatusReconnecting				// Auto reconnecting
	}
	BBBLEHRMDeviceStatus;

typedef enum
	{
	BBBLEHRMDeviceBodyContactStatusUnknown,			// Status Unknown (not determined yet)
	BBBLEHRMDeviceBodyContactStatusNotSupported,	// Body connect detection is not supported by the sensor
	BBBLEHRMDeviceBodyContactStatusNoContact,		// No sensor to body contact
	BBBLEHRMDeviceBodyContactStatusContact			// Contact
	}
	BBBLEHRMDeviceBodyContactStatus;

typedef enum
	{
	BBBLEHRMDeviceLocationUnknown,		// Device location unknown or not supported
	BBBLEHRMDeviceLocationOther,		// 'Other'
	BBBLEHRMDeviceLocationChest,		// Chest
	BBBLEHRMDeviceLocationWrist,		// Wrist
	BBBLEHRMDeviceLocationFinger,		// Finder
	BBBLEHRMDeviceLocationHand,			// Hand
	BBBLEHRMDeviceLocationEarLobe,		// Ear lobe
	BBBLEHRMDeviceLocationFoot,			// Foot
	BBBLEHRMDeviceLocationReserved		// Reserved for future use
	}
	BBBLEHRMDeviceLocation;

typedef enum
	{
	BBBLEDeviceHRMCharacteristicDeviceName,
	BBBLEDeviceHRMCharacteristicSerial,
	BBBLEDeviceHRMCharacteristicModelName,
	BBBLEDeviceHRMCharacteristicManufacturerName,
	BBBLEDeviceHRMCharacteristicHardwareRevision,
	BBBLEDeviceHRMCharacteristicFirmwareRevision,
	BBBLEDeviceHRMCharacteristicSoftwareRevision,
	BBBLEDeviceHRMCharacteristicTxPower,
	BBBLEDeviceHRMCharacteristicBatteryLevel,
	BBBLEDeviceHRMCharacteristicHeartRate,
	BBBLEDeviceHRMCharacteristicSensorLocation
	}
	BBBLEDeviceHRMCharacteristic;

@class BBBLEHRMDevice;

@protocol BBBLEHRMDeviceDelegate <NSObject>

@required

@optional

- (void) BBBLEHRMDeviceStatusUpdated:(BBBLEHRMDevice *)device;				// Device status changed
- (void) BBBLEHRMDeviceBodyContactStatusUpdated:(BBBLEHRMDevice *)device;	// Body contact status changed (not including the device status change)
- (void) BBBLEHRMDeviceDataUpdated:(BBBLEHRMDevice *)device;				// Realtime device data updated (heart rate, body contact)
- (void) BBBLEHRMDeviceSimulatedHeartBeat:(BBBLEHRMDevice *)device;			// Simalated single heart rate beat (simulated using most resent data from the sensor)
- (void) BBBLEHRMDeviceCharacteristicUpdated:(BBBLEHRMDevice *)device characteristic:(BBBLEDeviceHRMCharacteristic)characteristic;	// New characteristic red from device. Useful with readDetailedData method

@end

@interface BBBLEHRMDevice : BBBLEDevice

@property (nonatomic) BBBLEHRMDeviceStatus status;							// Device status

@property (nonatomic) BBBLEHRMDeviceBodyContactStatus bodyContactStatus;	// Sensor to body contact status

@property (nonatomic) NSUInteger heartRate;		// Current heart rate. Updated on sensor's demand when connected (usually every 1 second)

@property (nonatomic) BBBLEHRMDeviceLocation deviceLocation;				// Device placement

	// Device object with known UUID and given restoration UID (effective only on iOS7 and later). If restoration is not required - pass nil
+ (BBBLEHRMDevice *) deviceWithUUID:(NSString *)UUID restorationIdentifier:(NSString *)restorationIdentifier;

	// Calculates current energy expedure (calories rate per 1 minute) for given congitions
- (float) currentEnergyExpedureForWieght:(float)weightKg age:(float)age isMale:(BOOL)isMale;

// Requests more detailed data from connected sensor (Manufacturer name, Firmware version, etc...)
- (void) readDetailedData;

+ (NSArray *)connectedDevices;

@end

