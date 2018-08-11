//
//  Constants.swift
//  HeartChart-iOS
//
//  Created by Larry Bulen on 8/11/18.
//  Copyright © 2018 Larry Bulen. All rights reserved.
//

import Foundation
import CoreGraphics

struct Constants {
    struct APIKEY {
        static let FacebookOauthKey = ""
        static let GoogleOauthKey = ""
    }

    struct FontSize {
        static let Button: CGFloat = 15
        static let Game: CGFloat = 18.0
        static let MainTitle: CGFloat = 40.0
        static let Menu: CGFloat = 20.0
        static let Title: CGFloat = 24.0
    }

    struct Limits {
        static let DefaultMove: Int = 15
        static let DefaultPlaceholder: Bool = false // "Clear"
        static let DefaultTime: Int = 5
        static let MaxMove: Float = 30.0
        static let MaxTime: Float = 30.0
        static let MinMove: Float = 5.0
        static let MinTime: Float = 1.0
    }
}
