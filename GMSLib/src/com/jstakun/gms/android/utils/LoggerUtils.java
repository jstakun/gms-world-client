/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import android.util.Log;

import org.acra.ACRA;

import com.jstakun.gms.android.config.ConfigurationManager;

/**
 *
 * @author jstakun
 */
public class LoggerUtils {

    private static final String TAG = "LandmarkManager";

    //public static void initialize() {
        //set log level
        
    //}

    //public static void close() {
        
    //}

    public static void debug(String msg) {
        //System.out.println(msg);
        if (isDebug()) {
            Log.d(TAG, msg);
        }
    }

    public static void error(String msg) {
        //System.out.println(msg);
        if (isError()) {
            Log.e(TAG, msg);
        }
    }

    public static void debug(String msg, Throwable t) {
        //System.out.println(msg);
        //t.printStackTrace();
        if (isDebug()) {
            Log.d(TAG, msg, t);
        }
    }

    public static void error(String msg, Throwable t) {
        //System.out.println(msg);
        //t.printStackTrace();
        if (isError()) {
            Log.e(TAG, msg, t);
        }
        //ifDebug() send crash report
        if (isDebug()) {
            ACRA.getErrorReporter().handleSilentException(t);
        }
    }
    
    private static boolean isError()
    {
        return (ConfigurationManager.getInstance().getInt(ConfigurationManager.LOG_LEVEL, 3) <= 3);
    }

    private static boolean isDebug()
    {
        return (ConfigurationManager.getInstance().getInt(ConfigurationManager.LOG_LEVEL, 3) == 0);
    }
}
