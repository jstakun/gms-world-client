package com.jstakun.gms.android.ui;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Context;

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
resToastText = R.string.Crash_error,
socketTimeout = 30000, 
formKey = "")
public class GMSClientApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LoggerUtils.debug("GMSClientApp.onCreate...");
        //TODO use initAcra()
        ACRA.init(this);  
        ConfigurationManager.getAppUtils().initApp(this);  
        UserTracker.getInstance().initialize(this);
        UserTracker.getInstance().setDryRun(false, this);
    }
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ACRA.init(this);
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
    
    /*private void initAcra(Map<String, String> headers) {
    try {
        ACRAConfiguration config = new ConfigurationBuilder(this)
                .setFormUri("https://www.gms-world.net/crashReport")
                .setMode(ReportingInteractionMode.TOAST)
                .setHttpHeaders(headers)
                .setResToastText(R.string.Crash_error)
                .setSocketTimeout(30000)
                .build();
        ACRA.init(this, config);
    } catch (Exception e) {
        Log.e(TAG, e.getMessage(), e);
    }*/
}
