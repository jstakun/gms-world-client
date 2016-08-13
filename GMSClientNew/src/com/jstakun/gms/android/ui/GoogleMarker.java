package com.jstakun.gms.android.ui;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;

import android.graphics.drawable.Drawable;

public class GoogleMarker implements ClusterItem {

	private final LatLng mPosition;
    private Drawable mIcon;
    private ExtendedLandmark mRelatedObject;
	
    public GoogleMarker(ExtendedLandmark landmark, Drawable icon) {
    	this.mRelatedObject = landmark;
    	this.mIcon = icon;
        mPosition = new LatLng(landmark.getQualifiedCoordinates().getLatitude(), landmark.getQualifiedCoordinates().getLongitude());
    }
	
	@Override
	public LatLng getPosition() {
		return mPosition;
	}
	
	public Drawable getIcon() {
		return mIcon;
	}
	
	public ExtendedLandmark getRelatedObject() {
		return mRelatedObject;
	}

}
