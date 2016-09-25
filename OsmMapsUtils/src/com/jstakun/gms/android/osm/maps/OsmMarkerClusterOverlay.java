package com.jstakun.gms.android.osm.maps;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.clustering.StaticCluster;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;

public class OsmMarkerClusterOverlay extends RadiusMarkerClusterer {

	public static final int SHOW_LANDMARK_DETAILS = 22;
	public static final int SHOW_LANDMARK_LIST = 23;
	
	//private static final int COLOR_WHITE = Color.argb(128, 255, 255, 255); //white
    //private static final int COLOR_LIGHT_SALMON = Color.argb(128, 255, 160, 122); //red Light Salmon
    //private static final int COLOR_PALE_GREEN = Color.argb(128, 152, 251, 152); //Pale Green
    
    private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    
    private Handler landmarkDetailsHandler;
	private float mDensity;
	
	public OsmMarkerClusterOverlay(Context ctx, Handler landmarkDetailsHandler) {
		super(ctx);
		this.landmarkDetailsHandler = landmarkDetailsHandler;
    
		//custom radius
		setRadius((int)(48f * ctx.getResources().getDisplayMetrics().density));
		
		setMaxClusteringZoomLevel(18);
		
		//and text
		mDensity = ctx.getResources().getDisplayMetrics().density;
		getTextPaint().setTypeface(Typeface.DEFAULT_BOLD);
		//this.mAnchorV = Marker.ANCHOR_BOTTOM;
		//this.mTextAnchorU = 0.70f;
		//this.mTextAnchorV = 0.27f;
	}
	
	@Override
	public Marker buildClusterMarker(final StaticCluster cluster, MapView mapView) {
		Marker m = new OsmMarker(mapView);
        m.setPosition(cluster.getPosition());
        m.setInfoWindow(null);
        m.setAnchor(mAnchorU, mAnchorV);

        int clusterSize = cluster.getSize();
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
        
        Bitmap finalIcon = Bitmap.createBitmap(mClusterIcon.getWidth(), mClusterIcon.getHeight(), mClusterIcon.getConfig());
        Canvas iconCanvas = new Canvas(finalIcon);
        iconCanvas.drawBitmap(mClusterIcon, 0, 0, null);
        int textHeight = (int) (mTextPaint.descent() + mTextPaint.ascent());
        iconCanvas.drawText(Integer.toString(clusterSize), mTextAnchorU * finalIcon.getWidth(),
                mTextAnchorV * finalIcon.getHeight() - textHeight / 2, mTextPaint);
        m.setIcon(new BitmapDrawable(mapView.getContext().getResources(), finalIcon));
		
		m.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
			@Override
			public boolean onMarkerClick(Marker selected, MapView arg1) {
				LandmarkManager.getInstance().clearLandmarkOnFocusQueue();
				for (int i=0;i<cluster.getSize();i++) {
					Marker m = cluster.getItem(i);
					Object o = m.getRelatedObject();
					if (o instanceof ExtendedLandmark) {
						LandmarkManager.getInstance().addLandmarkToFocusQueue((ExtendedLandmark)o);
					}
				}
				Message msg = new Message();
				msg.what = SHOW_LANDMARK_LIST;
				msg.arg1 = selected.getPosition().getLatitudeE6();
				msg.arg2 = selected.getPosition().getLongitudeE6();
				landmarkDetailsHandler.sendMessage(msg);			
				return true;
			}
		});
		return m;
	}
	
	public void addMarkers(String layerKey, MapView mapView) {
		List<ExtendedLandmark> landmarks = LandmarkManager.getInstance().getLandmarkStoreLayer(layerKey);
		//LoggerUtils.debug("Loading " + landmarks.size() + " markers from layer " + layerKey);
		readWriteLock.writeLock().lock();
		int size = getItems().size();
		try {
			for (final ExtendedLandmark landmark : landmarks) {		
				addMarker(landmark, mapView);			
			}
		} finally {
			readWriteLock.writeLock().unlock();
		}
		//LoggerUtils.debug(getItems().size() + " markers stored in cluster.");
		if (getItems().size() > size) {
			invalidate();
		}
	}
	
	public void addMarker(ExtendedLandmark landmark, MapView mapView) {
		Marker marker = null; 
		if (landmark.getRelatedUIObject() != null && landmark.getRelatedUIObject() instanceof Marker) {
			marker = (Marker)landmark.getRelatedUIObject();
		}
		if (marker == null) {
			//LoggerUtils.debug("Creating new marker " + landmark.getName() + "," + landmark.getLayer() + ": " + landmark.hashCode());
			marker = new OsmMarker(mapView);
				
			marker.setRelatedObject(landmark);
			landmark.setRelatedUIObject(marker);
        		
			marker.setPosition(new GeoPoint(landmark.getLatitudeE6(), landmark.getLongitudeE6())); 
			marker.setTitle(landmark.getName());
		
			marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
			
				@Override
				public boolean onMarkerClick(Marker m, MapView arg1) {
					LandmarkManager.getInstance().setSelectedLandmark((ExtendedLandmark)m.getRelatedObject());
					LandmarkManager.getInstance().clearLandmarkOnFocusQueue();
					landmarkDetailsHandler.sendEmptyMessage(SHOW_LANDMARK_DETAILS);
					return true;
				}
			});
        
			//LoggerUtils.debug("Adding marker " + landmark.getName() + "," + landmark.getLayer() + ": " + landmark.hashCode());
			add(marker);
		} else if (!getItems().contains(marker)) {
			//LoggerUtils.debug("Adding marker " + landmark.getName() + "," + landmark.getLayer() + ": " + landmark.hashCode());
			add(marker);
		} 
	}
	
	public void clearMarkers() {
		if (!getItems().isEmpty()) {
			LoggerUtils.debug("Removing all markers from OSM maps!");
			readWriteLock.writeLock().lock();
			try {
				getItems().clear();
			} finally {
				readWriteLock.writeLock().unlock();
			}
			invalidate();
		}
	}
	
	public void loadAllMarkers(MapView mapView) {
		LoggerUtils.debug("Loading all markers to OSM maps!");
		for (String layer : LayerManager.getInstance().getLayers()) {
    		if (LayerManager.getInstance().getLayer(layer).getType() != LayerManager.LAYER_DYNAMIC && LayerManager.getInstance().getLayer(layer).isEnabled() && LandmarkManager.getInstance().getLayerSize(layer) > 0) {
    			addMarkers(layer, mapView);
    		}      		
    	}
		//load routes landmarks
		for (String routeKey : RoutesManager.getInstance().getRoutes()) {
			List<ExtendedLandmark> routePoints = RoutesManager.getInstance().getRoute(routeKey);
			if (routePoints.size() > 1) {
				addMarker(routePoints.get(0), mapView);
            	if (! routeKey.equals(RouteRecorder.CURRENTLY_RECORDED)) {
           			addMarker(routePoints.get(routePoints.size()-1), mapView);
           		}
			}
		}	
	}
}
