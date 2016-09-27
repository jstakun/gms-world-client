package com.jstakun.gms.android.landmarks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.deals.Category;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;

/**
 *
 * @author jstakun
 */
public class LayerManager {

	public static final int LAYER_LOCAL = 1;
    public static final int LAYER_EXTERNAL = 2;
    public static final int LAYER_DYNAMIC = 0;
    public static final int LAYER_FILESYSTEM = 3;
    public static final int LAYER_ICON_SMALL = 0;
    public static final int LAYER_ICON_LARGE = 1;
    
    private static final Map<String, Layer> layers = new LinkedHashMap<String, Layer>();
    private static final Map<String, Layer> allLayers = new LinkedHashMap<String, Layer>();
    private static final List<String> dynamicLayers = new ArrayList<String>();
    
    private static boolean initialized = false;
    
    private static LayerManager instance = new LayerManager();

    public static LayerManager getInstance() {
    	return instance;
    }
    
    private LayerManager() {
    	allLayers.put(Commons.LOCAL_LAYER, LayerFactory.getLayer(Commons.LOCAL_LAYER, false, true, isLayerEnabledConf(Commons.LOCAL_LAYER), false, false, Arrays.asList(new LayerReader[]{new LandmarkDBReader()}), null, R.drawable.ok16, null, R.drawable.ok, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Phone_Landmarks_desc), Commons.LOCAL_LAYER, FileManager.ClearPolicy.ONE_MONTH, -1)); 
        allLayers.put(Commons.FACEBOOK_LAYER, LayerFactory.getLayer(Commons.FACEBOOK_LAYER, false, true, isLayerEnabledConf(Commons.FACEBOOK_LAYER), true, true, Arrays.asList(new LayerReader[]{new FbTaggedReader(), /*new FbCheckinsReader(),*/ new FbPhotosReader(), new FbPlacesReader()}), null, R.drawable.facebook_icon, null, R.drawable.facebook_24, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Facebook_desc), Commons.FACEBOOK_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.facebook_128));  
        allLayers.put(Commons.FOURSQUARE_LAYER, LayerFactory.getLayer(Commons.FOURSQUARE_LAYER, false, true, isLayerEnabledConf(Commons.FOURSQUARE_LAYER), true, true, Arrays.asList(new LayerReader[]{new FsCheckinsReader(), new FsRecommendsReader(), new FoursquareReader()}), null, R.drawable.foursquare, null, R.drawable.foursquare_24, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Foursquare_desc), Commons.FOURSQUARE_LAYER, FileManager.ClearPolicy.ONE_YEAR, R.drawable.foursquare_128)); 
        allLayers.put(Commons.YELP_LAYER, LayerFactory.getLayer(Commons.YELP_LAYER, false, true, isLayerEnabledConf(Commons.YELP_LAYER), false, true, Arrays.asList(new LayerReader[]{new YelpReader()}), null, R.drawable.yelp, null, R.drawable.yelp_24, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Yelp_desc), Commons.YELP_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.yelp_128));
        allLayers.put(Commons.HOTELS_LAYER, LayerFactory.getLayer(Commons.HOTELS_LAYER, false, true, isLayerEnabledConf(Commons.HOTELS_LAYER), false, true, Arrays.asList(new LayerReader[]{new HotelsReader()}), null, R.drawable.hotel, null, R.drawable.hotel_24, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Hotels_Combined_desc), Commons.HOTELS_LAYER, FileManager.ClearPolicy.ONE_YEAR, R.drawable.travel_img)); 
        allLayers.put(Commons.GOOGLE_PLACES_LAYER, LayerFactory.getLayer(Commons.GOOGLE_PLACES_LAYER, false, true, isLayerEnabledConf(Commons.GOOGLE_PLACES_LAYER), true, true, Arrays.asList(new LayerReader[]{new GooglePlacesReader()}), null, R.drawable.google_icon, null, R.drawable.google_24, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Google_Places_desc), Commons.GOOGLE_PLACES_LAYER, FileManager.ClearPolicy.ONE_YEAR, R.drawable.google_places_128)); 
        allLayers.put(Commons.TWITTER_LAYER, LayerFactory.getLayer(Commons.TWITTER_LAYER, false, true, isLayerEnabledConf(Commons.TWITTER_LAYER), false, true, Arrays.asList(new LayerReader[]{new TwFriendsReader(), new TwitterReader()}), null, R.drawable.twitter, null, R.drawable.twitter_24, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Twitter_desc), Commons.TWITTER_LAYER, FileManager.ClearPolicy.ONE_WEEK, R.drawable.twitter_128)); 
        allLayers.put(Commons.MC_ATM_LAYER, LayerFactory.getLayer(Commons.MC_ATM_LAYER, false, true, isLayerEnabledConf(Commons.MC_ATM_LAYER), false, true, Arrays.asList(new LayerReader[]{new MastercardAtmReader()}), null, R.drawable.mastercard, null, R.drawable.mastercard_24, LAYER_LOCAL, Locale.getMessage(R.string.Layer_MasterCard_ATMs_desc), Commons.MC_ATM_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.mastercard_128)); 
        allLayers.put(Commons.OSM_ATM_LAYER, LayerFactory.getLayer(Commons.OSM_ATM_LAYER, false, true, isLayerEnabledConf(Commons.OSM_ATM_LAYER), false, true, Arrays.asList(new LayerReader[]{new OsmReader()}), null, R.drawable.credit_card_16, null, R.drawable.credit_card_24, LAYER_LOCAL, Locale.getMessage(R.string.Layer_OSM_ATMs_desc), Commons.OSM_ATM_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.atm_128)); 
        //allLayers.put(Commons.LASTFM_LAYER, LayerFactory.getLayer(Commons.LASTFM_LAYER, false, true, isLayerEnabledConf(Commons.LASTFM_LAYER), false, true, Arrays.asList(new LayerReader[]{new LastFmReader()}), null, R.drawable.last_fm, null, -1, LAYER_LOCAL, Locale.getMessage(R.string.Layer_LastFM_desc), Commons.LASTFM_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.lastfm_128)); 
        allLayers.put(Commons.MEETUP_LAYER, LayerFactory.getLayer(Commons.MEETUP_LAYER, false, true, isLayerEnabledConf(Commons.MEETUP_LAYER), false, true, Arrays.asList(new LayerReader[]{new MeetupReader()}), null, R.drawable.meetup, null, -1, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Meetup_desc), Commons.MEETUP_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.meetup_128)); 
        allLayers.put(Commons.EVENTFUL_LAYER, LayerFactory.getLayer(Commons.EVENTFUL_LAYER, false, true, isLayerEnabledConf(Commons.EVENTFUL_LAYER), false, true, Arrays.asList(new LayerReader[]{new EventfulReader()}), null, R.drawable.eventful, null, -1, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Eventful_desc), Commons.EVENTFUL_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.eventful_128)); 
        allLayers.put(Commons.OSM_PARKING_LAYER, LayerFactory.getLayer(Commons.OSM_PARKING_LAYER, false, true, isLayerEnabledConf(Commons.OSM_PARKING_LAYER), false, true, Arrays.asList(new LayerReader[]{new OsmReader()}), null, R.drawable.parking, null, R.drawable.parking_24, LAYER_LOCAL, Locale.getMessage(R.string.Layer_OSM_Parkings_desc), Commons.OSM_PARKING_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.parking_img)); 
        allLayers.put(Commons.FOURSQUARE_MERCHANT_LAYER, LayerFactory.getLayer(Commons.FOURSQUARE_MERCHANT_LAYER, false, true, isLayerEnabledConf(Commons.FOURSQUARE_MERCHANT_LAYER), true, true, Arrays.asList(new LayerReader[]{new FoursquareMerchantReader()}), null, R.drawable.gift, null, R.drawable.gift_24, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Foursquare_Merchant_desc), "Merchant by Foursquare", FileManager.ClearPolicy.ONE_YEAR, R.drawable.gift_128)); 
        allLayers.put(Commons.GROUPON_LAYER, LayerFactory.getLayer(Commons.GROUPON_LAYER, false, true, isLayerEnabledConf(Commons.GROUPON_LAYER), false, true, Arrays.asList(new LayerReader[]{new GrouponReader()}), null, R.drawable.groupon_icon, null, R.drawable.groupon_24, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Groupon_desc), Commons.GROUPON_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.groupon_128)); 
        allLayers.put(Commons.COUPONS_LAYER, LayerFactory.getLayer(Commons.COUPONS_LAYER, false, true, isLayerEnabledConf(Commons.COUPONS_LAYER), false, true, Arrays.asList(new LayerReader[]{new CouponsReader()}), null, R.drawable.dollar, null, -1, LAYER_LOCAL, Locale.getMessage(R.string.Layer_8Coupons_desc), Commons.COUPONS_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.coupon_128)); 
        //allLayers.put(Commons.EXPEDIA_LAYER, LayerFactory.getLayer(Commons.EXPEDIA_LAYER, false, true, isLayerEnabledConf(Commons.EXPEDIA_LAYER), false, true, Arrays.asList(new LayerReader[]{new ExpediaReader()}), null, R.drawable.expedia, null, R.drawable.expedia_24, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Expedia_desc), Commons.EXPEDIA_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.expedia_128)); 
        allLayers.put(Commons.WIKIPEDIA_LAYER, LayerFactory.getLayer(Commons.WIKIPEDIA_LAYER, false, true, isLayerEnabledConf(Commons.WIKIPEDIA_LAYER), false, true, Arrays.asList(new LayerReader[]{new GeonamesReader()}), null, R.drawable.wikipedia, null, -1, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Wikipedia_desc), Commons.WIKIPEDIA_LAYER, FileManager.ClearPolicy.ONE_YEAR, R.drawable.wikipedia_128)); 
        allLayers.put(Commons.FREEBASE_LAYER, LayerFactory.getLayer(Commons.FREEBASE_LAYER, false, true, isLayerEnabledConf(Commons.FREEBASE_LAYER), false, true, Arrays.asList(new LayerReader[]{new FreebaseReader()}), null, R.drawable.freebase, null, -1, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Freebase_desc), Commons.FREEBASE_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.freebase_128)); 
        allLayers.put(Commons.PANORAMIO_LAYER, LayerFactory.getLayer(Commons.PANORAMIO_LAYER, false, true, isLayerEnabledConf(Commons.PANORAMIO_LAYER), false, true, Arrays.asList(new LayerReader[]{new PanoramioReader()}), null, R.drawable.panoramio, null, -1, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Panoramio_desc), Commons.PANORAMIO_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.panoramio_128)); 
        allLayers.put(Commons.FLICKR_LAYER, LayerFactory.getLayer(Commons.FLICKR_LAYER, false, true, isLayerEnabledConf(Commons.FLICKR_LAYER), false, true, Arrays.asList(new LayerReader[]{new FlickrReader()}), null, R.drawable.flickr, null, -1, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Flickr_desc), Commons.FLICKR_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.flickr_128)); 
        //allLayers.put(Commons.INSTAGRAM_LAYER, LayerFactory.getLayer(Commons.INSTAGRAM_LAYER, false, true, isLayerEnabledConf(Commons.INSTAGRAM_LAYER), false, true, Arrays.asList(new LayerReader[]{new InstagramReader()}), null, R.drawable.instagram_16, null, -1, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Instagram_desc), Commons.INSTAGRAM_LAYER, FileManager.ClearPolicy.ONE_WEEK, R.drawable.instagram_128)); 
        //allLayers.put(Commons.PICASA_LAYER, LayerFactory.getLayer(Commons.PICASA_LAYER, false, true, isLayerEnabledConf(Commons.PICASA_LAYER), false, true, Arrays.asList(new LayerReader[]{new PicasaReader()}), null, R.drawable.picasa_icon, null, -1, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Picasa_desc), Commons.PICASA_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.picasa_128)); 
        allLayers.put(Commons.YOUTUBE_LAYER, LayerFactory.getLayer(Commons.YOUTUBE_LAYER, false, true, isLayerEnabledConf(Commons.YOUTUBE_LAYER), false, true, Arrays.asList(new LayerReader[]{new YouTubeReader()}), null, R.drawable.youtube_icon, null, -1, LAYER_LOCAL, Locale.getMessage(R.string.Layer_YouTube_desc), Commons.YOUTUBE_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.youtube_128)); 
        allLayers.put(Commons.WEBCAM_LAYER, LayerFactory.getLayer(Commons.WEBCAM_LAYER, false, true, isLayerEnabledConf(Commons.WEBCAM_LAYER), false, true, Arrays.asList(new LayerReader[]{new WebcamReader()}), null, R.drawable.webcam, null, -1, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Travel_Webcams_desc), "Webcams", FileManager.ClearPolicy.ONE_DAY, R.drawable.webcam_128)); 
        allLayers.put(Commons.LM_SERVER_LAYER, LayerFactory.getLayer(Commons.LM_SERVER_LAYER, false, true, isLayerEnabledConf(Commons.LM_SERVER_LAYER), true, true, Arrays.asList(new LayerReader[]{new GMSWorldReader()}), null, R.drawable.globe16_new, null, R.drawable.globe24_new, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Public_desc, ConfigurationManager.GMS_WORLD), Commons.LM_SERVER_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.discover_128)); 
        allLayers.put(Commons.ROUTES_LAYER, LayerFactory.getLayer(Commons.ROUTES_LAYER, false, true, isLayerEnabledConf(Commons.ROUTES_LAYER), false, false, null, null, R.drawable.route, null, R.drawable.start_marker, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Routes_desc), Commons.ROUTES_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.routes_128));
        allLayers.put(Commons.MY_POSITION_LAYER, LayerFactory.getLayer(Commons.MY_POSITION_LAYER, false, true, isLayerEnabledConf(Commons.MY_POSITION_LAYER), false, false, null, null, R.drawable.mypos16, null, R.drawable.ic_maps_indicator_current_position, LAYER_LOCAL, null, Commons.MY_POSITION_LAYER, FileManager.ClearPolicy.ONE_MONTH, 0));
    }

    public boolean isLayerEnabled(String layerName) {
        //System.out.println("Calling IsLayerEnabled with param " + name);
        Layer layer = layers.get(layerName);
        if (layer != null) {
            return layer.isEnabled();
        } else {
            return false;
        }
    }

    public boolean isLayerCheckinable(String layerName) {
        //System.out.println("Calling IsLayerEnabled with param " + name);
        Layer layer = layers.get(layerName);
        if (layer != null) {
            return layer.isCheckinable();
        } else {
            return false;
        }
    }

    public void setLayerEnabled(String name, boolean enabled) {
        Layer layer = layers.get(name);
        if (layer != null) {
            layer.setEnabled(enabled);
        }
    }

    public void enableAllLayers() {
        for (String key: getLayers()) {
            setLayerEnabled(key, true);
        }
    }
    
    public boolean isAllLayersEnabled() {
        for (String key : getLayers()) {
            Layer layer = layers.get(key);
            if (layer != null && (!layer.isEnabled() && layer.getType() != LAYER_DYNAMIC)) {
                return false;
            }
        }
        return true;
    }
    
    public void disableAllLayers() {
        for (String key : getLayers()) {
        	setLayerEnabled(key, false);
        }
    }

    public boolean isEmpty() {
        return layers.isEmpty();
    }

    public Layer getLayer(String key) {
        return layers.get(key);
    }
    
    public boolean containsLayer(String key) {
    	return layers.containsKey(key);
    }

    public List<String> getLayers() {
        if (initialized) {
            synchronized (layers) {
                return new ArrayList<String>(layers.keySet());
            }
        } else {
            return new ArrayList<String>();
        }
    }

    public Map<String, String> getExternalLayers() {
        Map<String, String> extLayers = new LinkedHashMap<String, String>();
        List<String> excluded = Arrays.asList(new String[]{"Social", "Geocodes"});

        synchronized (layers) {
            for (Iterator<String> iter = Iterables.filter(layers.keySet(), new LayerExternalNotInListPredicate(excluded)).iterator(); iter.hasNext();) {
                Layer layer = layers.get(iter.next());
                extLayers.put(layer.getName(), layer.getFormatted());
            }
        }

        return Collections.unmodifiableMap(extLayers);
    }

    public List<String> getEnabledLayers() {
        List<String> enabled = new ArrayList<String>();
        synchronized (layers) {
            for (Iterator<Map.Entry<String, Layer>> i = layers.entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, Layer> entry = i.next();
                Layer layer = entry.getValue();

                if (layer.isEnabled() && layer.getType() != LAYER_DYNAMIC) {
                    enabled.add(entry.getKey());
                }
            }
        }

        return enabled;
    }

    protected int getLoadableLayersCount() {
        int counter = 0;
        for (String key : getLayers()) {
            Layer layer = layers.get(key);
            if (layer != null) {
                int size = layer.getLayerReader().size();
                counter += size;
            }
        }
        return counter;
    }

    private boolean isLayerEnabledConf(String layerName) {
        boolean enabled = true;
        String key = layerName + "_status";

        if (ConfigurationManager.getInstance().isOff(key)) {
            enabled = false;
        }

        return enabled;
    }

    protected void initialize(String... layerNames) {
        synchronized (layers) {
            layers.clear();
            dynamicLayers.clear();
            initializeDynamicLayers();

            if (layerNames.length == 0) {
                for (Iterator<String> iter = Iterables.filter(allLayers.keySet(), new LayerEnabledPredicate()).iterator(); iter.hasNext();) {
                    String layer = iter.next();
                    layers.put(layer, allLayers.get(layer));
                }
                for (Iterator<String> iter = Iterables.filter(allLayers.keySet(), new LayerDisabledPredicate()).iterator(); iter.hasNext();) {
                    String layer = iter.next();
                    layers.put(layer, allLayers.get(layer));
                }
            } else {
                List<String> layerNamesList = Arrays.asList(layerNames);
                for (Iterator<String> iter = Iterables.filter(allLayers.keySet(), new LayerEnabledInListPredicate(layerNamesList)).iterator(); iter.hasNext();) {
                    String layer = iter.next();
                    layers.put(layer, allLayers.get(layer));
                }
                for (Iterator<String> iter = Iterables.filter(allLayers.keySet(), new LayerDisabledInListPredicate(layerNamesList)).iterator(); iter.hasNext();) {
                    String layer = iter.next();
                    layers.put(layer, allLayers.get(layer));
                }
            }
            initialized = true;
        }
    }

    protected void addLayer(String name, boolean extensible, boolean manageable, boolean enabled, boolean checkinable, boolean searchable, String smallIconPath, String largeIconPath, String desc, String formatted) {
        synchronized (layers) {
            if (smallIconPath == null) {
                layers.put(name, LayerFactory.getLayer(name, extensible, manageable, enabled, checkinable, searchable, null, null, R.drawable.custom, null, -1, LAYER_LOCAL, desc, formatted, FileManager.ClearPolicy.ONE_MONTH, 0));
            } else {
                layers.put(name, LayerFactory.getLayer(name, extensible, manageable, enabled, checkinable, searchable, null, smallIconPath, -1, largeIconPath, -1, LAYER_FILESYSTEM, desc, formatted, FileManager.ClearPolicy.ONE_MONTH, 0));
            }
        }
    }

    public void removeLayer(String name) {
        synchronized (layers) {
            if (layers.containsKey(name)) {
                layers.remove(name);
            }
        }
    }

    protected final String initializeExternalLayers(List<String> excluded, double latitude, double longitude, int zoom, int width, int height) {
        LoggerUtils.debug("I'm initilizing external server layers!");
    	Map<String, Layer> externalLayers = new LinkedHashMap<String, Layer>();
        LayerJSONParser lp = new LayerJSONParser();
        String response = lp.parse(externalLayers, excluded, latitude, longitude, zoom, width, height);
        synchronized (layers) {
            layers.putAll(externalLayers);
        }
        return response;
    }

    public boolean layerExists(String layerName) {
        return layers.containsKey(layerName);
    }

    public static int getDealCategoryIcon(int categoryId, int type) {
        int icon = -1;
        try {
        	Category c = CategoriesManager.getInstance().getCategory(categoryId);
            if (c != null) {
            	if (type == LAYER_ICON_LARGE) {
            		icon = c.getLargeIcon();
            	} else {
            		icon = c.getIcon();
            	}
           }	
        } catch (Exception e) {
            LoggerUtils.error("LayerManager.getDealCategoryIcon() exception:", e);
        }
        if (icon == -1) {
        	if (type == LAYER_ICON_LARGE) {
        		icon = R.drawable.image_missing32;
        	} else {
        		icon = R.drawable.image_missing16;
        	}
        }
        return icon;
    }

    public static int getLayerIcon(String layerName, int type) {
    	int icon = -1;
    	
    	if (StringUtils.isNotEmpty(layerName)) {
        	Layer layer = layers.get(layerName);
        	if (layer != null) {
        		if (type == LAYER_ICON_LARGE) {
    				icon = layer.getLargeIconResource();
    			} else {
    				icon = layer.getSmallIconResource();
    			}	
        	}
    	}
    	
    	if (icon == -1) {
        	if (type == LAYER_ICON_LARGE) {
        		icon = R.drawable.image_missing32;
        	} else {
        		icon = R.drawable.image_missing16;
        	}
        }
    	
    	return icon;
    }
    
    public static int getLayerImage(String layerName) {
    	int imageId = 0;
    	if (StringUtils.isNotEmpty(layerName)) {
        	Layer layer = layers.get(layerName);
            if (layer != null) {
            	imageId = layer.getImage();
            } 
    	}    
    	return imageId;
    }
    
    public static String getLayerIconUri(String layerName, int type) {
    	String icon = null;
    	if (StringUtils.isNotEmpty(layerName)) {
        	Layer layer = layers.get(layerName);
        	if (type == LAYER_ICON_LARGE) {
    			icon = layer.getLargeIconPath();
    		} else {
    			icon = layer.getSmallIconPath();
    		}	
    	}
    	return icon;
    }
    
    public static BitmapDrawable getLayerIcon(String layerName, int type, DisplayMetrics displayMetrics, Handler handler) {
        BitmapDrawable layerDrawable = null;
        if (StringUtils.isNotEmpty(layerName)) {
        	try {
            	Layer layer = layers.get(layerName);
            	if (layer != null) {
            		if (type == LAYER_ICON_LARGE && (layer.getLargeIconPath() != null || layer.getLargeIconResource() != -1)) {
            	  		//layer has large icon
            	  		layerDrawable = IconCache.getInstance().getLayerImageResource(layer.getName(), "_large", layer.getLargeIconPath(),
                        layer.getLargeIconResource(), null, layer.getType(), displayMetrics, handler);
              		} else {
            	  		//SMALL icon default
            	  		layerDrawable = IconCache.getInstance().getLayerImageResource(layer.getName(), "_small", layer.getSmallIconPath(),
                        layer.getSmallIconResource(), null, layer.getType(), displayMetrics, handler);
              		}	
            	} 
        	} catch (Exception e) {
        		LoggerUtils.error("LayerManager.getLayerIcon() exception", e);
        	}
        }
        if (layerDrawable == null || layerDrawable.getBitmap() == null || layerDrawable.getBitmap().isRecycled()) {
        	if (type == LAYER_ICON_SMALL) {
        		layerDrawable = IconCache.getInstance().getImageDrawable(IconCache.ICON_MISSING16);
        	} else {
        		layerDrawable = IconCache.getInstance().getImageDrawable(IconCache.ICON_MISSING32);
        	}	
        }
        return layerDrawable;
    }

    public String getLayerDesc(String layerName) {
        if (layers.containsKey(layerName)) {
            Layer layer = layers.get(layerName);
            return layer.getDesc();
        }
        return null;
    }

    public String getLayerFormatted(String layerName) {
        if (layers.containsKey(layerName)) {
            Layer layer = layers.get(layerName);
            return layer.getFormatted();
        }
        return null;
    }

    public FileManager.ClearPolicy getClearPolicy(String layerName) {
        if (layers.containsKey(layerName)) {
            Layer layer = layers.get(layerName);
            return layer.getClearPolicy();
        }
        return null;
    }
    
    public Bundle loadLayersGroup() {
        Bundle extras = new Bundle();
        List<String> names = new ArrayList<String>();
        List<Boolean> enabled = new ArrayList<Boolean>();

        synchronized (layers) {
            for (Iterator<Map.Entry<String, Layer>> i = layers.entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, Layer> entry = i.next();
                Layer layer = entry.getValue();

                if (layer.isManageable()) {
                    names.add(layer.getName());
                    enabled.add(layer.isEnabled());
                }
            }
        }

        int size = names.size();
        String[] codes = new String[size];
        String[] namesArray = new String[size];
        boolean[] enabledArray = new boolean[size];

        for (int i = 0; i < enabled.size(); i++) {
            enabledArray[i] = enabled.get(i);
            namesArray[i] = names.get(i);
            codes[i] = Integer.toString(i);

            //System.out.println(namesArray[i] + " " + enabledArray[i]);
        }

        extras.putStringArray("names", namesArray);
        extras.putBooleanArray("enabled", enabledArray);
        extras.putStringArray("codes", codes);

        return extras;
    }

    public void saveLayersAction(String[] names, String[] codes) {
        if (names != null && codes != null) {
            int maxj = codes.length;
            int j = 0;
            for (int i = 0; i < names.length; i++) {    //0 1 2 3 4
                //0   2   4
                if (j < maxj && Integer.parseInt(codes[j]) == i) {
                    //System.out.println(names[i] + " selected");
                    setLayerEnabled(names[i], true);
                    j++;
                } else {
                    if (j >= maxj || Integer.parseInt(codes[j]) > i) {
                        //System.out.println(names[i] + " unselected");
                        setLayerEnabled(names[i], false);
                    }
                }
            }
        }
    }

    //DYNAMIC LAYERS
    //
    
    private void initializeDynamicLayers() {
        String dl = ConfigurationManager.getInstance().getString(ConfigurationManager.DYNAMIC_LAYERS);
        if (StringUtils.isNotEmpty(dl)) {
            String[] dynamicLayersStr = StringUtils.split(dl, ConfigurationManager.LAYER_SEPARATOR);
            Map<String, Layer> dynLayers = new HashMap<String, Layer>();
            for (int i = 0; i < dynamicLayersStr.length; i++) {
                String[] tokens = StringUtils.split(dynamicLayersStr[i], ",");
                String layer = tokens[0];

                int res = getDynamicLayerIcon(tokens);

                int image = getDynamicLayerImage(layer);

                Layer newLayer = LayerFactory.getLayer(layer, false, false, true, false, true, null, null, res, null, -1, LAYER_DYNAMIC, null, StringUtils.capitalize(layer), FileManager.ClearPolicy.ONE_MONTH, image);
                newLayer.setKeywords(tokens);
                dynLayers.put(layer, newLayer);
            }
            if (!dynLayers.isEmpty()) {
                synchronized (layers) {
                    layers.putAll(dynLayers);
                    dynamicLayers.addAll(dynLayers.keySet());
                }
            }

        }
    }

    public List<String> getDynamicLayers() {
        return dynamicLayers;
    }

    public boolean addDynamicLayer(String keywords) {
        String dl = ConfigurationManager.getInstance().getString(ConfigurationManager.DYNAMIC_LAYERS);
        if (dl == null) {
            dl = "";
        }
        String[] tokens = StringUtils.split(keywords, ",");
        String layerName = tokens[0];

        boolean containsLayer = false;
        if (layers.containsKey(layerName)) {
            containsLayer = true;
        } else {
            if (StringUtils.isNotEmpty(dl)) {
                String[] dynamicLayersStr = StringUtils.split(dl, ConfigurationManager.LAYER_SEPARATOR);
                for (int i = 0; i < dynamicLayersStr.length; i++) {
                    String layer = StringUtils.split(dynamicLayersStr[i], ",")[0];
                    if (layer.equalsIgnoreCase(layerName)) {
                        containsLayer = true;
                        break;
                    }
                }
            }
        }

        if (!containsLayer) {
            if (StringUtils.isNotEmpty(dl)) {
                dl += ConfigurationManager.LAYER_SEPARATOR;
            }
            dl += keywords;
            ConfigurationManager.getInstance().putString(ConfigurationManager.DYNAMIC_LAYERS, dl);

            int res = getDynamicLayerIcon(tokens);
            
            int image = getDynamicLayerImage(layerName);

            Layer layer = LayerFactory.getLayer(layerName, false, false, true, false, true, null, null, res, null, -1, LAYER_DYNAMIC, null, StringUtils.capitalize(layerName), FileManager.ClearPolicy.ONE_MONTH, image);
            layer.setKeywords(tokens);

            synchronized (layers) {
                Map<String, Layer> copy = new LinkedHashMap<String, Layer>(layers);
                layers.clear();
                layers.put(layerName, layer);
                layers.putAll(copy);
                dynamicLayers.add(layerName);
            }
        }

        //System.out.println("Dynamic layers " + dl + " ----------------------------------------");
        return containsLayer;
    }

    public void removeDynamicLayer(String layerName) {
        String dl = ConfigurationManager.getInstance().getString(ConfigurationManager.DYNAMIC_LAYERS);
        if (StringUtils.isNotEmpty(dl)) {
            String newdl = "";
            String[] dynamicLayersStr = StringUtils.split(dl, ConfigurationManager.LAYER_SEPARATOR);
            for (int i = 0; i < dynamicLayersStr.length; i++) {
                String[] tokens = StringUtils.split(dynamicLayersStr[i], ",");
                if (!tokens[0].equalsIgnoreCase(layerName)) {
                    if (newdl.length() > 0) {
                        newdl += ConfigurationManager.LAYER_SEPARATOR;
                    }
                    newdl += dynamicLayersStr[i];
                }
            }
            dynamicLayers.remove(layerName);
            ConfigurationManager.getInstance().putString(ConfigurationManager.DYNAMIC_LAYERS, newdl);
        }
    }

    private static int getDynamicLayerIcon(String[] tokens) {

        Context c = ConfigurationManager.getInstance().getContext();
        if (c != null) {
            for (int i = 0; i < tokens.length; i++) {
                int res = c.getResources().getIdentifier(tokens[i].toLowerCase(java.util.Locale.US), "drawable", c.getPackageName());
                if (res > 0) {
                    return res;
                }
            }
        }

        return R.drawable.search;
    }
    
    private static int getDynamicLayerImage(String layer) {
    	Context c = ConfigurationManager.getInstance().getContext();
        if (c != null) {
        	String formattedName = StringUtils.replaceChars(layer.toLowerCase(java.util.Locale.US), ' ', '_');
            return c.getResources().getIdentifier(formattedName + "_img", "drawable", c.getPackageName());
        }
        return 0;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    
    private class LayerEnabledPredicate implements Predicate<String> {

        public boolean apply(String layer) {
            if (allLayers.containsKey(layer)) {
                return allLayers.get(layer).isEnabled();
            } else {
                return false;
            }
        }
    }

    private class LayerDisabledPredicate implements Predicate<String> {

        public boolean apply(String layer) {
            if (allLayers.containsKey(layer)) {
                return !allLayers.get(layer).isEnabled();
            } else {
                return false;
            }
        }
    }

    private class LayerEnabledInListPredicate implements Predicate<String> {

        private List<String> layers;

        public LayerEnabledInListPredicate(List<String> layers) {
            this.layers = layers;
        }

        public boolean apply(String layer) {
            if (allLayers.containsKey(layer) && layers.contains(layer)) {
                return allLayers.get(layer).isEnabled();
            } else {
                return false;
            }
        }
    }

    private class LayerDisabledInListPredicate implements Predicate<String> {

        private List<String> layers;

        public LayerDisabledInListPredicate(List<String> layers) {
            this.layers = layers;
        }

        public boolean apply(String layer) {
            if (allLayers.containsKey(layer) && layers.contains(layer)) {
                return !allLayers.get(layer).isEnabled();
            } else {
                return false;
            }
        }
    }

    private class LayerExternalNotInListPredicate implements Predicate<String> {

        private List<String> exLayers;

        public LayerExternalNotInListPredicate(List<String> layers) {
            this.exLayers = layers;
        }

        public boolean apply(String layerName) {
            Layer layer = layers.get(layerName);
            if (layer.getType() == LayerManager.LAYER_EXTERNAL && !exLayers.contains(layerName)) {
                return true;
            } else {
                return false;
            }
        }
    }
}
