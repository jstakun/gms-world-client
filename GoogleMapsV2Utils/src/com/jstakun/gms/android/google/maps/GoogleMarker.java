package com.jstakun.gms.android.google.maps;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;

public class GoogleMarker implements ClusterItem {

	private final LatLng mPosition;
    //private Drawable mIcon;
    private ExtendedLandmark mRelatedObject;
	
    public GoogleMarker(ExtendedLandmark landmark) {
    	this.mRelatedObject = landmark;
        mPosition = new LatLng(landmark.getQualifiedCoordinates().getLatitude(), landmark.getQualifiedCoordinates().getLongitude());
    }
    
    //public GoogleMarker(ExtendedLandmark landmark, Drawable icon) {
    //	this(landmark);
    //	this.mIcon = icon;
    //}
	
	@Override
	public LatLng getPosition() {
		return mPosition;
	}
	
	//public Drawable getIcon() {
	//	return mIcon;
	//}
	
	public ExtendedLandmark getRelatedObject() {
		return mRelatedObject;
	}

}
