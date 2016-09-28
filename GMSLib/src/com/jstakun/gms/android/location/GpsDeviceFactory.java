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

     public static AndroidDevice initGpsDevice(Context context)
     {
         if (device == null)
         {
            //change device
            LoggerUtils.debug("Starting GPS...");
            device = new AndroidDevice(context);
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

     public static void startDevice(Context context)
     {
        if (device == null)
        {
            initGpsDevice(context);
        }
        device.startListening();
     }
     
     public static void startDevice(Handler handler, Context context)
     {
        if (device == null)
        {
            initGpsDevice(context);
        }
        device.startListening();
        device.setPositionHandler(handler);
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
