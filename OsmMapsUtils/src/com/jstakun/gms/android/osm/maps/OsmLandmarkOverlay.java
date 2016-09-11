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
    
    private Handler landmarkDetailsHandler;
    private String[] excluded;
    private final Rect viewportRect = new Rect();

    public OsmLandmarkOverlay(Context context, Handler landmarkDetailsHandler) {
        super(context);
        this.landmarkDetailsHandler = landmarkDetailsHandler;
        this.excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER};
    }

    public OsmLandmarkOverlay(Context context, Handler landmarkDetailsHandler, String[] excluded) {
        super(context);
        this.landmarkDetailsHandler = landmarkDetailsHandler;
        this.excluded = excluded;
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow == false) {
            ProjectionInterface projection = new OsmLandmarkProjection(mapView);
            LandmarkManager.getInstance().paintLandmarks(canvas, projection, mapView.getWidth(), mapView.getHeight(), excluded, mapView.getResources().getDisplayMetrics());

            List<Drawable> landmarkDrawables = LandmarkManager.getInstance().getLandmarkDrawables();
            for (Drawable d : landmarkDrawables) {
                d.draw(canvas);
            }

            Drawable selectedLandmark = LandmarkManager.getInstance().getSelectedLandmarkDrawable();
            if (selectedLandmark != null) {
                selectedLandmark.draw(canvas);
            }
        }
        //no shadow by default
    }

    @Override
    public boolean onSingleTapUp(final MotionEvent e, final MapView mapView) {
        //System.out.println("onSingleTapUp");
        ProjectionInterface proj = new OsmLandmarkProjection(mapView);
        viewportRect.set(mapView.getProjection().getScreenRect());
        if (LandmarkManager.getInstance().findLandmarksInRadius(viewportRect.left + (int) e.getX(), viewportRect.top + (int) e.getY(), proj, true, mapView.getResources().getDisplayMetrics())) {
            landmarkDetailsHandler.sendEmptyMessage(SHOW_LANDMARK_DETAILS);
        }
        return true;
    }
}
