package com.jstakun.gms.android.google.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.google.android.maps.MapView;
import com.jstakun.gms.android.ui.ZoomChangeListener;

public class ObservableMapView extends MapView {
	
	private ZoomChangeListener zoomListener;
	private int currentZoomLevel = -1;

	public ObservableMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ObservableMapView(Context context, String apiKey) {
        super(context, apiKey);
    }

    public ObservableMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
