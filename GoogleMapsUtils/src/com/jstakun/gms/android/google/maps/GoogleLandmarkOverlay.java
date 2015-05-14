package com.jstakun.gms.android.google.maps;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.MotionEvent;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.ProjectionInterface;
import java.util.List;

/**
 *
 * @author jstakun
 */
public class GoogleLandmarkOverlay extends Overlay {

    public static final int SHOW_LANDMARK_DETAILS = 20;
    
    private LandmarkManager lm;
    private Handler landmarkDetailsHandler;
    private int xmove, ymove;
    private boolean tapEventHandled;
    private String[] excluded;
    
    public GoogleLandmarkOverlay(LandmarkManager lm, Handler landmarkDetailsHandler) {
        this.landmarkDetailsHandler = landmarkDetailsHandler;
        this.lm = lm;
        this.excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER};
    }

    public GoogleLandmarkOverlay(LandmarkManager lm, Handler landmarkDetailsHandler, String[] excluded) {
        this.landmarkDetailsHandler = landmarkDetailsHandler;
        this.lm = lm;
        this.excluded = excluded;
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        try {
            super.draw(canvas, mapView, shadow);
            if (shadow == false && lm != null) {

                lm.paintLandmarks(canvas, new GoogleLandmarkProjection(mapView), mapView.getWidth(), mapView.getHeight(), excluded, mapView.getResources().getDisplayMetrics());
                
                List<Drawable> landmarkDrawables = lm.getLandmarkDrawables();
                for (Drawable d : landmarkDrawables) {
                    d.draw(canvas);
                }

                Drawable selectedLandmark = lm.getSelectedLandmarkDrawable();
                if (selectedLandmark != null) {
                    selectedLandmark.draw(canvas);
                }
                
                //System.out.println("GoogleLandmarkOverlay.draw() ------------------------------------");
            }
            
        
        } catch (Exception e) {
            LoggerUtils.error("GoogleLandmarkOverlay.draw() exception", e);
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
                ProjectionInterface projection = new GoogleLandmarkProjection(mapView);
                if (lm.findLandmarksInRadius(x, y, projection, true, mapView.getResources().getDisplayMetrics())) {
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
            ProjectionInterface projection = new GoogleLandmarkProjection(mapView);
            Point point = new Point();
            projection.toPixels(p.getLatitudeE6(), p.getLongitudeE6(), point);
            if (lm.findLandmarksInRadius(point.x, point.y, projection, true, mapView.getResources().getDisplayMetrics())) {
                landmarkDetailsHandler.sendEmptyMessage(SHOW_LANDMARK_DETAILS);
            }
            return true;
        } else {
            return false;
        }
    }
}
