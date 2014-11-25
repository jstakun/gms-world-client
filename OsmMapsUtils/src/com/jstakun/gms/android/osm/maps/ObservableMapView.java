package com.jstakun.gms.android.osm.maps;

import org.osmdroid.ResourceProxy;
import org.osmdroid.views.MapView;

import com.jstakun.gms.android.ui.ZoomChangeListener;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class ObservableMapView extends MapView {
	
	private ZoomChangeListener zoomListener;
	private int currentZoomLevel = -1;

	public ObservableMapView(Context context, int tileSizePixels, ResourceProxy resourceProxy) {
		super(context, tileSizePixels, resourceProxy);
	}

	public ObservableMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
}

	public ObservableMapView(Context context, int tileSizePixels) {
		super(context, tileSizePixels);
	}
	
	public void setOnZoomChangeListener(ZoomChangeListener listener){
        zoomListener = listener;
    }
    
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if(getZoomLevel() != currentZoomLevel){
            if(zoomListener != null)
                zoomListener.onZoom(currentZoomLevel, getZoomLevel());
            currentZoomLevel = getZoomLevel();
        }
    }

}
