package com.jstakun.gms.android.osm.maps;

import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.views.MapView;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LayerManager;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

public class OsmMarker extends Marker {

	private static final int COLOR_WHITE = Color.argb(128, 255, 255, 255); //white
    private static final int COLOR_LIGHT_SALMON = Color.argb(128, 255, 160, 122); //red Light Salmon
    private static final int COLOR_PALE_GREEN = Color.argb(128, 152, 251, 152); //Pale Green
    
	public OsmMarker(MapView mapView) {
		super(mapView);
	}
	
	public OsmMarker(MapView mapView, final ResourceProxy resourceProxy) {
		super(mapView, resourceProxy);
	}
	
	@Override 
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (shadow == false) {
			if (getImage() == null) {
				ExtendedLandmark landmark = (ExtendedLandmark) getRelatedObject();
				if (landmark != null) {
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
					} else { 
						BitmapDrawable icon = LayerManager.getLayerIcon(landmark.getLayer(), LayerManager.LAYER_ICON_LARGE, displayMetrics, null);
						frame = IconCache.getInstance().getLayerBitmap(icon, landmark.getLayer(), color, !isMyPosLayer, displayMetrics);
					} 
        		
					if (frame != null) {
						setIcon(frame); 
					}
				}
			}
		}
		super.draw(canvas, mapView, shadow);
	}

	private String getLayer() {
		Object o = getRelatedObject();
		if (o != null && o instanceof ExtendedLandmark) {
			return ((ExtendedLandmark) o).getLayer();
		} else {
			return null;
		}
	}
}
