/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 *
 * @author jstakun
 */
public class ServicesUtils {

    public static boolean isGpsActive(Context context) {
        try {
            final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            LoggerUtils.error("ServiceUtils.isGpsActive exception:", e);
            return false;
        } 
    }

    public static boolean isNetworkActive(Context context) {
        try {
            final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();       
            return (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnectedOrConnecting());
        } catch (Exception e) {
            LoggerUtils.error("ServiceUtils.isNetworkActive exception:", e);
            return false;
        }
    }
    
    public static boolean isWifiActive(Context context) {
        try {
            final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();       
            boolean isWifiAvailable = networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
            if (!isWifiAvailable) {
               LoggerUtils.error("Wifi not available. Skipping image upload!"); 
            }
            return isWifiAvailable;
        } catch (Exception e) {
            LoggerUtils.error("ServiceUtils.isNetworkActive exception:", e);
            return false;
        }
    }
}
