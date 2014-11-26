package com.jstakun.gms.android.google.maps;

import android.graphics.Point;
import android.graphics.Rect;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.ProjectionInterface;

/**
 *
 * @author jstakun
 */
public class GoogleLandmarkProjection implements ProjectionInterface {

    private Projection proj;
    private final Point p = new Point();
    private final Rect currentMapBoundsRect = new Rect();
    private int width, height;  
    
    public GoogleLandmarkProjection(MapView mapView) {
        proj = mapView.getProjection();
        mapView.getDrawingRect(currentMapBoundsRect);
        width = mapView.getWidth();
        height = mapView.getHeight();
    }
    
    public void toPixels(int latE6, int lonE6, Point point) {
        GeoPoint g = new GeoPoint(latE6, lonE6);
        proj.toPixels(g, point);
    }

    public int[] fromPixels(int x, int y) {
        GeoPoint g = proj.fromPixels(x, y);
        return new int[]{g.getLatitudeE6(), g.getLongitudeE6()};
    }

    public boolean isVisible(int latE6, int lonE6) {
        boolean isVisible = false;
        
        try {
            toPixels(latE6, lonE6, p);
            isVisible = isVisible(p);
        } catch (Throwable t) {
        } 

        return isVisible;
    }
    
    public boolean isVisible(Point p) {
        //return (p.x >= 0 && p.x <= width && p.y <= height && p.y >= 0);
        return currentMapBoundsRect.contains(p.x, p.y);
    }
    
    //mapView.getWidth(), mapView.getHeight()
    public BoundingBox getBoundingBox() {
    	 GeoPoint nw = proj.fromPixels(0, 0);
    	 
         GeoPoint se = proj.fromPixels(width, height);
  
         BoundingBox bbox = new BoundingBox();
         
         bbox.north = MathUtils.coordIntToDouble(nw.getLatitudeE6());
         bbox.south = MathUtils.coordIntToDouble(se.getLatitudeE6());
         bbox.east = MathUtils.coordIntToDouble(se.getLongitudeE6());
         bbox.west = MathUtils.coordIntToDouble(nw.getLongitudeE6()); 		 
         
         //System.out.println("north: " + bbox.north + ", south: " + bbox.south + ", west: " + bbox.west + ", east: " + bbox.east);
         
         return bbox;
    }
    
    public float getViewDistance() {
    	GeoPoint p1 = proj.fromPixels(3 * width / 8 , height / 2);
        GeoPoint p2 = proj.fromPixels(5 * width / 8, height / 2);
        
        return DistanceUtils.distanceInKilometer(MathUtils.coordIntToDouble(p1.getLatitudeE6()),
        		MathUtils.coordIntToDouble(p1.getLongitudeE6()),
        		MathUtils.coordIntToDouble(p2.getLatitudeE6()),
        		MathUtils.coordIntToDouble(p2.getLongitudeE6()));
    }
}
