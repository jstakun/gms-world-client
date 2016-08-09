package com.jstakun.gms.android.ui;

import android.graphics.Point;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.ProjectionInterface;

public class GoogleLandmarkProjectionV2 implements ProjectionInterface {

	private Projection mProj;
	
	public GoogleLandmarkProjectionV2(GoogleMap map) {
		mProj = map.getProjection();
	}
	
	@Override
	public void toPixels(int latE6, int lngE6, Point point) {
		point = mProj.toScreenLocation(new LatLng(MathUtils.coordIntToDouble(latE6), MathUtils.coordIntToDouble(lngE6)));
	}

	@Override
	public int[] fromPixels(int x, int y) {
		LatLng pos = mProj.fromScreenLocation(new Point(x,y));
		return new int[]{MathUtils.coordDoubleToInt(pos.latitude),MathUtils.coordDoubleToInt(pos.longitude)};
	}

	@Override
	public boolean isVisible(int latE6, int lngE6) {
		double lat = MathUtils.coordIntToDouble(latE6);
		double lng = MathUtils.coordIntToDouble(lngE6);
		return mProj.getVisibleRegion().latLngBounds.contains(new LatLng(lat, lng));
	}

	@Override
	public boolean isVisible(Point point) {
		LatLng pos = mProj.fromScreenLocation(point);
		return mProj.getVisibleRegion().latLngBounds.contains(pos);
	}

	@Override
	public BoundingBox getBoundingBox() {
		
		BoundingBox bbox = new BoundingBox();
        
        bbox.north = mProj.getVisibleRegion().latLngBounds.northeast.latitude;
        bbox.south = mProj.getVisibleRegion().latLngBounds.southwest.latitude;
        bbox.east = mProj.getVisibleRegion().latLngBounds.northeast.longitude;
        bbox.west = mProj.getVisibleRegion().latLngBounds.southwest.longitude;
        
        return bbox;
	}

	@Override
	public float getViewDistance() {
		return (float)SphericalUtil.computeDistanceBetween(mProj.getVisibleRegion().latLngBounds.northeast, mProj.getVisibleRegion().latLngBounds.southwest) / 1000f;
	}

}
