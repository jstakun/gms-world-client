package com.jstakun.gms.android.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FavouritesDAO;
import com.jstakun.gms.android.data.FavouritesDbDataSource;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.UserTracker;
import com.openlapi.AddressInfo;

/**
 *
 * @author jstakun
 */
public class CheckinManager {

    private static CheckinManager instance = null;
    
    private static final List<String> checkinInProgress = new ArrayList<String>();
    
    private CheckinManager() {
    }
    
    public synchronized static CheckinManager getInstance() {
    	if (instance == null) {
    		instance = new CheckinManager();
    	}
    	return instance;
    }
   
    
    public boolean checkinAction(boolean addToFavourites, boolean silent, ExtendedLandmark selectedLandmark) {
    	String key = getLandmarkKey(selectedLandmark);
        if (addToFavourites) {
            FavouritesDbDataSource fdb = ConfigurationManager.getDatabaseManager().getFavouritesDatabase();
            if (fdb != null && !fdb.hasLandmark(key)) {
            	FavouritesDAO favouritesDAO = new FavouritesDAO(selectedLandmark.hashCode(), selectedLandmark.getName(), selectedLandmark.getQualifiedCoordinates().getLatitude(), 
            			selectedLandmark.getQualifiedCoordinates().getLongitude(), selectedLandmark.getLayer(), 0, System.currentTimeMillis(), key);
            	fdb.addLandmark(favouritesDAO, key);
            }
        }
        return checkinAction(selectedLandmark.getLayer(), selectedLandmark.getName(), key, selectedLandmark.getQualifiedCoordinates().getLatitude(), selectedLandmark.getQualifiedCoordinates().getLongitude(), silent);
    }

    private boolean checkinAction(String selectedLayer, String name, String venueid, Double lat, Double lng, boolean silent) {
        boolean result = false;
        UserTracker.getInstance().trackEvent("AutoCheckin", "CheckinManager.AutoCheckinAction", selectedLayer, 0);
        String checkinat = Locale.getMessage(R.string.Social_checkin_prompt, name);
        if ((selectedLayer.equals(Commons.FOURSQUARE_LAYER) || selectedLayer.equals(Commons.FOURSQUARE_MERCHANT_LAYER)) && ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)) {
        	AsyncTaskManager.getInstance().executeSocialCheckInTask(checkinat, R.drawable.checkin_24, silent, selectedLayer, venueid, name, lat, lng, checkinInProgress);
            result = true;
        } else if (selectedLayer.equals(Commons.FACEBOOK_LAYER) && ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
        	AsyncTaskManager.getInstance().executeSocialCheckInTask(checkinat, R.drawable.checkin_24, silent, selectedLayer, venueid, name, lat, lng, checkinInProgress);
            result = true;
        } else if (selectedLayer.equals(Commons.GOOGLE_PLACES_LAYER) && ConfigurationManager.getInstance().isOn(ConfigurationManager.GL_AUTH_STATUS)) {
            if (venueid != null) {
            	AsyncTaskManager.getInstance().executeSocialCheckInTask(checkinat, R.drawable.checkin_24, silent, selectedLayer, venueid, name, lat, lng, checkinInProgress);
            	result = true;
            }
        } else if (selectedLayer.equals(Commons.MY_POSITION_LAYER)) {
        	AsyncTaskManager.getInstance().executeSocialSendMyLocationTask(silent);
            result = true;
        } else if (ConfigurationManager.getUserManager().isUserLoggedIn() && 
        		!(selectedLayer.equals(Commons.FOURSQUARE_LAYER) || selectedLayer.equals(Commons.FOURSQUARE_MERCHANT_LAYER) ||
        		selectedLayer.equals(Commons.FACEBOOK_LAYER) || selectedLayer.equals(Commons.GOOGLE_PLACES_LAYER))) {
        	AsyncTaskManager.getInstance().executeLocationCheckInTask(R.drawable.checkin_24, venueid, Locale.getMessage(R.string.searchcheckin), name, silent, checkinInProgress);
            result = true;
        }

        return result;
    }

    public synchronized int autoCheckin(double lat, double lon, boolean silent) {
        int checkinCount = 0;
        FavouritesDbDataSource fdb = ConfigurationManager.getDatabaseManager().getFavouritesDatabase();
        if (fdb != null) {
        	List<FavouritesDAO> favourites = fdb.fetchAllLandmarks();
        	for (FavouritesDAO favourite : favourites) {
            	long distInMeter = (long) DistanceUtils.distanceInMeter(lat, lon, favourite.getLatitude(), favourite.getLongitude());
            	//System.out.println("Checking landmark " + favourite.getName() + " in distance " + distInMeter + " meter.");
            	if (!checkinInProgress.contains(favourite.getKey())) {
            		CheckinCandidatePredicate candidatePredicate = new CheckinCandidatePredicate(distInMeter, favourites);
            		CheckinDeletePredicate deletePredicate = new CheckinDeletePredicate();	
            		if (candidatePredicate.apply(favourite)) {
            			checkinInProgress.add(favourite.getKey());
            			if (checkinAction(favourite.getLayer(), favourite.getName(), favourite.getKey(), favourite.getLatitude(), favourite.getLongitude(), silent)) {
                    		checkinCount++;
                    		LoggerUtils.debug("CheckinManager.autoCheckin() initialized check-in at " + favourite.getName());
                		} 
            		} else if (deletePredicate.apply(favourite)) {
            			LoggerUtils.debug("CheckinManager.autoCheckin() deleting check-in " + favourite.getName());
            	    	fdb.deleteLandmark(favourite.getId());
            		} else if (distInMeter > favourite.getMaxDistance()) {
                		fdb.updateMaxDist(distInMeter, favourite.getId());
            		}
            	}
        	}
        }
        
        return checkinCount;
    }

    private static String getLandmarkKey(ExtendedLandmark selectedLandmark) {
    	String venueid = selectedLandmark.getUrl();
    	String selectedLayer = selectedLandmark.getLayer();
    	if (selectedLayer.equals(Commons.FOURSQUARE_LAYER) || selectedLayer.equals(Commons.FOURSQUARE_MERCHANT_LAYER)) {
    		venueid = OAuthServiceFactory.getSocialUtils(Commons.FOURSQUARE).getKey(selectedLandmark.getUrl());         
    	} else if (selectedLayer.equals(Commons.FACEBOOK_LAYER)) {
    		venueid = OAuthServiceFactory.getSocialUtils(Commons.FACEBOOK).getKey(selectedLandmark.getUrl());            
    	} else if (selectedLayer.equals(Commons.GOOGLE_PLACES_LAYER))  {
    		venueid = selectedLandmark.getAddressInfo().getField(AddressInfo.EXTENSION);
    	} //else { //if (landmarkManager.getLayerManager().isLayerCheckinable(selectedLayer)) {
    		//if (venueid != null) {
    			//String[] s = venueid.split("=");
    			//if (s.length > 0) {
    				//venueid = s[s.length - 1];
    			//} 
    		//}
    	//}
        return venueid;
    }
    
    private class CheckinCandidatePredicate implements Predicate<FavouritesDAO> {

        private long distInMeter;
        private List<FavouritesDAO> favourites;

        public CheckinCandidatePredicate(long distInMeter, List<FavouritesDAO> favourites) {
            this.distInMeter = distInMeter;
            this.favourites = favourites;
        }

        //checkinTimeInterval;8
        //minCheckinDistance;1000
        //maxCurrentDistance;200
        
        //rule1
        //last check-in xx hours ago, 
        //max distance xx meters, 
        //current distance xx meters max
        /*private boolean rule1(FavouritesDAO favourite) {
        	return ((System.currentTimeMillis() - favourite.getLastCheckinDate()) > (ConfigurationManager.getInstance().getLong(ConfigurationManager.CHECKIN_TIME_INTERVAL) * ONE_HOUR)
                    && favourite.getMaxDistance() >= ConfigurationManager.getInstance().getInt(ConfigurationManager.MIN_CHECKIN_DISTANCE)  
                    && distInMeter < ConfigurationManager.getInstance().getInt(ConfigurationManager.MAX_CURRENT_DISTANCE));
        }*/
        
        //rule2
        //last check-in xx hours ago
        //current distance xx meters max
        /*private boolean rule2(FavouritesDAO favourite) {
        	return ((System.currentTimeMillis() - favourite.getLastCheckinDate()) > (ConfigurationManager.getInstance().getLong(ConfigurationManager.CHECKIN_TIME_INTERVAL) * ONE_HOUR)
                    && distInMeter < ConfigurationManager.getInstance().getInt(ConfigurationManager.MAX_CURRENT_DISTANCE));
        }*/
        
        //rule3
        //current distance xx meters max
        //last check-in xx hours ago or max distance xx meters or checked-in to other landmark in the meantime
        private boolean rule3(FavouritesDAO favourite) {
        	return (distInMeter < ConfigurationManager.getInstance().getInt(ConfigurationManager.MAX_CURRENT_DISTANCE) &&
        		   ((System.currentTimeMillis() - favourite.getLastCheckinDate()) > (ConfigurationManager.getInstance().getLong(ConfigurationManager.CHECKIN_TIME_INTERVAL) * DateTimeUtils.ONE_HOUR)
                   || favourite.getMaxDistance() >= ConfigurationManager.getInstance().getInt(ConfigurationManager.MIN_CHECKIN_DISTANCE)
                   || checkinToLayer(favourite)));
        }
        
        private boolean checkinToLayer(FavouritesDAO favourite) {
        	//String layer = favourite.getLayer();
        	long lastCheckin = favourite.getLastCheckinDate() + (ConfigurationManager.DEFAULT_REPEAT_TIME * 1000);
        	
        	boolean checkinToLayer = false;
        	
        	for (FavouritesDAO fav : favourites) {
        		if (!fav.equals(favourite) && fav.getLastCheckinDate() >= lastCheckin) { //&& fav.getLayer().equals(layer)) {
        			return true;
        		}
        	}
        	
        	return checkinToLayer;
        }
        
        public boolean apply(FavouritesDAO favourite) {
        	return rule3(favourite);
        	//return rule2(favourite);
        	//return rule1(favourite);
        }
    }
    
    private class CheckinDeletePredicate implements Predicate<FavouritesDAO> {

		@Override
		public boolean apply(FavouritesDAO favourite) {
			return System.currentTimeMillis() - favourite.getLastCheckinDate() > DateTimeUtils.ONE_YEAR;
		}
    	
    }
}
