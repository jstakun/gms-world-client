/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.amz.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import com.amazon.geo.maps.GeoPoint;
import com.amazon.geo.maps.MapView;
import com.amazon.geo.maps.MyLocationOverlay;
import com.amazon.geo.maps.Projection;
import com.jstakun.gms.android.location.AndroidDevice;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.LoggerUtils;

/**
 *
 * @author jstakun
 */
public class AmzMyLocationOverlay extends MyLocationOverlay {

    public static final int UPDATE_LOCATION = 21;
    private Handler positionHandler;
    private boolean bugged = false;
    private Paint accuracyPaint;
    private Drawable drawable;

    public AmzMyLocationOverlay(Context context, MapView mapView, Handler handler, Drawable drawable) {
        super(context, mapView);
        this.positionHandler = handler;
        this.drawable = drawable;
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

    @Override
    protected void drawMyLocation(Canvas canvas, MapView mapView, Location lastFix, GeoPoint myLoc, long when) {
        if (!bugged) {
            try {
                super.drawMyLocation(canvas, mapView, lastFix, myLoc, when);
            } catch (Exception e) {
                bugged = true;
            }
        }

        if (bugged) {

            accuracyPaint = new Paint();
            accuracyPaint.setAntiAlias(true);
            accuracyPaint.setStrokeWidth(2.0f);

            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();

            Point center = new Point();
            Point left = new Point();
            Projection projection = mapView.getProjection();

            double latitude = lastFix.getLatitude();
            double longitude = lastFix.getLongitude();
            float accuracy = lastFix.getAccuracy();

            float[] result = new float[1];

            Location.distanceBetween(latitude, longitude, latitude, longitude + 1, result);
            float longitudeLineDistance = result[0];

            GeoPoint leftGeo = new GeoPoint((int) (latitude * 1e6), (int) ((longitude - accuracy
                    / longitudeLineDistance) * 1e6));
            projection.toPixels(leftGeo, left);
            projection.toPixels(myLoc, center);

            int radius = center.x - left.x;
            accuracyPaint.setColor(0xff6666ff);
            accuracyPaint.setStyle(Style.STROKE);
            canvas.drawCircle(center.x, center.y, radius, accuracyPaint);

            accuracyPaint.setColor(0x186666ff);
            accuracyPaint.setStyle(Style.FILL);
            canvas.drawCircle(center.x, center.y, radius, accuracyPaint);

            int w = width / 2;
            int h = height / 2;
            drawable.setBounds(center.x - w, center.y - h, center.x + w, center.y + h);
            drawable.draw(canvas);
            mapView.postInvalidate();
        }
    }

    @Override
    public boolean onTap(GeoPoint p, MapView mapView) {
        return false;
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        try {
            super.draw(canvas, mapView, shadow);
        } catch (Exception e) {
            LoggerUtils.error("AmzMyLocationOverlay exception", e);
        }
    }
}

