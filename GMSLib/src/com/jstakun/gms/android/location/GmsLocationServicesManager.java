package com.jstakun.gms.android.location;

import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;

public class GmsLocationServicesManager implements GoogleApiClient.ConnectionCallbacks,
												   GoogleApiClient.OnConnectionFailedListener,
												   LocationListener {
	
	public static final int GMS_CONNECTED = 300;
	public static final int UPDATE_LOCATION = 301;
	
	private static final int LOCATION_READ_INTERVAL = 5000; //ms
	
	private boolean isEnabled = false;
	
	private GoogleApiClient mGoogleApiClient;
	private static Map<String, Handler> mLocationHandlers = new HashMap<String, Handler>();
	private LocationRequest mLocationRequest;
	
	public static final GmsLocationServicesManager instance = new GmsLocationServicesManager();
	
	private GmsLocationServicesManager() {
		
	}
	
	public static GmsLocationServicesManager getInstance() {
		return instance;
	}
	
	public void enable(String handlerName, Handler locationHandler, Context context) {
		buildGoogleApiClient(context);
		if (mGoogleApiClient != null && !mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
			mGoogleApiClient.connect();
		}
		if (!isEnabled) {
			mLocationRequest = new LocationRequest();
			mLocationRequest.setInterval(LOCATION_READ_INTERVAL);
			mLocationRequest.setFastestInterval(LOCATION_READ_INTERVAL);
			mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//.PRIORITY_BALANCED_POWER_ACCURACY);
			isEnabled = true;
		}
		mLocationHandlers.put(handlerName, locationHandler);
	}
	
	public void disable(String handlerName) {
		mLocationHandlers.remove(handlerName);
		if (mLocationHandlers.isEmpty() && mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			LoggerUtils.debug("GmsLocationServicesManager removed location updates");
			LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
			if (mGoogleApiClient != null) {
				mGoogleApiClient.disconnect();
				mGoogleApiClient = null;
			}
			isEnabled = false;
		} else {
			LoggerUtils.debug("GmsLocationServicesManager has " + mLocationHandlers.size() + " handlers");
		}
	}
	
	private synchronized void buildGoogleApiClient(Context context) {
		if (mGoogleApiClient == null && context != null) {
			try {
				mGoogleApiClient = new GoogleApiClient.Builder(context)
						.addConnectionCallbacks(this)
						.addOnConnectionFailedListener(this)
						.addApi(LocationServices.API)
						.build();
			} catch (Exception e) {
				LoggerUtils.error("GmsLocationServicesManager.buildGoogleApiClient() exception:", e);
				
			}
		}
    }

	@Override
	public void onLocationChanged(Location location) {
		//if (AndroidDevice.isBetterLocation(location, ConfigurationManager.getInstance().getLocation())) {
		LoggerUtils.debug("GmsLocationServicesManager received new location");
        ConfigurationManager.getInstance().setLocation(location);
        for (Map.Entry<String, Handler> entry : mLocationHandlers.entrySet()) {
        	entry.getValue().sendEmptyMessage(UPDATE_LOCATION);
        }
        //}
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*if (connectionResult.hasResolution()) {
			try {
            	// Start an Activity that tries to resolve the error
            	connectionResult.startResolutionForResult(this, 0);
        	} catch (IntentSender.SendIntentException e) {
            	//e.printStackTrace();
        	}
    	} else {*/
		LoggerUtils.error("GmsLocationServicesManager connection failed with code " + connectionResult.getErrorCode());
    	//}
	}

	@Override
	public void onConnected(Bundle bundle) {
		Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
		if (location != null) {
			ConfigurationManager.getInstance().setLocation(location);
			for (Map.Entry<String, Handler> entry : mLocationHandlers.entrySet()) {
	        	entry.getValue().sendEmptyMessage(UPDATE_LOCATION);
	        }
		}
		LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
		LoggerUtils.debug("GmsLocationServicesManager requested location updates");
	}

	@Override
	public void onConnectionSuspended(int reason) {
		//call logger
		LoggerUtils.debug("GmsLocationServicesManager connection suspended with code " + reason);
	}

}
