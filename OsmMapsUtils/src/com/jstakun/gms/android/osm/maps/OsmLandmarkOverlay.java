package com.jstakun.gms.android.osm.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.MotionEvent;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.utils.ProjectionInterface;
import java.util.List;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

/**
 *
 * @author jstakun
 */
public class OsmLandmarkOverlay extends Overlay {

    public static final int SHOW_LANDMARK_DETAILS = 22;
    
    private LandmarkManager lm;
    private Handler landmarkDetailsHandler;
    private String[] excluded;
    private final Rect viewportRect = new Rect();

    public OsmLandmarkOverlay(Context context, LandmarkManager lm, Handler landmarkDetailsHandler) {
        super(context);
        this.landmarkDetailsHandler = landmarkDetailsHandler;
        this.lm = lm;
        this.excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER};
    }

    public OsmLandmarkOverlay(Context context, LandmarkManager lm, Handler landmarkDetailsHandler, String[] excluded) {
        super(context);
        this.landmarkDetailsHandler = landmarkDetailsHandler;
        this.lm = lm;
        this.excluded = excluded;
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow == false && lm != null) {
            ProjectionInterface projection = new OsmLandmarkProjection(mapView);
            lm.paintLandmarks(canvas, projection, mapView.getWidth(), mapView.getHeight(), excluded, mapView.getResources().getDisplayMetrics());

            List<Drawable> landmarkDrawables = lm.getLandmarkDrawables();
            for (Drawable d : landmarkDrawables) {
                d.draw(canvas);
            }

            Drawable selectedLandmark = lm.getSelectedLandmarkDrawable();
            if (selectedLandmark != null) {
                selectedLandmark.draw(canvas);
            }
        }
        //no shadow by default
    }

    /*@Override
     public boolean onTouchEvent(android.view.MotionEvent event, MapView mapView) {
     final int action = event.getAction();

     if (event.getAction() == MotionEvent.ACTION_DOWN) {
     //System.out.println("Action Down x: " + event.getX() + " y: " + event.getY());
     xmove = (int) event.getX();
     ymove = (int) event.getY();
     } else if (action == MotionEvent.ACTION_UP) {
     int x = (int) event.getX();
     int y = (int) event.getY();
     int movex = xmove - x;
     int movey = ymove - y;

     if (movex == 0 && movey == 0) {
     ProjectionInterface proj = new OsmLandmarkProjection(mapView);
     final Projection projection = mapView.getProjection();
     viewportRect.set(projection.getScreenRect());
     if (lm.findLandmarksInRadius(viewportRect.left + x, viewportRect.top + y, proj, true, mapView.getResources().getDisplayMetrics())) {
     landmarkDetailsHandler.sendEmptyMessage(0);
     }
     }
     }
     return super.onTouchEvent(event, mapView);
     }*/
    @Override
    public boolean onSingleTapUp(final MotionEvent e, final MapView mapView) {
        //System.out.println("onSingleTapUp");
        ProjectionInterface proj = new OsmLandmarkProjection(mapView);
        viewportRect.set(mapView.getProjection().getScreenRect());
        if (lm.findLandmarksInRadius(viewportRect.left + (int) e.getX(), viewportRect.top + (int) e.getY(), proj, true, mapView.getResources().getDisplayMetrics())) {
            landmarkDetailsHandler.sendEmptyMessage(SHOW_LANDMARK_DETAILS);
        }
        return true;
    }
}
