package com.jstakun.gms.android.google.maps;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.ProjectionInterface;

import android.graphics.Point;

public class GoogleLandmarkProjectionV2 implements ProjectionInterface {

	private GoogleMap mMap;
	
	public GoogleLandmarkProjectionV2(GoogleMap map) {
		this.mMap = map;
	}
	
	@Override
	public void toPixels(int latE6, int lngE6, Point point) {
		Point p = mMap.getProjection().toScreenLocation(new LatLng(MathUtils.coordIntToDouble(latE6), MathUtils.coordIntToDouble(lngE6)));
	    point.set(p.x, p.y);
	}

	@Override
	public int[] fromPixels(int x, int y) {
		LatLng pos = mMap.getProjection().fromScreenLocation(new Point(x,y));
		return new int[]{MathUtils.coordDoubleToInt(pos.latitude),MathUtils.coordDoubleToInt(pos.longitude)};
	}

	@Override
	public boolean isVisible(int latE6, int lngE6) {
		LatLng point = new LatLng(MathUtils.coordIntToDouble(latE6), MathUtils.coordIntToDouble(lngE6));
		return mMap.getProjection().getVisibleRegion().latLngBounds.contains(point);
	}

	@Override
	public boolean isVisible(Point point) {
		Projection proj = mMap.getProjection();
		LatLng pos = proj.fromScreenLocation(point);
		return proj.getVisibleRegion().latLngBounds.contains(pos);
	}

	@Override
	public BoundingBox getBoundingBox() {
		
		BoundingBox bbox = new BoundingBox();
		Projection proj = mMap.getProjection();
        
        bbox.north = proj.getVisibleRegion().latLngBounds.northeast.latitude;
        bbox.south = proj.getVisibleRegion().latLngBounds.southwest.latitude;
        bbox.east = proj.getVisibleRegion().latLngBounds.northeast.longitude;
        bbox.west = proj.getVisibleRegion().latLngBounds.southwest.longitude;
        
        return bbox;
	}

	@Override
	public float getViewDistance() {
		Projection proj = mMap.getProjection();
		return (float)SphericalUtil.computeDistanceBetween(proj.getVisibleRegion().latLngBounds.northeast, proj.getVisibleRegion().latLngBounds.southwest) / (4f * 1000f); //1/4 in kilometers
	}

}
