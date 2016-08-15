package com.jstakun.gms.android.osm.maps;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.StringUtils;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.clustering.StaticCluster;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.utils.LoggerUtils;

public class OsmMarkerClusterOverlay extends RadiusMarkerClusterer {

	public static final int SHOW_LANDMARK_DETAILS = 22;
	public static final int SHOW_LANDMARK_LIST = 23;
	
	private static final int COLOR_WHITE = Color.argb(128, 255, 255, 255); //white
    private static final int COLOR_LIGHT_SALMON = Color.argb(128, 255, 160, 122); //red Light Salmon
    private static final int COLOR_PALE_GREEN = Color.argb(128, 152, 251, 152); //Pale Green
    
    private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    
    private LandmarkManager lm;
	private Handler landmarkDetailsHandler;
	
	public OsmMarkerClusterOverlay(Context ctx, LandmarkManager lm, Handler landmarkDetailsHandler) {
		super(ctx);
		this.lm = lm;
		this.landmarkDetailsHandler = landmarkDetailsHandler;
    
		//custom icon 
		Drawable clusterIconD = ctx.getResources().getDrawable(R.drawable.marker_cluster); //marker_poi_cluster
		Bitmap clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();
		setIcon(clusterIcon);
		
		//custom radius
		setRadius((int)(48f * ctx.getResources().getDisplayMetrics().density));
		
		setMaxClusteringZoomLevel(18);
		
		//and text
		getTextPaint().setTextSize(14 * ctx.getResources().getDisplayMetrics().density);
		getTextPaint().setTypeface(Typeface.DEFAULT_BOLD);
		//this.mAnchorV = Marker.ANCHOR_BOTTOM;
		//this.mTextAnchorU = 0.70f;
		//this.mTextAnchorV = 0.27f;
	}
	
	public void addMarkers(String layerKey, MapView mapView) {
		List<ExtendedLandmark> landmarks = lm.getLandmarkStoreLayer(layerKey);
		//LoggerUtils.debug("Loading " + landmarks.size() + " markers from layer " + layerKey);
		readWriteLock.writeLock().lock();
		int size = getItems().size();
		for (final ExtendedLandmark landmark : landmarks) {		
			addMarker(landmark, mapView);			
		}
		readWriteLock.writeLock().unlock();
		//LoggerUtils.debug(getItems().size() + " markers stored in cluster.");
		if (getItems().size() > size) {
			invalidate();
		}
	}
	
	@Override
	public Marker buildClusterMarker(final StaticCluster cluster, MapView mapView) {
		Marker m = super.buildClusterMarker(cluster, mapView);
		m.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
			@Override
			public boolean onMarkerClick(Marker selected, MapView arg1) {
				lm.clearLandmarkOnFocusQueue();
				for (int i=0;i<cluster.getSize();i++) {
					Marker m = cluster.getItem(i);
					Object o = m.getRelatedObject();
					if (o instanceof ExtendedLandmark) {
						lm.addLandmarkToFocusQueue((ExtendedLandmark)o);
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
	
	public void addMarker(ExtendedLandmark landmark, MapView mapView) {
		Marker marker = null; 
		if (landmark.getRelatedUIObject() != null && landmark.getRelatedUIObject() instanceof Marker) {
			marker = (Marker)landmark.getRelatedUIObject();
		}
		if (marker == null) {
			//LoggerUtils.debug("Creating new marker " + landmark.getName() + "," + landmark.getLayer() + ": " + landmark.hashCode());
			marker = new Marker(mapView);
				
			marker.setRelatedObject(landmark);
			landmark.setRelatedUIObject(marker);
        		
			marker.setPosition(new GeoPoint(landmark.getLatitudeE6(), landmark.getLongitudeE6())); 
			marker.setTitle(landmark.getName());
		
			boolean isMyPosLayer = landmark.getLayer().equals(Commons.MY_POSITION_LAYER);
			
			int color = COLOR_WHITE;
			if (landmark.isCheckinsOrPhotos()) {
				color = COLOR_LIGHT_SALMON;
			} else if (landmark.getRating() >= 0.85) {
				color = COLOR_PALE_GREEN;
			}

			DisplayMetrics displayMetrics = mapView.getResources().getDisplayMetrics();
			Drawable frame = null;
        
        	if (landmark.getCategoryId() != -1) {
            	int icon = LayerManager.getDealCategoryIcon(landmark.getCategoryId(), LayerManager.LAYER_ICON_LARGE);
            	frame = IconCache.getInstance().getCategoryBitmap(icon, Integer.toString(landmark.getCategoryId()), color, !isMyPosLayer, displayMetrics);
        	} else if (!StringUtils.equals(landmark.getLayer(), Commons.LOCAL_LAYER)) {
           		//doesn't work with local layer
        		BitmapDrawable icon = LayerManager.getLayerIcon(landmark.getLayer(), LayerManager.LAYER_ICON_LARGE, displayMetrics, null);
           		frame = IconCache.getInstance().getLayerBitmap(icon, landmark.getLayer(), color, !isMyPosLayer, displayMetrics);
        	} else if (StringUtils.equals(landmark.getLayer(), Commons.LOCAL_LAYER)) {
        		frame = IconCache.getInstance().getCategoryBitmap(R.drawable.ok, "local", -1, false, null);
        	}
        		
        	if (frame != null) {
        		marker.setIcon(frame); 
        	}
				
			marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
			
				@Override
				public boolean onMarkerClick(Marker m, MapView arg1) {
						lm.setSelectedLandmark((ExtendedLandmark)m.getRelatedObject());
						lm.clearLandmarkOnFocusQueue();
						landmarkDetailsHandler.sendEmptyMessage(SHOW_LANDMARK_DETAILS);
						return true;
				}
			});
        
			//LoggerUtils.debug("Adding marker " + landmark.getName() + "," + landmark.getLayer() + ": " + landmark.hashCode());
			add(marker);
		} else if (!getItems().contains(marker)) {
			//LoggerUtils.debug("Adding marker " + landmark.getName() + "," + landmark.getLayer() + ": " + landmark.hashCode());
			add(marker);
		} else {
			//LoggerUtils.debug("Marker exists " + landmark.getName() + "," + landmark.getLayer() + ": " + landmark.hashCode());
		}
	}
	
	public void clearMarkers() {
		if (!getItems().isEmpty()) {
			LoggerUtils.debug("Removing all markers from OSM maps!");
			readWriteLock.writeLock().lock();
			getItems().clear();
			readWriteLock.writeLock().unlock();
			invalidate();
		}
	}
	
	public void loadAllMarkers(MapView mapView) {
		LoggerUtils.debug("Loading all markers to OSM maps!");
		clearMarkers();
		for (String layer : lm.getLayerManager().getLayers()) {
    		if (lm.getLayerManager().getLayer(layer).getType() != LayerManager.LAYER_DYNAMIC && lm.getLayerManager().getLayer(layer).isEnabled() && lm.getLayerSize(layer) > 0) {
    			addMarkers(layer, mapView);
    		}      		
    	}
	}
}
