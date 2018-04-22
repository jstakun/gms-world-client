package com.jstakun.gms.android.ui.deals;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.UserTracker;

/**
 *
 * @author jstakun
 */
@ReportsCrashes(
formUri = ConfigurationManager.CRASH_REPORT_URL,
mode = ReportingInteractionMode.TOAST,
socketTimeout = 30000,
resToastText = R.string.Crash_error, formKey = "")
public class DealMapApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LoggerUtils.debug("DealsAnywhereApp.onCreate...");
        ACRA.init(this);
        ConfigurationManager.getAppUtils().initApp(this);
        UserTracker.getInstance().initialize(this);
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
