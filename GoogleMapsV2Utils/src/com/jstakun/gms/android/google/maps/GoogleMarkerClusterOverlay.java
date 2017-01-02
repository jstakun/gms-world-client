package com.jstakun.gms.android.google.maps;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;

public class GoogleMarkerClusterOverlay implements ClusterManager.OnClusterClickListener<GoogleMarker>, ClusterManager.OnClusterInfoWindowClickListener<GoogleMarker>, ClusterManager.OnClusterItemClickListener<GoogleMarker>, ClusterManager.OnClusterItemInfoWindowClickListener<GoogleMarker> {
	
	public static final int SHOW_LANDMARK_DETAILS = 22;
	public static final int SHOW_LANDMARK_LIST = 23;
	
	private static final int COLOR_WHITE = Color.argb(128, 255, 255, 255); //white
    private static final int COLOR_LIGHT_SALMON = Color.argb(128, 255, 160, 122); //red Light Salmon
    private static final int COLOR_PALE_GREEN = Color.argb(128, 152, 251, 152); //Pale Green
   
	private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	
	private ClusterManager<GoogleMarker> mClusterManager;
	private Handler landmarkDetailsHandler;
	private DisplayMetrics mDisplayMetrics; 
	
	public GoogleMarkerClusterOverlay(Activity activity, GoogleMap map, Handler landmarkDetailsHandler, DisplayMetrics displayMetrics) {
		mClusterManager = new ClusterManager<GoogleMarker>(activity, map);
		mClusterManager.setRenderer(new MarkerRenderer(activity, map));
		this.landmarkDetailsHandler =landmarkDetailsHandler;
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
		LandmarkManager.getInstance().setSelectedLandmark(item.getRelatedObject());
		LandmarkManager.getInstance().clearLandmarkOnFocusQueue();
		landmarkDetailsHandler.sendEmptyMessage(SHOW_LANDMARK_DETAILS);
		return true;
	}

	@Override
	public void onClusterInfoWindowClick(Cluster<GoogleMarker> cluster) {
		// Does nothing, but you could go to a list of markers.		
	}

	@Override
	public boolean onClusterClick(Cluster<GoogleMarker> cluster) {
		LandmarkManager.getInstance().clearLandmarkOnFocusQueue();
		for (GoogleMarker item : cluster.getItems()) {
			LandmarkManager.getInstance().addLandmarkToFocusQueue(item.getRelatedObject());
		}
		Message msg = new Message();
		msg.what = SHOW_LANDMARK_LIST;
		msg.arg1 = MathUtils.coordDoubleToInt(cluster.getPosition().latitude);
		msg.arg2 = MathUtils.coordDoubleToInt(cluster.getPosition().longitude);
		landmarkDetailsHandler.sendMessage(msg);			
		return true;
	}

	public int addMarkers(String layerKey) {
		List<ExtendedLandmark> landmarks = LandmarkManager.getInstance().getLandmarkStoreLayer(layerKey);
		int count = 0;
		for (final ExtendedLandmark landmark : landmarks) {
			if (addMarker(landmark, false)) {
				count++;
			}
		}
		
		if (count > 0) {
			LoggerUtils.debug("Loaded " + count + " markers");
			mClusterManager.cluster();
		}
		return count;
	}
	
	public boolean addMarker(ExtendedLandmark landmark, boolean cluster) {
		boolean added = false;
		GoogleMarker marker = null;
		readWriteLock.writeLock().lock();
		
		try {
			if (landmark.getRelatedUIObject() != null && landmark.getRelatedUIObject() instanceof GoogleMarker) {
				marker = (GoogleMarker)landmark.getRelatedUIObject();
			}
			if (marker == null) {
				marker = new GoogleMarker(landmark);
				landmark.setRelatedUIObject(marker);
				mClusterManager.addItem(marker);
				added = true;
			} else { 
				mClusterManager.addItem(marker);
				added = true;
			}	
		} finally {
			readWriteLock.writeLock().unlock();
		}
		if (cluster) {
			mClusterManager.cluster();
		}
		
		return added;
	}
	
	public void clearMarkers() {
		LoggerUtils.debug("Removing all markers from Google Map!");
		readWriteLock.writeLock().lock();
		try {
			mClusterManager.clearItems();
		} finally {
			readWriteLock.writeLock().unlock();
		}
		mClusterManager.cluster();		
	}
	
	public void removeItem(GoogleMarker item) {
		mClusterManager.removeItem(item);
	}
	
	public void loadAllMarkers() {
		LoggerUtils.debug("Loading all markers to Google Map!");
		clearMarkers();
		for (String layer : LayerManager.getInstance().getLayers()) {
    		if (LayerManager.getInstance().getLayer(layer).getType() != LayerManager.LAYER_DYNAMIC && LayerManager.getInstance().getLayer(layer).isEnabled() && LandmarkManager.getInstance().getLayerSize(layer) > 0) {
    			LoggerUtils.debug("Loading markers from layer " + layer);
    			addMarkers(layer);
    		}      		
    	}
	}
	
	public void cluster() {
		mClusterManager.cluster();
	}
	
	private class MarkerRenderer extends DefaultClusterRenderer<GoogleMarker> {
		
		protected Paint mTextPaint;
		private float mDensity;
		
		public MarkerRenderer(Context context, GoogleMap map) {
			super(context, map, mClusterManager);
			
			mTextPaint = new Paint();
	        mTextPaint.setColor(Color.WHITE);
	        mTextPaint.setFakeBoldText(true);
	        mTextPaint.setTextAlign(Paint.Align.CENTER);
	        mTextPaint.setAntiAlias(true);
	        
	        mDensity = context.getResources().getDisplayMetrics().density;
	    }
		
		@Override
	    protected void onBeforeClusterItemRendered(GoogleMarker marker, MarkerOptions markerOptions) {
			ExtendedLandmark landmark = marker.getRelatedObject();
			
			boolean isMyPosLayer = landmark.getLayer().equals(Commons.MY_POSITION_LAYER);
			
			int color = COLOR_WHITE;
			if (landmark.isCheckinsOrPhotos()) {
				color = COLOR_LIGHT_SALMON;
			} else if (landmark.getRating() >= 0.85) {
				color = COLOR_PALE_GREEN;
			}

			Drawable frame = null;
			
			if (landmark.getCategoryId() != -1) {
        		int icon = LayerManager.getDealCategoryIcon(landmark.getCategoryId(), LayerManager.LAYER_ICON_LARGE);
        		frame = IconCache.getInstance().getCategoryBitmap(icon, Integer.toString(landmark.getCategoryId()), color, !isMyPosLayer, mDisplayMetrics);
    		} else { 
    			BitmapDrawable icon = LayerManager.getLayerIcon(landmark.getLayer(), LayerManager.LAYER_ICON_LARGE, mDisplayMetrics, null);
       			frame = IconCache.getInstance().getLayerBitmap(icon, landmark.getLayer(), color, !isMyPosLayer, mDisplayMetrics);
    		} 
			
			if (frame != null) {
				try {
					markerOptions.icon(BitmapDescriptorFactory.fromBitmap(((BitmapDrawable)frame).getBitmap()));
				} catch (Exception e) {
					LoggerUtils.error("Unable to load bitmap for layer " + marker.getRelatedObject().getLayer(), e);
				}
			}
	    }
		
		@Override
		protected boolean shouldRenderAsCluster(Cluster<GoogleMarker> cluster) {
	        return cluster.getSize() > 1;
	    }
		
		@Override
		protected String getClusterText(int bucket) {
			return Integer.toString(bucket);
		}
		
		@Override
		protected void onBeforeClusterRendered(Cluster<GoogleMarker> cluster, MarkerOptions markerOptions) {
			int clusterSize = cluster.getSize();
	        Bitmap mClusterIcon = null;
	        if (clusterSize < 10) {
	        	mClusterIcon = IconCache.getInstance().getImage(IconCache.MARKER_CLUSTER_M1);
	        	mTextPaint.setTextSize(15f * mDensity);
	        } else if (clusterSize < 100) {
	        	mClusterIcon = IconCache.getInstance().getImage(IconCache.MARKER_CLUSTER_M2);
	        	mTextPaint.setTextSize(15f * mDensity);
	        } else if (clusterSize < 1000) {
	        	mClusterIcon = IconCache.getInstance().getImage(IconCache.MARKER_CLUSTER_M3);
	        	mTextPaint.setTextSize(16f * mDensity);
	        } else if (clusterSize < 10000) {
	        	mClusterIcon = IconCache.getInstance().getImage(IconCache.MARKER_CLUSTER_M4);
	        	mTextPaint.setTextSize(17f * mDensity);
	        } else {
	        	mClusterIcon = IconCache.getInstance().getImage(IconCache.MARKER_CLUSTER_M5);
	        	mTextPaint.setTextSize(18f * mDensity);
	        } 
	        if (mClusterIcon != null) {
	        	Bitmap finalIcon = Bitmap.createBitmap(mClusterIcon.getWidth(), mClusterIcon.getHeight(), mClusterIcon.getConfig());
	        	Canvas iconCanvas = new Canvas(finalIcon);
	        	iconCanvas.drawBitmap(mClusterIcon, 0, 0, null);
	        	int textHeight = (int) (mTextPaint.descent() + mTextPaint.ascent());
	        	iconCanvas.drawText(getClusterText(clusterSize), 0.5f * finalIcon.getWidth(),
	        		0.5f * finalIcon.getHeight() - textHeight / 2, mTextPaint);	        
	        	markerOptions.icon(BitmapDescriptorFactory.fromBitmap(finalIcon));
	        }
		}
	}
}
