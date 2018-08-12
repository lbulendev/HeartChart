//
//  MainViewController.swift
//  HeartChart-iOS
//
//  Created by Larry Bulen on 8/11/18.
//  Copyright © 2018 Larry Bulen. All rights reserved.
//

import Foundation
import GoogleSignIn

class MainViewController: UIViewController, GIDSignInDelegate, GIDSignInUIDelegate {
    
    @IBOutlet weak var googleSignInButton: GIDSignInButton!
    @IBOutlet weak var facebookSignInButton: UIButton!

    @IBOutlet weak var oauthLabel: UILabel!
    let persistentStore = PersistentStore()

    override func viewDidLoad() {
        super.viewDidLoad()
        
        GIDSignIn.sharedInstance().uiDelegate = self
        GIDSignIn.sharedInstance().delegate = self
        
        // Uncomment to automatically sign in the user.
        //GIDSignIn.sharedInstance().signInSilently()
        
        // TODO(developer) Configure the sign-in button look/feel
        // ...
    }

    @IBAction func didTapSignOut(_ sender: AnyObject) {
        GIDSignIn.sharedInstance().signOut()
    }

    // GIDSignInDelegate - delegate method
    func sign(_ signIn: GIDSignIn!, didSignInFor user: GIDGoogleUser!, withError error: Error!) {
        print("Signed in!")
        print(user.authentication.idToken!)
        persistentStore.setGoogleUserToken(token: user.authentication.idToken!)
        persistentStore.setGoogleUserAuth(auth: true)
        print(persistentStore.getGoogleUserToken())
        
//        if (GIDSignIn.sharedInstance().hasAuthInKeychain()) {
            oauthLabel.text = "token: " + persistentStore.getGoogleUserToken()
//        }
    }
}
