package com.jstakun.gms.android.osm.maps;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.clustering.StaticCluster;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

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
		
		//custom radius
		setRadius((int)(48f * ctx.getResources().getDisplayMetrics().density));
		
		setMaxClusteringZoomLevel(17);
		
		//and text
		getTextPaint().setTextSize(14 * ctx.getResources().getDisplayMetrics().density);
		getTextPaint().setTypeface(Typeface.DEFAULT_BOLD);
		//this.mAnchorV = Marker.ANCHOR_BOTTOM;
		//this.mTextAnchorU = 0.70f;
		//this.mTextAnchorV = 0.27f;
	}
	
	public void addMarkers(String layerKey, MapView mapView) {
		List<ExtendedLandmark> landmarks = lm.getLandmarkStoreLayer(layerKey);
		LoggerUtils.debug("Loading " + landmarks.size() + " markers from layer " + layerKey);
		int size = getItems().size();
		for (final ExtendedLandmark landmark : landmarks) {
			synchronized (landmark) {
				Marker marker = null; 
				if (landmark.getRelatedUIObject() != null && landmark.getRelatedUIObject() instanceof Marker) {
					marker = (Marker)landmark.getRelatedUIObject();
				}
				if (marker == null) {
					marker = new Marker(mapView);
					
					marker.setRelatedObject(landmark);
					landmark.setRelatedUIObject(marker);
            		
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
                		int icon = LayerManager.getDealCategoryIcon(landmark.getCategoryId(), LayerManager.LAYER_ICON_LARGE);
                		frame = IconCache.getInstance().getLayerBitmap(icon, Integer.toString(landmark.getCategoryId()), color, !isMyPosLayer, displayMetrics);
            		} else {
                		BitmapDrawable icon = LayerManager.getLayerIcon(layerKey, LayerManager.LAYER_ICON_LARGE, displayMetrics, null);
                		frame = IconCache.getInstance().getLayerBitmap(icon, layerKey, color, !isMyPosLayer, displayMetrics);
            		}
            		
            		marker.setIcon(frame); 
					
					//String iconUri = layerKey + "_selected_" + Integer.toString(color) + ".bmp";
					//File fc = PersistenceManagerFactory.getFileManager().getExternalDirectory(FileManager.getIconsFolderPath(), iconUri);
					//Picasso.with(mapView.getContext()).load(fc).into(new MarkerTarget(marker, mapView.getContext(), landmark));
            		       		
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
				} else if (!getItems().contains(marker)) {
					add(marker);
				}
			}
		}
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
	
	public void deleteOrphanMarkers() {
		List<Marker> toRemove = new ArrayList<Marker>();
		for (Marker m : mItems) {
			ExtendedLandmark l = (ExtendedLandmark)m.getRelatedObject();
			if (l.getRelatedUIObject() == null) {
				LoggerUtils.debug("Removing marker for landmark " + l.getName());
				toRemove.add(m);
			}
		}		
		if (!toRemove.isEmpty()) {
			try {
				mItems.removeAll(toRemove);
				invalidate();
			} catch (Exception e) {
				LoggerUtils.error(e.getMessage(), e);
			}
		}
	}
	
	public void clearMarkers() {
		getItems().clear();
		invalidate();
	}
	
	/*private class MarkerTarget implements Target {

		private Marker marker;
		private Context ctx;
		private ExtendedLandmark landmark;
		
		public MarkerTarget(Marker marker, Context ctx, ExtendedLandmark landmark) {
			this.marker = marker;
			this.ctx = ctx;
			this.landmark = landmark;
		}
		
		@Override
		public void onBitmapFailed(Drawable error) {
			int color = COLOR_WHITE;
			if (landmark.isCheckinsOrPhotos()) {
				color = COLOR_LIGHT_SALMON;
			} else if (landmark.getRating() >= 0.85) {
				color = COLOR_PALE_GREEN;
			}
		
			boolean isMyPosLayer = landmark.getLayer().equals(Commons.MY_POSITION_LAYER);
			DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
			
			Drawable frame;
				
			LoggerUtils.debug("Loading " + landmark.getLayer() + " icon...");
			if (landmark.getCategoryId() != -1) {
        		int icon = LayerManager.getDealCategoryIcon(landmark.getCategoryId(), LayerManager.LAYER_ICON_LARGE);
        		frame = IconCache.getInstance().getLayerBitmap(icon, Integer.toString(landmark.getCategoryId()), color, !isMyPosLayer, displayMetrics);
    		} else {
    			BitmapDrawable icon = LayerManager.getLayerIcon(landmark.getLayer(), LayerManager.LAYER_ICON_LARGE, displayMetrics, null);
    			frame = IconCache.getInstance().getLayerBitmap(icon, landmark.getLayer(), color, !isMyPosLayer, displayMetrics);
    		}
			LoggerUtils.debug("Loaded " + landmark.getLayer() + " icon.");
				
			marker.setIcon(frame);
		}

		@Override
		public void onBitmapLoaded(Bitmap bmp, LoadedFrom arg1) {
			marker.setIcon(getBitmapDrawable(bmp));
		}

		@Override
		public void onPrepareLoad(Drawable placeholder) {
			
		}
		
	}
	
	private static BitmapDrawable getBitmapDrawable(Bitmap bitmap) {
    	try {
    		//API version >= 4
    		Context ctx = ConfigurationManager.getInstance().getContext();
    		return BitmapDrawableHelperInternal.getBitmapDrawable(bitmap, ctx.getResources());
    	} catch (Throwable e) {
    		//API version 3
    		return new BitmapDrawable(bitmap);
    	}
    }
    
    private static class BitmapDrawableHelperInternal { 
        private static BitmapDrawable getBitmapDrawable(Bitmap bitmap, Resources res) {
            return new BitmapDrawable(res, bitmap);
        }
    }*/
	
}
