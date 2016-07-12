package com.jstakun.gms.android.osm.maps;

import java.util.List;

import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.clustering.StaticCluster;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
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
		
		setRadius((int)(48f * ctx.getResources().getDisplayMetrics().density));
		
		this.setMaxClusteringZoomLevel(18);
		
		//and text
		getTextPaint().setTextSize(14 * ctx.getResources().getDisplayMetrics().density);
		//this.mAnchorV = Marker.ANCHOR_BOTTOM;
		//this.mTextAnchorU = 0.70f;
		//this.mTextAnchorV = 0.27f;
	}
	
	public void addMarkers(String layerKey, MapView mapView) {
		List<ExtendedLandmark> landmarks = lm.getLandmarkStoreLayer(layerKey);
		LoggerUtils.debug("Loading " + landmarks.size() + " markers from layer " + layerKey);
		for (final ExtendedLandmark landmark : landmarks) {
			Marker marker = new Marker(mapView);
			marker.setPosition(new GeoPoint(landmark.getLatitudeE6(), landmark.getLongitudeE6())); 
			marker.setTitle(landmark.getName());
			
			boolean isMyPosLayer = landmark.getLayer().equals(Commons.MY_POSITION_LAYER);
			DisplayMetrics displayMetrics = mapView.getResources().getDisplayMetrics();
			
			int color = COLOR_WHITE;
            if (landmark.isCheckinsOrPhotos()) {
                color = COLOR_LIGHT_SALMON;
            } else if (landmark.getRating() >= 0.85) {
                color = COLOR_PALE_GREEN;
            }

            Drawable frame;
            
            if (landmark.getCategoryId() != -1) {
                int icon = LayerManager.getDealCategoryIcon(layerKey, LayerManager.LAYER_ICON_LARGE, displayMetrics, landmark.getCategoryId());
                frame = IconCache.getInstance().getLayerBitmap(icon, Integer.toString(landmark.getCategoryId()), color, !isMyPosLayer, displayMetrics);
            } else {
                //if layer icon is loading, frame can't be cached
                BitmapDrawable icon = LayerManager.getLayerIcon(layerKey, LayerManager.LAYER_ICON_LARGE, displayMetrics, null);
                frame = IconCache.getInstance().getLayerBitmap(icon, layerKey, color, !isMyPosLayer, displayMetrics);
            }

            marker.setIcon(frame); 
            marker.setRelatedObject(landmark);
            
            marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
				
				@Override
				public boolean onMarkerClick(Marker m, MapView arg1) {
					lm.setSelectedLandmark((ExtendedLandmark)m.getRelatedObject());
					lm.clearLandmarkOnFocusQueue();
					landmarkDetailsHandler.sendEmptyMessage(SHOW_LANDMARK_DETAILS);
					return true;
				}
			});
            
            add(marker);
		}
		LoggerUtils.debug(getItems().size() + " markers stored in cluster.");
		invalidate();
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
	
	public void deleteOrphanMarkers() {
		for (Marker m : mItems) {
			if (m.getRelatedObject() == null) {
				mItems.remove(m);
			}
		}
		
	}
}
