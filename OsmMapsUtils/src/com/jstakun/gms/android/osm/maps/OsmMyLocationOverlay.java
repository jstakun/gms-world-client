/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.osm.maps;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import com.jstakun.gms.android.location.AndroidDevice;
import com.jstakun.gms.android.config.ConfigurationManager;
import org.osmdroid.api.IMyLocationOverlay;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MyLocationOverlay;

/**
 *
 * @author jstakun
 */
public class OsmMyLocationOverlay extends MyLocationOverlay implements IMyLocationOverlay {

    public static final int UPDATE_LOCATION = 21;
    private Handler positionHandler;

    public OsmMyLocationOverlay(Context context, MapView mapView, Handler handler) {
        super(context, mapView, new OsmResourceProxy(context));
        this.positionHandler = handler;
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        if (AndroidDevice.isBetterLocation(location, ConfigurationManager.getInstance().getLocation())) {
            ConfigurationManager.getInstance().setLocation(location);
            updatePositionUi(location);
        }
    }

    private void updatePositionUi(Location currentLocation) {
        /*double lat = MercatorUtils.normalizeE6(currentLocation.getLatitude());
        double lon = MercatorUtils.normalizeE6(currentLocation.getLongitude());
        float altitude = (float) currentLocation.getAltitude();
        float accuracy = currentLocation.getAccuracy();
        float bearing = currentLocation.getBearing();
        float speed = currentLocation.getSpeed();

        if (positionHandler != null) {
            Message msg = positionHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putDouble("lat", lat);
            b.putDouble("lng", lon);
            b.putFloat("alt", altitude);
            b.putFloat("acc", accuracy);
            b.putFloat("bea", bearing);
            b.putFloat("spe", speed);
            msg.setData(b);
            positionHandler.handleMessage(msg);
        }*/
        if (positionHandler != null) {
            Message msg = positionHandler.obtainMessage(UPDATE_LOCATION, currentLocation);
            positionHandler.handleMessage(msg);
        }    
    }
}
