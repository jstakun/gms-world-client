package com.jstakun.gms.android.ui;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.LandmarkManager;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AutoCheckinService extends Service {

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		double lat = intent.getDoubleExtra("lat", Double.NaN);
        double lng = intent.getDoubleExtra("lng", Double.NaN);
		
		LandmarkManager landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
		AsyncTaskManager asyncTaskManager = (AsyncTaskManager) ConfigurationManager.getInstance().getObject("asyncTaskManager", AsyncTaskManager.class);
		
		if (landmarkManager != null && asyncTaskManager != null && lat != Double.NaN && lng != Double.NaN) {
		
			CheckinManager checkinManager = new CheckinManager(landmarkManager, asyncTaskManager);
		
			checkinManager.autoCheckin(lat, lng, false); //change to true if running in background
		}
		
	    return Service.START_NOT_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
