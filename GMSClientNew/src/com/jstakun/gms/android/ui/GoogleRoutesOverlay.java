package com.jstakun.gms.android.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.content.Context;
import android.graphics.Color;

public class GoogleRoutesOverlay {
	
	private LandmarkManager mLm;
	private GoogleMap mMap;
	private RoutesManager routesManager;
	private float mDensity;
	
	public GoogleRoutesOverlay(GoogleMap map, LandmarkManager lm, RoutesManager rm, float density) {
		this.mMap = map;
		this.mLm = lm;
		this.routesManager = rm;      
		this.mDensity = density;
	}
	
	public void showRouteAction(String routeKey, boolean animateTo) {
    	LoggerUtils.debug("Adding route to map view: " + routeKey);
        if (routesManager.containsRoute(routeKey)) {
            if (routeKey.startsWith(RouteRecorder.CURRENTLY_RECORDED) || (!routeKey.startsWith(RouteRecorder.CURRENTLY_RECORDED) && mLm.getLayerManager().isLayerEnabled(Commons.ROUTES_LAYER))) {
            	List<ExtendedLandmark> points = routesManager.getRoute(routeKey);
                if (points.size() > 1) {
                	List<LatLng> pointsLatLng = new ArrayList<LatLng>();
                	for (ExtendedLandmark l : points) {
                		LatLng p = new LatLng(l.getQualifiedCoordinates().getLatitude(), l.getQualifiedCoordinates().getLongitude());
                		pointsLatLng.add(p);
                	}
                
                	mMap.addMarker(new MarkerOptions().position(pointsLatLng.get(0)).icon(BitmapDescriptorFactory.fromResource(R.drawable.start_marker)));
                	mMap.addMarker(new MarkerOptions().position(pointsLatLng.get(pointsLatLng.size()-1)).icon(BitmapDescriptorFactory.fromResource(R.drawable.start_marker)));
                	for (int i=0;i<pointsLatLng.size()-1;i++) {
                		mMap.addPolyline(new PolylineOptions()
                				.add(pointsLatLng.get(i), pointsLatLng.get(i+1))
                				.width(5f * mDensity)
                				.color(Color.RED)
                				.geodesic(true));
                	}
                
                	if (animateTo) {
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
        }
    }
	
	public void loadAllRoutes() {
		if (mLm.getLayerManager().isLayerEnabled(Commons.ROUTES_LAYER)) {
			LoggerUtils.debug("Loading all routes to map view");
			for(String routeKey : routesManager.getRoutes()) {
				showRouteAction(routeKey, false);
			}
		}
	}
	

}
