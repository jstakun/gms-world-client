package com.jstakun.gms.android.ui;

import java.util.List;

import android.app.Activity;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;

public class GoogleMarkerClusterOverlay implements ClusterManager.OnClusterClickListener<GoogleMarker>, ClusterManager.OnClusterInfoWindowClickListener<GoogleMarker>, ClusterManager.OnClusterItemClickListener<GoogleMarker>, ClusterManager.OnClusterItemInfoWindowClickListener<GoogleMarker> {
	
	private ClusterManager<GoogleMarker> mClusterManager;
	
	public GoogleMarkerClusterOverlay(Activity activity, GoogleMap map) {
		mClusterManager = new ClusterManager<GoogleMarker>(activity, map);
		map.setOnMarkerClickListener(mClusterManager);
        map.setOnInfoWindowClickListener(mClusterManager);
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterInfoWindowClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);
        mClusterManager.setOnClusterItemInfoWindowClickListener(this);
	}

	@Override
	public void onClusterItemInfoWindowClick(GoogleMarker item) {
		// Does nothing, but you could go into the landmark view
	}

	@Override
	public boolean onClusterItemClick(GoogleMarker item) {
		// show landmark details view
		return false;
	}

	@Override
	public void onClusterInfoWindowClick(Cluster<GoogleMarker> cluster) {
		// Does nothing, but you could go to a list of markers.
		
	}

	@Override
	public boolean onClusterClick(Cluster<GoogleMarker> cluster) {
		for (GoogleMarker item : cluster.getItems()) {
			//TODO send ids of landmarks to be shown in the list
		}
		return true;
	}

	public void addMarkers(List<ExtendedLandmark> landmarks) {
		for (final ExtendedLandmark landmark : landmarks) {
			synchronized (landmark) {
				GoogleMarker marker = null;
			
				if (landmark.getRelatedUIObject() != null && landmark.getRelatedUIObject() instanceof GoogleMarker) {
					marker = (GoogleMarker)landmark.getRelatedUIObject();
				}
				if (marker == null) {
					int icon = -1;
					if (landmark.getCategoryId() != -1) {
                		//TODO implement later
						icon = -1; //LayerManager.getDealCategoryIcon(landmark.getCategoryId(), LayerManager.LAYER_ICON_LARGE);
                	} else {
                		icon = -1;
            		}
					marker = new GoogleMarker(landmark, icon);
					landmark.setRelatedUIObject(marker);
					mClusterManager.addItem(marker);
				} else if (!mClusterManager.getMarkerCollection().getMarkers().contains(marker)) {
					mClusterManager.addItem(marker);
				}
			}	
		}
		mClusterManager.cluster();	
	}
	
	public void clearMarkers() {
		if (!mClusterManager.getMarkerCollection().getMarkers().isEmpty()) {
			mClusterManager.clearItems();
		}
	}
}
