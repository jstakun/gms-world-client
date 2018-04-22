package com.jstakun.gms.android.ui.deals;

import org.acra.ACRA;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.IntentsHelper;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

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
                
                Intent mapActivity = new Intent(this, DealMap3Activity.class); 
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
