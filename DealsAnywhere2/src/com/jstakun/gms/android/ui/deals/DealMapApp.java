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
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.StringUtil;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.bouncycastle.util.encoders.Base64;

/**
 *
 * @author jstakun
 */
@ReportsCrashes(formKey = "",
formUri = ConfigurationManager.CRASH_REPORT_URL,
mode = ReportingInteractionMode.TOAST,
resToastText = R.string.Crash_error)
public class DealMapApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LoggerUtils.debug("DealsAnywhereApp.onCreate...");
        ACRA.init(this);
        Map<String, String> headers = new HashMap<String, String>();
        byte[] userpassword = StringUtil.concat((Commons.DA_APP_USER + ":").getBytes(), Base64.decode(Commons.APP_USER_PWD));
		String encodedAuthorization = new String(Base64.encode(userpassword));
        headers.put("Authorization", "Basic " + encodedAuthorization);
        ACRA.getConfig().setHttpHeaders(headers);
        ConfigurationManager.getInstance().initApp(getApplicationContext());
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
