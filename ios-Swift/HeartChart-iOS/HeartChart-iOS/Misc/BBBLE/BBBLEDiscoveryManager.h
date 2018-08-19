//
//  BBBLEDiscoveryManager.h
//
//  Copyright (c) 2012 Beets BLU Electronics. All rights reserved.
//

#import "BBBLEDevice.h"

typedef enum
	{
	BBBLEDiscoveryManagerStateUnknown,				// Status is unkown (not determined yet)
	BBBLEDiscoveryManagerStateBLEIsNotSupported,	// BLE is not supported by the device hardware
	BBBLEDiscoveryManagerStateUnauthorized,			// App is not authorized to use Bluetooth
	BBBLEDiscoveryManagerStateBLEIsPoweredOff,		// Bluetooth is powered off (Airplane mode?)
	BBBLEDiscoveryManagerStateResetting,			// Bluetooth is resetting
	BBBLEDiscoveryManagerStateDiscoveryIdle,		// Does not discovering
	BBBLEDiscoveryManagerStateDiscoveryInProgress	// Discovering in progress
	}
	BBBLEDiscoveryManagerState;

@class BBBLEDiscoveryData;
@class BBBLEDiscoveryManager;

@protocol BBBLEDiscoveryManagerDelegate <NSObject>

@optional

- (void)BBBLEDiscoveryManagerStarted:(BBBLEDiscoveryManager *)manager;			// Devices discovery started
- (void)BBBLEDiscoveryManagerFailed:(BBBLEDiscoveryManager *)manager;			// Devices discovery failed (no hardware support of Bluetooth disabled)
- (void)BBBLEDiscoveryManagerCancelled:(BBBLEDiscoveryManager *)manager;		// Devices discovery cancelled manually
- (void)BBBLEDiscoveryManagerDeviceDiscovered:(BBBLEDiscoveryManager *)manager deviceData:(BBBLEDiscoveryData *)deviceData;		// New device discovered
- (void)BBBLEDiscoveryManagerCompleted:(BBBLEDiscoveryManager *)manager devicesData:(NSArray *)devicesData;						// Discovery completed. Provides full list of discovered devices of type BBBLEDiscoveryData
- (void)BBBLEDiscoveryManagerProgressUpdated:(BBBLEDiscoveryManager *)manager;	// Discovery progress updated (ticks overy 0.1 of second during discovery)
- (void)BBBLEDiscoveryManagerStatusUpdated:(BBBLEDiscoveryManager *)manager;	// Status updated

@end

@interface BBBLEDiscoveryManager : NSObject 

@property (nonatomic, weak) id <BBBLEDiscoveryManagerDelegate> delegate;			// Delegate for BBBLEManagerDelegate protocol

@property (nonatomic) float discoveryProgress;				// Discovery progress 0.0-1.0 (ticks overy 0.1 of second during discovery)

@property (nonatomic) BBBLEDiscoveryManagerState state;		// Current manager state

@property (nonatomic) float discoveryTime;					// Maximum discovery time threshold, in seconds

@property (nonatomic) BOOL debugLogging;					// Do provide debug logging to NSLog

@property (nonatomic) CBCentralManagerState bluetoothState;	// Status of Bluetooth Smart subsystem

- (id) initWithDelegate:(id)delegate;						// Init manager with delegate

- (void) discover;											// Start devices discovery
- (void) stopDevicesDiscovery;								// Manually stop devices discovery (graceful)
- (void) cancelDevicesDiscovery;							// Manually cancel devices discovery (forced)

@end
