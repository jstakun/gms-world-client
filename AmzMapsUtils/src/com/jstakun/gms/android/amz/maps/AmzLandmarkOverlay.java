/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.amz.maps;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.MotionEvent;

import com.amazon.geo.maps.GeoPoint;
import com.amazon.geo.maps.MapView;
import com.amazon.geo.maps.Overlay;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.ProjectionInterface;

/**
 *
 * @author jstakun
 */
public class AmzLandmarkOverlay extends Overlay {
    
    public static final int SHOW_LANDMARK_DETAILS = 20;
    
    private Handler landmarkDetailsHandler;
    private int xmove, ymove;
    private boolean tapEventHandled;
    private String[] excluded;

    public AmzLandmarkOverlay(Handler landmarkDetailsHandler) {
        this.landmarkDetailsHandler = landmarkDetailsHandler;
        this.excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER};
    }
    
    public AmzLandmarkOverlay(Handler landmarkDetailsHandler, String[] excluded) {
        this.landmarkDetailsHandler = landmarkDetailsHandler;
        this.excluded = excluded;
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        try {
            super.draw(canvas, mapView, shadow);
            if (shadow == false) {
            	LandmarkManager.getInstance().paintLandmarks(canvas, new AmzLandmarkProjection(mapView), mapView.getWidth(), mapView.getHeight(), excluded, mapView.getResources().getDisplayMetrics());

                List<Drawable> landmarkDrawables = LandmarkManager.getInstance().getLandmarkDrawables();
                for (Drawable d : landmarkDrawables) {
                    d.draw(canvas);
                }

                Drawable selectedLandmark = LandmarkManager.getInstance().getSelectedLandmarkDrawable();
                if (selectedLandmark != null) {
                    selectedLandmark.draw(canvas);
                }

                mapView.postInvalidate();
            }
        } catch (Exception e) {
            LoggerUtils.error("AmzLandmarkOverlay exception", e);
        }
        //no shadow by default
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event, MapView mapView) {
        final int action = event.getAction();
        //System.out.println("onTouch event " + action);

        if (action == MotionEvent.ACTION_DOWN) {
            //System.out.println("Action Down x: " + event.getX() + " y: " + event.getY());
            xmove = (int) event.getX();
            ymove = (int) event.getY();
            tapEventHandled = false;

        } else if (action == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            int movex = xmove - x;
            int movey = ymove - y;

            if (movex == 0 && movey == 0) {
                tapEventHandled = true;
                ProjectionInterface projection = new AmzLandmarkProjection(mapView);
                if (LandmarkManager.getInstance().findLandmarksInRadius(x, y, projection, true, mapView.getResources().getDisplayMetrics())) {
                    landmarkDetailsHandler.sendEmptyMessage(SHOW_LANDMARK_DETAILS);
                }
            }

        }
        return super.onTouchEvent(event, mapView);
    }

    @Override
    public boolean onTap(GeoPoint p, MapView mapView) {
        //System.out.println("onTap event");
        if (!tapEventHandled) {
            ProjectionInterface projection = new AmzLandmarkProjection(mapView);
            Point point = new Point();
            projection.toPixels(p.getLatitudeE6(), p.getLongitudeE6(), point);
            if (LandmarkManager.getInstance().findLandmarksInRadius(point.x, point.y, projection, true, mapView.getResources().getDisplayMetrics())) {
                landmarkDetailsHandler.sendEmptyMessage(SHOW_LANDMARK_DETAILS);
            }
            return true;
        } else {
            return false;
        }
    }
}
