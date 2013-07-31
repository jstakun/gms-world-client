package com.jstakun.gms.android.service;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

public class AutoCheckinStartServiceReceiver extends BroadcastReceiver {

	 @Override
	  public void onReceive(Context context, Intent intent) {
		 Location location = ConfigurationManager.getInstance().getLocation();
		 
		 if (location == null) {
	         LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	         if (locationManager != null) {
	        	 location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	        	 if (location == null) {
	        		location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	        	 }
	        	 if (location == null) {
	        		LoggerUtils.debug("AutoCheckinStartServiceReceiver.doReceive() no location from location manager available"); 
	        	 }
	         } else {
	        	 LoggerUtils.debug("AutoCheckinStartServiceReceiver.doReceive() no location manager available");
	         }
	     } else {
	    	 LoggerUtils.debug("AutoCheckinStartServiceReceiver.doReceive() no saved location");
	     }
		 
		 if (location != null) {
			 Intent service = new Intent(context, AutoCheckinService.class);
			 service.putExtra("lat", location.getLatitude());
			 service.putExtra("lng", location.getLongitude());
			 context.startService(service);
		 } else {
			 LoggerUtils.debug("AutoCheckinStartServiceReceiver.doReceive() no location available");
		 }
	  }	 
}
