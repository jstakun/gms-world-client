package com.jstakun.gms.android.service;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.ui.AsyncTaskManager;
import com.jstakun.gms.android.ui.CheckinManager;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class AutoCheckinService extends Service {

	private final IBinder gmsBinder = new GMSBinder();
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		double lat = intent.getDoubleExtra("lat", Double.NaN);
        double lng = intent.getDoubleExtra("lng", Double.NaN);
		
        LoggerUtils.debug("AutoCheckinService.doReceive() Running at " + lat + "," + lng + "...");
        
		LandmarkManager landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
		AsyncTaskManager asyncTaskManager = (AsyncTaskManager) ConfigurationManager.getInstance().getObject("asyncTaskManager", AsyncTaskManager.class);
		
		if (landmarkManager != null && asyncTaskManager != null && lat != Double.NaN && lng != Double.NaN) {
		
			CheckinManager checkinManager = new CheckinManager(landmarkManager, asyncTaskManager);
		
			int checkinCount = checkinManager.autoCheckin(lat, lng, false); //change to true if running in background
			
			LoggerUtils.debug("AutoCheckinService.doReceive() Finishing with " + checkinCount + " checkins...");
		} else {
			LoggerUtils.debug("AutoCheckinService.doReceive() Finishing lm: " + (landmarkManager != null) +
					", atm: " + (asyncTaskManager != null) + ", lat: " + (lat != Double.NaN) + ", lat: " + (lng != Double.NaN));
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
