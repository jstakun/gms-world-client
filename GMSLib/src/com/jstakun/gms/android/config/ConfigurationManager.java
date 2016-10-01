package com.jstakun.gms.android.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.acra.ACRA;
import org.apache.commons.lang.StringUtils;
import org.spongycastle.util.encoders.Base64;

import com.jstakun.gms.android.data.ConfigDbDataSource;
import com.jstakun.gms.android.data.FavouritesDbDataSource;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.LandmarkDbDataSource;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkFactory;
import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.social.GMSUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.ServicesUtils;
import com.jstakun.gms.android.utils.StringUtil;
import com.openlapi.QualifiedCoordinates;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap.Config;
import android.location.Location;

/**
 *
 * @author jstakun
 */
public final class ConfigurationManager {

    private static final Map<String, ExtendedLandmark> default_locations = new HashMap<String, ExtendedLandmark>();
    private static volatile Map<String, String> configuration = new ConcurrentHashMap<String, String>();
    private static volatile Map<String, String> changedConfig = new ConcurrentHashMap<String, String>();
    private static volatile Map<String, Object> objectCache = new ConcurrentHashMap<String, Object>();
    private static volatile ConfigurationManager instance = null;
    private static volatile UserManager userManager = null;
    private static volatile DatabaseManager databaseManager = null;
    private static volatile AppUtils appUtils = null;
    
    //configuration parameters
    public static final String PERSISTENCE_MANAGER = "persistenceManager";
    public static final String LOCALE = "locale";
    public static final String LOG_LEVEL = "logLevel";
    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";
    public static final String ZOOM = "zoom";
    public static final String PERSIST_METHOD = "addLandmark";
    public static final String MAP_PROVIDER = "mapProvider";
    public static final String PACKET_DATA_SENT = "packetDataSent";
    public static final String PACKET_DATA_RECEIVED = "packetDataReceived";
    public static final String PACKET_DATA_DATE = "packetDataDate";
    public static final String MAP_RELOAD = "mapReload";
    public static final String BLUETOOTH_ENABLED = "bluetoothEnabled";
    public static final String FOLLOW_MY_POSITION = "followMyPos";
    public static final String RECORDING_ROUTE = "recordingRoute";
    public static final String GOOGLE_MAPS_TYPE = "googleMapsType";
    public static final String OSM_MAPS_TYPE = "osmMapsType";
    public static final String UNIT_OF_LENGHT = "unitOfLength";
    public static final String LAYERS = "layers";
    public static final String ISO3COUNTRY = "iso3country";
    public static final String SEARCH_QUERY_RESULT = "searchQueryResult";
    public static final String SHOW_GRID = "showGrid";
    public static final String TRACK_USER = "sendMyPos";
    public static final String SEND_MY_POS_AT_STARTUP = "sendMyPosAtStartup";
    public static final String LANDMARKS_PER_LAYER = "landmarksPerLayer";
    public static final String SEARCH_RADIUS = "radius";
    public static final String LANDMARKS_CONCURRENT_COUNT = "landmarksConcurrentCount";
    public static final String SET_LAST_KNOWN_LOC_AT_STARTUP = "setLastKnownLocAtStartup";
    public static final String ADS_PROVIDER = "adsProvider";
    public static final String MEMORY_TILES_CACHE_SIZE = "memoryCacheSize";
    public static final String ROUTES_TOKEN = "routesToken";
    public static final String ROUTE_TYPE = "routeType";
    public static final String SEARCH_TYPE = "searchType";
    public static final String LOAD_DISABLED_LAYERS = "loadDisabledLayers";
    public static final String DYNAMIC_LAYERS = "dynamicLayers";
    public static final String DEAL_CATEGORIES = "dealCategories";
    public static final String DEAL_RECOMMEND_CAT_STATS = "dealReccomendCatStats";
    public static final String SHOW_DEAL_OF_THE_DAY = "showDealOfTheDay";
    public static final String RECOMMENDED_DEALS_SHOWN = "recommendedDealsShown";
    public static final String BUILD_VERSION = "buildVersion";
    public static final String BITMAP_TYPE = "bitmapType";
    public static final String APP_CLOSING = "appClosing";
    public static final String LAST_STARTING_DATE = "lastStartingDate";
    public static final String NOTIFICATION_RUNNING_DATE = "notificationRunningDate";
    public static final String LOCATION = "location";
    public static final String LAYER_PAINT_LIMIT = "layerPaintLimit";
    public static final String TOTAL_PAINT_LIMIT = "totalPaintLimit";
    public static final String DEAL_LIMIT = "dealLimit";
    public static final String APP_ID = "appId";
    public static final String USE_COUNT = "useCount";
    public static final String APP_RATED = "appRated";
    public static final String AUTO_CHECKIN_REPEAT_TIME = "autoCheckinRepeatTime";
    public static final String GMS_AUTH_STATUS = "gmsAuthStatus";
    public static final String SCREEN_SIZE = "screenSize";
    public static final String MAP_CENTER = "mapCenter";
    public static final String AUTO_CHECKIN = "autoCheckin";
    public static final String MIN_CHECKIN_DISTANCE = "minCheckinDistance";
    public static final String CHECKIN_TIME_INTERVAL = "checkinTimeInterval";
    public static final String MAX_CURRENT_DISTANCE = "maxCurrentDistance";
    public static final String APP_URL = "appUrl";
    public static final String NETWORK_MODE = "networkMode";
    private static final String DEFAULT_LATITUDE = "defaultLatitude";
    private static final String DEFAULT_LONGITUDE = "defaultLongitude";
    public static final String DEFAULT_SORT = "defaultSort";
    public static final String DEV_MODE = "devMode";
    
    //static values
    private static final String ON = "1";
    private static final String OFF = "0";
    public static final int GOOGLE_MAPS = 0;
    public static final int OSM_MAPS = 1;
    public static final int OSM_TILES = 2;
    private static final String DISABLED = "-1";
    public static final String LAYER_SEPARATOR = "-=+=-";
    
    public static final String SERVER_HOST = "gms-world.appspot.com";
    public static final String SERVER_URL = "http://www.gms-world.net/";
    public static final String SSL_SERVER_URL = "https://" + SERVER_HOST + "/";
    private static final String SSL_RHCLOUD_URL = "https://landmarks-gmsworld.rhcloud.com/";
    public static final String SHOW_LANDMARK_URL = SERVER_URL + "showLandmark/";
    public static final String BITLY_URL = "http://bit.ly/";
    public static final String REGISTER_URL = SERVER_URL + "m/register.jsp";
    public static final String SERVICES_SUFFIX = "s/";
    private static final String SSL_SERVER_SERVICES_URL = SSL_SERVER_URL + SERVICES_SUFFIX;
    private static final String SSL_RHCLOUD_SERVICES_URL = SSL_RHCLOUD_URL + SERVICES_SUFFIX;
    public static final String CRASH_REPORT_URL = SSL_SERVER_SERVICES_URL + "crashReport";
    
    public static final String GMS_WORLD = "GMS World";
    public static final int PERSIST_SERVER = 0;
    public static final int PERSIST_LOCAL = 1;
    public static final int PERSIST_SMS = 2;
    public static final int TILE_SIZE = 256;
    public static final int TEN_SECONDS = 10 * 1000;
    public static final int FIVE_SECONDS = 5 * 1000;
    public static final int PHRASE_SEARCH = 0;
    public static final int WORDS_SEARCH = 1;
    public static final int FUZZY_SEARCH = 2;
    private static final int NETWORK_ALL = 0;
    private static final int NETWORK_TEXT = 1;
    public static final int LM = 0;
    public static final int DA = 1;
    public static final int ROUTE_WALK = 2;
    public static final int ROUTE_BICYCLE = 1;
    
    //private static final int NETWORK_NONE = 2;
    
    //User Manager
    public static final String GMS_USERNAME = "gmsUsername";
    public static final String GMS_NAME = "gmsName";
    public static final String GMS_TOKEN = "gmsToken";
    public static final String GMS_SCOPE = "gmsScope";
    public static final String GMS_LOGIN_DATE = "gmsLoginDate";
    
    public static final String FB_AUTH_STATUS = "fbAuthStatus";   
    public static final String FB_SEND_STATUS = "fbSendStatus";
    public static final String FB_USERNAME = "fbUsername";
    public static final String FB_GENDER = "fbGender";
    public static final String FB_BIRTHDAY = "fbBirthday";
    public static final String FB_NAME = "fbName";
    public static final String FB_EXPIRES_IN = "fbExpiresIn";
    public static final String FB_LOGIN_DATE = "fbLoginDate";
    
    public static final String TWEET_AUTH_STATUS = "tweetAuthStatus";
    public static final String TWEET_SEND_STATUS = "tweetSendStatus";
    public static final String TWEET_USERNAME = "twUsername";
    public static final String TWEET_NAME = "twName";
    public static final String TWEET_LOGIN_DATE = "twLoginDate";
    
    public static final String LN_AUTH_STATUS = "lnAuthStatus";
    public static final String LN_SEND_STATUS = "lnSendStatus";
    public static final String LN_USERNAME = "lnUsername";
    public static final String LN_NAME = "lnName";
    public static final String LN_EXPIRES_IN = "lnExpiresIn";
    public static final String LN_LOGIN_DATE = "lnLoginDate";
    
    public static final String FS_AUTH_STATUS = "fsAuthStatus";
    public static final String FS_USERNAME = "fsUsername";
    public static final String FS_NAME = "fsName";
    public static final String FS_SEND_STATUS = "fsSendStatus";
    public static final String FS_LOGIN_DATE = "fsLoginDate";
    
    public static final String GL_AUTH_STATUS = "glAuthStatus";
    public static final String GL_EXPIRES_IN = "glExpiresIn";
    public static final String GL_SEND_STATUS = "glSendStatus";
    public static final String GL_USERNAME = "glUsername";
    public static final String GL_NAME = "glName";
    public static final String GL_GENDER = "glGender";
    public static final String GL_BIRTHDAY = "glBirthday";
    public static final String GL_LOGIN_DATE = "glLoginDate";
    
    public static final String USER_EMAIL = "userEmail";
    private static final int GUEST_LIMIT = 10;
    //
	//Restart service every x seconds
	public static final long DEFAULT_REPEAT_TIME = 300;
	
	public static final int DYNAMIC_LAYERS_MODE = 1;
	public static final int ALL_LAYERS_MODE = 0;
    
	private Context context = null;
	
    private ConfigurationManager() {
    }

    public static ConfigurationManager getInstance() {
    	if (instance == null) {
    		instance = new ConfigurationManager();
    	}
        return instance;
    }

    private void setDefaultConfiguration() {
    	FileManager.getInstance().readResourceBundleFile(configuration, R.raw.defaultconfig, getContext());
        changedConfig.clear();
    }

    public void putString(String key, String value) {
        configuration.put(key, value);
        changedConfig.put(key, value);
    }

    public void putAll(Map<String, String> config) {
        configuration.putAll(config);
        changedConfig.putAll(config);
    }

    public void putObject(String key, Object value) {
        //System.out.println("Put object " + key);
        objectCache.put(key, value);
    }

    public String getString(String key) {
        if (containsKey(key)) {
            return configuration.get(key);
        }
        return null;
    }

    public String getString(String key, String defaultValue) {
        if (containsKey(key)) {
            return configuration.get(key);
        }
        return defaultValue;
    }

    public String remove(String key) {
        if (containsKey(key)) {
            getDatabaseManager().getConfigDatabase().deleteConfigParam(key);
            return configuration.remove(key);
        } else {
            return null;
        }
    }

    public void removeAll(String[] keys) {
        for (String key : keys) {
            remove(key);
        }
    }

    public Object getObject(String key, Class<? extends Object> c) {
        if (containsObject(key, c)) {
            return objectCache.get(key);
        }
        return null;
    }

    public Map<String, String> getConfiguration() {
        return Collections.unmodifiableMap(configuration);
    }

    public boolean containsKey(String key) {
        return configuration.containsKey(key);
    }

    public boolean containsObject(String key, Class<? extends Object> c) {
        if (objectCache.containsKey(key)) {
            Object o = objectCache.get(key);
            return c.isAssignableFrom(o.getClass());
        } else {
            return false;
        }
    }
    
    public Context getContext() {
        return context;
    }

    public void setContext(Context c) {
        context = c;
    }

    //server urls

    public String getSecuredRHCloudUrl() {
    	return SSL_RHCLOUD_SERVICES_URL; //"http://10.0.2.2:8080/s/"; //
    }
    
    public String getSecuredServicesUrl() {
    	return SSL_SERVER_SERVICES_URL; //"http://10.0.2.2:8080/s/"; //
    }
    
    public String getAnonymousServerUrl() {
    	return SERVER_URL; //"http://10.0.2.2:8080/"; //
    }

    public String getSecuredServerUrl() {
        if (getUserManager().isTokenPresent()) {
            return getSecuredServicesUrl();
        } else {
            return SSL_SERVER_URL; //"http://10.0.2.2:8080/"; // 
        }
    }
    
    public String getServerUrl() {
        if (getUserManager().isTokenPresent()) {
            return getSecuredServicesUrl();
        } else {
            return getAnonymousServerUrl();
        }
    }
   
    //

    public void clearObjectCache() {
        objectCache.clear();
    }

    public Object removeObject(String key, Class<?> c) {
        //System.out.println("Removing object " + key);
        if (containsObject(key, c)) {
            return objectCache.remove(key);
        } else {
            return null;
        }
    }

    public void setLocation(Location loc) {
        putObject(LOCATION, loc);
        putDouble(LATITUDE, loc.getLatitude());
        putDouble(LONGITUDE, loc.getLongitude());
    }

    public Location getLocation() {
        if (containsObject(LOCATION, Location.class)) {
            return (Location) getObject(LOCATION, Location.class);
        }
        return null;
    }

    public boolean isOn(String key) {
        String value = getString(key);
        if (value != null) {
            return value.equals(ON);
        }
        return false;
    }

    public void setOn(String key) {
        putString(key, ON);
    }

    public boolean isOff(String key) {
        String value = getString(key);
        if (value != null) {
            return value.equals(OFF);
        }
        return false;
    }

    public void setOff(String key) {
        putString(key, OFF);
    }

    public boolean isDisabled(String key) {
        String value = getString(key);
        if (value != null) {
            return value.equals(DISABLED);
        }
        return false;
    }

    public void setDisabled(String key) {
        putString(key, DISABLED);
    }

    public int getInt(String key, int defaultValue) {
        return StringUtil.parseInteger(getString(key), defaultValue);
    }

    public int getInt(String key) {
        String value = getString(key);
        if (StringUtils.isNotEmpty(value)) {
            return Integer.parseInt(value);
        } else {
            return -1;
        }
    }

    public long getLong(String key) {
        return StringUtil.parseLong(getString(key), -1);
    }

    public double getDouble(String key) {
        return StringUtil.parseDouble(getString(key), -1);
    }

    public void putDouble(String key, double value) {
        putString(key, Double.toString(value));
    }

    public void putInteger(String key, int value) {
        putString(key, Integer.toString(value));
    }
    
    public void putLong(String key, long value) {
        putString(key, Long.toString(value));
    }

    public Config getBitmapConfig() {
        int config = getInt(BITMAP_TYPE);

        if (config == 0) {
            return Config.RGB_565;
        } else {
            return Config.ARGB_8888;
        }
    }

    public java.util.Locale getCurrentLocale() {
        try {
            return getContext().getResources().getConfiguration().locale;
        } catch (Exception e) { //might cause NPE on some devices
        	return java.util.Locale.getDefault();
        }
    }

    public boolean isClosing() {
        return containsObject(ConfigurationManager.APP_CLOSING, Object.class);
    }

    public List<LandmarkParcelable> getLandmarkList(String key, Class<? extends Object> type) {
    	if (containsObject(key, type)) {
    		return (List<LandmarkParcelable>) objectCache.get(key);
    	} else {
    		return null;
    	}
    }

    public boolean isDefaultCoordinate() {
        return (getString(LATITUDE, "").equals(getString(DEFAULT_LATITUDE, ""))
                && getString(LONGITUDE, "").equals(getString(DEFAULT_LONGITUDE, "")));
    }

    public ExtendedLandmark getDefaultCoordinate() {
        String iso3Country = getString(ConfigurationManager.ISO3COUNTRY, "USA");
        if (default_locations.containsKey(iso3Country)) {
            return default_locations.get(iso3Country);
        } else {
            return default_locations.get("USA");
        }
    }
    
    public boolean isNetworkModeAccepted() {
    	int networkMode = getInt(NETWORK_MODE);
    	boolean isWifiActive = ServicesUtils.isWifiActive(getContext());
    	return networkMode == NETWORK_ALL || (networkMode == NETWORK_TEXT && isWifiActive); 	
    }

    public static AppUtils getAppUtils() {
    	if (appUtils == null) {
    		appUtils = getInstance().new AppUtils();
    	}
    	return appUtils;
    }
    
    public class AppUtils {
    	
    	private AppUtils() {
    	}
    
    	public void initApp(Context applicationContext) {
    		setContext(applicationContext);
    		LoggerUtils.setTag(Locale.getMessage(R.string.app_name));
    		setDefaultConfiguration();
    		
    		if (FileManager.getInstance().fileExists(null, FileManager.CONFIGURATION_FILE)) {
    			FileManager.getInstance().readConfigurationFile();
    			getDatabaseManager().saveConfiguration(true);
    			FileManager.getInstance().deleteFile(null, FileManager.CONFIGURATION_FILE);
    		} else {
    			getDatabaseManager().readConfiguration();
    		}

    		Resources applicationResources = applicationContext.getResources();
    		
    		if (applicationResources != null) {
    			String[] limitArray = applicationResources.getStringArray(com.jstakun.gms.android.ui.lib.R.array.landmarksPerLayer);
    			int index = getInt(ConfigurationManager.LANDMARKS_PER_LAYER, 0);
    			if (index >= 0 && index < limitArray.length) {
    				putInteger(ConfigurationManager.LANDMARKS_PER_LAYER, Integer.parseInt(limitArray[index]));
    			}
    		} else {
    			putInteger(ConfigurationManager.LANDMARKS_PER_LAYER, 30);
    		}

    		if (applicationResources != null) {
    			int dealIndex = getInt(ConfigurationManager.DEAL_LIMIT, 0);
    			String[] dealLimitArray = applicationResources.getStringArray(com.jstakun.gms.android.ui.lib.R.array.dealLimit);
    			if (dealIndex >= 0 && dealIndex < dealLimitArray.length) {
    				putInteger(ConfigurationManager.DEAL_LIMIT, Integer.parseInt(dealLimitArray[dealIndex]));
    			}
    		} else {
    			putInteger(ConfigurationManager.DEAL_LIMIT, 300);
    		}

    		if (applicationResources != null) {
        		String[] array = applicationResources.getStringArray(com.jstakun.gms.android.ui.lib.R.array.radius);
        		int pos = getInt(ConfigurationManager.SEARCH_RADIUS);
    			if (pos == 0) {
    				putInteger(ConfigurationManager.SEARCH_RADIUS, 10);
    			} else if (pos > 0 && pos < array.length) {
    				putInteger(ConfigurationManager.SEARCH_RADIUS, Integer.valueOf(array[pos]).intValue());
    			}
    		} else {
    			putInteger(ConfigurationManager.SEARCH_RADIUS, 10);
    		}

    		long installed = System.currentTimeMillis();
    		try {
    			PackageManager pm = applicationContext.getPackageManager();
    			//Version
    			ApplicationInfo appInfo = pm.getApplicationInfo(applicationContext.getPackageName(), 0);
    			String appFile = appInfo.sourceDir;
    			installed = new File(appFile).lastModified();
    		} catch (Exception ex) {
    			LoggerUtils.error("ConfigurationManager.initApp() exception", ex);   			
    		}

    		default_locations.clear();
    		default_locations.put("USA_CA", LandmarkFactory.getLandmark("United States, Los Angeles", "", new QualifiedCoordinates(34.052234, -118.243685, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //United States Los Angeles 34.052234,-118.243685
    		default_locations.put("USA_NY", LandmarkFactory.getLandmark("United States, New York", "", new QualifiedCoordinates(40.71427, -74.00597, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //United States Los Angeles 34.052234,-118.243685
    		default_locations.put("USA", LandmarkFactory.getLandmark("United States, San Francisco", "", new QualifiedCoordinates(37.77493, -122.41942, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //United States Los Angeles 34.052234,-118.243685
    		default_locations.put("FRA", LandmarkFactory.getLandmark("France, Paris", "", new QualifiedCoordinates(48.856918, 2.34121, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //France, Paris 48.856918, 2.34121 
    		default_locations.put("DEU", LandmarkFactory.getLandmark("Germany, Berlin", "", new QualifiedCoordinates(52.516071, 13.37698, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Germany, Berlin 52.516071, 13.37698 
    		default_locations.put("ITA", LandmarkFactory.getLandmark("Italy, Rome", "", new QualifiedCoordinates(41.901514, 12.460774, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Italy, Rome 41.901514, 12.460774
    		default_locations.put("ESP", LandmarkFactory.getLandmark("Spain, Madrid", "", new QualifiedCoordinates(40.4203, -3.70577, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Spain, Madrid 40.4203,-3.70577, 
    		default_locations.put("JPN", LandmarkFactory.getLandmark("Japan, Tokyo", "", new QualifiedCoordinates(35.689488, 139.691706, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Japan, Tokyo, 35.689488,139.691706 
    		default_locations.put("GBR", LandmarkFactory.getLandmark("United Kingdom, London", "", new QualifiedCoordinates(51.506321, -0.12714, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //United Kingdom, London, 51.506321,-0.12714  
        	default_locations.put("IND", LandmarkFactory.getLandmark("India, Mumbai", "", new QualifiedCoordinates(19.076191, 72.875877, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //India, Mumbai, 19.076191,72.875877 
        	default_locations.put("CHN", LandmarkFactory.getLandmark("China, Beijing", "", new QualifiedCoordinates(39.90403, 116.407526, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //China, Beijing 39.90403, 116.407526
        	default_locations.put("POL", LandmarkFactory.getLandmark("Poland, Warsaw", "", new QualifiedCoordinates(52.235352, 21.00939, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Poland, Warsaw, 52.235352,21.00939
        	default_locations.put("CAN", LandmarkFactory.getLandmark("Canada, Toronto", "", new QualifiedCoordinates(43.64856, -79.38533, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Canada, Toronto, 43.64856,-79.38533
        	default_locations.put("BRA", LandmarkFactory.getLandmark("Brazil, Sao Paolo", "", new QualifiedCoordinates(-23.548943, -46.638818, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Brazil, Sao Paolo -23.548943,-46.638818,     
        	default_locations.put("IDN", LandmarkFactory.getLandmark("Indonesia, Jakarta", "", new QualifiedCoordinates(-6.17144, 106.82782, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //IDN Indonesia, Jakarta -6.17144, 106.82782
        	default_locations.put("THA", LandmarkFactory.getLandmark("Thailand, Bangkok", "", new QualifiedCoordinates(13.75333, 100.504822, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //THA Thailand, Bangkok 13.75333, 100.504822
        	default_locations.put("RUS", LandmarkFactory.getLandmark("Russia, Moscow", "", new QualifiedCoordinates(55.755786, 37.617633, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //RUS Russia, Moscow 55.755786, 37.617633
        	default_locations.put("MEX", LandmarkFactory.getLandmark("Mexico, Mexico City", "", new QualifiedCoordinates(19.432608, -99.133208, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //MEX Mexico, Mexico City 19.432608, -99.133208
        	default_locations.put("MYS", LandmarkFactory.getLandmark("Malaysia, Kuala Lumpur", "", new QualifiedCoordinates(3.15248, 101.71727, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //MYS Malaysia, Kuala Lumpur 3.15248, 101.71727
        	default_locations.put("TUR", LandmarkFactory.getLandmark("Turkey, Istanbul", "", new QualifiedCoordinates(41.00527, 28.97696, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //TUR Turkey, Istanbul 41.00527, 28.97696
        	default_locations.put("PHL", LandmarkFactory.getLandmark("Philippines, Manilia", "", new QualifiedCoordinates(14.5995124, 120.9842195, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //PHL Philippines, Manilia 14.5995124, 120.9842195
        	default_locations.put("NLD", LandmarkFactory.getLandmark("Netherlands, Amsterdam", "", new QualifiedCoordinates(52.373119, 4.89319, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //NLD Netherlands, Amsterdam 52.373119, 4.89319
        	default_locations.put("SAU", LandmarkFactory.getLandmark("Saudi Arabia, Riyadh", "", new QualifiedCoordinates(24.64732, 46.714581, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //SAU Saudi Arabia, Riyadh 24.64732, 46.714581
        	default_locations.put("PRT", LandmarkFactory.getLandmark("Portugal, Lisbon", "", new QualifiedCoordinates(38.7252993, -9.1500364, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //PRT Portugal, Lisbon 38.7252993, 9.1500364
        	default_locations.put("PAK", LandmarkFactory.getLandmark("Pakistan, Islamabad", "", new QualifiedCoordinates(33.718151, 73.060547, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //PAK Pakistan, Islamabad 33.718151, 73.060547
        	default_locations.put("SWE", LandmarkFactory.getLandmark("Sweden, Stockholm", "", new QualifiedCoordinates(59.32893, 18.06491, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //SWE Sweden, Stockholm 59.32893, 18.06491  	       	      	
    	}

    	public String collectSystemInformation() {

    		String ReturnVal = "";
    		String VersionName = null;
    		String PackageName = null;
    		int VersionCode = 0;
    		
    			PackageInfo pi = getPackageInfo();
            
    			if (pi != null) {
    				VersionName = pi.versionName;
    				PackageName = pi.packageName;
    				VersionCode = pi.versionCode;
    			}
    			
    			String PhoneModel = android.os.Build.MODEL;
    			// Android version
    			String AndroidVersion = android.os.Build.VERSION.RELEASE;
    			String Board = android.os.Build.BOARD;
    			String Brand = android.os.Build.BRAND;
    			String Device = android.os.Build.DEVICE;
            	String Display = android.os.Build.DISPLAY;
            	//String FingerPrint = android.os.Build.FINGERPRINT;
            	String Host = android.os.Build.HOST;
            	String ID = android.os.Build.ID;
            	String Model = android.os.Build.MODEL;
            	String Product = android.os.Build.PRODUCT;
            	String Tags = android.os.Build.TAGS;

            	long Time = android.os.Build.TIME;
            	String Type = android.os.Build.TYPE;
            	//String User = android.os.Build.USER;

            	ReturnVal += "Package: " + PackageName + ", ";
            	ReturnVal += "Version: " + VersionName + ", ";
            	ReturnVal += "Version Code: " + VersionCode + ", ";
            	ReturnVal += "Phone Model: " + PhoneModel + ", ";
            	ReturnVal += "Android Version: " + AndroidVersion + ", ";
            	ReturnVal += "Board: " + Board + ", ";
            	ReturnVal += "Brand: " + Brand + ", ";
            	ReturnVal += "Device: " + Device + ", ";
            	ReturnVal += "Display: " + Display + ", ";
            	//ReturnVal += "Finger Print: " + FingerPrint + ", ";
            	ReturnVal += "Host: " + Host + ", ";
            	ReturnVal += "ID: " + ID + ", ";
            	ReturnVal += "Model: " + Model + ", ";
            	ReturnVal += "Product: " + Product + ", ";
            	ReturnVal += "Tags: " + Tags + ", ";
            	ReturnVal += "Time: " + Time + ", ";
            	ReturnVal += "Type: " + Type;
            	//ReturnVal += "User : " + User;

        	return ReturnVal;
    	}
    	
    	public String getAboutMessage() {
    		int versionCode = 0;
    		String versionName = "Latest";
    		PackageInfo info = getPackageInfo(); 
    		if (info != null) {
    			versionCode = info.versionCode;
    			versionName = info.versionName;
    		} 		
            String app_name = Locale.getMessage(R.string.app_name);
            String message = Locale.getMessage(R.string.Info_about, app_name, versionName, versionCode, getBuildDate(), ConfigurationManager.SERVER_URL);
    	    return message;
    	}
    	
    	public int getVersionCode() {
    		int versionCode = 0;
    		PackageInfo info = getPackageInfo(); 
    		if (info != null) {
    			versionCode = info.versionCode;
    		} 		
    		return versionCode;
    	}
    	
    	private String getBuildDate() {
    		ZipFile zf = null;
    		String s = "recently";
    		try{
    		     ApplicationInfo ai = getApplicationInfo();
    		     zf = new ZipFile(ai.sourceDir);
    		     ZipEntry ze = zf.getEntry("classes.dex");
    		     long time = ze.getTime();
    		     s = DateTimeUtils.getShortDateTimeString(time, getCurrentLocale());

    		 } catch(Exception e) {
    		 } finally {
    			  try {
    				  if (zf != null) {
    					  zf.close();
    				  }
    			  } catch (Exception e) {
    			  }
    		 }
    		 return s;
    	}
    
    	public PackageInfo getPackageInfo() {  
    		PackageInfo pi = null;
    		try {
    			Context c = getContext();
    			if (c != null) {
    				pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
    			}
    		} catch (Exception e) {
    			LoggerUtils.error(e.getMessage(), e);
    		}
    		return pi;
    	}
    	
    	public ApplicationInfo getApplicationInfo() throws NameNotFoundException {
    		PackageManager pm = getContext().getPackageManager();
    		ApplicationInfo ai = pm.getApplicationInfo(getContext().getPackageName(), 0);
    		return ai;
    	}
    	
    	public void reset() {
    		getDatabaseManager().deleteConfiguration();
    		remove(ConfigurationManager.GMS_TOKEN);
    		setDefaultConfiguration();
    		getDatabaseManager().readConfiguration();
    		clearCounter();
    	}
    	
    	public void increaseCounter(int sent, int received) {
            long allDataReceivedCounter = getLong(PACKET_DATA_RECEIVED);
            long allDataSentCounter = getLong(PACKET_DATA_SENT);
            allDataReceivedCounter += received;
            allDataSentCounter += sent;
            putString(ConfigurationManager.PACKET_DATA_RECEIVED, Long.toString(allDataReceivedCounter));
            putString(ConfigurationManager.PACKET_DATA_SENT, Long.toString(allDataSentCounter));
        }

        public void clearCounter() {
            putString(ConfigurationManager.PACKET_DATA_RECEIVED, "0");
            putString(ConfigurationManager.PACKET_DATA_SENT, "0");
            putString(ConfigurationManager.PACKET_DATA_DATE, Long.toString(System.currentTimeMillis()));
        }

        public String[] formatCounter() {
            double mb = 1024.0 * 1024.0 * 8.0;

            long allDataReceivedCounter = getLong(PACKET_DATA_RECEIVED);
            long allDataSentCounter = getLong(PACKET_DATA_SENT);
            long packetDateDate = getLong(PACKET_DATA_DATE);
            String sd = null;
            if (packetDateDate == -1) {
            	sd = "unknown";
            } else {
            	sd = DateTimeUtils.getDefaultDateTimeString(packetDateDate, getCurrentLocale());
            }
            
            String ds = StringUtil.formatDouble((allDataSentCounter / mb), 2);
            String dr = StringUtil.formatDouble((allDataReceivedCounter / mb), 2);
            
            return new String[]{ds, dr, sd};
        }
    }
    
    public static DatabaseManager getDatabaseManager() {
    	if (databaseManager == null) {
    		databaseManager = getInstance().new DatabaseManager();
    	}
    	return databaseManager;
    }
    
    public class DatabaseManager {
    
    	private static final String FAVOURITESDB = "FAVOURITESDB";
    	private static final String CONFIGDB = "CONFIGDB";
    	private static final String LANDMARKDB = "LANDMARKDB";
    	
    	private DatabaseManager() {}
    	
    	public boolean saveConfiguration(boolean force) {
    		boolean status = false;
        	if (force || !changedConfig.isEmpty()) {
            	ConfigDbDataSource cdb = getConfigDatabase();
            	if (force) {
                	status = cdb.putAll(configuration);
            	} else {
                	status = cdb.putAll(changedConfig);
            	}
            	changedConfig.clear();
        	}
        	return status;
    	}

    	private void readConfiguration() {
        	ConfigDbDataSource cdb = getConfigDatabase();
        	Map<String, String> userConfig = cdb.fetchAllConfig();
        	String dynamicLayers = userConfig.remove(DYNAMIC_LAYERS);
        	//System.out.println("user layers: " + dynamicLayers + " ---------------------------- ");
        	if (StringUtils.isNotEmpty(dynamicLayers)) {
        		String[] first = StringUtils.split(getString(DYNAMIC_LAYERS), LAYER_SEPARATOR);
        		String[] second = StringUtils.split(dynamicLayers, LAYER_SEPARATOR);
        		List<String> dedup = StringUtil.removeDuplicates(first, second);
        		putString(DYNAMIC_LAYERS, StringUtils.join(dedup, LAYER_SEPARATOR));
        	}
        	putAll(userConfig);
        	changedConfig.clear();
    	}
    	
    	private void deleteConfiguration() {
    		ConfigDbDataSource cdb = getConfigDatabase();
    		cdb.delete();
    	}

    	private synchronized ConfigDbDataSource getConfigDatabase() {
        	ConfigDbDataSource cdb = (ConfigDbDataSource) getObject(CONFIGDB, ConfigDbDataSource.class);
        	if (cdb == null) {
            	cdb = new ConfigDbDataSource(getContext());
            	putObject(CONFIGDB, cdb);
        	}
        	return cdb;
    	}
    	
    	public synchronized FavouritesDbDataSource getFavouritesDatabase() {
    		FavouritesDbDataSource fdb = (FavouritesDbDataSource) ConfigurationManager.getInstance().getObject(FAVOURITESDB, FavouritesDbDataSource.class);
            if (fdb == null) {
                fdb = new FavouritesDbDataSource(getContext());
                ConfigurationManager.getInstance().putObject(FAVOURITESDB, fdb);
            }
            return fdb;
    	}

    	public synchronized List<ExtendedLandmark> getLandmarkDatabase() {
        	LandmarkDbDataSource db = new LandmarkDbDataSource(getContext());
        	List<ExtendedLandmark> lmdb;
        	
        	//file to landmarkdb migration code
        	if (FileManager.getInstance().fileExists(null, FileManager.LANDMARKDB_FILE)) {
            	lmdb = new ArrayList<ExtendedLandmark>();
            	FileManager.getInstance().readLandmarkStore(lmdb);
            	FileManager.getInstance().deleteFile(null, FileManager.LANDMARKDB_FILE);
            	db.addAll(lmdb);
        	} else {
            	lmdb = db.fetchAllLandmarks();
        	}
        	lmdb.addAll(default_locations.values());
        	putObject(LANDMARKDB, db);

        	return lmdb;
    	}

    	public synchronized void closeAllDatabases() {
        	ConfigDbDataSource cdb = (ConfigDbDataSource) getObject(CONFIGDB, ConfigDbDataSource.class);
        	if (cdb != null) {
            	cdb.close();
            	objectCache.remove(CONFIGDB);
        	}

        	LandmarkDbDataSource db = (LandmarkDbDataSource) getObject(LANDMARKDB, LandmarkDbDataSource.class);
        	if (db != null) {
            	db.close();
            	objectCache.remove(LANDMARKDB);
        	}

        	FavouritesDbDataSource fdb = (FavouritesDbDataSource) getObject(FAVOURITESDB, FavouritesDbDataSource.class);
        	if (fdb != null) {
            	fdb.close();
            	objectCache.remove(FAVOURITESDB);
        	}
    	}
    }

    public static UserManager getUserManager() {
    	if (userManager == null) {
    		userManager = getInstance().new UserManager();
    	}
    	return userManager;
    }
    
    public class UserManager {
    	
    	private UserManager() {}
    
    	public boolean isUserLoggedIn() {
    		return (isOn(TWEET_AUTH_STATUS)
	            || isOn(FB_AUTH_STATUS)
	            || isOn(LN_AUTH_STATUS)
	            || isOn(GL_AUTH_STATUS)
	            || isOn(FS_AUTH_STATUS)
	            || isOn(GMS_AUTH_STATUS));
    	}
    	
    	public boolean isFriendSocialLoggedIn() {
    		return (isOn(TWEET_AUTH_STATUS)
    	            || isOn(FB_AUTH_STATUS)
    	            || isOn(FS_AUTH_STATUS));
    	}
    	
    	public boolean isTokenPresent() {
    		return (StringUtils.isNotEmpty(getString(GMS_TOKEN)) && StringUtils.isNotEmpty(getString(GMS_SCOPE)));
    	}
    	
    	public boolean isUserLoggedInFully() {
    		return (isOn(TWEET_AUTH_STATUS)
	            && isOn(FB_AUTH_STATUS)
	            && isOn(LN_AUTH_STATUS)
	            && isOn(GL_AUTH_STATUS)
	            && isOn(FS_AUTH_STATUS)
	            && isOn(GMS_AUTH_STATUS));
    	}
    	
    	public boolean isUserLoggedInGMSWorld() {
    		return isOn(GMS_AUTH_STATUS);
    	}
    
    	public String getLoggedInUsername() {
    		if (isOn(GMS_AUTH_STATUS)) {
    			return getString(GMS_USERNAME);
    		} else if (isOn(FB_AUTH_STATUS)) {
            	return getString(FB_USERNAME);
        	} else if (isOn(TWEET_AUTH_STATUS)) {
            	return getString(TWEET_USERNAME);
        	} else if (isOn(LN_AUTH_STATUS)) {
            	return getString(LN_USERNAME);
        	} else if (isOn(GL_AUTH_STATUS)) {
            	return getString(GL_USERNAME);
        	} else if (isOn(FS_AUTH_STATUS)) {
            	return getString(FS_USERNAME);
        	} 

        	return null;
    	}

    	public List<String> getLoginItems(boolean withSuffix) {
        	List<String> items = new ArrayList<String>();
        	if (isOff(FB_AUTH_STATUS)) {
        		if (withSuffix) {
        			items.add(OAuthServiceFactory.getServiceName(Commons.FACEBOOK) + ";" + Commons.FACEBOOK);
        		} else {
        			items.add(OAuthServiceFactory.getServiceName(Commons.FACEBOOK));
        		}
        	}
        	if (isOff(FS_AUTH_STATUS)) {
        		if (withSuffix) {
        			items.add(OAuthServiceFactory.getServiceName(Commons.FOURSQUARE) + ";" + Commons.FOURSQUARE);
        		} else {
        			items.add(OAuthServiceFactory.getServiceName(Commons.FOURSQUARE));
        		}
        	}
        	if (isOff(GL_AUTH_STATUS)) {
        		if (withSuffix) {
        			items.add(OAuthServiceFactory.getServiceName(Commons.GOOGLE) + ";" + Commons.GOOGLE);
        		} else {
        			items.add(OAuthServiceFactory.getServiceName(Commons.GOOGLE));
        		}
        	}
        	if (isOff(GMS_AUTH_STATUS)) {
        		if (withSuffix) {
                	items.add(GMS_WORLD + ";");
            	} else {
                	items.add(GMS_WORLD);
            	}
        	}
        	if (isOff(TWEET_AUTH_STATUS)) {
            	if (withSuffix) {
                	items.add(OAuthServiceFactory.getServiceName(Commons.TWITTER) + ";" + Commons.TWITTER);
            	} else {
                	items.add(OAuthServiceFactory.getServiceName(Commons.TWITTER));
            	}
        	}
        	if (isOff(LN_AUTH_STATUS)) {
            	if (withSuffix) {
                	items.add(OAuthServiceFactory.getServiceName(Commons.LINKEDIN) + ";" + Commons.LINKEDIN);
            	} else {
                	items.add(OAuthServiceFactory.getServiceName(Commons.LINKEDIN));
            	}
        	}
        	return items;
    	}

    	public boolean putStringAndEncrypt(String key, String value) {
    		if (StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(value)) {
    			try {
    				String tmp = new String(Base64.encode(BCTools.encrypt(value.getBytes(), getContext())));
    				putString(key, tmp);
    				//System.out.println("Encrypted: " +  key + " = " + tmp);
    				tmp = null;
    				return true;
    			} catch (Exception ex) {
    				LoggerUtils.error("UserManager.putStringAndEncrypt() exception:", ex);
    				return false;
    			}
    		} else {
    			return false;
    		}
    	}
    
    	public String getStringDecrypted(String key) {
    		String decrypted = null;
    		if (StringUtils.isNotEmpty(key)) {
    			String encValue = getString(key);
				if (encValue != null) {
					try {
						decrypted = new String(BCTools.decrypt(Base64.decode(encValue.getBytes()), getContext()));
						//System.out.println("Decrypted: " +  key + " = " + decrypted);
					} catch (Exception e) {
						LoggerUtils.error("UserManager.getDecryptedString exception: ", e);
					}		
				}
    		}
			return decrypted;
    	}
    	
    	public String getUserEmail() {
    		return getStringDecrypted(USER_EMAIL);
    	}
    	
    	public boolean isUserAllowedAction() {
    		return (isUserLoggedIn() || getInt(ConfigurationManager.USE_COUNT, 0) < GUEST_LIMIT);
    	}
    	
    	public boolean isItemRestricted(int itemId) {
    		final int[] restrictedItems = new int[]{R.id.search, R.id.addLandmark, R.id.autocheckin, R.id.qrcheckin,
    				R.id.refreshLayers, R.id.addLayer, R.id.blogeo, R.id.friendsCheckins, R.id.trackPos, R.id.saveRoute,
    				R.id.loadRoute, R.id.pauseRoute, R.id.loadPoiFile, R.id.config, R.id.pickMyPos, R.id.deals, 
    				R.id.newestLandmarks, R.id.events, R.id.reset};
    		for (int i=0; i<restrictedItems.length; i++) {
    			if (restrictedItems[i] == itemId) {
    				return true;
    			}
    		}
    		return false;
    	}
    	
    	public void verifyToken() {
    		if (!StringUtils.isNotEmpty(getString(GMS_TOKEN))) {
    			String errorMessage = GMSUtils.generateToken(getString(GMS_SCOPE));
    			if (errorMessage != null) {
    				LoggerUtils.error("UserManager.verifyToken exception: " + errorMessage);
    			} 
    		} 
    		if (StringUtils.isNotEmpty(getString(GMS_TOKEN))) { 
        		Map<String, String> headers = new HashMap<String, String>();
        		headers.put(Commons.TOKEN_HEADER, getString(ConfigurationManager.GMS_TOKEN));
        		headers.put(Commons.SCOPE_HEADER, getString(ConfigurationManager.GMS_SCOPE));
        		ACRA.getConfig().setHttpHeaders(headers);
    		}
    		LoggerUtils.debug("GMS Token is present!");
    	}
    	
    	public String getSocialIds() {
    		List<String> ids = new ArrayList<String>();
    		if (isOn(GMS_AUTH_STATUS)) {
    			ids.add(getString(GMS_USERNAME));
    		} 
    		if (isOn(FB_AUTH_STATUS)) {
    			ids.add(getString(FB_USERNAME));
        	} 
    		if (isOn(TWEET_AUTH_STATUS)) {
    			ids.add(getString(TWEET_USERNAME));
        	} 
    		if (isOn(LN_AUTH_STATUS)) {
    			ids.add(getString(LN_USERNAME));
        	} 
    		if (isOn(GL_AUTH_STATUS)) {
        		ids.add(getString(GL_USERNAME));
        	} 
    		if (isOn(FS_AUTH_STATUS)) {
    			ids.add(getString(FS_USERNAME));
        	} 

        	return StringUtils.join(ids, ',');
    	}
    }
}
