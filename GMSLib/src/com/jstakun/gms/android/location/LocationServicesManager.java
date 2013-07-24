/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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

    private static SkyhookUtils skyhook;
    private static IMyLocationOverlay myLocation;
    private static boolean isGpsHardwarePresent = false;

    public static void initLocationServicesManager(Context context, Handler locationHandler, IMyLocationOverlay imyLocation) {

        /*LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
         if (locationManager != null) {
         List<String> providers = locationManager.getProviders(false);
         for (String provider : providers) {
         System.out.println(provider);
         }
         }*/
        if (imyLocation != null && isGpsHardwarePresent(context)) {
            LoggerUtils.debug("GPS is present !!!");
            isGpsHardwarePresent = true;
            myLocation = imyLocation;
        } else {
            LoggerUtils.debug("GPS is missing. Using Skyhook !!!");
            skyhook = new SkyhookUtils(context, locationHandler);
        }
    }

    private static boolean isGpsHardwarePresent(Context context) {
        try {
            return HelperInternal.isGpsHardwarePresent(context);
        } catch (VerifyError e) {
            return true;
        }
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
        if (isGpsHardwarePresent) {
            //if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
            myLocation.disableCompass();
            //} else {
            myLocation.disableMyLocation();
            //}
        } else {
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
            return new GeoPoint(MathUtils.coordDoubleToInt(location.getLatitude()),
                    MathUtils.coordDoubleToInt(location.getLongitude()));
        } else {
            return null;
        }
    }

    private static class HelperInternal {

        public static boolean isGpsHardwarePresent(Context context) {
            //LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            //return (locationManager != null && locationManager.getProvider(LocationManager.GPS_PROVIDER) != null);
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        }
    }
}
