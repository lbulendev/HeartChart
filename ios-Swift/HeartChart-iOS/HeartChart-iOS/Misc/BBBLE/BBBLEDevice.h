//
//  BBBLEDevice.h
//
//  Copyright (c) 2012 Beets BLU Electronics. All rights reserved.
//

#include <CoreBluetooth/CoreBluetooth.h>

typedef enum
	{
	BBBLEDeviceTypeUnknown,	// Unknown device
	BBBLEDeviceTypeHRM,		// Heart rate monitor
	BBBLEDeviceTypeBSC,		// Bike Speed Cadence sensor
	BBBLEDeviceTypeWS,		// Weight scale
	BBBLEDeviceTypeKeyFob,	// Key fob
	BBBLEDeviceTypeSoftFob	// Soft fob
	}
	BBBLEDeviceType;

@class BBBLEDiscoveryData;

@interface BBBLEDevice : NSObject

@property (weak, nonatomic, getter = deviceUUID, readonly) NSString *deviceUUID;		// Device UUID (May be nil until connect)

@property (nonatomic, strong) id delegate;				// Delegate for the appropriate device protocol (See device class)

@property (nonatomic) BOOL debugLogging;				// Provide debug logging to NSLog

@property (nonatomic) double autoReconnectPeriod;		// Automatic reconnect period in seconds in case of unexpected disconnection, 0 - to disable auto reconnect.

@property (nonatomic) float deviceConnectTimeout;			// Device connection timeout

@property (nonatomic, strong) NSDate *connectDate;			// Recent connection date


@property (nonatomic, strong) NSString *name;				// Device name (may be custom - will be stored into NSUserDefaults). If custom name is not present - equals to hardcodedName
@property (nonatomic, strong) NSString *hardcodedName;		// Device hardcoded name (Read on demand, but usually available immediately because cached in NSUserDefaults)
@property (nonatomic, strong) NSString *manufacturer;		// Device manufacturer (Read on demand - see readDetailedData)
@property (nonatomic, strong) NSString *model;				// Device model  (Read on demand - see readDetailedData
@property (nonatomic, strong) NSString *serial;				// Device serial number  (Read on demand - see readDetailedData)
@property (nonatomic, strong) NSString *hardwareRevision;	// Device hardware revision (Read on demand - see readDetailedData)
@property (nonatomic, strong) NSString *firmwareRevision;	// Device firmware revision (Read on demand - see readDetailedData)
@property (nonatomic, strong) NSString *softwareRevision;	// Device software revision (Read on demand - see readDetailedData)
@property (nonatomic) float signalLevel;					// Current signal level from 0.0 to 1.0. May be broken on iOS 8
@property (nonatomic) int batteryLevel;						// Battery level in percents (%) or -1 if no battery service present
@property (nonatomic, strong) NSDictionary *connectionOptions;	// For available options see method connectPeripheral:options: of CBPeripheral. Default - nil

- (void) connect;											// Connect device
- (void) disconnect;										// Disconnect device

+ (NSString *) deviceNameForUUID:(NSString *)uuid;			// Returns: 1) custom OR 2) hardcoded name, for given UUID
+ (BBBLEDevice *) existingDeviceForUUID:(NSString *)uuid;	// Returns existing device with given UUID or nil if no device

@end
