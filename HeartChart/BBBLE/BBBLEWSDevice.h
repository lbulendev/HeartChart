//
//  BBBLEWSDevice.h
//
//  Copyright (c) 2012 Beets BLU Electronics. All rights reserved.
//

#import "BBBLEDevice.h"

#define	kBodyValuesFatKey		@"fat_key"
#define	kBodyValuesWaterKey		@"water_key"
#define	kBodyValuesMuscleKey	@"muscle_key"
#define	kBodyValuesBoneKey		@"bone_key"
#define	kBodyValuesBMIKey		@"bmi_key"
#define	kBodyValuesCalorieKey	@"calorie_key"


typedef enum
	{
	BBBLEWSDeviceStatusUnknown,						// Status Unknown (not determined yet)
	BBBLEWSDeviceStatusBLEIsNotSupported,			// BLE is not supported by device hardware
	BBBLEWSDeviceStatusBLEIsPoweredOff,				// Bluetooth is powered off (Airplane mode?)
	BBBLEWSDeviceStatusBLEIsUnauthorized,			// Application is not authorized to use Bluetooth (In privacy settings?)
	BBBLEWSDeviceStatusBLEIsPending,				// Bluetooth in pending state (unkown or resetting)
	BBBLEWSDeviceStatusSearching,					// Searching for device
	BBBLEWSDeviceStatusNotFound,					// Device not found
	BBBLEWSDeviceStatusNotConnected,				// Device is not connected
	BBBLEWSDeviceStatusConnecting,					// Connecting and reading device data
	BBBLEWSDeviceStatusConnected,					// Connected and sampling data
	BBBLEWSDeviceStatusReconnecting					// Auto reconnecting
	}
	BBBLEWSDeviceStatus;


typedef enum
	{
	BBBLEDeviceWSCharacteristicDeviceName,
	BBBLEDeviceWSCharacteristicSerial,
	BBBLEDeviceWSCharacteristicModelName,
	BBBLEDeviceWSCharacteristicManufacturerName,
	BBBLEDeviceWSCharacteristicHardwareRevision,
	BBBLEDeviceWSCharacteristicFirmwareRevision,
	BBBLEDeviceWSCharacteristicSoftwareRevision,
	BBBLEDeviceWSCharacteristicTxPower,
	BBBLEDeviceWSCharacteristicBatteryLevel,
	BBBLEDeviceWSCharacteristicFeature,
	BBBLEDeviceWSCharacteristicMeasurement
	}
	BBBLEDeviceWSCharacteristic;

@class BBBLEWSDevice;

@protocol BBBLEWSDeviceDelegate <NSObject>

@required

@optional

- (void) BBBLEWSDeviceStatusUpdated:(BBBLEWSDevice *)device;																	// Device status changed
- (void) BBBLEWSDeviceCharacteristicUpdated:(BBBLEWSDevice *)device characteristic:(BBBLEDeviceWSCharacteristic)characteristic;	// New characteristic red from device. Useful with readDetailedData method
- (void) BBBLEWSDeviceGotMeasurement:(BBBLEWSDevice *)device weightKg:(float)weightKg impedance50kHzOhm:(float)impedance50kHzOhm date:(NSDate *)date;	// New measurement reading downloaded

@end

@interface BBBLEWSDevice : BBBLEDevice

@property (nonatomic) BBBLEWSDeviceStatus status;							// Device status

	// Device object with known UUID and given restoration UID (effective only on iOS7 and later). If restoration is not required - pass nil
+ (BBBLEWSDevice *) deviceWithUUID:(NSString *)UUID restorationIdentifier:(NSString *)restorationIdentifier;

	// Requests more detailed data from connected sensor (Manufacturer name, Firmware version, etc...)
- (void) readDetailedData;

	// Currently connected devices of this type
+ (NSArray *)connectedDevices;

	// Calculate additional measurement data useing user's profile
+ (NSDictionary *)bodyValuesWithImpedance:(float)impedance isMale:(BOOL)isMale isAthletic:(BOOL)isAthletic weightKg:(float)weightKg heightM:(float)heightM ageYears:(float)ageYears;

@end
