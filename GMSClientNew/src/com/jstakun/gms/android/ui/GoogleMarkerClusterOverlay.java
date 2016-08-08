package com.jstakun.gms.android.ui;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;

public class GoogleMarkerClusterOverlay implements ClusterManager.OnClusterClickListener<GoogleMarker>, ClusterManager.OnClusterInfoWindowClickListener<GoogleMarker>, ClusterManager.OnClusterItemClickListener<GoogleMarker>, ClusterManager.OnClusterItemInfoWindowClickListener<GoogleMarker> {
	
	public static final int SHOW_LANDMARK_DETAILS = 22;
	public static final int SHOW_LANDMARK_LIST = 23;
	
	private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	
	private ClusterManager<GoogleMarker> mClusterManager;
	private Handler landmarkDetailsHandler;
	
	public GoogleMarkerClusterOverlay(Activity activity, GoogleMap map, Handler landmarkDetailsHandler) {
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
		//lm.setSelectedLandmark(item.getRelatedObject());
		//lm.clearLandmarkOnFocusQueue();
		landmarkDetailsHandler.sendEmptyMessage(SHOW_LANDMARK_DETAILS);
		return true;
	}

	@Override
	public void onClusterInfoWindowClick(Cluster<GoogleMarker> cluster) {
		// Does nothing, but you could go to a list of markers.		
	}

	@Override
	public boolean onClusterClick(Cluster<GoogleMarker> cluster) {
		//lm.clearLandmarkOnFocusQueue();
		for (GoogleMarker item : cluster.getItems()) {
			//lm.addLandmarkToFocusQueue(item.getRelatedObject());
		}
		Message msg = new Message();
		msg.what = SHOW_LANDMARK_LIST;
		//msg.arg1 = selected.getPosition().getLatitudeE6();
		//msg.arg2 = selected.getPosition().getLongitudeE6();
		landmarkDetailsHandler.sendMessage(msg);			
		return true;
	}

	public void addMarkers(List<ExtendedLandmark> landmarks) {
		boolean added = false;
		for (final ExtendedLandmark landmark : landmarks) {
			readWriteLock.writeLock().lock();
			
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
				//Log.d(this.getClass().getName(), "Adding marker " + landmark.getName());
				mClusterManager.addItem(marker);
				added = true;
			} else if (!mClusterManager.getMarkerCollection().getMarkers().contains(marker)) {
				//Log.d(this.getClass().getName(), "Adding marker " + landmark.getName());
				mClusterManager.addItem(marker);
				added = true;
			}
			readWriteLock.writeLock().unlock();	
		}
		
		if (added) {
			mClusterManager.cluster();
		}
	}
	
	public void clearMarkers() {
		if (!mClusterManager.getMarkerCollection().getMarkers().isEmpty()) {
			readWriteLock.writeLock().lock();
			mClusterManager.clearItems();
			readWriteLock.writeLock().unlock();
		}
	}
}
