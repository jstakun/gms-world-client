/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.amz.maps;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.amazon.geo.maps.GeoPoint;
import com.amazon.geo.maps.MapView;
import com.amazon.geo.maps.Overlay;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.MathUtils;

/**
 *
 * @author jstakun
 */
public class AmzInfoOverlay extends Overlay {

    private int fontSize = 0;
    private static final Map<String, String> distanceValues = new HashMap<String, String>();
    private final Paint paint = new Paint();
            
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);    
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow == false) {
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            float dip = mapView.getResources().getDisplayMetrics().density;
            paint.setTextSize(14f * dip);

            //draw zoom
            String zoomText = Locale.getMessage(R.string.Zoom_info, mapView.getZoomLevel(), mapView.getMaxZoomLevel());
            canvas.drawText(zoomText, 5, mapView.getHeight() - 10 - fontSize, paint);

            //draw distance
            String key = mapView.getWidth() + "x" + mapView.getHeight() + "_" + mapView.getZoomLevel() + "_" + DistanceUtils.getUoL();
            String text = distanceValues.get(key);
            if (text == null) {
                float distance = 40075.16f;
                if (mapView.getZoomLevel() >= 0) {
                    //distance = DistanceUtils.distanceInKilometer(mapView.getLatitudeSpan(), mapView.getLongitudeSpan(),
                    //        mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6());
                	
                	int width = mapView.getWidth() / 8;
                	int height = mapView.getHeight()/2;
                	GeoPoint p1 = mapView.getProjection().fromPixels(3 * width, height);
                    GeoPoint p2 = mapView.getProjection().fromPixels(5 * width, height);
                    
                    //System.out.println("left: " + p1.getLatitudeE6() + ", " + p1.getLongitudeE6() +
                    //		", right: " + p2.getLatitudeE6() + ", " + p2.getLongitudeE6());
                   
                    distance = DistanceUtils.distanceInKilometer(MathUtils.coordIntToDouble(p1.getLatitudeE6()),
                    		MathUtils.coordIntToDouble(p1.getLongitudeE6()),
                    		MathUtils.coordIntToDouble(p2.getLatitudeE6()),
                    		MathUtils.coordIntToDouble(p2.getLongitudeE6()));
                }
                text = DistanceUtils.formatDistance(distance);
                distanceValues.put(key, text);
            } //else {
            //System.out.println(key + ": " + text);
            //}

            float textSize = paint.measureText(text);
            canvas.drawText(text, mapView.getWidth() - textSize - 5, mapView.getHeight() - 11 - fontSize, paint);

            //draw line
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f * dip);

            canvas.drawLine(3 * mapView.getWidth() / 4 - 5, mapView.getHeight() - 6 - fontSize, mapView.getWidth() - 5, mapView.getHeight() - 6 - fontSize, paint);
            canvas.drawLine(3 * mapView.getWidth() / 4 - 5, mapView.getHeight() - 2 - fontSize, 3 * mapView.getWidth() / 4 - 5, mapView.getHeight() - 10 - fontSize, paint);
            canvas.drawLine(mapView.getWidth() - 5, mapView.getHeight() - 2 - fontSize, mapView.getWidth() - 5, mapView.getHeight() - 10 - fontSize, paint);
        }
    }

    @Override
    public boolean onTap(GeoPoint p, MapView mapView) {
        return false;
    }

}
