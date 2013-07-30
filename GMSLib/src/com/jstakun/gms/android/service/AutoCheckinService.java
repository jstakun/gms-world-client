package com.jstakun.gms.android.service;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.AsyncTaskManager;
import com.jstakun.gms.android.ui.CheckinManager;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;

public class AutoCheckinService extends Service {

	private final IBinder gmsBinder = new GMSBinder();
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		double lat = intent.getDoubleExtra("lat", Double.NaN);
        double lng = intent.getDoubleExtra("lng", Double.NaN);
		
        LoggerUtils.debug("AutoCheckinService.doReceive() Running at " + lat + "," + lng + "...");
        
        boolean silent = false;
        AsyncTaskManager asyncTaskManager = (AsyncTaskManager) ConfigurationManager.getInstance().getObject("asyncTaskManager", AsyncTaskManager.class);
		
        /*if (asyncTaskManager == null) {
        	Context context = getApplicationContext();
        	asyncTaskManager = new AsyncTaskManager(null, null);
        	silent = true;
        	//TODO load lat, lng from location service
        	LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        	if (locationManager != null) {
        		//locationManager.getLastKnownLocation(provider);
        	}      	
        }*/
		
        if (lat != Double.NaN && lng != Double.NaN) {
        	CheckinManager checkinManager = new CheckinManager(asyncTaskManager);
        	int checkinCount = checkinManager.autoCheckin(lat, lng, silent); //change to true if running in background
        	LoggerUtils.debug("AutoCheckinService.doReceive() Finishing with " + checkinCount + " checkins...");
        } 
		
	    return Service.START_NOT_STICKY;
	}
	
	
	public class GMSBinder extends Binder {
	    public AutoCheckinService getService() {
	      return AutoCheckinService.this;
	    }
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return gmsBinder;
	}

}
