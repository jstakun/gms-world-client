/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.osm.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.Locale;
import java.util.HashMap;
import java.util.Map;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

/**
 *
 * @author jstakun
 */
public class OsmInfoOverlay extends Overlay {

    private int fontSize = 0;
    private static final Map<String, String> distanceValues = new HashMap<String, String>();
    private final Paint paint = new Paint();
    private final Rect viewportRect = new Rect();

    public OsmInfoOverlay(final Context ctx) {
        this(ctx, new DefaultResourceProxyImpl(ctx));
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public OsmInfoOverlay(final Context ctx, final ResourceProxy pResourceProxy) {
        super(pResourceProxy);
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow == false) {
            viewportRect.set(mapView.getProjection().getScreenRect());

            float dip = mapView.getResources().getDisplayMetrics().density;
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(14f * dip);

            //draw zoom
            String zoomText = Locale.getMessage(R.string.Zoom_info, mapView.getZoomLevel(), mapView.getMaxZoomLevel());
            canvas.drawText(zoomText, viewportRect.left + 5, viewportRect.bottom - 10 - fontSize, paint);

            //draw distance
            String key = mapView.getWidth() + "x" + mapView.getHeight() + "_" + mapView.getZoomLevel() + "_" + DistanceUtils.getUoL();
            String text = distanceValues.get(key);
            if (text == null) {
                float distance = 40075.16f;
                if (mapView.getZoomLevel() > 2) {
                    distance = DistanceUtils.distanceInKilometer(mapView.getLatitudeSpan(), mapView.getLongitudeSpan(),
                            mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6());
                }
                text = DistanceUtils.formatDistance(distance / 4.0f);
                distanceValues.put(key, text);
            }

            float textSize = paint.measureText(text);
            canvas.drawText(text, viewportRect.right - textSize - 5, viewportRect.bottom - 11 - fontSize, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f * dip);

            //draw lines
            canvas.drawLine(viewportRect.right - (mapView.getWidth() / 4) - 5, viewportRect.bottom - 6 - fontSize, viewportRect.right - 5, viewportRect.bottom - 6 - fontSize, paint);
            canvas.drawLine(viewportRect.right - (mapView.getWidth() / 4) - 5, viewportRect.bottom - 2 - fontSize, viewportRect.right - (mapView.getWidth() / 4) - 5, viewportRect.bottom - 10 - fontSize, paint);
            canvas.drawLine(viewportRect.right - 5, viewportRect.bottom - 2 - fontSize, viewportRect.right - 5, viewportRect.bottom - 10 - fontSize, paint);
        }
    }
}
