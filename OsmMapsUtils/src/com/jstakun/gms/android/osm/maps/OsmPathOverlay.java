/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.osm.maps;

import android.content.Context;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.routes.RoutesManager;
import org.osmdroid.views.overlay.PathOverlay;

/**
 *
 * @author jstakun
 */
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
