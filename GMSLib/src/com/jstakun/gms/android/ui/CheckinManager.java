/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.content.Context;
import com.google.common.base.Predicate;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FavouritesDAO;
import com.jstakun.gms.android.data.FavouritesDbDataSource;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.lib.R;
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

    private AsyncTaskManager asyncTaskManager;  

    public CheckinManager(AsyncTaskManager asyncTaskManager, Context context) {
        this.asyncTaskManager = asyncTaskManager;

        FavouritesDbDataSource fdb = (FavouritesDbDataSource) ConfigurationManager.getInstance().getObject("FAVOURITESDB", FavouritesDbDataSource.class);
        if (fdb == null) {
            fdb = new FavouritesDbDataSource(context);
            ConfigurationManager.getInstance().putObject("FAVOURITESDB", fdb);
        }
    }

    public boolean checkinAction(boolean addToFavourites, boolean silent, ExtendedLandmark selectedLandmark) {
    	String key = getLandmarkKey(selectedLandmark);
        if (addToFavourites) {
            FavouritesDbDataSource fdb = (FavouritesDbDataSource) ConfigurationManager.getInstance().getObject("FAVOURITESDB", FavouritesDbDataSource.class);
            if (fdb != null && !fdb.hasLandmark(selectedLandmark)) {
            	fdb.addLandmark(selectedLandmark, key);
            }
        }
        return checkinAction(selectedLandmark.getLayer(), selectedLandmark.getName(), key, silent);
    }

    private boolean checkinAction(String selectedLayer, String name, String venueid, boolean silent) {
        boolean result = false;
        
        UserTracker.getInstance().trackEvent("AutoCheckin", "CheckinManager.AutoCheckinAction", selectedLayer, 0);
        String checkinat = Locale.getMessage(R.string.Social_checkin_prompt, name);
        if ((selectedLayer.equals(Commons.FOURSQUARE_LAYER) || selectedLayer.equals(Commons.FOURSQUARE_MERCHANT_LAYER))
                && ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)) {
            asyncTaskManager.executeSocialCheckInTask(checkinat, R.drawable.foursquare_24, silent, selectedLayer, venueid, name);
            result = true;
        } else if (selectedLayer.equals(Commons.FACEBOOK_LAYER) && ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
            asyncTaskManager.executeSocialCheckInTask(checkinat, R.drawable.facebook_24, silent, selectedLayer, venueid, name);
            result = true;
        } else if (selectedLayer.equals(Commons.GOOGLE_PLACES_LAYER)
                && ConfigurationManager.getInstance().isOn(ConfigurationManager.GL_AUTH_STATUS)) {
            if (venueid != null) {
            	asyncTaskManager.executeSocialCheckInTask(checkinat, R.drawable.google_24, silent, selectedLayer, venueid, name);
            	result = true;
            }
        } else if (selectedLayer.equals(Commons.MY_POSITION_LAYER)) {
            asyncTaskManager.executeSocialSendMyLocationTask(silent);
            result = true;
        } else { //if (landmarkManager.getLayerManager().isLayerCheckinable(selectedLayer) && ConfigurationManager.getInstance().isUserLoggedIn()) {
            asyncTaskManager.executeLocationCheckInTask(-1, venueid, Locale.getMessage(R.string.searchcheckin), name, silent);
            result = true;
        }

        return result;
    }

    public synchronized int autoCheckin(double lat, double lon, boolean silent) {
        int checkinCount = 0;
    	FavouritesDbDataSource fdb = (FavouritesDbDataSource) ConfigurationManager.getInstance().getObject("FAVOURITESDB", FavouritesDbDataSource.class);
        if (fdb != null) {
        	for (FavouritesDAO favourite : fdb.fetchAllLandmarks()) {
            	long distInMeter = (long) DistanceUtils.distanceInMeter(lat, lon, favourite.getLatitude(), favourite.getLongitude());
            	//System.out.println("Checking landmark " + favourite.getName() + " in distance " + distInMeter + " meter.");
            	CheckinCandidatePredicate candidatePredicate = new CheckinCandidatePredicate(distInMeter);
            	if (candidatePredicate.apply(favourite)) {
                	if (checkinAction(favourite.getLayer(), favourite.getName(), favourite.getKey(), silent)) {
                    	checkinCount++;
                    	LoggerUtils.debug("CheckinManager.autoCheckin() initialized check-in at " + favourite.getName());
                	} 
            	} else if (distInMeter > favourite.getMaxDistance()) {
                	fdb.updateMaxDist(distInMeter, favourite.getId());
                	//favourite.setMaxDistance(distInMeter);
            	}
        	}
        }
        
        return checkinCount;
    }

    private String getLandmarkKey(ExtendedLandmark selectedLandmark) {
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
        private static final long ONE_HOUR = 60 * 60 * 1000;

        public CheckinCandidatePredicate(long distInMeter) {
            this.distInMeter = distInMeter;
        }

        //checkinTimeInterval;8
        //minCheckinDistance;1000
        //maxCurrentDistance;200
        
        //rule1
        //last check-in xx hours ago, 
        //max distance xx meters, 
        //current distance xx meters max
        private boolean rule1(FavouritesDAO favourite) {
        	return ((System.currentTimeMillis() - favourite.getLastCheckinDate()) > (ConfigurationManager.getInstance().getLong(ConfigurationManager.CHECKIN_TIME_INTERVAL) * ONE_HOUR)
                    && favourite.getMaxDistance() >= ConfigurationManager.getInstance().getInt(ConfigurationManager.MIN_CHECKIN_DISTANCE)  
                    && distInMeter < ConfigurationManager.getInstance().getInt(ConfigurationManager.MAX_CURRENT_DISTANCE));
        }
        
        //rule2
        //last check-in xx hours ago
        //current distance xx meters max
        private boolean rule2(FavouritesDAO favourite) {
        	return ((System.currentTimeMillis() - favourite.getLastCheckinDate()) > (ConfigurationManager.getInstance().getLong(ConfigurationManager.CHECKIN_TIME_INTERVAL) * ONE_HOUR)
                    && distInMeter < ConfigurationManager.getInstance().getInt(ConfigurationManager.MAX_CURRENT_DISTANCE));
        }
        
        public boolean apply(FavouritesDAO favourite) {
        	return rule2(favourite);
        	//return rule1(favourite);
        }
    }
}
