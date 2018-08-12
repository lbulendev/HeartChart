//
//  MainViewController.swift
//  HeartChart-iOS
//
//  Created by Larry Bulen on 8/11/18.
//  Copyright © 2018 Larry Bulen. All rights reserved.
//

import Foundation
import FBSDKCoreKit
import FBSDKLoginKit
import GoogleSignIn

class MainViewController: UIViewController, FBSDKLoginButtonDelegate, GIDSignInDelegate, GIDSignInUIDelegate {
    
    @IBOutlet weak var googleSignInButton: GIDSignInButton!
    @IBOutlet weak var facebookSignInButton: FBSDKLoginButton!

    @IBOutlet weak var oauthLabel: UILabel!
    let persistentStore = PersistentStore()

    override func viewDidLoad() {
        super.viewDidLoad()
        GIDSignIn.sharedInstance().uiDelegate = self
        GIDSignIn.sharedInstance().delegate = self
        facebookSignInButton.delegate = self
        if (persistentStore.getGoogleUserAuth()) {
            googleSignInButton.isEnabled = false
            facebookSignInButton.isEnabled = false
            oauthLabel.text = persistentStore.getGoogleUserToken()
        } else if (persistentStore.getFacebookUserAuth()) {
            googleSignInButton.isEnabled = false
            facebookSignInButton.isEnabled = false
            oauthLabel.text = persistentStore.getFacebookUserToken()
        }
    }

    @IBAction func didTapSignOut(_ sender: AnyObject) {
        GIDSignIn.sharedInstance().signOut()
    }

    // GIDSignInDelegate - delegate method
    func sign(_ signIn: GIDSignIn!, didSignInFor user: GIDGoogleUser!, withError error: Error!) {
        print("Signed in!")
        print(user.authentication.idToken!)
        let idToken: String = user.authentication.idToken!
        if (idToken != persistentStore.getGoogleUserToken()) {
            persistentStore.setGoogleUserToken(token: user.authentication.idToken!)
            persistentStore.setGoogleUserAuth(auth: true)
            print(persistentStore.getGoogleUserToken())
        }
        oauthLabel.text = "Google token: " + persistentStore.getGoogleUserToken()
    }

    // FBSDKLoginButtonDelegate - delegate methods
    func loginButton(_ loginButton: FBSDKLoginButton!, didCompleteWith result: FBSDKLoginManagerLoginResult!, error: Error!) {
        if (error == nil) {
            persistentStore.setFacebookUserAuth(auth: true)
            let fbToken: String = FBSDKAccessToken.current().tokenString!
            persistentStore.setFacebookUserToken(token: fbToken)
            oauthLabel.text = "FB token: " + fbToken
        }
    }
    
    func loginButtonDidLogOut(_ loginButton: FBSDKLoginButton!) {
        print("FB Logout!")
    }
    
}
