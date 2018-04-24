package com.jstakun.gms.android.ui;

import org.acra.ACRA;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

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
import android.widget.Toast;

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
                	String schemePart = data.getEncodedSchemeSpecificPart();
            		int length = data.getPathSegments() != null ? data.getPathSegments().size() : 0;
                	LoggerUtils.debug("Deep link: " + scheme + "://" + host);
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
                	} else if (schemePart != null) {
                		LoggerUtils.debug("Decoding: " + schemePart);
    					try {
                			String[] coords = StringUtils.split(schemePart,",");
                    		if (coords != null && coords.length == 2) {
                    			String latStr = coords[0];
                    			String lngStr = coords[1];
                    			if (StringUtils.contains(coords[1], ";")) {
                    				lngStr = StringUtils.split(coords[1], ";")[0];
                    			} else if (StringUtils.contains(coords[1], "?")) {
                    				lngStr = StringUtils.split(coords[1], "?")[0];
                    			}
                    			if (NumberUtils.isNumber(latStr) && NumberUtils.isNumber(lngStr)) {
                    				try {		
                    					LoggerUtils.debug("Decoded: " + latStr + " " + lngStr);
                    					lat = Double.parseDouble(latStr);
                    					lng = Double.parseDouble(lngStr);
                    				} catch (NumberFormatException e) {
                    					LoggerUtils.debug("Failed to decode: " + latStr + " " + lngStr);
                    				}
                    			} else {
                    				LoggerUtils.debug("Unable to decode: " + latStr + " " + lngStr);
                    				lat = lng = null;
                    			}
                    		}
                    		
                    		if (data.isHierarchical()) {
                    			String q = data.getQueryParameter("q");
                    			if (StringUtils.isNotEmpty(q)) {
                    				query = Uri.decode(q);
                    				//Toast.makeText(this, "Search query decoded: " + query, Toast.LENGTH_LONG).show();
                    			}
                    		} else {
                    			LoggerUtils.debug("This is non hierarchical uri " + schemePart);
                    			String[] decomposed = StringUtils.split(schemePart, "?");
                        		if (decomposed.length == 2) {
                        			String queryString = Uri.decode(decomposed[1]);
                        			String[] params = StringUtils.split(queryString, "&");
                        			for (int i=0;i<params.length;i++) {
                        				if (params[i].startsWith("q=")) {
                        					query = Uri.decode(params[i].substring(2));
                        					Toast.makeText(this, "Search query decoded: " + query + " from " + schemePart, Toast.LENGTH_LONG).show();
                        					break;
                        				}
                        			}
                        		}
                    		}
                		} catch (Throwable e) {
                			LoggerUtils.debug("Unable to decode geo " + schemePart);
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
                    if (lat != null && lat != 0d && lng != null && lng != 0d) {
                    	LoggerUtils.debug("Setting lat: " + lat + ", lng: " + lng);
                    	mapActivity.putExtra("lat", lat);
                    	mapActivity.putExtra("lng", lng);
                    } 
                	if (query != null) {
                		mapActivity.putExtra("query", query);
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
