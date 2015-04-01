package com.jstakun.gms.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.location.AndroidDevice;
import com.jstakun.gms.android.utils.LoggerUtils;

public class AutoCheckinStartServiceReceiver extends BroadcastReceiver {
	
	private static final long THRITY_MINS = 1000 * 60 * 30;

	@Override
	public void onReceive(Context context, Intent intent) {
		Location location = AndroidDevice.getLastKnownLocation(context);

		if (location != null && (System.currentTimeMillis() - location.getTime()) < THRITY_MINS) {
			Intent service = new Intent(context, AutoCheckinService.class);
			service.putExtra("lat", location.getLatitude());
			service.putExtra("lng", location.getLongitude());
			context.startService(service);
		} else {
			LoggerUtils.debug("AutoCheckinStartServiceReceiver.doReceive() no location available");
		}
	}
}
