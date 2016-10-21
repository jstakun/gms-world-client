package com.jstakun.gms.android.osm.maps;

import java.util.ArrayList;
import java.util.List;

import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;

/**
 *
 * @author jstakun
 */
public class OsmRoutesOverlay extends Overlay {

    private final Paint paint = new Paint();
    private final Path path = new Path();
    private final Point point1 = new Point();
    private final Point point2 = new Point();
    //private final Rect viewportRect = new Rect();
    private final Point gp1 = new Point();
    private final Point gp2 = new Point();
    private List<Point> projectedPoints = new ArrayList<Point>();
    private int routeSize = 0;//, w, h;
    private boolean isCurrentlyRecording = false;
    //private Bitmap routesLayerBitmap;
    
    public OsmRoutesOverlay(MapView mapView, Context context, String routeName, OsmMarkerClusterOverlay markerCluster) {
        super(context);
        isCurrentlyRecording = routeName.equals(RouteRecorder.CURRENTLY_RECORDED);
        if (RoutesManager.getInstance().containsRoute(routeName)) {
            List<ExtendedLandmark> routePoints = RoutesManager.getInstance().getRoute(routeName);
            routeSize = routePoints.size();
            if (routeSize > 1) {
            	for (ExtendedLandmark l : routePoints) {
            		Point p = mapView.getProjection().toProjectedPixels(l.getLatitudeE6(), l.getLongitudeE6(), null);
            		projectedPoints.add(p);
            	}
            	if (markerCluster != null) {
            		LoggerUtils.debug("Adding route points to marker cluster!");
            		markerCluster.addMarker(routePoints.get(0), mapView);
            		if (! routeName.equals(RouteRecorder.CURRENTLY_RECORDED)) {
            			markerCluster.addMarker(routePoints.get(routePoints.size()-1), mapView);
            		}
            	}
            }
        }

        //routesLayerBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.bullet_blue); //.start_marker;);    
        //w = routesLayerBitmap.getWidth();
        //h = routesLayerBitmap.getHeight();
        
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

            if (!isCurrentlyRecording && routeSize > 1) {
                projection.toPixelsFromProjected(projectedPoints.get(0), point1); 
                //canvas.drawBitmap(routesLayerBitmap, point1.x - (w / 2), point1.y - h, lmpaint);
                path.moveTo(point1.x, point1.y);

                for (int i = 1; i < routeSize; i++) {
                    projection.toPixelsFromProjected(projectedPoints.get(i), point2);
                    
                    path.lineTo(point2.x, point2.y);
                    point1.x = point2.x;
                    point1.y = point2.y;
                }

                canvas.drawPath(path, paint);
                //canvas.drawBitmap(routesLayerBitmap, point1.x - (w / 2), point1.y - (h / 2), lmpaint);
            } else if (isCurrentlyRecording) {
                //draw currently recorded route from the end to first point
                List<ExtendedLandmark> routePoints = RoutesManager.getInstance().getRoute(RouteRecorder.CURRENTLY_RECORDED);
                routeSize = routePoints.size();

                if (routeSize > 1) {
                    //viewportRect.set(projection.getScreenRect());
                	//TODO testing
                    Location l = ConfigurationManager.getInstance().getLocation();
                    int i = -1;
            		if (l != null) {
            			projection.toProjectedPixels(MathUtils.coordDoubleToInt(l.getLatitude()), MathUtils.coordDoubleToInt(l.getLongitude()), gp2);
                        projection.toPixelsFromProjected(gp1, point1);
                        path.moveTo(point1.x, point1.y);
                        i = routeSize - 1;
            		} else {
            			ExtendedLandmark lastPoint = routePoints.get(routeSize - 1);
            			projection.toProjectedPixels(lastPoint.getLatitudeE6(), lastPoint.getLongitudeE6(), gp1);
            			projection.toPixelsFromProjected(gp1, point1);
            			path.moveTo(point1.x, point1.y);
            			i = routeSize - 2;
            		}
            		//

                    while (i >= 0) {
                        ExtendedLandmark secondPoint = routePoints.get(i);

                        projection.toProjectedPixels(secondPoint.getLatitudeE6(), secondPoint.getLongitudeE6(), gp2);
                        projection.toPixelsFromProjected(gp2, point2);

                        path.lineTo(point2.x, point2.y);
                        //if (!viewportRect.contains(point1.x, point1.y)) {
                        //   break;
                        //}
                        
                        point1.x = point2.x;
                        point1.y = point2.y;
                        i--;
                    }
            		
                    canvas.drawPath(path, paint);

                    //System.out.println("Painting landmark: " + xy1[0] + " " + xy1[1]);
                    //if (i == -1) {
                    //    canvas.drawBitmap(routesLayerBitmap, point1.x - (w / 2), point1.y - (h / 2), lmpaint);
                    //}
                }
            }
        }
    }
}
