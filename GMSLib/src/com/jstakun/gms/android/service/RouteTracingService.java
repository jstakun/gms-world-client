package com.jstakun.gms.android.service;

//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.location.LocationListener;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationServices;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.location.GmsLocationServicesManager;
import com.jstakun.gms.android.location.LocationServicesManager;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.ui.IntentsHelper;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;

public class RouteTracingService extends Service { //implements LocationListener,
												   //			GoogleApiClient.ConnectionCallbacks,
												   //			GoogleApiClient.OnConnectionFailedListener {

	//private GoogleApiClient mGoogleApiClient;
	//private LocationRequest mLocationRequest;
	
	private PowerManager.WakeLock mWakeLock;
	
	public static final String COMMAND = "RouteTracingService.COMMAND";
	public static final int COMMAND_START = 1;
	public static final int COMMAND_STOP = 0;	
	public static final int COMMAND_REGISTER_CLIENT = 2;
	public static final int COMMAND_SHOW_ROUTE = 50;
	//private static final int LOCATION_READ_INTERVAL = 5000; //ms
	
	private final Handler incomingHandler = new IncomingHandler();
	private final Messenger mMessenger = new Messenger(incomingHandler); 
	private Messenger mClient;
	
	private boolean isGoogleApiAvailable = false;
    
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
    	isGoogleApiAvailable = IntentsHelper.getInstance().isGoogleApiAvailable();
    	if (!isGoogleApiAvailable) {
    		LocationServicesManager.initLocationServicesManager(this, incomingHandler, null);
    	}
    	startTracking();
    }
    
    @Override
    public void onDestroy() {
    	LoggerUtils.debug("RouteTracingService onDestroy()");
    	stopTracking();
    }
	
    private synchronized void startTracking() {
    	LoggerUtils.debug("RouteTracingService startTracking()");
    	
    	if (this.mWakeLock != null)
        {
           this.mWakeLock.release();
           this.mWakeLock = null;
        }
    	
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
    	this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LoggerUtils.getTag());
        this.mWakeLock.acquire();
        
        if (!isGoogleApiAvailable) {
        	LocationServicesManager.enableMyLocation();
        } else {
        	GmsLocationServicesManager.getInstance().enable(incomingHandler);
        }
        //buildGoogleApiClient();
    	
    	//if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
        //	mGoogleApiClient.connect();
        //}
        
        //mLocationRequest = new LocationRequest();
        //mLocationRequest.setInterval(LOCATION_READ_INTERVAL); 
        //mLocationRequest.setFastestInterval(LOCATION_READ_INTERVAL);
        //mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//.PRIORITY_BALANCED_POWER_ACCURACY);
    }
    
    private synchronized void stopTracking() {
    	LoggerUtils.debug("RouteTracingService stopTracking()");
    	
    	if (this.mWakeLock != null)
        {
           this.mWakeLock.release();
           this.mWakeLock = null;
        }
    	
    	//if (mGoogleApiClient.isConnected()) {
        //	LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        //    mGoogleApiClient.disconnect();
        //}
    	
    	if (!isGoogleApiAvailable) {
        	LocationServicesManager.enableMyLocation();
        } else {
        	GmsLocationServicesManager.getInstance().disable();
        }
    }
    
    /*private synchronized void buildGoogleApiClient() {
    	if (mGoogleApiClient == null) {
    		mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    	}
    }*/	
    
	/*@Override
	public void onLocationChanged(Location location) {
		// add location to route points
		LoggerUtils.debug("RouteTracingService received new location");
		ConfigurationManager.getInstance().setLocation(location);
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
	}*/

	/*@Override
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
	}*/	
	
	private class IncomingHandler extends Handler {
		@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            	case COMMAND_REGISTER_CLIENT:
            		mClient = msg.replyTo;
            		break;
            	case GmsLocationServicesManager.GMS_CONNECTED:
            	case GmsLocationServicesManager.UPDATE_LOCATION:
            	case LocationServicesManager.UPDATE_LOCATION:	
            		LoggerUtils.debug("RouteTracingService received new location");
            		if (RouteRecorder.getInstance().addCoordinate(ConfigurationManager.getInstance().getLocation())) {
            			//notify ui to repaint route
            			if (mClient != null) {
            				try {
            					Message message = Message.obtain(null, COMMAND_SHOW_ROUTE);
            					mClient.send(message); 
            				} catch (Exception e) {
            					LoggerUtils.error(e.getMessage(), e);
            				}
            			} else {
            				LoggerUtils.debug("RouteTrackingService is unable to notify client");
            			}
            		}
            		break;
            	default:
            		super.handleMessage(msg);
            }
        }
	}
}
