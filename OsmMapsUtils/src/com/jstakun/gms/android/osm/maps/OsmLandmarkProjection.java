/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.osm.maps;

import android.graphics.Point;
import android.graphics.Rect;
import com.jstakun.gms.android.utils.ProjectionInterface;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

/**
 *
 * @author jstakun
 */
public class OsmLandmarkProjection implements ProjectionInterface {

    private final Rect viewportRect = new Rect();
    private final Point p = new Point();
    private final Point tmp = new Point();
    private Projection proj = null;
            
    public OsmLandmarkProjection(MapView mapView) {
    	proj = mapView.getProjection();
        viewportRect.set(proj.getScreenRect());
    }

    public void toPixels(int latE6, int lonE6, Point point) {
        //GeoPoint g = new GeoPoint(latE6, lonE6);
        //proj.toPixels(g, point);
        proj.toProjectedPixels(latE6, lonE6, tmp);
        proj.toPixelsFromProjected(tmp, point);
    }

    public boolean isVisible(int latE6, int lonE6) {
        try {
            toPixels(latE6, lonE6, p);
            return isVisible(p);
        } catch (Throwable t) {
            return false;
        }
    }

    public int[] fromPixels(int x, int y) {
        IGeoPoint g = proj.fromPixels(x, y);
        return new int[]{g.getLatitudeE6(), g.getLongitudeE6()};
    }

    public boolean isVisible(Point p) {
        return viewportRect.contains(p.x, p.y);
        //return (p.x >= viewportRect.left && p.x <= viewportRect.right && p.y <= viewportRect.bottom && p.y >= viewportRect.top);
    }
}
