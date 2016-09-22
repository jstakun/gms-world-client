package com.jstakun.gms.android.location;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.jstakun.gms.android.config.ConfigurationManager;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;

public class GmsLocationServicesManager implements GoogleApiClient.ConnectionCallbacks,
												   GoogleApiClient.OnConnectionFailedListener,
												   LocationListener {
	
	public static final int GMS_CONNECTED = 300;
	public static final int UPDATE_LOCATION = 301;
	
	private boolean isEnabled = false;
	
	private GoogleApiClient mGoogleApiClient;
	private Handler mLocationHandler;
	private LocationRequest mLocationRequest;
	
	public static final GmsLocationServicesManager instance = new GmsLocationServicesManager();
	
	private GmsLocationServicesManager() {
		
	}
	
	public static GmsLocationServicesManager getInstance() {
		return instance;
	}
	
	public void enable(Handler locationHandler) {
		buildGoogleApiClient();
		if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
			mGoogleApiClient.connect();
		}
		if (!isEnabled) {
			mLocationHandler = locationHandler;
			mLocationRequest = new LocationRequest();
			mLocationRequest.setInterval(1000);
			mLocationRequest.setFastestInterval(1000);
			mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
			isEnabled = true;
		}
	}

	public void disable() {
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
			mGoogleApiClient.disconnect();
		}
		isEnabled = false;
	}
	
	private void buildGoogleApiClient() {
		if (mGoogleApiClient == null) {
			mGoogleApiClient = new GoogleApiClient.Builder(ConfigurationManager.getInstance().getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
		}
    }

	@Override
	public void onLocationChanged(Location location) {
		if (AndroidDevice.isBetterLocation(location, ConfigurationManager.getInstance().getLocation())) {
            ConfigurationManager.getInstance().setLocation(location);
            mLocationHandler.sendEmptyMessage(UPDATE_LOCATION);
        }
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
    	} else {
        	LoggerUtils.error("Location services connection failed with code " + connectionResult.getErrorCode());
    	}*/
	}

	@Override
	public void onConnected(Bundle bundle) {
		Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
		if (location != null) {
			ConfigurationManager.getInstance().setLocation(location);
			mLocationHandler.sendEmptyMessage(GMS_CONNECTED);
		}
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
	}

	@Override
	public void onConnectionSuspended(int reason) {
		//call logger
	}

}
