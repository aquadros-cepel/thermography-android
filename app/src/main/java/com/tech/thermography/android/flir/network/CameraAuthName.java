/*******************************************************************
 * @title FLIR Atlas Android SDK
 * @file CameraAuthName.java
 * @Author Teledyne FLIR
 *
 * @brief Create a application name that will be persistent between re-install of the application,
 * will be changed if the application is uninstalled and then re-installed
 *
 * It's a work-around a bug in the camera, the camera can't handle different authorization information with the same "name"
 *
 * Copyright 2023:    Teledyne FLIR
 *******************************************************************/

package com.tech.thermography.android.flir.network;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class CameraAuthName {

    public static final String KEY = "com.samples.networkcamera.Application_ID";
    public static final String PREFS_FILE = "flir_camera_auth_prefs";
    public static final String BaseName = "NetworkCamera";

    /**
     * Return a "persistent" name using Context (ViewModel-friendly).
     */
    public static String getApplicationName(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        String name;
        if (sharedPref.contains(KEY)) {
            name = sharedPref.getString(KEY, "");
        } else {
            name = BaseName + (System.currentTimeMillis() % 10000);
            editor.putString(KEY, name);
            editor.commit();
        }
        return name;
    }

    /**
     * Overload for backward compatibility with Activity.
     */
    public static String getApplicationName(Activity activity) {
        return getApplicationName((Context) activity);
    }
}
