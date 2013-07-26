/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FavouritesDAO;
import com.jstakun.gms.android.data.FavouritesDbDataSource;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.UserTracker;
import java.util.List;

/**
 *
 * @author jstakun
 */
public class CheckinManager {

    private LandmarkManager landmarkManager;
    private AsyncTaskManager asyncTaskManager;
    private List<FavouritesDAO> favourites;

    public CheckinManager(LandmarkManager landmarkManager, AsyncTaskManager asyncTaskManager) {
        this.landmarkManager = landmarkManager;
        this.asyncTaskManager = asyncTaskManager;

        FavouritesDbDataSource fdb = (FavouritesDbDataSource) ConfigurationManager.getInstance().getObject("FAVOURITESDB", FavouritesDbDataSource.class);
        if (fdb == null) {
            fdb = new FavouritesDbDataSource(ConfigurationManager.getInstance().getContext());
            ConfigurationManager.getInstance().putObject("FAVOURITESDB", fdb);
        }
        favourites = fdb.fetchAllLandmarks();
        //System.out.println("Number of auto checkin landmarks: " + favourites.size());
    }

    public boolean checkinAction(boolean addToFavourites, boolean silent) {
        ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
        return checkinAction(addToFavourites, selectedLandmark, silent);
    }

    private boolean checkinAction(boolean addToFavourites, ExtendedLandmark selectedLandmark, boolean silent) {
        boolean result = false;
        String selectedLayer = selectedLandmark.getLayer();

        if (addToFavourites) {
            FavouritesDbDataSource fdb = (FavouritesDbDataSource) ConfigurationManager.getInstance().getObject("FAVOURITESDB", FavouritesDbDataSource.class);
            FavouritesDAO favourite = fdb.addLandmark(selectedLandmark);
            favourites.add(favourite);
        }

        if ((selectedLayer.equals(Commons.FOURSQUARE_LAYER) || selectedLayer.equals(Commons.FOURSQUARE_MERCHANT_LAYER))
                && ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)) {
            String checkinat = Locale.getMessage(R.string.Social_checkin_prompt, selectedLandmark.getName());
            asyncTaskManager.executeSocialCheckInTask(checkinat, R.drawable.foursquare_24, silent, selectedLandmark);
            result = true;
        } else if (selectedLayer.equals(Commons.FACEBOOK_LAYER)
                && ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
            String checkinat = Locale.getMessage(R.string.Social_checkin_prompt, selectedLandmark.getName());
            asyncTaskManager.executeSocialCheckInTask(checkinat, R.drawable.facebook_24, silent, selectedLandmark);
            result = true;
        } else if (selectedLayer.equals(Commons.GOOGLE_PLACES_LAYER)
                && ConfigurationManager.getInstance().isOn(ConfigurationManager.GL_AUTH_STATUS)) {
            String checkinat = Locale.getMessage(R.string.Social_checkin_prompt, selectedLandmark.getName());
            asyncTaskManager.executeSocialCheckInTask(checkinat, R.drawable.google_24, silent, selectedLandmark);
            result = true;
        } else if (selectedLayer.equals(Commons.MY_POSITION_LAYER)) {
            asyncTaskManager.executeSocialSendMyLocationTask(silent);
            result = true;
        } else if (landmarkManager.getLayerManager().isLayerCheckinable(selectedLayer)
                && ConfigurationManager.getInstance().isUserLoggedIn()) {
            String venueid = selectedLandmark.getUrl();
            if (venueid != null) {
                String[] s = venueid.split("=");
                if (s.length > 0) {
                    venueid = s[s.length - 1];
                }
            }
            UserTracker.getInstance().trackEvent("AutoCheckin", "CheckinManager.AutoCheckinAction", selectedLayer, 0);
            asyncTaskManager.executeLocationCheckInTask(-1, venueid, Locale.getMessage(R.string.searchcheckin), selectedLandmark.getName(), silent);
            result = true;
        }

        return result;
    }

    public synchronized int autoCheckin(double lat, double lon, boolean silent) {
        int checkinCount = 0;
    	FavouritesDbDataSource fdb = (FavouritesDbDataSource) ConfigurationManager.getInstance().getObject("FAVOURITESDB", FavouritesDbDataSource.class);
        
        for (FavouritesDAO favourite : favourites) {
            long distInMeter = (long) DistanceUtils.distanceInMeter(lat, lon, favourite.getLatitude(), favourite.getLongitude());
            //System.out.println("Checking landmark " + favourite.getName() + " in distance " + distInMeter + " meter.");
            CheckinCandidatePredicate candidatePredicate = new CheckinCandidatePredicate(distInMeter);
            if (candidatePredicate.apply(favourite)) {
                ExtendedLandmark landmark = findSimilarLandmark(favourite.getName(), favourite.getLatitude(), favourite.getLongitude(), favourite.getLayer());
                if (landmark != null && fdb != null) {
                    //change in production
                    if (checkinAction(false, landmark, silent)) {
                        fdb.updateOnCheckin(favourite.getId());
                        favourite.setMaxDistance(0);
                        favourite.setLastCheckinDate(System.currentTimeMillis());
                        checkinCount++;
                    }
                }
            } else if (distInMeter > favourite.getMaxDistance() && fdb != null) {
                fdb.updateMaxDist(distInMeter, favourite.getId());
                favourite.setMaxDistance(distInMeter);
            }
        }
        
        return checkinCount;
    }

    private ExtendedLandmark findSimilarLandmark(String name, double lat, double lng, String layer) {
        for (ExtendedLandmark landmark : landmarkManager.getUnmodifableLayer(layer)) {
            if (Objects.equal(name, landmark.getName()) && 
                MathUtils.abs(lat - landmark.getQualifiedCoordinates().getLatitude()) < 0.01 &&
                MathUtils.abs(lng - landmark.getQualifiedCoordinates().getLongitude()) < 0.01) {
                return landmark;
            }
        }
        return null;
    }
    
    private class CheckinCandidatePredicate implements Predicate<FavouritesDAO> {

        private long distInMeter;

        public CheckinCandidatePredicate(long distInMeter) {
            this.distInMeter = distInMeter;
        }

        public boolean apply(FavouritesDAO favourite) {
            return ((System.currentTimeMillis() - favourite.getLastCheckinDate()) > ConfigurationManager.getInstance().getLong(ConfigurationManager.CHECKIN_TIME_INTERVAL)
                    && favourite.getMaxDistance() >= ConfigurationManager.getInstance().getInt(ConfigurationManager.MIN_CHECKIN_DISTANCE)  
                    && distInMeter < ConfigurationManager.getInstance().getInt(ConfigurationManager.MAX_CURRENT_DISTANCE));
        }
    }
}
