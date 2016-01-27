package com.jstakun.gms.android.osm.maps;

import android.content.Context;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.routes.RoutesManager;
import org.osmdroid.views.overlay.PathOverlay;

/**
 *
 * @author jstakun
 */
//TODO replace with https://github.com/grote/OSMBonusPack/blob/master/src/org/osmdroid/bonuspack/overlays/Polyline.java
public class OsmPathOverlay extends PathOverlay {
    public OsmPathOverlay(final int color, final Context ctx, RoutesManager routesManager, String routeName) { 
        super(color, ctx);
        if (routesManager.containsRoute(routeName)) {
            for (ExtendedLandmark routePoint : routesManager.getRoute(routeName)) {
                addPoint(routePoint.getLatitudeE6(), routePoint.getLongitudeE6());
            }
        }
    }
}
