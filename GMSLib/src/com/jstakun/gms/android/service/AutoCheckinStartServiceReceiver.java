package com.jstakun.gms.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.jstakun.gms.android.location.AndroidDevice;
import com.jstakun.gms.android.utils.LoggerUtils;

public class AutoCheckinStartServiceReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Location location = AndroidDevice.getLastKnownLocation(context, 30);

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
