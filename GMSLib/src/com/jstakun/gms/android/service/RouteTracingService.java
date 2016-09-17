package com.jstakun.gms.android.service;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;

public class RouteTracingService extends Service implements LocationListener,
															GoogleApiClient.ConnectionCallbacks,
															GoogleApiClient.OnConnectionFailedListener{

	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;
	private PowerManager.WakeLock mWakeLock;
	
	public static final String COMMAND = "RouteTracingService.COMMAND";
	public static final int COMMAND_START = 1;
	public static final int COMMAND_STOP = 0;	
	public static final int COMMAND_REGISTER_CLIENT = 2;
	public static final int COMMAND_SHOW_ROUTE = 50;
	
	private final Messenger mMessenger = new Messenger(new IncomingHandler()); 
	private Messenger mClient;
    
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		LoggerUtils.debug("RouteTracingService onStartCommand()");
        super.onStartCommand(intent, flags, startId);
        
        if (intent != null && intent.hasExtra(COMMAND)) {
        	if (intent.getIntExtra(COMMAND, -1) == COMMAND_START) {
        		startTracking();
        	} else if (intent.getIntExtra(COMMAND, -1) == COMMAND_STOP) {
        		stopTracking();
        	}
        }
        
        return START_STICKY;
    }

    @Override
    public void onCreate() {
    	super.onCreate();
    	LoggerUtils.debug("RouteTracingService onCreate()");
    	startTracking();
    }
    
    @Override
    public void onDestroy() {
    	LoggerUtils.debug("RouteTracingService onDestroy()");
    	stopTracking();
    }
	
    private synchronized void startTracking() {
    	LoggerUtils.debug("RouteTracingService startTracking()");
    	buildGoogleApiClient();
    	
    	if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
        	mGoogleApiClient.connect();
        }
        
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (this.mWakeLock != null)
        {
           this.mWakeLock.release();
           this.mWakeLock = null;
        }
        this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LoggerUtils.getTag());
        this.mWakeLock.acquire();
        
    	mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }
    
    private synchronized void stopTracking() {
    	LoggerUtils.debug("RouteTracingService stopTracking()");
    	
    	if (mGoogleApiClient.isConnected()) {
        	LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    	if (this.mWakeLock != null)
        {
           this.mWakeLock.release();
           this.mWakeLock = null;
        }
    }
    
    private synchronized void buildGoogleApiClient() {
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
		if (RouteRecorder.getInstance().addCoordinate(location)) {
			//notify ui to repaint route
			if (mClient != null) {
				try {
					Message message = Message.obtain(null, COMMAND_SHOW_ROUTE);
					mClient.send(message); 
				} catch (Exception e) {
					LoggerUtils.error(e.getMessage(), e);
				}
			} else {
				LoggerUtils.debug("Unable to notify client");
			}
		}
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
			RouteRecorder.getInstance().addCoordinate(location);
		}
		LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
	}

	@Override
	public void onConnectionSuspended(int reasonCode) {
	}	
	
	private class IncomingHandler extends Handler {
		@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            	case COMMAND_REGISTER_CLIENT:
            		LoggerUtils.debug("New client registered!");
            		mClient = msg.replyTo;
            		break;
            	default:
            		super.handleMessage(msg);
            }
        }
	}
}
