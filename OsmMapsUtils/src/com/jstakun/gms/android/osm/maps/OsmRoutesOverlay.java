/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.osm.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.utils.MathUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.Overlay;

/**
 *
 * @author jstakun
 */
public class OsmRoutesOverlay extends Overlay {

    private RoutesManager routesManager = null;
    private String routeName = null;
    private final Paint lmpaint = new Paint();
    private final Paint paint = new Paint();
    private final Path path = new Path();
    private final Point point1 = new Point();
    private final Point point2 = new Point();
    private final Rect viewportRect = new Rect();
    private final Point gp1 = new Point();
    private final Point gp2 = new Point();
    private List<Point> projectedPoints = new ArrayList<Point>();
    private int routeSize = 0;
    private boolean isCurrentlyRecording = false;

    public OsmRoutesOverlay(MapView mapView, Context context, RoutesManager routesManager, String routeName) {
        super(context);
        this.routesManager = routesManager;
        this.routeName = routeName;
        isCurrentlyRecording = StringUtils.startsWith(routeName, RouteRecorder.CURRENTLY_RECORDED);
        if (!isCurrentlyRecording && routesManager.containsRoute(routeName)) {
            List<ExtendedLandmark> routePoints = routesManager.getRoute(routeName);
            routeSize = routePoints.size();
            for (ExtendedLandmark l : routePoints) {
                Point p = mapView.getProjection().toMapPixelsProjected(l.getLatitudeE6(), l.getLongitudeE6(), null); //toProjectedPixels
                projectedPoints.add(p);
            }
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
            Bitmap b = LayerManager.getLayerIcon(Commons.ROUTES_LAYER, LayerManager.LAYER_ICON_LARGE, mapView.getResources().getDisplayMetrics(), null).getBitmap();
            path.rewind();

            if (!isCurrentlyRecording && routeSize > 1) {
                projection.toMapPixelsTranslated(projectedPoints.get(0), point1); //toPixelsFromProjected
                canvas.drawBitmap(b, point1.x - (b.getWidth() / 2), point1.y - b.getHeight(), lmpaint);
                path.moveTo(point1.x, point1.y);

                for (int i = 1; i < routeSize; i++) {
                    projection.toMapPixelsTranslated(projectedPoints.get(i), point2);
                    if (MathUtils.abs(point2.x - point1.x) + MathUtils.abs(point2.y - point1.y) <= 1) {
                        continue;
                    }

                    path.lineTo(point2.x, point2.y);
                    point1.x = point2.x;
                    point1.y = point2.y;
                }

                canvas.drawPath(path, paint);
                canvas.drawBitmap(b, point1.x - (b.getWidth() / 2), point1.y - (b.getHeight() / 2), lmpaint);
            } else if (isCurrentlyRecording) {
                //draw currently recorded route from the end to first hidden point
                List<ExtendedLandmark> routePoints = routesManager.getRoute(routeName);
                routeSize = routePoints.size();

                if (routeSize > 1) {
                    viewportRect.set(projection.getScreenRect());
                    ExtendedLandmark lastPoint = routePoints.get(routeSize - 1);

                    projection.toMapPixelsProjected(lastPoint.getLatitudeE6(), lastPoint.getLongitudeE6(), gp1);
                    projection.toMapPixelsTranslated(gp1, point1);

                    canvas.drawBitmap(b, point1.x - (b.getWidth() / 2), point1.y - b.getHeight(), lmpaint);

                    int i = routeSize - 2;
                    boolean visible = true;
                    path.moveTo(point1.x, point1.y);

                    while (i >= 0 && visible) {
                        ExtendedLandmark secondPoint = routePoints.get(i);

                        projection.toMapPixelsProjected(secondPoint.getLatitudeE6(), secondPoint.getLongitudeE6(), gp2);
                        projection.toMapPixelsTranslated(gp2, point2);

                        if (MathUtils.abs(point2.x - point1.x) + MathUtils.abs(point2.y - point1.y) <= 1) {
                            continue;
                        }

                        if (viewportRect.contains(point1.x, point1.y)) {
                            path.lineTo(point2.x, point2.y);
                        } else {
                            visible = false;
                        }

                        point1.x = point2.x;
                        point1.y = point2.y;
                        i--;
                    }

                    canvas.drawPath(path, paint);

                    //System.out.println("Painting landmark: " + xy1[0] + " " + xy1[1]);
                    if (i == -1) {
                        canvas.drawBitmap(b, point1.x - (b.getWidth() / 2), point1.y - (b.getHeight() / 2), lmpaint);
                    }
                }
            }
        }
    }
}
