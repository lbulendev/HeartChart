//
//  PersistentStore.swift
//  HeartChart-iOS
//
//  Created by Larry Bulen on 8/11/18.
//  Copyright © 2018 Larry Bulen. All rights reserved.
//

import Foundation
class PersistentStore {
    // Preference keys:
    private let FACEBOOKUSERAUTH: String = "facebookUserAuth"
    private let FACEBOOKUSERTOKEN: String = "facebookUserToken"
    private let GOOGLEUSERAUTH: String = "googleUserAuth"
    private let GOOGLEUSERTOKEN: String = "googleUserToken"

    private var userDefaults: UserDefaults
    
    init() {
        userDefaults = UserDefaults()
    }

    func getFacebookUserAuth() -> Bool {
        return userDefaults.bool(forKey: FACEBOOKUSERAUTH)
    }

    func setFacebookUserAuth(auth: Bool) -> Void {
        userDefaults.set(auth, forKey: FACEBOOKUSERAUTH)
        userDefaults.synchronize()
    }

    func getFacebookUserToken() -> String {
        if (userDefaults.object(forKey: FACEBOOKUSERTOKEN) == nil) {
            return ""
        }
        return userDefaults.string(forKey: FACEBOOKUSERTOKEN)!
    }

    func setFacebookUserToken(token: String) -> Void {
        userDefaults.set(token, forKey: FACEBOOKUSERTOKEN)
        userDefaults.synchronize()
    }

    func getGoogleUserAuth() -> Bool {
        return userDefaults.bool(forKey: GOOGLEUSERAUTH)
    }
    
    func setGoogleUserAuth(auth: Bool) -> Void {
        userDefaults.set(auth, forKey: GOOGLEUSERAUTH)
        userDefaults.synchronize()
    }

    func getGoogleUserToken() -> String {
        if (userDefaults.object(forKey: GOOGLEUSERTOKEN) == nil) {
            return ""
        }
        return userDefaults.string(forKey: GOOGLEUSERTOKEN)!
    }

    func setGoogleUserToken(token: String) -> Void {
        userDefaults.set(token, forKey: GOOGLEUSERTOKEN)
        userDefaults.synchronize()
    }
}
