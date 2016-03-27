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
    private static SkyhookUtils skyhook;
    private static IMyLocationOverlay myLocation;
    private static boolean isGpsHardwarePresent = false;

    public static void initLocationServicesManager(Context context, Handler locationHandler, IMyLocationOverlay imyLocation) {
        if (imyLocation != null && isGpsHardwarePresent(context)) {
            LoggerUtils.debug("GPS is present !!!");
            isGpsHardwarePresent = true;
            myLocation = imyLocation;
        } else {
            LoggerUtils.debug("GPS is missing. Using Skyhook !!!");
            isGpsHardwarePresent = false;
            skyhook = new SkyhookUtils(context, locationHandler);
        }
    }

    public static boolean isGpsHardwarePresent(Context context) {
    	try {
            return HelperInternal.isGpsHardwarePresent(context);
        } catch (VerifyError e) {
            return true;
        }
    	//return false;
    }
    
    public static boolean isGpsHardwarePresent() {
        return isGpsHardwarePresent;
    }

    public static void enableMyLocation() {
        if (isGpsHardwarePresent) {
            myLocation.enableMyLocation();
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
                myLocation.enableCompass();
            }
        } else {
            skyhook.enableMyLocation();
        }
    }

    public static void disableMyLocation() {
        if (myLocation != null) {
            //if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
            myLocation.disableCompass();
            //} else {
            myLocation.disableMyLocation();
            //}
        } else if (skyhook != null) {
            skyhook.disableMyLocation();
        }
    }

    public static void runOnFirstFix(Runnable r) {
        if (isGpsHardwarePresent) {
            myLocation.runOnFirstFix(r);
        } else {
            skyhook.runOnFirstFix(r);
        }
    }

    public static void enableCompass() {
        if (isGpsHardwarePresent) {
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
                myLocation.enableCompass();
            } else {
                myLocation.disableCompass();
            }
        }
    }

    public static GeoPoint getMyLocation() {
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
