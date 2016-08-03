package com.jstakun.gms.android.ui;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;

public class GoogleMarker implements ClusterItem {

	private final LatLng mPosition;
    private int mIcon;
    private ExtendedLandmark mRelatedObject;
	
    public GoogleMarker(ExtendedLandmark landmark, int icon) {
    	this.mRelatedObject = landmark;
    	this.mIcon = icon;
        mPosition = new LatLng(landmark.getLatitudeE6(), landmark.getLongitudeE6());
    }
	
	@Override
	public LatLng getPosition() {
		return mPosition;
	}
	
	public int getIcon() {
		return mIcon;
	}
	
	public ExtendedLandmark getRelatedObject() {
		return mRelatedObject;
	}

}
