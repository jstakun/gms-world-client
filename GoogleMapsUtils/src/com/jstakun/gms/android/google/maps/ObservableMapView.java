package com.jstakun.gms.android.google.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.maps.MapView;
import com.jstakun.gms.android.ui.ZoomChangeListener;

public class ObservableMapView extends MapView {
	
	private ZoomChangeListener zoomListener;
	private int currentZoomLevel = -1;
	private float distance = 0f;

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
        //if(getZoomLevel() != currentZoomLevel){
        //    if(zoomListener != null)
        //        zoomListener.onZoom(currentZoomLevel, getZoomLevel());
        //    currentZoomLevel = getZoomLevel();
        //}
        float dist = new GoogleLandmarkProjection(this).getViewDistance();
        if (dist != distance) {
        	distance = dist;
        	zoomListener.onZoom(currentZoomLevel, getZoomLevel(), distance);
        	currentZoomLevel = getZoomLevel();
        	//System.out.println("------ " + currentZoomLevel + " " + distance);
        }
    }
    
    public boolean onTouchEvent(MotionEvent ev) {
        if(ev.getAction() == MotionEvent.ACTION_MOVE){
        	float dist = new GoogleLandmarkProjection(this).getViewDistance();
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
