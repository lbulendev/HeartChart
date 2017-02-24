//
//  BBBLEDiscoveryData.h
//
//  Copyright (c) 2012 Beets BLU Electronics. All rights reserved.
//

#import "BBBLEDevice.h"

@interface BBBLEDiscoveryData : NSObject

@property (nonatomic, strong) NSString *hardcodedName;	// Discovered device hardcoded name
@property (nonatomic, readonly) NSString *name;			// Device name (may be custom - will be stored into NSUserDefaults). If custom name is not present - equals to hardcodedName
@property (nonatomic) float signalLevel;				// Discovered current signal level 0.0 - 100.0 (%%)
@property (nonatomic) BOOL isConnectable;				// Is device ready to accept connections? (iOS7 and later, otherwise - always YES)
@property (nonatomic, strong) NSDate *timestamp;		// Discovery timestamp
@property (nonatomic, strong) NSString *UUID;			// Device UUID

@end
