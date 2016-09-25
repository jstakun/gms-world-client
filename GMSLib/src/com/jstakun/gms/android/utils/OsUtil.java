/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.config.ConfigurationManager;

/**
 *
 * @author jstakun
 */
public class OsUtil {

    /*
     * Android 1.5 Cupcake (API level 3)
     Android 1.6 Donut (API level 4)
     Android 2.0 Eclair (API level 5)
     Android 2.0.1 Eclair (API level 6)
     Android 2.1 Eclair (API level 7)
     Android 2.2–2.2.3 Froyo (API level 8)
     Android 2.3–2.3.2 Gingerbread (API level 9)
     Android 2.3.3–2.3.7 Gingerbread (API level 10)
     Android 3.0 Honeycomb (API level 11)
     Android 3.1 Honeycomb (API level 12)
     Android 3.2 Honeycomb (API level 13)
     Android 4.0–4.0.2 Ice Cream Sandwich (API level 14) 
     Android 4.0.3–4.0.4 Ice Cream Sandwich (API level 15)
     Android 4.1 Jelly Bean (API level 16)
     Android 4.2 Jelly Bean (API level 17)
     */
	private static final int DONUT = 4;
    private static final int FROYO = 8;
    private static final int GINGERBREAD = 9;
    private static final int HONEYCOMB_3_0 = 11;
    private static final int HONEYCOMB_3_2 = 13;
    private static final int ICE_CREAM_SANDWICH = 14;

    public static boolean isDonutOrHigher() {
        return getSdkVersion() >= DONUT;
    }
    
    public static boolean isFroyoOrHigher() {
        return getSdkVersion() >= FROYO;
    }

    public static boolean isGingerbreadOrHigher() {
        return getSdkVersion() >= GINGERBREAD;
    }

    public static boolean isIceCreamSandwichOrHigher() {
        return getSdkVersion() >= ICE_CREAM_SANDWICH;
    }

    public static boolean isHoneycomb2OrHigher() {
        return getSdkVersion() >= HONEYCOMB_3_2;
    }

    public static boolean isHoneycombOrHigher() {
        return getSdkVersion() >= HONEYCOMB_3_0;
    }

    public static int getSdkVersion() {
        if (Build.VERSION.RELEASE.startsWith("1.5")) {
            return 3;
        }

        try {
            return BuildVersionHelperInternal.getSdkIntInternal();
        } catch (VerifyError e) {
            return 3;
        }
    }

    public static String getDisplayType() {
        String display = (String) ConfigurationManager.getInstance().getObject("display", String.class);
        if (display == null) {
            display = "n";
        }
        return display;
    }

    public static void setDisplayType(Configuration c) {
        String display = "n";
        try {
            display = DisplayHelperInternal.getDisplayType(c);
        } catch (VerifyError e) {
        }
        ConfigurationManager.getInstance().putObject("display", display);
    }

    public static boolean hasSystemSharedLibraryInstalled(Context ctx, String libraryName) {
        boolean hasLibraryInstalled = false;
        if (!StringUtils.isEmpty(libraryName)) {
            String[] installedLibraries = ctx.getPackageManager().getSystemSharedLibraryNames();
            if (installedLibraries != null) {
                for (String s : installedLibraries) {
                    if (libraryName.equals(s)) {
                        hasLibraryInstalled = true;
                        break;
                    }
                }
            }
        }
        return hasLibraryInstalled;
    }

    public static boolean isGoogleMapActivityInstalled() {
        try {
            // check if Google Maps is supported on given device
            Class<?> c = Class.forName("com.google.android.maps.MapActivity");
            if (c == null) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean isAmazonMapActivityInstalled() {
        try {
            // check if Amazon Maps is supported on given device
            Class<?> c = Class.forName("com.amazon.geo.maps.MapActivity");
            if (c == null) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isGoogleMapsInstalled(Context ctx) {
        try {
            ctx.getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    public static boolean hasTelephony(Context ctx) {
    	try {
    		return HasTelephoneHelperInternal.hasTelephony(ctx);
    	} catch (VerifyError e) {
            return true;
    	}
    }
    
    public static String getDeviceId(Context context) {
        //String telephonyDeviceId = null;
        String androidDeviceId = null;

        if (context != null) {
        	//android.Manifest.permission.READ_PHONE_STATE required
        	// get telephony id
        	//try {
        	//	final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        	//	telephonyDeviceId = tm.getDeviceId();     		
        	//} catch (Exception e) {
        	//	LoggerUtils.error("OsUtil.getUniqueId() exception", e);
        	//}
        
            // get internal android device id
        	try {
        		androidDeviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);          
        	} catch (Exception e) {
        		LoggerUtils.error("OsUtil.getUniqueId() exception", e);  
        	}
        }
        
        //if (telephonyDeviceId == null) {
		//	telephonyDeviceId = "NoTelephonyId";
		//}

        if (androidDeviceId == null) {
            androidDeviceId = "NoAndroidId";
        }

        //return androidDeviceId.equals("NoAndroidId") ? telephonyDeviceId : androidDeviceId;
        return androidDeviceId;
    }

    private static class BuildVersionHelperInternal {

        private static int getSdkIntInternal() {
            return Build.VERSION.SDK_INT;
        }
    }
    
    private static class HasTelephoneHelperInternal {
    	private static boolean hasTelephony(Context ctx) {
        	return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);    
        }
    }

    private static class DisplayHelperInternal {

        private static String getDisplayType(Configuration c) {
            int screenLayout = c.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
            if (screenLayout == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
                return "n";
            } else if (screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE) {
                return "l";
            } else if (screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
                return "xl";
            } else if (screenLayout == Configuration.SCREENLAYOUT_SIZE_SMALL) {
                return "s";
            } else {
                return "n";
            }
        }
    }
}
