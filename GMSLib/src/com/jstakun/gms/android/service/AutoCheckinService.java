package com.jstakun.gms.android.service;

import com.jstakun.gms.android.config.ConfigurationManager;
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
		
        LoggerUtils.debug("AutoCheckinService.doReceive() Running at " + lat + "," + lng);
        
        if (!Double.isNaN(lat) && !Double.isNaN(lng)) {               
            boolean silent = false;
            AsyncTaskManager.getInstance().setContext(this);
        	if (ConfigurationManager.getInstance().getContext() == null) {
        		ConfigurationManager.getInstance().setContext(this);
        		silent= true;
        	}
        	int checkinCount = CheckinManager.getInstance().autoCheckin(lat, lng, silent); //set silent to true if running in background
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
