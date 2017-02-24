//
//  BBBLEWSSimpleCollector.h
//  libBeetsBLU
//
//  Created by a on 06.10.13.
//  Copyright (c) 2013 Beets BLU. All rights reserved.
//

#import <Foundation/Foundation.h>

typedef enum {
	BBBLEWSSimpleCollectorStatusUnknown,					// Status Unknown (not determined yet)
	BBBLEWSSimpleCollectorStatusBLEIsNotSupported,			// BLE is not supported by device hardware
	BBBLEWSSimpleCollectorStatusBLEIsUnauthorized,			// Application is not authorized to use Bluetooth (In privacy settings?)
	BBBLEWSSimpleCollectorStatusNotConnected,				// Device is not connected
	BBBLEWSSimpleCollectorStatusConnected					// Connected and waiting for data
} BBBLEWSSimpleCollectorStatus;

@class BBBLEWSSimpleCollector;

@protocol BBBLEWSSimpleCollectorDelegate <NSObject>

@optional

- (void) BBBLEWSSimpleCollectorUpdatedStatus:(BBBLEWSSimpleCollectorStatus)status;	// Status is updated
- (void) BBBLEWSSimpleCollectorGotDeviceUUID:(NSString *)uuid;						// UUID can be used for later non-wildcard connections
- (void) BBBLEWSSimpleCollectorGotMeasurement:(float)weightKilogramms				// Measured weight in kg
								fatPercents:(float)fatPercents						// Measured body fat in % (only if profile data has been provided, otherwise = 0.0)
							   musclePercents:(float)musclePercents			// Measured body muscle mass in % (only if profile data has been provided, otherwise = 0.0)
								waterPercents:(float)waterPercents			// Measured body water in % (only if profile data has been provided, otherwise = 0.0)
								 bonePercents:(float)bonePercents			// Measured body bone mass in % (only if profile data has been provided, otherwise = 0.0)
									 bmiIndex:(float)bmiIndex				// BMI (only if profile data has been provided, otherwise = 0.0)
						 dailyCalorieRequired:(float)dailyCalorieRequired;	// Calories required daily (only if profile data has been provided, otherwise = 0.0)

@end

@interface BBBLEWSSimpleCollector : NSObject

+ (void) updateCollectorSettings:(id)delegate		// Delegate for posting data
		uuid:(NSString *)uuid						// UUID of the scale, pass nil for wildcard connection
		debugLogging:(BOOL)debugLogging				// Provide debug logging
		enableBackground:(BOOL)enableBackground		// Read data even in the background (Experemental!)
		// Optional profile data:
		age:(float)age									// Age of the user in years. Pass 0.0 if unknown (preventing additional measurements)
		heightMeters:(float)heightMeters				// Height of the user in meters. Pass 0.0 if unknown (preventing additional measurements)
		sexIsWoman:(BOOL)sexIsWoman						// Is the user - woman ?
		athleticComposition:(BOOL)athleticComposition;	// Has the user athletic body composition?

+ (void) pauseCollecting;		// Pause collecting data or discovering device
+ (void) resumeCollecting;		// Resume collecting data or discovering device
+ (BBBLEWSSimpleCollectorStatus) getStatus;	// Get current collector status
+ (NSString *) getCurrentDeviceUUID;	// Get current device UUID. May return nil or there is not device or device UUID is unknown


@end
