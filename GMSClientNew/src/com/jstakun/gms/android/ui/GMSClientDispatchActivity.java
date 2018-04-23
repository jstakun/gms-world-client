package com.jstakun.gms.android.ui;

import org.acra.ACRA;
import org.apache.commons.lang.StringUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.StringUtil;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
                
                Double lat = null, lng = null;
                String query = null;
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
                			String lngSegment = StringUtils.split(data.getLastPathSegment(), ";")[0]; 
                			lng = StringUtil.decode(lngSegment);
                			LoggerUtils.debug("Decoded params " + latSegment + "," + lngSegment + " to " + lat + "," + lng);
                		} catch (Exception e) {
                			LoggerUtils.debug("Unable to decode " + data.getPathSegments().get(length-2) + "," + data.getLastPathSegment());
                		}
                	} else {
                		try {
                			String[] coords = data.getEncodedSchemeSpecificPart().split(",");
                    		if (coords.length == 2) {
                    			lat = Double.valueOf(coords[0].trim());
                				lng = Double.valueOf(coords[1].trim());
                    		}
                		} catch (Exception e) {
                			LoggerUtils.debug("Unable to decode geo:" + data.getEncodedSchemeSpecificPart());
                		}
                	}
                }
                
            	Intent mapActivity;
                if (OsUtil.isHoneycombOrHigher()) {
                    if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext()) == ConnectionResult.SUCCESS &&
                    		ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER) == ConfigurationManager.GOOGLE_MAPS) {
                        mapActivity = new Intent(this, GMSClient3MainActivity.class);
                    } else {
                        ConfigurationManager.getInstance().putInteger(ConfigurationManager.MAP_PROVIDER, ConfigurationManager.OSM_MAPS);
                        mapActivity = new Intent(this, GMSClient2OSMMainActivity.class);
                    }
                    if (lat != null && lng != null) {
                    	mapActivity.putExtra("lat", lat);
                    	mapActivity.putExtra("lng", lng);
                    	try {
                    		String q = data.getQueryParameter("q");
                    		if (StringUtils.isNotEmpty(q)) {
                    			query = Uri.decode(q);
                    		}
                    	} catch (Exception e) {
                    		LoggerUtils.debug("Unable to decode query " + data.getQueryParameter("q"));
                    	}
                    	if (query != null) {
                    		mapActivity.putExtra("query", query);
                    	}
                    }
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
