package com.jstakun.gms.android.service;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.location.GmsLocationServicesManager;
import com.jstakun.gms.android.location.GpsDeviceFactory;
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

public class RouteTracingService extends Service { 
	
	private PowerManager.WakeLock mWakeLock;
	
	public static final String COMMAND = "RouteTracingService.COMMAND";
	public static final int COMMAND_START = 1;
	public static final int COMMAND_STOP = 0;	
	public static final int COMMAND_REGISTER_CLIENT = 2;
	public static final int COMMAND_SHOW_ROUTE = 50;
	
	private final Handler incomingHandler = new IncomingHandler();
	private final Messenger mMessenger = new Messenger(incomingHandler); 
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
    	
    	if (this.mWakeLock != null)
        {
           this.mWakeLock.release();
           this.mWakeLock = null;
        }
    	
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
    	this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LoggerUtils.getTag());
        this.mWakeLock.acquire();
        
        if (!IntentsHelper.getInstance().isGoogleApiAvailable()) {
        	GpsDeviceFactory.initGpsDevice(this, incomingHandler);
        	GpsDeviceFactory.startDevice();
        } else {
        	GmsLocationServicesManager.getInstance().enable(incomingHandler);
        }
    }
    
    private synchronized void stopTracking() {
    	LoggerUtils.debug("RouteTracingService stopTracking()");
    	
    	if (this.mWakeLock != null)
        {
           this.mWakeLock.release();
           this.mWakeLock = null;
        }
    	
    	if (!IntentsHelper.getInstance().isGoogleApiAvailable()) {
    		GpsDeviceFactory.stopDevice();
        } else {
        	GmsLocationServicesManager.getInstance().disable();
        }
    }

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
