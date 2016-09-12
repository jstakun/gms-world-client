package com.jstakun.gms.android.google.maps;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.graphics.Color;

public class GoogleRoutesOverlay {
	
	private GoogleMap mMap;
	private float mDensity;
	private GoogleMarkerClusterOverlay mMarkerCluster;
	private Polyline mRoutePolyline = null;
	
	public GoogleRoutesOverlay(GoogleMap map, GoogleMarkerClusterOverlay markerCluster, float density) {
		this.mMap = map;
		this.mDensity = density;
		this.mMarkerCluster = markerCluster;
	}
	
	public void showRouteAction(String routeKey, boolean animateTo) {
    	LoggerUtils.debug("Adding route to map view: " + routeKey);
    	if (routeKey.startsWith(RouteRecorder.CURRENTLY_RECORDED) || (!routeKey.startsWith(RouteRecorder.CURRENTLY_RECORDED) && LayerManager.getInstance().isLayerEnabled(Commons.ROUTES_LAYER))) {
    		drawRoute(routeKey, animateTo);
        } 
    }
	
	public void showRecordedRoute() {
		drawRoute(RouteRecorder.CURRENTLY_RECORDED, false);
	}
	
	private void drawRoute(String routeKey, boolean animateTo) {
		List<ExtendedLandmark> points = RoutesManager.getInstance().getRoute(routeKey);
        LoggerUtils.debug("Drawing route " + routeKey + " containing " + points.size() + " points");
        if (points.size() > 1) {            	
        	boolean isCurrentlyRecorded = routeKey.equals(RouteRecorder.CURRENTLY_RECORDED);
            List<LatLng> pointsLatLng = new ArrayList<LatLng>();
        	for (ExtendedLandmark l : points) {
                LatLng p = new LatLng(l.getQualifiedCoordinates().getLatitude(), l.getQualifiedCoordinates().getLongitude());
                pointsLatLng.add(p);
           	}
                
        	//final int resourceId = R.drawable.bullet_blue; //.start_marker;
        	
        	//mMap.addMarker(new MarkerOptions().position(pointsLatLng.get(0)).icon(BitmapDescriptorFactory.fromResource(resourceId)));
           	mMarkerCluster.addMarker(points.get(0), true);
            
        	if (!isCurrentlyRecorded) {
        		//mMap.addMarker(new MarkerOptions().position(pointsLatLng.get(pointsLatLng.size()-1)).icon(BitmapDescriptorFactory.fromResource(resourceId)));
                mMarkerCluster.addMarker(points.get(points.size()-1), true);
           	}
            
           	if (isCurrentlyRecorded && mRoutePolyline != null) {
            	mRoutePolyline.remove();
            }
            	
            Polyline po = mMap.addPolyline(new PolylineOptions()
            			.addAll(pointsLatLng)
            			.width(5f * mDensity)
            			.color(Color.RED)
            			.geodesic(true));
            	
            if (isCurrentlyRecorded) {           		
            	mRoutePolyline = po;
            }
            	
            if (animateTo && !isCurrentlyRecorded) {
            	LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng p : pointsLatLng) {
                	builder.include(p);
                }
            	LatLngBounds bounds = builder.build();
            	int padding = (int)(8 * mDensity); // offset from edges of the map in pixels
            	CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            	mMap.animateCamera(cu);
            }
        }
	}
	
	public void loadAllRoutes() {
		if (LayerManager.getInstance().isLayerEnabled(Commons.ROUTES_LAYER)) {
			LoggerUtils.debug("Loading all routes to map view");
			for(String routeKey : RoutesManager.getInstance().getRoutes()) {
				showRouteAction(routeKey, false);
			}
		}
	}
}
