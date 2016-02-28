package com.jstakun.gms.android.osm.maps;

import org.osmdroid.api.IMyLocationOverlay;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.location.AndroidDevice;
import com.jstakun.gms.android.location.LocationServicesManager;

public class OsmMyLocationNewOverlay extends MyLocationNewOverlay implements IMyLocationOverlay {
	
	private Handler positionHandler;

    public OsmMyLocationNewOverlay(Context context, MapView mapView, Handler handler) {
        super(context, mapView);
        this.positionHandler = handler;
    }

    @Override
    public void onLocationChanged(Location location, IMyLocationProvider provider) {
        super.onLocationChanged(location, provider);
        if (AndroidDevice.isBetterLocation(location, ConfigurationManager.getInstance().getLocation())) {
            ConfigurationManager.getInstance().setLocation(location);
            updatePositionUi(location);
        }
    }

    private void updatePositionUi(Location currentLocation) {
        if (positionHandler != null) {
            Message msg = positionHandler.obtainMessage(LocationServicesManager.UPDATE_LOCATION, currentLocation);
            positionHandler.handleMessage(msg);
        }    
    }

	@Override
	public void disableCompass() {
	}

	@Override
	public boolean enableCompass() {
		return false;
	}

	@Override
	public float getOrientation() {
		return 0;
	}

	@Override
	public boolean isCompassEnabled() {
		return false;
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
