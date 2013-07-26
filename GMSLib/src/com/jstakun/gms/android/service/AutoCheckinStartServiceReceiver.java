package com.jstakun.gms.android.service;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

public class AutoCheckinStartServiceReceiver extends BroadcastReceiver {

	 @Override
	  public void onReceive(Context context, Intent intent) {
		 Location location = ConfigurationManager.getInstance().getLocation();
		 if (location != null) {
			 Intent service = new Intent(context, AutoCheckinService.class);
			 service.putExtra("lat", location.getLatitude());
			 service.putExtra("lng", location.getLongitude());
			 context.startService(service);
		 } else {
			 LoggerUtils.debug("AutoCheckinStartServiceReceiver.doReceive() missing location...");
		 }
	  }	 
}
