/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Application;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.LoggerUtils;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 *
 * @author jstakun
 */
@ReportsCrashes(formKey = "", 
formUri = ConfigurationManager.CRASH_REPORT_URL,
formUriBasicAuthLogin = Commons.GMS_APP_USER,
formUriBasicAuthPassword = Commons.APP_USER_PWD,
mode = ReportingInteractionMode.TOAST,
resToastText = R.string.Crash_error)
public class GMSClientApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LoggerUtils.debug("GMSClientApp.onCreate...");
        ACRA.init(this);      
        ConfigurationManager.getInstance().putString(ConfigurationManager.GA_ID, Commons.DEFAULT_GA_ID);
        ConfigurationManager.getInstance().initApp(getApplicationContext());    
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        LoggerUtils.debug("GMSClientApp.onTerminate...");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LoggerUtils.debug("GMSClientApp.onLowMemory...");
        System.gc();
    }
}
