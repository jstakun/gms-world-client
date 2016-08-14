package com.jstakun.gms.android.ui;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.StringUtils;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.osm.maps.R;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.widget.ImageView;

public class GoogleMarkerClusterOverlay implements ClusterManager.OnClusterClickListener<GoogleMarker>, ClusterManager.OnClusterInfoWindowClickListener<GoogleMarker>, ClusterManager.OnClusterItemClickListener<GoogleMarker>, ClusterManager.OnClusterItemInfoWindowClickListener<GoogleMarker> {
	
	public static final int SHOW_LANDMARK_DETAILS = 22;
	public static final int SHOW_LANDMARK_LIST = 23;
	
	private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	
	private ClusterManager<GoogleMarker> mClusterManager;
	private Handler landmarkDetailsHandler;
	private LandmarkManager lm;
	private DisplayMetrics mDisplayMetrics; 
	
	public GoogleMarkerClusterOverlay(Activity activity, GoogleMap map, Handler landmarkDetailsHandler, LandmarkManager lm, DisplayMetrics displayMetrics) {
		mClusterManager = new ClusterManager<GoogleMarker>(activity, map);
		mClusterManager.setRenderer(new MarkerRenderer(activity, map));
		this.landmarkDetailsHandler =landmarkDetailsHandler;
		this.lm = lm;
		this.mDisplayMetrics = displayMetrics;
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
		lm.setSelectedLandmark(item.getRelatedObject());
		lm.clearLandmarkOnFocusQueue();
		landmarkDetailsHandler.sendEmptyMessage(SHOW_LANDMARK_DETAILS);
		return true;
	}

	@Override
	public void onClusterInfoWindowClick(Cluster<GoogleMarker> cluster) {
		// Does nothing, but you could go to a list of markers.		
	}

	@Override
	public boolean onClusterClick(Cluster<GoogleMarker> cluster) {
		lm.clearLandmarkOnFocusQueue();
		for (GoogleMarker item : cluster.getItems()) {
			lm.addLandmarkToFocusQueue(item.getRelatedObject());
		}
		Message msg = new Message();
		msg.what = SHOW_LANDMARK_LIST;
		msg.arg1 = MathUtils.coordDoubleToInt(cluster.getPosition().latitude);
		msg.arg2 = MathUtils.coordDoubleToInt(cluster.getPosition().longitude);
		landmarkDetailsHandler.sendMessage(msg);			
		return true;
	}

	public void addMarkers(String layerKey) {
		List<ExtendedLandmark> landmarks = lm.getLandmarkStoreLayer(layerKey);
		int count = 0;
		for (final ExtendedLandmark landmark : landmarks) {
			if (addMarker(landmark)) {
				count++;
			}
		}
		
		if (count > 0) {
			mClusterManager.cluster();
		}
		//LoggerUtils.debug(count + " markers from layer " + layerKey + " stored in cluster.");
	}
	
	public boolean addMarker(ExtendedLandmark landmark) {
		boolean added = false;
		readWriteLock.writeLock().lock();
		GoogleMarker marker = null;
		
		if (landmark.getRelatedUIObject() != null && landmark.getRelatedUIObject() instanceof GoogleMarker) {
			marker = (GoogleMarker)landmark.getRelatedUIObject();
		}
		if (marker == null) {
			Drawable icon = null;
			if (landmark.getCategoryId() != -1) {
				int iconId = LayerManager.getDealCategoryIcon(landmark.getCategoryId(), LayerManager.LAYER_ICON_LARGE);
				icon = IconCache.getInstance().getCategoryBitmap(iconId, Integer.toString(landmark.getCategoryId()), -1, false, mDisplayMetrics);
			} else if (!StringUtils.equals(landmark.getLayer(), Commons.LOCAL_LAYER)) { 
				//doesn't work with local layer
				icon = LayerManager.getLayerIcon(landmark.getLayer(), LayerManager.LAYER_ICON_LARGE, mDisplayMetrics, null);
			} else if (StringUtils.equals(landmark.getLayer(), Commons.LOCAL_LAYER)) {
        		icon = IconCache.getInstance().getCategoryBitmap(R.drawable.ok, "local", -1, false, null);
        	}
			marker = new GoogleMarker(landmark, icon);
			landmark.setRelatedUIObject(marker);
			mClusterManager.addItem(marker);
			added = true;
		} else if (!mClusterManager.getMarkerCollection().getMarkers().contains(marker)) {
			mClusterManager.addItem(marker);
			added = true;
		}
		readWriteLock.writeLock().unlock();
		return added;
	}
	
	public void clearMarkers() {
		if (!mClusterManager.getMarkerCollection().getMarkers().isEmpty()) {
			LoggerUtils.debug("Removing all markers from Google Map!");
			readWriteLock.writeLock().lock();
			mClusterManager.clearItems();
			readWriteLock.writeLock().unlock();
			mClusterManager.cluster();
		}
	}
	
	public void loadAllMarkers() {
		LoggerUtils.debug("Loading all markers to Google Map!");
		clearMarkers();
		for (String layer : lm.getLayerManager().getLayers()) {
    		if (lm.getLayerManager().getLayer(layer).getType() != LayerManager.LAYER_DYNAMIC && lm.getLayerManager().getLayer(layer).isEnabled() && lm.getLayerSize(layer) > 0) {
    			addMarkers(layer);
    		}      		
    	}
	}
	
	public void cluster() {
		mClusterManager.cluster();
	}
	
	private class MarkerRenderer extends DefaultClusterRenderer<GoogleMarker> {

		private final IconGenerator mIconGenerator;
		private final ImageView mImageView;
		
		public MarkerRenderer(Context context, GoogleMap map) {
			super(context, map, mClusterManager);
			
			mImageView = new ImageView(context);
			mIconGenerator = new IconGenerator(context);
            //mDimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
            //mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
            //int padding = (int) getResources().getDimension(R.dimen.custom_profile_padding);
            //mImageView.setPadding(padding, padding, padding, padding);
            mIconGenerator.setContentView(mImageView);
		}
		
		@Override
	    protected void onBeforeClusterItemRendered(GoogleMarker marker, MarkerOptions markerOptions) {
	            // Draw a single person.
	            // Set the info window to show their name.
			mImageView.setImageDrawable(marker.getIcon());
			Bitmap icon = mIconGenerator.makeIcon();
	        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));//.title("title");
	    }
		
		@Override
		protected boolean shouldRenderAsCluster(Cluster<GoogleMarker> cluster) {
	        return cluster.getSize() > 1;
	    }
		
	}
}
