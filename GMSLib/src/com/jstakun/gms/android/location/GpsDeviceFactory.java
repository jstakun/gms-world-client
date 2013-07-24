/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.location;

import android.os.Handler;
import com.jstakun.gms.android.utils.LoggerUtils;

/**
 *
 * @author jstakun
 */
public class GpsDeviceFactory {
     private static AndroidDevice device = null;

     public static AndroidDevice initGpsDevice()
     {
         if (device == null)
         {
            //change device
            LoggerUtils.debug("Starting GPS...");
            device = new AndroidDevice();
            //device = new MockAndroidDevice();
         }
         return device;
     }

     public static void stopDevice()
     {
        if (device != null)
        {
            device.stopListening();
        }
     }

     public static void startDevice()
     {
        if (device == null)
        {
            initGpsDevice();
        }
        device.startListening();
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
