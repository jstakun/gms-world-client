package com.jstakun.gms.android.service;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;

public class RouteTracingService extends Service implements LocationListener,
															GoogleApiClient.ConnectionCallbacks,
															GoogleApiClient.OnConnectionFailedListener{

	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;
    
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		LoggerUtils.debug("RouteTracingService onStartCommand()");
        super.onStartCommand(intent, flags, startId);
        
        buildGoogleApiClient();
        
        if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
        	mGoogleApiClient.connect();
        }
        
        return START_STICKY;
    }

    @Override
    public void onCreate() {
    	super.onCreate();
    	LoggerUtils.debug("RouteTracingService onCreate()");
    	
    	buildGoogleApiClient();
    	
    	mGoogleApiClient.connect();
    	
    	mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }
    
    @Override
    public void onDestroy() {
    	LoggerUtils.debug("RouteTracingService onDestroy()");
    	if (mGoogleApiClient.isConnected()) {
        	LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }
	
    protected synchronized void buildGoogleApiClient() {
    	if (mGoogleApiClient == null) {
    		mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    	}
    }	
    
	@Override
	public void onLocationChanged(Location location) {
		// add location to route points
		LoggerUtils.debug("RouteTracingService received new location");
		//int mode =
		RouteRecorder.getInstance().addCoordinate(location.getLatitude(), location.getLongitude(), (float)location.getAltitude(), location.getAccuracy(), location.getSpeed(), location.getBearing());
		//TODO notify ui to repaint route
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
	}

	@Override
	public void onConnected(Bundle arg0) {
		Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
		if (location != null) {
			// add location to route points
			LoggerUtils.debug("RouteTracingService received last known location");
			RouteRecorder.getInstance().addCoordinate(location.getLatitude(), location.getLongitude(), (float)location.getAltitude(), location.getAccuracy(), location.getSpeed(), location.getBearing());
		}
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
	}

	@Override
	public void onConnectionSuspended(int reasonCode) {
	}	
}
