/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.routes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkFactory;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.openlapi.QualifiedCoordinates;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jstakun
 */
public class RoutesManager {

    private static Map<String, List<ExtendedLandmark>> routes = new HashMap<String, List<ExtendedLandmark>>();
    private static Map<String, String> descs = new HashMap<String, String>();
    private double minLat, maxLat, minLon, maxLon;

    public RoutesManager() {
        routes.clear();
        descs.clear();
    }

    public void addRoute(String key, List<ExtendedLandmark> routePoints, String description) {
        routes.put(key, routePoints);
        if (description != null) {
            descs.put(key, description);
        }
    }

    public List<ExtendedLandmark> getRoute(String key) {
        if (routes.containsKey(key)) {
            return routes.get(key);
        } else {
            return Collections.emptyList();
        }
    }

    public void removeRoute(String key) {
        if (routes.containsKey(key)) {
            routes.remove(key);
        }
    }

    public int getCount() {
        return routes.size();
    }

    public Set<String> getRoutes() {
        return routes.keySet();
    }

    /*public void paintRoutes(Canvas c, BoundingBox bbox, Paint paint, int width, int height, DisplayMetrics displayMetrics) {
        if (landmarkManager.getLayerManager().isLayerEnabled(LayerManager.ROUTES_LAYER)) {

            Bitmap b = LayerManager.getLayerIcon(LayerManager.ROUTES_LAYER, LayerManager.LAYER_ICON_LARGE, displayMetrics, null);

            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);

            for (Iterator<Map.Entry<String, List<ExtendedLandmark>>> iter = routes.entrySet().iterator(); iter.hasNext();) {
                Map.Entry<String, List<ExtendedLandmark>> entry = iter.next();
                String key = entry.getKey();
                List<ExtendedLandmark> routePoints = entry.getValue();
                LoggerUtils.debug("Painting route " + key + " has " + routePoints.size() + " points.");

                if (!routePoints.isEmpty()) {
                    int size = routePoints.size();
                    boolean visible = false;
                    Path path = new Path();

                    if (!key.startsWith(RouteRecorder.CURRENTLY_RECORDED)) {

                        ExtendedLandmark firstPoint = routePoints.get(0);
                        //int[] xy1 = MercatorUtils.coordsToXY(width, height, firstPoint.getQualifiedCoordinates().getLatitude(), firstPoint.getQualifiedCoordinates().getLongitude(), bbox);

                        int y1 = Integer.MAX_VALUE;

                        int x1 = MercatorUtils.lonToX(width, firstPoint.getQualifiedCoordinates().getLongitude(), bbox);
                        {
                            if (x1 >= 0 && x1 <= width) {
                                y1 = MercatorUtils.latToY(height, firstPoint.getQualifiedCoordinates().getLatitude(), bbox);
                                if (y1 >= 0 && y1 <= height) {
                                    drawLandmark(c, b, x1, y1, paint);
                                    visible = true;
                                }
                            }
                        }

                        for (int i = 1; i < size; i++) {
                            ExtendedLandmark secondPoint = routePoints.get(i);
                            boolean newVisible = false;

                            int y2 = Integer.MAX_VALUE;
                            int x2 = MercatorUtils.lonToX(width, secondPoint.getQualifiedCoordinates().getLongitude(), bbox);

                            if (x2 >= 0 && x2 <= width || visible) {
                                y2 = MercatorUtils.latToY(height, secondPoint.getQualifiedCoordinates().getLatitude(), bbox);
                                if (y2 >= 0 && y2 <= height) {
                                    newVisible = true;
                                    if (y1 == Integer.MAX_VALUE) {
                                        y1 = MercatorUtils.latToY(height, firstPoint.getQualifiedCoordinates().getLatitude(), bbox);
                                    }
                                }
                            }

                            if (visible || newVisible) {
                                path.moveTo(x1, y1);
                                path.lineTo(x2, y2);
                            }

                            x1 = x2;
                            y1 = y2;
                            visible = newVisible;
                            firstPoint = secondPoint;
                        }

                        c.drawPath(path, paint);

                        if (x1 >= 0 && x1 <= width && y1 >= 0 && y1 <= height) {
                            drawLandmark(c, b, x1, y1, paint);
                        }

                    } else {

                        //draw route only from end
                        ExtendedLandmark lastPoint = routePoints.get(size - 1);
                        int[] xy1 = MercatorUtils.coordsToXY(width, height, lastPoint.getQualifiedCoordinates().getLatitude(), lastPoint.getQualifiedCoordinates().getLongitude(), bbox);

                        int i = size - 2;
                        visible = true;

                        while (i >= 0 && visible) {
                            ExtendedLandmark secondPoint = routePoints.get(i);
                            int[] xy2 = MercatorUtils.coordsToXY(width, height, secondPoint.getQualifiedCoordinates().getLatitude(), secondPoint.getQualifiedCoordinates().getLongitude(), bbox);

                            if ((xy1[0] >= 0 && xy1[0] <= width && xy1[1] >= 0 && xy1[1] <= height)
                                    || (xy2[0] >= 0 && xy2[0] <= width && xy2[1] >= 0 && xy2[1] <= height)) {

                                //System.out.println("Drawing line: " + xy1[0] + "," + xy1[1] + " " + xy2[0] + "," + xy2[1] + " " + i);

                                path.moveTo(xy1[0], xy1[1]);
                                path.lineTo(xy2[0], xy2[1]);

                            } else {
                                visible = false;
                            }

                            xy1[0] = xy2[0];
                            xy1[1] = xy2[1];
                            lastPoint = secondPoint;
                            i--;
                        }

                        c.drawPath(path, paint);

                        //System.out.println("Painting landmark: " + xy1[0] + " " + xy1[1]);
                        if (i == -1) {
                            drawLandmark(c, b, xy1[0], xy1[1], paint);
                        }

                        //System.out.println("Painted " + (size - i) + " from " + size + " points.");
                    }
                }
            }
        }
    }*/

    public double[] calculateRouteCenter(String routeKey) {
        double coords[] = new double[2];
        List<ExtendedLandmark> routePoints = routes.get(routeKey);
        ExtendedLandmark start = routePoints.get(0);
        minLat = start.getQualifiedCoordinates().getLatitude();
        maxLat = minLat;
        minLon = start.getQualifiedCoordinates().getLongitude();
        maxLon = minLon;

        for (int i = 0; i < routePoints.size(); i++) {
            ExtendedLandmark l = routePoints.get(i);
            double lat = l.getQualifiedCoordinates().getLatitude();
            if (lat > maxLat) {
                maxLat = lat;
            } else if (lat < minLat) {
                minLat = lat;
            }
            double lon = l.getQualifiedCoordinates().getLongitude();
            if (lon > maxLon) {
                maxLon = lon;
            } else if (lon < minLon) {
                minLon = lon;
            }
        }

        coords[0] = (maxLat + minLat) * 0.5;
        coords[1] = (maxLon + minLon) * 0.5;

        return coords;
    }

    public int calculateRouteZoom(Object routeKey) {
        double latDiff = Math.abs(maxLat - minLat);
        double lonDiff = Math.abs(maxLon - minLon);

        int zoom = 0;
        while (zoom < 21) {
            double s = 180 / MathUtils.pow(2, zoom);

            if (s > latDiff && s > lonDiff) {
                zoom++;
            } else {
                break;
            }
        }

        return zoom + 2;
    }

    public void clearRoutesStore() {
        Set entries = routes.entrySet();
        Iterator iter = entries.iterator();

        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
    }

    public boolean containsRoute(String key) {
        return routes.containsKey(key);
    }

    public String loadRouteFromServer(String lat_start, String lng_start, String lat_end, String lng_end, String type, String routeName, boolean saveToFile) {
        List<ExtendedLandmark> routePoints = new ArrayList<ExtendedLandmark>();
        String message = null;

        String url = ConfigurationManager.SERVER_SERVICES_URL + "routeProvider?";

        url += "lat_start=" + lat_start;
        url += "&lng_start=" + lng_start;

        url += "&lat_end=" + lat_end;
        url += "&lng_end=" + lng_end;

        url += "&type=" + type;
        url += "&tId=" + System.currentTimeMillis();

        String oauthUser = ConfigurationManager.getInstance().getOAuthLoggedInUsername();
        if (oauthUser != null) {
            url += "&username=" + oauthUser;
        } else {
            url += "&username=" + ConfigurationManager.getInstance().getString(ConfigurationManager.USERNAME);
        }

        if (ConfigurationManager.getInstance().containsKey(ConfigurationManager.ROUTES_TOKEN)) {
            url += "&token=" + ConfigurationManager.getInstance().getString(ConfigurationManager.ROUTES_TOKEN);
        }

        HttpUtils utils = new HttpUtils();

        String[] desc = null;

        try {
            byte[] response = utils.loadHttpFile(url, true, "json");

            message = utils.getResponseCodeErrorMessage();

            if (response != null && response.length > 0) {
                JSONObject json = new JSONObject(new String(response));
                desc = parse(json, routePoints);
                message = desc[0];
                //System.out.println(json);
            } else if (message != null) {
                message = Locale.getMessage(R.string.Routes_loading_error_1, message);
            } else {
                message = Locale.getMessage(R.string.Routes_loading_error_0);
            }

        } catch (Exception ex) {
            LoggerUtils.error("RoutesManager.readRouteFromServer exception", ex);
            message = ex.getMessage();
        } finally {
            try {
                if (utils != null) {
                    utils.close();
                }
            } catch (IOException ioe) {
            }
            if (!routePoints.isEmpty()) {
                String descr = desc[0];
                if (saveToFile) {
                    String[] details = RouteRecorder.saveRoute(routePoints, routeName + ".kml", desc[3], desc[2], desc[1]);
                    if (details != null && details.length > 1) {
                        descr = details[1];
                    }
                }
                addRoute(routeName, routePoints, descr);
            } else {
                if (StringUtils.isEmpty(message)) {
                    message = Locale.getMessage(R.string.Routes_loading_error_0);
                }
            }
        }

        return message;
    }

    private String[] parse(JSONObject json, List<ExtendedLandmark> routePoints) throws JSONException {
        String[] response = new String[4];
        int status = json.getInt("status");

        if (status == 0) {
            JSONArray points = json.getJSONArray("route_geometry");

            for (int i = 0; i < points.length(); i++) {
                JSONArray point = points.getJSONArray(i);

                double lat = point.getDouble(0);
                double lon = point.getDouble(1);

                QualifiedCoordinates qc = new QualifiedCoordinates(lat, lon, Float.NaN, Float.NaN, Float.NaN);

                ExtendedLandmark lm = LandmarkFactory.getLandmark("", "", qc, Commons.ROUTES_LAYER, 0);

                routePoints.add(lm);
            }

            JSONObject summary = json.getJSONObject("route_summary");
            double total_dist_km = summary.getInt("total_distance") / 1000.0;
            int total_time = summary.getInt("total_time");

            response[1] = DistanceUtils.formatDistance(total_dist_km); //dist km

            response[2] = DateTimeUtils.convertSecondsToTimeStamp(total_time); //time

            double avgSpeed = (total_dist_km * 3600.0) / (double)total_time;

            response[3] = DistanceUtils.formatSpeed(avgSpeed); //avg speed

            response[0] = Locale.getMessage(R.string.Routes_Server_route_loaded, response[1], response[2]);
        } else {
            response[0] = Locale.getMessage(R.string.Routes_loading_error_1, json.getString("status_message"));
        }

        return response;
    }

    private void drawLandmark(Canvas c, Bitmap b, int x, int y, Paint p) {
        int x1 = x - (b.getWidth() / 2);
        int y1 = y - (b.getHeight() / 2);
        c.drawBitmap(b, x1, y1, p);
    }

    private String getRouteDesc(String key) {
        return descs.get(key);
    }

    public List<ExtendedLandmark> getBoundingRouteLandmarks() {
        List<ExtendedLandmark> routeLandmarks = new ArrayList<ExtendedLandmark>();
        for (Iterator<String> iter = routes.keySet().iterator(); iter.hasNext();) {
            String routeKey = iter.next();
            List<ExtendedLandmark> route = routes.get(routeKey);
            String desc = getRouteDesc(routeKey);
            if (route.size() > 0) {
                ExtendedLandmark el = route.get(0);
                if (desc != null) {
                    el.setDescription(desc);
                }
                el.setName(Locale.getMessage(R.string.Routes_starting_point));
                routeLandmarks.add(route.get(0));
            }
            if (route.size() > 1) {
                ExtendedLandmark el = route.get(route.size() - 1);
                if (desc != null) {
                    el.setDescription(desc);
                }
                el.setName(Locale.getMessage(R.string.Routes_end_point));
                routeLandmarks.add(el);
            }
        }
        return routeLandmarks;
    }
}
