package com.jstakun.gms.android.osm.maps;

import android.graphics.Point;
import android.graphics.Rect;

import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.MathUtils;
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
    private int width, height;  
            
    public OsmLandmarkProjection(MapView mapView) {
    	proj = mapView.getProjection();
        viewportRect.set(proj.getScreenRect());
        width = mapView.getWidth();
        height = mapView.getHeight();
    }

    public void toPixels(int latE6, int lonE6, Point point) {
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
    
    public BoundingBox getBoundingBox() {
   	 	IGeoPoint nw = proj.fromPixels(0, 0);
   	 
        IGeoPoint se = proj.fromPixels(width, height);
 
        BoundingBox bbox = new BoundingBox();
        
        bbox.north = MathUtils.coordIntToDouble(nw.getLatitudeE6());
        bbox.south = MathUtils.coordIntToDouble(se.getLatitudeE6());
        bbox.east = MathUtils.coordIntToDouble(se.getLongitudeE6());
        bbox.west = MathUtils.coordIntToDouble(nw.getLongitudeE6()); 		 
        
        //System.out.println("north: " + bbox.north + ", south: " + bbox.south + ", west: " + bbox.west + ", east: " + bbox.east);
        
        return bbox;
   }
    
   public float getViewDistance() {
    	IGeoPoint p1 = proj.fromPixels(3 * width / 8, height / 2);
        IGeoPoint p2 = proj.fromPixels(5 * width / 8, height / 2);
        
        return DistanceUtils.distanceInKilometer(MathUtils.coordIntToDouble(p1.getLatitudeE6()),
        		MathUtils.coordIntToDouble(p1.getLongitudeE6()),
        		MathUtils.coordIntToDouble(p2.getLatitudeE6()),
        		MathUtils.coordIntToDouble(p2.getLongitudeE6()));
   }
}
