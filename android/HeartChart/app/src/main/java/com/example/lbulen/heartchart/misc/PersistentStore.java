package com.example.lbulen.heartchart.misc;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by lbulen on 8/12/18.
 */

public class PersistentStore {
    // Preference keys:
    private static final String HEARTCHART_PREFS = "HeartChartPrefs";
    private static final String HEARTCHART_FACEBOOKUSERAUTH = "facebookUserAuth";
    private static final String HEARTCHART_FACEBOOKUSERTOKEN = "facebookUserToken";
    private static final String HEARTCHART_GOOGLEUSERAUTH = "googleUserAuth";
    private static final String HEARTCHART_GOOGLEUSERTOKEN = "googleUserToken";

    private SharedPreferences mSharedPreferences;

    public PersistentStore(Context context) {
        mSharedPreferences = context.getApplicationContext().getSharedPreferences(HEARTCHART_PREFS, Context.MODE_PRIVATE);
    }

    public synchronized boolean getFacebookUserAuth() {
        return mSharedPreferences.getBoolean(HEARTCHART_FACEBOOKUSERAUTH, false);
    }
    public synchronized void setFacebookUserAuth(boolean auth) {
        mSharedPreferences.edit().putBoolean(HEARTCHART_FACEBOOKUSERAUTH, auth).apply();
    }

    public synchronized String getFacebookUserToken() {
        return mSharedPreferences.getString(HEARTCHART_FACEBOOKUSERTOKEN, null);
    }

    public synchronized void setFacebookUserToken(String token) {
        mSharedPreferences.edit().putString(HEARTCHART_FACEBOOKUSERTOKEN, token).apply();
    }

    public synchronized boolean getGoogleUserAuth() {
        return mSharedPreferences.getBoolean(HEARTCHART_GOOGLEUSERAUTH, false);
    }

    public synchronized void setGoogleUserAuth(boolean auth) {
        mSharedPreferences.edit().putBoolean(HEARTCHART_GOOGLEUSERAUTH, auth).apply();
    }

    public synchronized String getGoogleUserToken() {
        return mSharedPreferences.getString(HEARTCHART_GOOGLEUSERTOKEN, null);
    }

    public synchronized void setGoogleUserToken(String token) {
        mSharedPreferences.edit().putString(HEARTCHART_GOOGLEUSERTOKEN, token).apply();
    }
}
