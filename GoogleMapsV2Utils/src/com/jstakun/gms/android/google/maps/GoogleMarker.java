package com.jstakun.gms.android.google.maps;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;

public class GoogleMarker implements ClusterItem {

	private final LatLng mPosition;
    private ExtendedLandmark mRelatedObject;
	
    public GoogleMarker(ExtendedLandmark landmark) {
    	this.mRelatedObject = landmark;
        mPosition = new LatLng(landmark.getQualifiedCoordinates().getLatitude(), landmark.getQualifiedCoordinates().getLongitude());
    }
    
    @Override
	public LatLng getPosition() {
		return mPosition;
	}
	
	public ExtendedLandmark getRelatedObject() {
		return mRelatedObject;
	}

}
