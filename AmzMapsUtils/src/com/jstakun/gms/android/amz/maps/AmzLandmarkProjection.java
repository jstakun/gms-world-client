/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.amz.maps;

import android.graphics.Point;
import android.graphics.Rect;
import com.amazon.geo.maps.GeoPoint;
import com.amazon.geo.maps.MapView;
import com.amazon.geo.maps.Projection;
import com.jstakun.gms.android.utils.ProjectionInterface;

/**
 *
 * @author jstakun
 */
public class AmzLandmarkProjection implements ProjectionInterface {

    private Projection proj;
    private final Point p = new Point();
    private final Rect currentMapBoundsRect = new Rect();
    
    public AmzLandmarkProjection(MapView mapView) {
        proj = mapView.getProjection();
        mapView.getDrawingRect(currentMapBoundsRect);
    }

    public void toPixels(int latE6, int lonE6, Point point) {
        GeoPoint g = new GeoPoint(latE6, lonE6);
        proj.toPixels(g, point);
    }

    public int[] fromPixels(int x, int y) {
        GeoPoint g = proj.fromPixels(x, y);
        return new int[] {g.getLatitudeE6(), g.getLongitudeE6()};
    }

    public boolean isVisible(int latE6, int lonE6) {
        try {
            toPixels(latE6, lonE6, p);
            return isVisible(p);
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean isVisible(Point p) {
        return currentMapBoundsRect.contains(p.x, p.y);
    }

}
