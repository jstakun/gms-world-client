/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import android.text.format.DateUtils;
import android.text.format.Formatter;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FavouritesDAO;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.StringUtil;
import java.io.File;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class LandmarkParcelableFactory {

    protected static LandmarkParcelable getLandmarkParcelable(ExtendedLandmark l, String key, double lat, double lng, java.util.Locale locale) {
        String desc;
        
        if (l == null) {
            throw new NullPointerException("ExtendedLandmark is null!");
        } else if (l.getQualifiedCoordinates() == null) {
            throw new NullPointerException("QualifiedCoordinates is null");
        }
        
        float distance = DistanceUtils.distanceInKilometer(lat, lng, l.getQualifiedCoordinates().getLatitude(), l.getQualifiedCoordinates().getLongitude());

        String layerName = l.getLayer();
        if (layerName.equals(Commons.LOCAL_LAYER)) {
            desc = l.getDescription();
            if (StringUtils.isNotEmpty(desc)) {
                desc += "<br/>";
            }
            if (l.getCreationDate() > 0) {
            	desc += Locale.getMessage(R.string.creation_date, DateTimeUtils.getDefaultDateTimeString(l.getCreationDate(), locale));
            }
        } else {
            desc = l.getDescription();
        }
        
        if (!StringUtils.isNotEmpty(desc) && l.getCreationDate() > 0){
        	desc += Locale.getMessage(R.string.creation_date, DateTimeUtils.getDefaultDateTimeString(l.getCreationDate(), locale));
        }
        
        String name = l.getName();
        //String name = StringUtils.abbreviate(l.getName(), 48);
        if (layerName.equals(Commons.LOCAL_LAYER)) {
            name = StringUtil.formatCommaSeparatedString(name);
        }

        return new LandmarkParcelable(l.hashCode(), name, key, layerName, desc, distance, l.getCreationDate(), l.getCategoryId(), l.getSubCategoryId(), l.getRating(), l.getNumberOfReviews(), l.getThumbnail(), l.getRevelance());
    }

    public static LandmarkParcelable getLandmarkParcelable(FavouritesDAO f, double lat, double lng) {
        float distance = DistanceUtils.distanceInKilometer(lat, lng, f.getLatitude(), f.getLongitude());
        long fromLastCheckinTime = System.currentTimeMillis() - f.getLastCheckinDate();
        CharSequence lastCheckinDate = DateUtils.getRelativeTimeSpanString(f.getLastCheckinDate(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        String fromLastCheckin = "<font color=\"#339933\">" + lastCheckinDate + "</font>";

        if (fromLastCheckinTime < (1000 * 60 * 60 * ConfigurationManager.getInstance().getLong(ConfigurationManager.CHECKIN_TIME_INTERVAL))) {
            fromLastCheckin = "<font color=\"#FF0000\">" + lastCheckinDate + "</font>";
        }
        
        String maxDistance = DistanceUtils.formatDistance(f.getMaxDistance() / 1000.0d);
        String distanceStatus = "#FF0000";
        if (f.getMaxDistance() > ConfigurationManager.getInstance().getLong(ConfigurationManager.MIN_CHECKIN_DISTANCE)) {
            distanceStatus = "#339933";
        }

        String desc = Locale.getMessage(R.string.lastCheckinDate, fromLastCheckin) +  ", " +
        		Locale.getMessage(R.string.Landmark_distance_max, "<font color=\"" + distanceStatus + "\">" + maxDistance + "</font>");
        return new LandmarkParcelable((int)f.getId(), f.getName(), Long.toString(f.getId()), f.getLayer(), desc, distance, f.getLastCheckinDate(), -1, -1, -1d, 0, null, 0);
    }

    public static LandmarkParcelable getLandmarkParcelable(File f, int id, String name, java.util.Locale locale) {
        String length = Formatter.formatFileSize(ConfigurationManager.getInstance().getContext(), f.length());
        String date = DateTimeUtils.getShortDateTimeString(f.lastModified(), locale);
        String desc = length + " | " + date;
        return new LandmarkParcelable(id, f.getName(), "", name, desc, 0.0f, f.lastModified(), -1, -1, -1d, 0, null, 0);
    }
    
    public static LandmarkParcelable getLandmarkParcelable(int id, String name, String value) {
    	return new LandmarkParcelable(id, name, "", Commons.LOCAL_LAYER, "Value: " + value, 0.0f, 0, -1, -1, -1d, 0, null, 0);
    }
}
