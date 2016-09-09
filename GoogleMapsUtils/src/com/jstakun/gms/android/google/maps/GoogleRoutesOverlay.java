package com.jstakun.gms.android.google.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.routes.RoutesManager;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jstakun
 */
public class GoogleRoutesOverlay extends Overlay {

    private final Path path = new Path();
    private final Paint lmpaint = new Paint();
    private final Paint paint = new Paint();
    private final Point point1 = new Point();
    private final Point point2 = new Point();
    private List<ExtendedLandmark> routePoints = new ArrayList<ExtendedLandmark>();
    private int routeSize = 0, w = 0, h = 0;
    private boolean isCurrentlyRecording = false;
    private String routeName = null;
    private final Rect viewportRect = new Rect();
    private Bitmap routesLayerBitmap = null;

    public GoogleRoutesOverlay(Context context, String routeName) {
        this.routeName = routeName;
        isCurrentlyRecording = routeName.startsWith(RouteRecorder.CURRENTLY_RECORDED);
        if (!isCurrentlyRecording && RoutesManager.getInstance().containsRoute(routeName)) {
            routePoints = RoutesManager.getInstance().getRoute(routeName);
            routeSize = routePoints.size();
        }
        
        routesLayerBitmap = LayerManager.getLayerIcon(Commons.ROUTES_LAYER, LayerManager.LAYER_ICON_LARGE, context.getResources().getDisplayMetrics(), null).getBitmap();
        if (routesLayerBitmap != null) {
            w = routesLayerBitmap.getWidth();
            h = routesLayerBitmap.getHeight();
        }
        
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setARGB(128, 225, 0, 0);
        paint.setStrokeWidth(5f * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow == false) {
            Projection projection = mapView.getProjection();
            path.rewind();
            
            //System.out.println("Drawing route " + routeName + " " + isCurrentlyRecording);

            if (!isCurrentlyRecording && routeSize > 1) {
                ExtendedLandmark firstPoint = routePoints.get(0);
                GeoPoint gp1 = new GeoPoint(firstPoint.getLatitudeE6(), firstPoint.getLongitudeE6());
                projection.toPixels(gp1, point1);
                if (routesLayerBitmap != null && !routesLayerBitmap.isRecycled()) {
                	canvas.drawBitmap(routesLayerBitmap, point1.x - (w / 2), point1.y - h, lmpaint); 
                }
                path.moveTo(point1.x, point1.y);

                for (int i = 1; i < routeSize; i++) {
                    ExtendedLandmark secondPoint = routePoints.get(i);
                    GeoPoint gp2 = new GeoPoint(secondPoint.getLatitudeE6(), secondPoint.getLongitudeE6());
                    projection.toPixels(gp2, point2);

                    path.lineTo(point2.x, point2.y);
                    point1.x = point2.x;
                    point1.y = point2.y;
                    //System.out.print(point1.x +  "," + point1.y + " ");
                }

                canvas.drawPath(path, paint);
                if (routesLayerBitmap != null && !routesLayerBitmap.isRecycled()) {
                	canvas.drawBitmap(routesLayerBitmap, point1.x - (w / 2), point1.y - (h / 2), lmpaint);
                }
            } else if (isCurrentlyRecording) {
                //draw currently recorded route from the end to first invisible point only
                routePoints = RoutesManager.getInstance().getRoute(routeName);
                routeSize = routePoints.size();

                if (routeSize > 1) {
                    ExtendedLandmark lastPoint = routePoints.get(routeSize - 1);
                    GeoPoint gp1 = new GeoPoint(lastPoint.getLatitudeE6(), lastPoint.getLongitudeE6());
                    projection.toPixels(gp1, point1);
                    //canvas.drawBitmap(b, point1.x - (b.getWidth() / 2), point1.y - b.getHeight(), lmpaint);
                    path.moveTo(point1.x, point1.y);

                    int i = routeSize - 2;
                    mapView.getDrawingRect(viewportRect);

                    while (i >= 0) {
                        ExtendedLandmark secondPoint = routePoints.get(i);
                        GeoPoint gp2 = new GeoPoint(secondPoint.getLatitudeE6(), secondPoint.getLongitudeE6());
                        projection.toPixels(gp2, point2);
                        
                        path.lineTo(point2.x, point2.y);
                        if (!viewportRect.contains(point1.x, point1.y)) {
                            break;
                        }

                        point1.x = point2.x;
                        point1.y = point2.y;
                        i--;
                    }

                    canvas.drawPath(path, paint);

                    //System.out.println("Painting landmark: " + xy1[0] + " " + xy1[1]);
                    if (i == -1 && routesLayerBitmap != null && !routesLayerBitmap.isRecycled()) {
                        canvas.drawBitmap(routesLayerBitmap, point1.x - (w / 2), point1.y - (h / 2), lmpaint); 
                    }
                }
            }

            super.draw(canvas, mapView, shadow);
        }
    }

    @Override
    public boolean onTap(GeoPoint p, MapView mapView) {
        return false;
    }
}
