package com.jstakun.gms.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.location.GpsDeviceFactory;
import com.jstakun.gms.android.location.LocationServicesManager;
import com.jstakun.gms.android.utils.LoggerUtils;

public class AutoCheckinStartServiceReceiver extends BroadcastReceiver {

	private Context context;
	private final Handler locationHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			if (data.containsKey("lat") && data.containsKey("lng")) {
				double lat = data.getDouble("lat");
				double lng = data.getDouble("lng");
				startService(context, lat, lng);
			} else {
				LoggerUtils.debug("AutoCheckinStartServiceReceiver.locationhandler: no location available");
			}
		}
	};

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

	/*
	 * @Override public void onReceive(Context context, Intent intent) {
	 * this.context = context; if (isGpsHardwarePresent(context)) {
	 * GpsDeviceFactory.startDevice(locationHandler, context); } else {
	 * LocationServicesManager.initLocationServicesManager(context,
	 * locationHandler, null); LocationServicesManager.enableMyLocation(); } }
	 */

	private void startService(Context context, double lat, double lng) {
		if (isGpsHardwarePresent(context)) {
			GpsDeviceFactory.stopDevice();
		} else {
			LocationServicesManager.disableMyLocation();
		}
		Intent service = new Intent(context, AutoCheckinService.class);
		service.putExtra("lat", lat);
		service.putExtra("lng", lng);
		context.startService(service);
	}

	private static boolean isGpsHardwarePresent(Context context) {
		try {
			return HelperInternal.isGpsHardwarePresent(context);
		} catch (VerifyError e) {
			return true;
		}
	}

	private static class HelperInternal {

		public static boolean isGpsHardwarePresent(Context context) {
			// LocationManager locationManager = (LocationManager)
			// context.getSystemService(Context.LOCATION_SERVICE);
			// return (locationManager != null &&
			// locationManager.getProvider(LocationManager.GPS_PROVIDER) !=
			// null);
			return context.getPackageManager().hasSystemFeature(
					PackageManager.FEATURE_LOCATION_GPS);
		}
	}
}
