package com.jstakun.gms.android.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.AsyncTaskManager;
import com.jstakun.gms.android.ui.CheckinManager;
import com.jstakun.gms.android.utils.LoggerUtils;

public class AutoCheckinService extends Service {

	private final IBinder gmsBinder = new GMSBinder();
	//private static double lastExecutedLat = Double.NaN;
	//private static double lastExecutedLng = Double.NaN;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		double lat = intent.getDoubleExtra("lat", Double.NaN);
        double lng = intent.getDoubleExtra("lng", Double.NaN);
		
        LoggerUtils.debug("AutoCheckinService.doReceive() Running at " + lat + "," + lng);
        
        //if (!Double.isNaN(lat) && !Double.isNaN(lng) && 
        //		(Double.isNaN(lastExecutedLat) || MathUtils.abs(lat-lastExecutedLat) > 0.01 ||
        //		Double.isNaN(lastExecutedLng) || MathUtils.abs(lng-lastExecutedLng) > 0.01)) {
        
        if (!Double.isNaN(lat) && !Double.isNaN(lng)) {               
            boolean silent = false;
            AsyncTaskManager asyncTaskManager = (AsyncTaskManager) ConfigurationManager.getInstance().getObject("asyncTaskManager", AsyncTaskManager.class); 		
            if (asyncTaskManager == null) {
            	asyncTaskManager = new AsyncTaskManager(null, null);
            	silent = true; 
            }
        	CheckinManager checkinManager = new CheckinManager(asyncTaskManager, getApplicationContext());
        	int checkinCount = checkinManager.autoCheckin(lat, lng, silent); //set silent to true if running in background
        	//lastExecutedLat = lat;
        	//lastExecutedLng = lng;
        	LoggerUtils.debug("AutoCheckinService.doReceive() Initiated " + checkinCount + " checkins");
        } else {
        	LoggerUtils.debug("AutoCheckinService.doReceive() skipping current run");
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
