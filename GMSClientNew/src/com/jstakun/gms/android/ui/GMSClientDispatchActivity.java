package com.jstakun.gms.android.ui;

import org.acra.ACRA;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.OsUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 *
 * @author jstakun
 */
public class GMSClientDispatchActivity extends Activity {

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {

        boolean abort = false;

        try {
            //handle java.lang.NoClassDefFoundError
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
                
            	Intent mapActivity;
                if (OsUtil.isHoneycombOrHigher()) {
                    if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext()) == ConnectionResult.SUCCESS &&
                    		ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER) == ConfigurationManager.GOOGLE_MAPS) {
                        mapActivity = new Intent(this, GMSClient3MainActivity.class);
                    } else {
                        ConfigurationManager.getInstance().putInteger(ConfigurationManager.MAP_PROVIDER, ConfigurationManager.OSM_MAPS);
                        mapActivity = new Intent(this, GMSClient2OSMMainActivity.class);
                    }
                    IntentsHelper.getInstance().setActivity(this);
                    IntentsHelper.getInstance().parseIntentData(intent, mapActivity);
                    startActivity(mapActivity);
                } else {
                	IntentsHelper.getInstance().showInfoToast("This application requires Android 3.0 or higher to run!");
                }
                
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
