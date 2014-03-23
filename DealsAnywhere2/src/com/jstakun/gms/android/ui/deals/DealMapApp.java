/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.ui.deals;

import java.util.HashMap;
import java.util.Map;

import android.app.Application;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.UserTracker;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 *
 * @author jstakun
 */
@ReportsCrashes(formKey = "",
formUri = ConfigurationManager.CRASH_REPORT_URL,
mode = ReportingInteractionMode.TOAST,
socketTimeout = 30000,
resToastText = R.string.Crash_error)
public class DealMapApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LoggerUtils.debug("DealsAnywhereApp.onCreate...");
        ACRA.init(this);
        ConfigurationManager.getAppUtils().initApp(this);
        UserTracker.getInstance().initialize(getApplicationContext());
        //comment in production
        //UserTracker.getInstance().setDebug(true, this);
        //
        UserTracker.getInstance().setDryRun(false, this);
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        LoggerUtils.debug("DealsAnywhereApp.onTerminate...");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LoggerUtils.debug("DealsAnywhereApp.onLowMemory...");
        System.gc();
    }
}
