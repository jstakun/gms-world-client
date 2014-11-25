package com.jstakun.gms.android.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.Locale;

public class MapInfoView extends View {

	private Paint paint;
	private static final float FONT_SIZE = 14;
	private int zoomLevel = 1, maxZoom = 21;
	private float distance = 40075.16f;
	
	public MapInfoView(Context context) {
		super(context);
		init();
	}
	
	public MapInfoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	private void init() {
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.BLACK);    
		paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(FONT_SIZE * getResources().getDisplayMetrics().density);
	}
	
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		
		float dip = getResources().getDisplayMetrics().density;
		
		paint.setStyle(Paint.Style.FILL);
		
		//System.out.println("------------ Drawing MapInfoView -------------");
        
        //draw zoom
        String zoomText = Locale.getMessage(R.string.Zoom_info, zoomLevel, maxZoom);
        canvas.drawText(zoomText, getPaddingLeft(), getHeight() - getPaddingBottom(), paint);
        
        //draw distance
        String text = DistanceUtils.formatDistance(getDistance()) + " ";
        
        float textSize = paint.measureText(text);
        canvas.drawText(text, getWidth() - textSize - getPaddingRight(), getHeight() - getPaddingBottom(), paint);

        //draw line
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f * dip);

        //------------
        canvas.drawLine(3 * getWidth() / 4 - getPaddingRight(), getHeight() - getPaddingBottom() + (3f * dip), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom() + (3f * dip), paint);
        //|-----------
        canvas.drawLine(3 * getWidth() / 4 - getPaddingRight(), getHeight() - getPaddingBottom(), 3 * getWidth() / 4 - getPaddingRight(), getHeight() - getPaddingBottom() + (6f * dip), paint);
        //-----------|
        canvas.drawLine(getWidth() - getPaddingRight(), getHeight() - getPaddingBottom(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom() + (6f * dip), paint);
	}
	
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

	    int desiredWidth = 100;
	    int desiredHeight = (int)(FONT_SIZE*getResources().getDisplayMetrics().density) + getPaddingBottom() + getPaddingTop();

	    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
	    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
	    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
	    int heightSize = MeasureSpec.getSize(heightMeasureSpec);

	    int width;
	    int height;

	    if (widthMode == MeasureSpec.EXACTLY) {
	        width = widthSize;
	    } else if (widthMode == MeasureSpec.AT_MOST) {
	        width = Math.min(desiredWidth, widthSize);
	    } else {
	        width = desiredWidth;
	    }

	    if (heightMode == MeasureSpec.EXACTLY) {
	        height = heightSize;
	    } else if (heightMode == MeasureSpec.AT_MOST) {
	        height = Math.min(desiredHeight, heightSize);
	    } else {
	        height = desiredHeight;
	    }

	    setMeasuredDimension(width, height);
	}

	public int getZoomLevel() {
		return zoomLevel;
	}

	public void setZoomLevel(int zoomLevel) {
		this.zoomLevel = zoomLevel;
	}

	public int getMaxZoom() {
		return maxZoom;
	}

	public void setMaxZoom(int maxZoom) {
		this.maxZoom = maxZoom;
	}

	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}

	
}
