//
//  BBBLE.h
//
//  Copyright (c) 2012 Beets BLU Electronics. All rights reserved.
//

#import <CoreBluetooth/CoreBluetooth.h>

#import "BBBLEDiscoveryData.h"			// Bleutooth Smart advertizing data
#import "BBBLEHRMDiscoveryManager.h"	// HRM Discovery Manager class (child of BBBLEDiscoveryManager)
#import "BBBLEBSCDiscoveryManager.h"	// Bike Speed/Cadance sensor Discovery Manager class (child of BBBLEDiscoveryManager)
#import "BBBLEWSDiscoveryManager.h"		// Weight Scale Discovery Manager class (child of BBBLEDiscoveryManager) (Low level access only)
#import "BBBLEHRMDevice.h"				// HRM device class
#import "BBBLEBSCDevice.h"				// Bike SC device class
#import "BBBLEWSDevice.h"				// Weight scale class (Low level access only)
#import "BBBLEWSSimpleCollector.h"		// Weight scale Simple Collector Class. RECOMMENDED
