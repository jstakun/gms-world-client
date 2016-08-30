package com.jstakun.gms.android.osm.maps;

import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.views.MapView;

import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.graphics.Canvas;

public class OsmMarker extends Marker {

	public OsmMarker(MapView mapView) {
		super(mapView);
	}
	
	public OsmMarker(MapView mapView, final ResourceProxy resourceProxy) {
		super(mapView, resourceProxy);
	}
	
	@Override 
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		try {
			super.draw(canvas, mapView, shadow);
		} catch (Exception e) {
			LoggerUtils.error("Unable to load bitmap for layer " + getLayer(), e);
		}
	}

	private String getLayer() {
		Object o = getRelatedObject();
		if (o != null && o instanceof ExtendedLandmark) {
			return ((ExtendedLandmark) o).getLayer();
		} else {
			return null;
		}
	}
}
