package com.jstakun.gms.android.ui.deals;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.IntentsHelper;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.StringUtil;

import org.acra.ACRA;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class DealMapDispatchActivity extends Activity {

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {

        boolean abort = false;


        try {
            super.onCreate(icicle);
        } catch (Throwable t) {
            ACRA.getErrorReporter().handleSilentException(t);
            IntentsHelper.getInstance().setActivity(this);
            IntentsHelper.getInstance().showInfoToast("Sorry. Your device is currently unsupported :(");
            abort = true;
        }

        if (!abort) {
            final Intent intent = getIntent();
            final String action = intent.getAction();
            // If the intent is a request to create a shortcut, we'll do that and exit
            if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            	IntentsHelper.getInstance().setActivity(this);
            	IntentsHelper.getInstance().setupShortcut();
                abort = true;
            } else {
            	long lastStartupTime = ConfigurationManager.getInstance().getLong(ConfigurationManager.LAST_STARTING_DATE);
                LoggerUtils.debug("Last startup time is: " + DateTimeUtils.getDefaultDateTimeString(lastStartupTime, ConfigurationManager.getInstance().getCurrentLocale()));
                ConfigurationManager.getInstance().putLong(ConfigurationManager.LAST_STARTING_DATE, System.currentTimeMillis());
                
                Double lat = null, lng = null;
                Uri data = intent.getData();
                if (data != null) {
                	String scheme = data.getScheme(); 
                	String host = data.getHost();
                	LoggerUtils.debug("Deep link: " + scheme + "://" + host);
                	int length = data.getPathSegments().size();
                	if (length > 2) {
                		try {
                			String latSegment = data.getPathSegments().get(length-2);
                			lat = StringUtil.decode(latSegment);
                			String lngSegment = StringUtils.split(data.getPathSegments().get(length-1), ";")[0]; 
                			lng = StringUtil.decode(lngSegment);
                			LoggerUtils.debug("Decoded params " + latSegment + "," + lngSegment + " to " + lat + "," + lng);
                		} catch (Exception e) {
                			LoggerUtils.debug("Unable to decode " + data.getPathSegments().get(length-2) + "," + data.getPathSegments().get(length-1));
                		}
                	}
                }
                
                Intent mapActivity;
                if (OsUtil.isHoneycombOrHigher()) {
                   mapActivity = new Intent(this, DealMap2Activity.class); 
                } else {
                   mapActivity = new Intent(this, DealMapActivity.class);    
                }
                
                if (lat != null && lng != null) {
                	mapActivity.putExtra("lat", lat);
                	mapActivity.putExtra("lng", lng);
                }
                
                startActivity(mapActivity);
            }
        }
        
        if (abort) {
            finish();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        finish();
    }
}
