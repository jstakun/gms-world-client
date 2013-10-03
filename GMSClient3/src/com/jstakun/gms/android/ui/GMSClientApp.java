/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import java.util.HashMap;
import java.util.Map;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.LoggerUtils;


/**
 *
 * @author jstakun
 */
@ReportsCrashes(formKey = "", 
formUri = ConfigurationManager.CRASH_REPORT_URL,
mode = ReportingInteractionMode.TOAST,
resToastText = R.string.Crash_error,
socketTimeout = 30000)
public class GMSClientApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LoggerUtils.debug("GMSClientApp.onCreate...");
        ACRA.init(this);  
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", HttpUtils.getBasicAuthHeader(Commons.GMS_APP_USER, true, Commons.APP_USER_PWD, true));
        ACRA.getConfig().setHttpHeaders(headers);
        ConfigurationManager.getAppUtils().initApp(getApplicationContext());    
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
