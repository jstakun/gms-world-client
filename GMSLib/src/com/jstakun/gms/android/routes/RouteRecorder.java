package com.jstakun.gms.android.routes;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
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
import com.openlapi.QualifiedCoordinates;

import android.location.Location;

/**
 *
 * @author jstakun
 */
public class RouteRecorder {

	public static final String CURRENTLY_RECORDED = "Current";
    public static final String ROUTE_PREFIX = "my_route";
    private static final int MAX_REASONABLE_SPEED = 90; //324 kilometer per hour or 201 mile per hour
    private static final int MAX_REASONABLE_ALTITUDECHANGE = 200; //meters
 
    private static final List<ExtendedLandmark> routePoints = new CopyOnWriteArrayList<ExtendedLandmark>();
    private static RouteRecorder instance = null;
    private long startTime = -1, endTime = -1;
    private int notificationId = -1;
    private boolean paused = false;
    
    private static final float mMaxAcceptableAccuracy = 50; //meters
    private static final Vector<Location> mWeakLocations = new Vector<Location>(3);
    private static final Queue<Double> mAltitudes = new LinkedList<Double>();;
    private Location mPreviousLocation;
    private boolean mSpeedSanityCheck = true;
    
    private RouteRecorder() {
    }
    
    public synchronized static RouteRecorder getInstance() {
    	if (instance == null) {
    		instance = new RouteRecorder();
    	}
    	return instance;
    }
        
    public String startRecording() {
    	ConfigurationManager.getInstance().setOn(ConfigurationManager.RECORDING_ROUTE);  	
    	if (!RoutesManager.getInstance().containsRoute(CURRENTLY_RECORDED)) {
    		startTime = System.currentTimeMillis();
    		RoutesManager.getInstance().addRoute(CURRENTLY_RECORDED, routePoints, null);
    	}
    	if (notificationId == -1) {
    		String msg = Locale.getMessage(R.string.Routes_Label);
    		notificationId = Integer.parseInt(AsyncTaskManager.getInstance().createNotification(R.drawable.route_24, msg, msg, false));  		
    	} 
    	return CURRENTLY_RECORDED;
    }

    public String stopRecording() {
        //System.out.println("RouteRecorder.stopRecording");
        String filename = null;
        if (routePoints.size() > 1) {
            filename = DateTimeUtils.getCurrentDateStamp() + ".kml";
        }
        RoutesManager.getInstance().removeRoute(CURRENTLY_RECORDED);
        ConfigurationManager.getInstance().setOff(ConfigurationManager.RECORDING_ROUTE);

        AsyncTaskManager.getInstance().cancelNotification(notificationId);
        notificationId = -1;
        
        return filename;
    }

    public String[] saveRoute(String prefix) {
        //System.out.println("RouteRecorder.saveRoute");

        String[] details = null;

        if (routePoints.size() > 2) { //save route only if longer than 2 points

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

    public boolean addCoordinate(Location newLocation) {
    	Location filteredLocation = filterLocation(newLocation);
    	boolean added = false;   
    	if (!paused && filteredLocation != null) {
    		mPreviousLocation = newLocation;
        	QualifiedCoordinates qc = new QualifiedCoordinates(filteredLocation.getLatitude(),filteredLocation.getLongitude(), (float)filteredLocation.getAltitude(), filteredLocation.getAccuracy(), Float.NaN); 
            String l = DateTimeUtils.getCurrentDateStamp();
            String description = null;
            if (!routePoints.isEmpty()) {
            	String[] details = getRouteDetails();
            	description = Locale.getMessage(R.string.Routes_Recording_description, details[0], details[1], details[2]);
            } 
            ExtendedLandmark lm = LandmarkFactory.getLandmark(l, description, qc, Commons.ROUTES_LAYER, System.currentTimeMillis());
            endTime = System.currentTimeMillis();
            LoggerUtils.debug(routePoints.size() + ". Adding route point: " + filteredLocation.getLatitude() + "," + filteredLocation.getLongitude() + " with speed: " + filteredLocation.getSpeed() + ", accuracy " + filteredLocation.getAccuracy() + " and bearing: " + filteredLocation.getBearing() + ".");
    		routePoints.add(lm);
            added = true;
        }
    	return added;
    }

    public void pause() {
        paused = !paused;
    }

    public boolean isPaused() {
        return paused;
    }
    
    public void onAppClose() {
    	//stop recording and save current route
    	//ConfigurationManager.getInstance().setOff(ConfigurationManager.RECORDING_ROUTE);
    	String[] details = saveRoute(null);
    	if (details != null) {
            LoggerUtils.debug("Saved route: " + details[0]);
        }
        
    	notificationId = -1;
    }
    
    private static float routeDistanceInKilometer(List<ExtendedLandmark> points) {
        float distanceInKilometer = 0.0f;

        if (points.size() > 1) {
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

        if (startTime > 0 && startTime < endTime) {
            long diff = (endTime - startTime);
            timeInterval = DateTimeUtils.getTimeInterval(diff);
            if (distanceInKilometer > 0.0f) {
            	double avgSpeed = (distanceInKilometer * 1000.0 * 3600.0) / (double) diff;
            	avg = DistanceUtils.formatSpeed(avgSpeed);
            }
        }
        
        return new String[] {dist, avg, timeInterval};
    }
    
    private Location filterLocation(Location proposedLocation) {
    	// Do no include log wrong 0.0 lat 0.0 long, skip to next value in while-loop
    	if (proposedLocation != null && (proposedLocation.getLatitude() == 0.0d || proposedLocation.getLongitude() == 0.0d)) {
    		LoggerUtils.debug("A wrong location was received, 0.0 latitude and 0.0 longitude... ");
    		proposedLocation = null;
    	}

    	// Do not log a point which is more inaccurate then is configured to be acceptable
    	if (proposedLocation != null && proposedLocation.getAccuracy() > mMaxAcceptableAccuracy) {
    		LoggerUtils.debug(String.format("A weak location was received, lots of inaccuracy... (%f is more then max %f)", proposedLocation.getAccuracy(), mMaxAcceptableAccuracy));
    		proposedLocation = addBadLocation(proposedLocation);
    	}

    	// Do not log a point which might be on any side of the previous point
    	if (proposedLocation != null && mPreviousLocation != null && proposedLocation.getAccuracy() > (mPreviousLocation.distanceTo(proposedLocation) * 3)) { //TODO testing
    		LoggerUtils.debug(String.format("A weak location was received, not quite clear from the previous route point... (%f more then max %f)", proposedLocation.getAccuracy(), mPreviousLocation.distanceTo(proposedLocation)));
    		proposedLocation = addBadLocation(proposedLocation);
    	}

    	// Speed checks, check if the proposed location could be reached from the previous one in sane speed
    	// Common to jump on network logging and sometimes jumps on Samsung Galaxy S type of devices
    	if (mSpeedSanityCheck && proposedLocation != null && mPreviousLocation != null) {
    		// To avoid near instant teleportation on network location or glitches cause continent hopping
    		float meters = proposedLocation.distanceTo(mPreviousLocation);
    		long seconds = (proposedLocation.getTime() - mPreviousLocation.getTime()) / 1000L;
    		float speed = meters / seconds;
    		if (speed > MAX_REASONABLE_SPEED) {
    			LoggerUtils.debug("A strange location was received, a really high speed of " + speed + " m/s, prob wrong...");
    			proposedLocation = addBadLocation(proposedLocation);
    			//Might be a messed up Samsung Galaxy S GPS, reset the logging
    			//stop and start gps listener
    		}
    	}

    	// Remove speed if not sane
    	if (mSpeedSanityCheck && proposedLocation != null && proposedLocation.getSpeed() > MAX_REASONABLE_SPEED) {
    		LoggerUtils.debug("A strange speed, a really high speed, prob wrong...");
    		proposedLocation.removeSpeed();
    	}

    	// Remove altitude if not sane
    	if (mSpeedSanityCheck && proposedLocation != null && proposedLocation.hasAltitude()) {
    		if (!addSaneAltitude(proposedLocation.getAltitude())) {
    			LoggerUtils.debug("A strange altitude, a really big difference, prob wrong...");
    			proposedLocation.removeAltitude();
    		}
    	}
       
    	// Older bad locations will not be needed
    	if (proposedLocation != null) {
    		mWeakLocations.clear();
    	}
    	return proposedLocation;
    }
    
    private Location addBadLocation(Location location) {
    	mWeakLocations.add(location);
    	if (mWeakLocations.size() < 3) {
    		location = null;
    	} else {
    		Location best = mWeakLocations.lastElement();
    		for (Location whimp : mWeakLocations) {
    			if (whimp.hasAccuracy() && best.hasAccuracy() && whimp.getAccuracy() < best.getAccuracy()) {
    				best = whimp;
    			} else if (whimp.hasAccuracy() && !best.hasAccuracy()) {
    				best = whimp;
                }          
    		}
    		synchronized (mWeakLocations) {
    			mWeakLocations.clear();
    		}
    		location = best;
       }
       return location;
    }
    
    private boolean addSaneAltitude(double altitude) {
    	boolean sane = true;
    	double avg = 0;
    	int elements = 0;
    	// Even insane altitude shifts increases alter perception
    	mAltitudes.add(altitude);
    	if (mAltitudes.size() > 3) {
    		mAltitudes.poll();
    	}
    	for (Double alt : mAltitudes) {
    		avg += alt;
    		elements++;
    	}
    	avg = avg / elements;
    	sane = Math.abs(altitude - avg) < MAX_REASONABLE_ALTITUDECHANGE;

    	return sane;
    }
}
