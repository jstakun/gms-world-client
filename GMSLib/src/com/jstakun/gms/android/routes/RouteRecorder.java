package com.jstakun.gms.android.routes;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkFactory;
import com.jstakun.gms.android.ui.AsyncTaskManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.openlapi.QualifiedCoordinates;

/**
 *
 * @author jstakun
 */
public class RouteRecorder {

	public static final String CURRENTLY_RECORDED = "Current";
    public static final String ROUTE_PREFIX = "my_route";
    //TODO MAX_BEARING_RANGE should be based on zoom level
    private static final float MAX_BEARING_RANGE = 9f;

    private static final List<ExtendedLandmark> routePoints = new CopyOnWriteArrayList<ExtendedLandmark>();
    private static final RouteRecorder instance = new RouteRecorder();
    private long startTime;
    private int notificationId = -1;
    private boolean paused = false, saveNextPoint = false;
    private float currentBearing = 0f;
    
    private RouteRecorder() {
    }
    
    public static RouteRecorder getInstance() {
    	return instance;
    }
        
    public String startRecording(RoutesManager routesManager) {
    	ConfigurationManager.getInstance().setOn(ConfigurationManager.RECORDING_ROUTE);  	
    	startTime = System.currentTimeMillis();
    	currentBearing = 0f;
    	routesManager.addRoute(CURRENTLY_RECORDED, routePoints, null);    		
    	AsyncTaskManager asyncTaskManager = (AsyncTaskManager) ConfigurationManager.getInstance().getObject("asyncTaskManager", AsyncTaskManager.class);
    	if (asyncTaskManager != null && notificationId == -1) {
    		String msg = Locale.getMessage(R.string.Routes_Label);
    		notificationId = Integer.parseInt(asyncTaskManager.createNotification(R.drawable.route_24, msg, msg, false));  		
    	} 
    	return CURRENTLY_RECORDED;
    }

    public String stopRecording(RoutesManager routesManager) {
        //System.out.println("RouteRecorder.stopRecording");
        String filename = null;
        if (routePoints.size() > 1) {
            filename = DateTimeUtils.getCurrentDateStamp() + ".kml";
        }
        routesManager.removeRoute(CURRENTLY_RECORDED);
        ConfigurationManager.getInstance().setOff(ConfigurationManager.RECORDING_ROUTE);

        AsyncTaskManager asyncTaskManager = (AsyncTaskManager) ConfigurationManager.getInstance().getObject("asyncTaskManager", AsyncTaskManager.class);
        asyncTaskManager.cancelNotification(notificationId);
        notificationId = -1;
        
        return filename;
    }

    public String[] saveRoute(String prefix) {
        //System.out.println("RouteRecorder.saveRoute");

        String[] details = null;

        if (routePoints.size() > 1) {

            String filename = DateTimeUtils.getCurrentDateStamp() + ".kml";
            if (StringUtils.isNotEmpty(prefix)) {
            	filename = prefix + "_" + filename;
            } else {
            	filename = ROUTE_PREFIX + "_" + filename;
            }
            
            String[] desc = getRouteDetails();

            details = saveRoute(routePoints, filename, desc[1], desc[2], desc[0]);

            if (ConfigurationManager.getInstance().isOff(ConfigurationManager.RECORDING_ROUTE)) {
                paused = false;
                routePoints.clear();
            }
        }

        return details;
    }

    public int addCoordinate(double lat, double lng, float altitude, float accuracy, float speed, float bearing) {
        int mode = -1; //-1 - nothing, 0 - replaced, 1 - added
    	if (!paused) {
            QualifiedCoordinates qc = new QualifiedCoordinates(lat, lng, accuracy, accuracy, Float.NaN); 
            String l = DateTimeUtils.getCurrentDateStamp();
            String[] details = getRouteDetails();
            String description = Locale.getMessage(R.string.Routes_Recording_description, details[0], details[1], details[2]);
            ExtendedLandmark lm = LandmarkFactory.getLandmark(l, description, qc, Commons.ROUTES_LAYER, System.currentTimeMillis());

            if (routePoints.isEmpty()) {
                routePoints.add(lm);
                saveNextPoint = true;
                mode = 1;
            } else {
                ExtendedLandmark current = routePoints.get(routePoints.size()-1);

                float dist = DistanceUtils.distanceInKilometer(current.getQualifiedCoordinates().getLatitude(), current.getQualifiedCoordinates().getLongitude(),
                        lm.getQualifiedCoordinates().getLatitude(), lm.getQualifiedCoordinates().getLongitude());

                if (((dist >= 0.008 && speed > 5) || (dist >= 0.005 && speed <= 5))) { // meters
                    
                	if (MathUtils.abs(bearing - currentBearing) > MAX_BEARING_RANGE) { //|| bearing == 0f
                		currentBearing = bearing;
                		routePoints.add(lm);
                		LoggerUtils.debug("Adding route point: " + lat + "," + lng + " with speed: " + speed + ", distance: " + (dist * 1000f) + " meters and bearing: " + bearing + ".");
                		saveNextPoint = true;
                		mode = 1;
                	} else if (saveNextPoint) {
                		currentBearing = bearing;
                		routePoints.add(lm);
                		LoggerUtils.debug("Adding route point: " + lat + "," + lng + " with speed: " + speed + ", distance: " + (dist * 1000f) + " meters and bearing: " + bearing + ".");
                		saveNextPoint = false;
                		mode = 1;
                	} else {
                		//replace last point
                		routePoints.add(lm);
                		routePoints.remove(routePoints.size()-2);
                		mode = 0;
                	}
                }
            }
        }
    	return mode;
    }

    public void pause() {
        paused = !paused;
    }

    public boolean isPaused() {
        return paused;
    }
    
    public void onAppClose() {
    	notificationId = -1;
    }
    
    private static float routeDistanceInKilometer(List<ExtendedLandmark> points) {
        float distanceInKilometer = 0.0f;

        if (points.size() > 2) {
            for (int i = 0; i < points.size() - 1; i++) {
                ExtendedLandmark current = points.get(i);
                ExtendedLandmark next = points.get(i + 1);

                float dist = DistanceUtils.distanceInKilometer(current.getQualifiedCoordinates().getLatitude(), current.getQualifiedCoordinates().getLongitude(),
                        next.getQualifiedCoordinates().getLatitude(), next.getQualifiedCoordinates().getLongitude());

                distanceInKilometer += dist;
            }
        }

        return distanceInKilometer;
    }

    protected static String[] saveRoute(List<ExtendedLandmark> points, String filename, String avg, String timeInterval, String dist) {
        //System.out.println("RouteRecorder.saveRoute");
        String description = Locale.getMessage(R.string.Routes_Recording_description, dist, avg, timeInterval);

        LoggerUtils.debug("Saving route - " + description);

        PersistenceManagerFactory.getFileManager().saveKmlRoute(points, description, filename);

        return new String[]{filename, description};
    }
    
    private String[] getRouteDetails() {
    	String avg = "n/a";
        String timeInterval = "n/a";
        String dist;

        float distanceInKilometer = routeDistanceInKilometer(routePoints);

        dist = DistanceUtils.formatDistance(distanceInKilometer);

        long endTime = System.currentTimeMillis();

        if (startTime < endTime) {
            long diff = (endTime - startTime);
            double avgSpeed = (distanceInKilometer * 1000.0 * 3600.0) / (double) diff;
            avg = DistanceUtils.formatSpeed(avgSpeed);
            timeInterval = DateTimeUtils.getTimeInterval(diff);
        }
        
        return new String[] {dist, avg, timeInterval};
    }
}
