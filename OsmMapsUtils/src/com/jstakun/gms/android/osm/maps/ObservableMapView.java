package com.jstakun.gms.android.osm.maps;

import org.osmdroid.ResourceProxy;
import org.osmdroid.views.MapView;

import com.jstakun.gms.android.ui.ZoomChangeListener;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ObservableMapView extends MapView {
	
	private ZoomChangeListener zoomListener;
	private int currentZoomLevel = -1;
	private float distance = 0f;

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
        //if(getZoomLevel() != currentZoomLevel){
        //    if(zoomListener != null)
        //        zoomListener.onZoom(currentZoomLevel, getZoomLevel());
        //    currentZoomLevel = getZoomLevel();
        //}
        float dist = new OsmLandmarkProjection(this).getViewDistance();
        if (dist != distance) {
        	distance = dist;
        	zoomListener.onZoom(currentZoomLevel, getZoomLevel(), distance);
        	currentZoomLevel = getZoomLevel();
        	//System.out.println("------ " + currentZoomLevel + " " + distance);
        }
    }
    
    public boolean onTouchEvent(MotionEvent ev) {
        if(ev.getAction() == MotionEvent.ACTION_MOVE){
        	float dist = new OsmLandmarkProjection(this).getViewDistance();
            if (dist != distance) {
            	distance = dist;
            	zoomListener.onZoom(currentZoomLevel, getZoomLevel(), distance);
            	currentZoomLevel = getZoomLevel();
            	//System.out.println("------ " + currentZoomLevel + " " + distance);
            }
        }
        return super.onTouchEvent(ev);
    }

}
