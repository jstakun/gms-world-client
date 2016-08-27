package com.jstakun.gms.android.service;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;

public class RouteTracingService extends Service {
	
	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;
    private RouteLocationListener mRouteLocationListener;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
    	mRouteLocationListener = new RouteLocationListener();
    	
    	mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(mRouteLocationListener)
                .addOnConnectionFailedListener(mRouteLocationListener)
                .addApi(LocationServices.API)
                .build();
    	
    	mGoogleApiClient.connect();
    	
    	mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }
    
    @Override
    public void onDestroy() {
    	if (mGoogleApiClient.isConnected()) {
        	LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mRouteLocationListener);
            mGoogleApiClient.disconnect();
        }
    }
	
	private class RouteLocationListener implements LocationListener,
												   GoogleApiClient.ConnectionCallbacks,
												   GoogleApiClient.OnConnectionFailedListener
    {

		@Override
		public void onLocationChanged(Location location) {
			// TODO add location to route point
			LoggerUtils.error("RouteTracingService received new location");
		}

		@Override
		public void onConnectionFailed(ConnectionResult arg0) {
		}

		@Override
		public void onConnected(Bundle arg0) {
			Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
			if (location != null) {
				// TODO add location to route point
				LoggerUtils.error("RouteTracingService received last known location");
			}
			LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
		}

		@Override
		public void onConnectionSuspended(int reasonCode) {
		}	
	}
}
