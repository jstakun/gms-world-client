/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.ui;

import com.google.android.maps.MapView;
import com.jstakun.gms.android.google.maps.GoogleLandmarkProjection;
import com.jstakun.gms.android.osm.maps.OsmLandmarkProjection;
import com.jstakun.gms.android.utils.ProjectionInterface;
import org.osmdroid.api.IMapView;

/**
 *
 * @author jstakun
 */
public class ProjectionFactory {
    public static ProjectionInterface getProjection(IMapView mapView, MapView googleMapView) {
        if (mapView instanceof org.osmdroid.views.MapView) {
            return new OsmLandmarkProjection((org.osmdroid.views.MapView) mapView);
        } else {
            return new GoogleLandmarkProjection(googleMapView);
        }
    }
}
