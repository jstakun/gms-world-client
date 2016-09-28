package com.jstakun.gms.android.location;

import android.content.Context;
import android.os.Handler;
import com.jstakun.gms.android.utils.LoggerUtils;

/**
 *
 * @author jstakun
 */
public class GpsDeviceFactory {
     private static AndroidDevice device = null;

     public static AndroidDevice initGpsDevice(Context context) {
         if (device == null) {
            LoggerUtils.debug("Starting GPS listener...");
            device = new AndroidDevice(context);
            //for testing change device
            //device = new MockAndroidDevice();
         }
         return device;
     }
     
     public static AndroidDevice initGpsDevice(Context context, Handler positionHandler) {
         if (device == null) {
            LoggerUtils.debug("Starting GPS listener...");
            device = new AndroidDevice(context);
            //for testing change device
            //device = new MockAndroidDevice();
            device.setPositionHandler(positionHandler);
         }
         return device;
     }

     public static void stopDevice() {
        if (device != null) {
            device.stopListening();
        }
     }

     public static void startDevice() {
        if (device != null) {
        	device.startListening();
     	}
     }
     
     public static void startDevice(Handler handler, Context context) {
        if (device != null) {
        	device.startListening();
        	device.setPositionHandler(handler);
        }
     }

     public static void setPositionHandler(Handler handler) {
         if (device != null) {
             device.setPositionHandler(handler);
         }
     }

     public static void resetDevice() {
         device = null;
     }
}
