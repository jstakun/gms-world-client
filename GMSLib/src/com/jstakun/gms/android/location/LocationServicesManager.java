package com.jstakun.gms.android.location;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import org.osmdroid.api.IMyLocationOverlay;
import org.osmdroid.util.GeoPoint;

/**
 *
 * @author jstakun
 */
public class LocationServicesManager {

	public static final int UPDATE_LOCATION = 21;
	
    private SkyhookUtils skyhook;
    private IMyLocationOverlay myLocation;
    private boolean isGpsHardwarePresent = false;
    
    public static final LocationServicesManager instance = new LocationServicesManager();
	
	private LocationServicesManager() {
		
	}
	
	public static LocationServicesManager getInstance() {
		return instance;
	}
    
    public void initLocationServicesManager(Context context, Handler locationHandler, IMyLocationOverlay imyLocation) {
        if (isGpsHardwarePresent(context)) {
            LoggerUtils.debug("GPS is present !!!");
            isGpsHardwarePresent = true;
            if (imyLocation != null) {
            	myLocation = imyLocation;
            } 
        } else {
            LoggerUtils.debug("GPS is missing. Using Skyhook !!!");
            isGpsHardwarePresent = false;
            skyhook = new SkyhookUtils(context, locationHandler);
        }
    }

    public boolean isGpsHardwarePresent(Context context) {
    	try {
            return HelperInternal.isGpsHardwarePresent(context);
        } catch (VerifyError e) {
            return true;
        }
    }
    
    public boolean isGpsHardwarePresent() {
        return isGpsHardwarePresent;
    }

    public void enableMyLocation() {
        if (isGpsHardwarePresent) {
        	if (myLocation != null) {
        		myLocation.enableMyLocation();
        		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
        			myLocation.enableCompass();
        		}		
        	} 
        } else {
            skyhook.enableMyLocation();
        }
    }
    
    public void disableMyLocation() {
    	if (isGpsHardwarePresent) {
    		if (myLocation != null) {
    			myLocation.disableCompass();
    			myLocation.disableMyLocation();
            } 
        } else if (skyhook != null) {
            skyhook.disableMyLocation();
        }
    }

    public void runOnFirstFix(Runnable r) {
        if (isGpsHardwarePresent && myLocation != null) {
            myLocation.runOnFirstFix(r);
        } else {
            skyhook.runOnFirstFix(r);
        }
    }

    public void enableCompass() {
        if (isGpsHardwarePresent && myLocation != null) {
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
                myLocation.enableCompass();
            } else {
                myLocation.disableCompass();
            }
        }
    }

    public GeoPoint getMyLocation() {
        Location location = ConfigurationManager.getInstance().getLocation();
        if (location == null && myLocation != null) {
            location = myLocation.getLastFix();
        }
        if (location != null) {
            return new GeoPoint(MathUtils.coordDoubleToInt(location.getLatitude()), MathUtils.coordDoubleToInt(location.getLongitude()));
        } else {
            return null;
        }
    }

    private static class HelperInternal {

        public static boolean isGpsHardwarePresent(Context context) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        }
    }
}
