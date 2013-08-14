/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.config;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap.Config;
import android.location.Location;
import com.jstakun.gms.android.data.ConfigDbDataSource;
import com.jstakun.gms.android.data.FavouritesDbDataSource;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.LandmarkDbDataSource;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkFactory;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.AsyncTaskManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MessageStack;
import com.jstakun.gms.android.utils.StringUtil;
import com.openlapi.QualifiedCoordinates;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class ConfigurationManager {

    private static final Map<String, ExtendedLandmark> default_locations = new HashMap<String, ExtendedLandmark>();
    private static final Map<String, String> configuration = new HashMap<String, String>();
    private static final Map<String, String> changedConfig = new ConcurrentHashMap<String, String>();
    private static final Map<String, Object> objectCache = new HashMap<String, Object>();
    private static ConfigurationManager instance = null;
    
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
    public static final String SHOW_ADS = "showAds";
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
    public static final String BUILD_INFO = "buildInfo";
    public static final String BITMAP_TYPE = "bitmapType";
    public static final String APP_CLOSING = "appClosing";
    public static final String LAST_STARTING_DATE = "lastStartingDate";
    public static final String LOCATION = "location";
    public static final String CONTEXT = "context";
    public static final String PACKAGE_NAME = "packageName";
    public static final String LAYER_PAINT_LIMIT = "layerPaintLimit";
    public static final String TOTAL_PAINT_LIMIT = "totalPaintLimit";
    public static final String DEAL_LIMIT = "dealLimit";
    public static final String APP_ID = "appId";
    public static final String USE_COUNT = "useCount";
    public static final String APP_RATED = "appRated";
    public static final String AUTO_CHECKIN_REPEAT_TIME = "autoCheckinRepeatTime";
    public static final String FB_AUTH_STATUS = "fbAuthStatus";
    public static final String FB_TOKEN = "fbToken";
    public static final String FB_AUTH_KEY = "fbauth_key";
    public static final String FB_AUTH_SECRET_KEY = "fbauth_secret_key";
    public static final String FB_SEND_STATUS = "fbSendStatus";
    public static final String FB_USERNAME = "fbUsername";
    public static final String FB_GENDER = "fbGender";
    public static final String FB_BIRTHDAY = "fbBirthday";
    public static final String FB_NAME = "fbName";
    public static final String FB_EXPIRES_IN = "fbExpiresIn";
    public static final String TWEET_AUTH_STATUS = "tweetAuthStatus";
    public static final String TWEET_AUTH_KEY = "auth_key";
    public static final String TWEET_AUTH_SECRET_KEY = "auth_secret_key";
    public static final String TWEET_SEND_STATUS = "tweetSendStatus";
    public static final String TWEET_USERNAME = "twUsername";
    public static final String TWEET_NAME = "twName";
    public static final String LN_AUTH_STATUS = "lnAuthStatus";
    public static final String LN_AUTH_KEY = "lnauth_key";
    public static final String LN_AUTH_SECRET_KEY = "lnauth_secret_key";
    public static final String LN_SEND_STATUS = "lnSendStatus";
    public static final String LN_USERNAME = "lnUsername";
    public static final String LN_NAME = "lnName";
    public static final String LN_EXPIRES_IN = "lnExpiresIn";
    public static final String FS_AUTH_STATUS = "fsAuthStatus";
    public static final String FS_AUTH_KEY = "fsauth_key";
    public static final String FS_AUTH_SECRET_KEY = "fsauth_secret_key";
    public static final String FS_USERNAME = "fsUsername";
    public static final String FS_NAME = "fsName";
    public static final String FS_SEND_STATUS = "fsSendStatus";
    public static final String GL_AUTH_STATUS = "glAuthStatus";
    public static final String GL_AUTH_KEY = "glauth_key";
    public static final String GL_REFRESH_TOKEN = "glRefreshToken";
    public static final String GL_EXPIRES_IN = "glExpiresIn";
    public static final String GL_AUTH_SECRET_KEY = "glauth_secret_key";
    public static final String GL_SEND_STATUS = "glSendStatus";
    public static final String GL_USERNAME = "glUsername";
    public static final String GL_NAME = "glName";
    public static final String GL_GENDER = "glGender";
    public static final String GL_BIRTHDAY = "glBirthday";
    //public static final String GW_AUTH_STATUS = "gwAuthStatus";
    //public static final String GW_AUTH_KEY = "gwauth_key";
    //public static final String GW_AUTH_SECRET_KEY = "gwauth_secret_key";
    //public static final String GW_USERNAME = "gwUsername";
    //public static final String GW_SEND_STATUS = "gwSendStatus";
    public static final String GMS_AUTH_STATUS = "gmsAuthStatus";
    public static final String SCREEN_SIZE = "screenSize";
    public static final String USER_EMAIL = "userEmail";
    public static final String MAP_CENTER = "mapCenter";
    public static final String AUTO_CHECKIN = "autoCheckin";
    public static final String MIN_CHECKIN_DISTANCE = "minCheckinDistance";
    public static final String CHECKIN_TIME_INTERVAL = "checkinTimeInterval";
    public static final String MAX_CURRENT_DISTANCE = "maxCurrentDistance";
    public static final String GA_ID = "gaId";
    public static final String APP_URL = "appUrl";
    private static final String DEFAULT_LATITUDE = "defaultLatitude";
    private static final String DEFAULT_LONGITUDE = "defaultLongitude";
    private static final String ON = "1";
    private static final String OFF = "0";
    public static final int GOOGLE_MAPS = 0;
    public static final int OSM_MAPS = 1;
    public static final int OSM_TILES = 2;
    private static final String DISABLED = "-1";
    public static final String SERVER_HOST = "gms-world.appspot.com";
    public static final String SERVER_URL = "http://www.gms-world.net/";
    public static final String SSL_SERVER_URL = "https://" + SERVER_HOST + "/";
    public static final String SHOW_LANDMARK_URL = SERVER_URL + "showLandmark/";
    public static final String SHOW_LOCATION_URL = SERVER_URL + "showLocation.do";
    public static final String CRASH_REPORT_URL = SERVER_URL + "crashReport";
    public static final String BITLY_URL = "http://bit.ly/";
    public static final String REGISTER_URL = SERVER_URL + "m/register.jsp";
    public static final String SERVER_SERVICES_URL = SERVER_URL + "services/";
    private static final String SSL_SERVER_SERVICES_URL = SSL_SERVER_URL + "services/";
    public static final String GMS_WORLD = "GMS World";
    public static final String LM_MARKET_URL = "http://play.google.com/store/apps/details?id=com.jstakun.gms.android.ui";
    
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String APP_USER = "appUser";
    private static final String APP_USER_PWD = "appUserPwd";
    private static final String MY_POS_USER = "myPosUser";
    
    public static final int PERSIST_SERVER = 0;
    public static final int PERSIST_LOCAL = 1;
    public static final int PERSIST_SMS = 2;
    public static final int TILE_SIZE = 256;
    public static final int TEN_SECONDS = 10 * 1000;
    public static final int FIVE_SECONDS = 5 * 1000;
    public static final int PHRASE_SEARCH = 0;
    public static final int WORDS_SEARCH = 1;
    public static final int FUZZY_SEARCH = 2;
    
    private ConfigurationManager() {
    }

    public static ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }

    private void setDefaultConfiguration() {
        FileManager fm = PersistenceManagerFactory.getFileManager();
        fm.readResourceBundleFile(configuration, R.raw.defaultconfig, getContext());
        putString(USERNAME, Commons.DEFAULT_USERNAME);
        putString(PASSWORD, Commons.DEFAULT_PASSWORD);
        putString(APP_USER_PWD, Commons.APP_USER_PWD);
        putString(MY_POS_USER, Commons.MY_POS_USER);
        fm.createDefaultDirs(); 
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
            getConfigDatabase().deleteConfigParam(key);
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
        if (containsObject(CONTEXT, Context.class)) {
            return (Context) objectCache.get(CONTEXT);
        }
        return null;
    }

    public void setContext(Context c) {
        putObject(CONTEXT, c);
    }

    public LandmarkManager getLandmarkManager() {
        if (containsObject("landmarkManager", LandmarkManager.class)) {
            return (LandmarkManager) objectCache.get("landmarkManager");
        }
        return null;
    }

    public RoutesManager getRoutesManager() {
        if (containsObject("routesManager", RoutesManager.class)) {
            return (RoutesManager) objectCache.get("routesManager");
        }
        return null;
    }

    public RouteRecorder getRouteRecorder() {
        if (containsObject("routeRecorder", RouteRecorder.class)) {
            return (RouteRecorder) objectCache.get("routeRecorder");
        }
        return null;
    }

    public AsyncTaskManager getTaskManager() {
        if (containsObject("asyncTaskManager", AsyncTaskManager.class)) {
            return (AsyncTaskManager) objectCache.get("asyncTaskManager");
        }
        return null;
    }

    public MessageStack getMessageStack() {
        if (containsObject("messageStack", MessageStack.class)) {
            return (MessageStack) objectCache.get("messageStack");
        }
        return null;
    }

    public void saveConfiguration(boolean force) {
        if (force || !changedConfig.isEmpty()) {
            //PersistenceManagerFactory.getPersistenceManagerInstance().saveConfigurationFile();
            ConfigDbDataSource cdb = getConfigDatabase();
            if (force) {
                cdb.putAll(configuration);
            } else {
                cdb.putAll(changedConfig);
            }
            changedConfig.clear();
        }
    }

    private void readConfiguration() {
        ConfigDbDataSource cdb = getConfigDatabase();
        putAll(cdb.fetchAllConfig());
        //PersistenceManagerFactory.getPersistenceManagerInstance().readConfigurationFile();
        changedConfig.clear();
    }

    public String getServicesUrl() {
        if (isUserLoggedIn()) {
            return SERVER_SERVICES_URL;
        } else {
            return SERVER_URL;
        }
    }

    public String getSecuredServicesUrl() {
        if (isUserLoggedIn()) {
            return SSL_SERVER_SERVICES_URL;
        } else {
            return SSL_SERVER_URL;
        }
    }

    public void clearObjectCache() {
        /*for (Iterator<Map.Entry<String, Object>> i = objectCache.entrySet().iterator(); i.hasNext();) {
         Map.Entry<String, Object> entry = i.next();
         String key = entry.getKey();
         LoggerUtils.debug("Removing object " + key + " ...");
         i.remove();
         }*/
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
        Context context = getContext();
        if (context != null) {
            return context.getResources().getConfiguration().locale;
        } else {
        	return Locale.getDefault();
        }
    }

    public boolean isClosing() {
        return containsObject(ConfigurationManager.APP_CLOSING, Object.class);
    }

    private List<? extends Object> getObjectList(String key, Class<? extends Object> type) {
        if (containsObject(key, type)) {
            return (List<? extends Object>) objectCache.get(key);
        }
        return null;
    }

    public List<LandmarkParcelable> getLandmarkList(String key, Class<? extends Object> type) {
        return (List<LandmarkParcelable>) getObjectList(key, type); 
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

    //Database management
    private ConfigDbDataSource getConfigDatabase() {
        ConfigDbDataSource cdb = (ConfigDbDataSource) getObject("CONFIGDB", ConfigDbDataSource.class);
        if (cdb == null) {
            cdb = new ConfigDbDataSource(getContext());
            putObject("CONFIGDB", cdb);
        }
        return cdb;
    }

    public List<ExtendedLandmark> getLandmarkDatabase() {
        LandmarkDbDataSource db = new LandmarkDbDataSource(getContext());
        List<ExtendedLandmark> lmdb;
        FileManager fm = PersistenceManagerFactory.getFileManager();

        //file to landmarkdb migration code
        if (fm.fileExists(null, FileManager.LANDMARKDB_FILE)) {
            lmdb = new ArrayList<ExtendedLandmark>();
            fm.readLandmarkStore(lmdb);
            fm.deleteFile(null, FileManager.LANDMARKDB_FILE);
            db.addAll(lmdb);
        } else {
            lmdb = db.fetchAllLandmarks();
        }
        lmdb.addAll(default_locations.values());
        putObject("LANDMARKDB", db);

        return lmdb;
    }

    public void closeAllDatabases() {
        ConfigDbDataSource cdb = (ConfigDbDataSource) getObject("CONFIGDB", ConfigDbDataSource.class);
        if (cdb != null) {
            cdb.close();
        }

        LandmarkDbDataSource db = (LandmarkDbDataSource) getObject("LANDMARKDB", LandmarkDbDataSource.class);
        if (db != null) {
            db.close();
        }

        FavouritesDbDataSource fdb = (FavouritesDbDataSource) getObject("FAVOURITESDB", FavouritesDbDataSource.class);
        if (fdb != null) {
            fdb.close();
        }
    }

    //App initialization
    public void initApp(Context applicationContext) {
        setContext(applicationContext);
        String buildInfo = collectSystemInformation();
        setDefaultConfiguration();

        if (PersistenceManagerFactory.getFileManager().fileExists(null, FileManager.CONFIGURATION_FILE)) {
            PersistenceManagerFactory.getPersistenceManagerInstance().readConfigurationFile();
            saveConfiguration(true);
            PersistenceManagerFactory.getFileManager().deleteFile(null, FileManager.CONFIGURATION_FILE);
        } else {
            readConfiguration();
        }

        if (StringUtils.isNotEmpty(buildInfo)) {
            putObject(ConfigurationManager.BUILD_INFO, buildInfo);
        }

        if (!isDefaultUser()) {
            setOn(ConfigurationManager.GMS_AUTH_STATUS);
        }

        String[] limitArray = applicationContext.getResources().getStringArray(com.jstakun.gms.android.ui.lib.R.array.landmarksPerLayer);
        int index = getInt(ConfigurationManager.LANDMARKS_PER_LAYER, 0);
        if (index >= 0 && index < limitArray.length) {
            putInteger(ConfigurationManager.LANDMARKS_PER_LAYER, Integer.parseInt(limitArray[index]));
        }

        int dealIndex = getInt(ConfigurationManager.DEAL_LIMIT, 0);
        String[] dealLimitArray = applicationContext.getResources().getStringArray(com.jstakun.gms.android.ui.lib.R.array.dealLimit);
        if (dealIndex >= 0 && dealIndex < dealLimitArray.length) {
            try {
                putInteger(ConfigurationManager.DEAL_LIMIT, Integer.parseInt(dealLimitArray[dealIndex]));
            } catch (Exception e) {
            }
        }

        String[] array = applicationContext.getResources().getStringArray(com.jstakun.gms.android.ui.lib.R.array.radius);
        int pos = getInt(ConfigurationManager.SEARCH_RADIUS);
        if (pos == 0) {
            putInteger(ConfigurationManager.SEARCH_RADIUS, 10);
        } else if (pos > 0 && pos < array.length) {
            putInteger(ConfigurationManager.SEARCH_RADIUS, Integer.valueOf(array[pos]).intValue());
        }

        long installed;
        try {
            PackageManager pm = applicationContext.getPackageManager();
            //Version
            ApplicationInfo appInfo = pm.getApplicationInfo(applicationContext.getPackageName(), 0);
            String appFile = appInfo.sourceDir;
            installed = new File(appFile).lastModified();
        } catch (Exception ex) {
            LoggerUtils.error("ConfigurationManager.initApp() exception", ex);
            installed = System.currentTimeMillis();
        }

        default_locations.clear();
        default_locations.put("USA", LandmarkFactory.getLandmark("United States, Los Angeles", "", new QualifiedCoordinates(34.052234, -118.243685, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //United States Los Angeles 34.052234,-118.243685
        default_locations.put("FRA", LandmarkFactory.getLandmark("France, Paris", "", new QualifiedCoordinates(48.856918, 2.34121, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //France, Paris 48.856918, 2.34121 
        default_locations.put("DEU", LandmarkFactory.getLandmark("Germany, Berlin", "", new QualifiedCoordinates(52.516071, 13.37698, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Germany, Berlin 52.516071, 13.37698 
        default_locations.put("ITA", LandmarkFactory.getLandmark("Italy, Rome", "", new QualifiedCoordinates(41.901514, 12.460774, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Italy, Rome 41.901514, 12.460774
        default_locations.put("ESP", LandmarkFactory.getLandmark("Spain, Madrid", "", new QualifiedCoordinates(40.4203, -3.70577, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Spain, Madrid 40.4203,-3.70577, 
        default_locations.put("JPN", LandmarkFactory.getLandmark("Japan, Tokyo", "", new QualifiedCoordinates(35.689488, 139.691706, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Japan, Tokyo, 35.689488,139.691706 
        default_locations.put("GBR", LandmarkFactory.getLandmark("United Kingdom, London", "", new QualifiedCoordinates(51.506321, -0.12714, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //United Kingdom, London, 51.506321,-0.12714  
        default_locations.put("IND", LandmarkFactory.getLandmark("India, Mumbai", "", new QualifiedCoordinates(19.076191, 72.875877, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //India, Mumbai, 19.076191,72.875877 
        default_locations.put("CHN", LandmarkFactory.getLandmark("China, Beijing", "", new QualifiedCoordinates(39.90403, 116.407526, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //China, Beijing 39.90403, 116.407526
        default_locations.put("CHN", LandmarkFactory.getLandmark("Poland, Warsaw", "", new QualifiedCoordinates(52.235352, 21.00939, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Poland, Warsaw, 52.235352,21.00939
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
        default_locations.put("PRT", LandmarkFactory.getLandmark("Portugal, Lisbon", "", new QualifiedCoordinates(38.7252993, 9.1500364, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //PRT Portugal, Lisbon 38.7252993, 9.1500364
        default_locations.put("PAK", LandmarkFactory.getLandmark("Pakistan, Islamabad", "", new QualifiedCoordinates(33.718151, 73.060547, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //PAK Pakistan, Islamabad 33.718151, 73.060547
        default_locations.put("SWE", LandmarkFactory.getLandmark("Sweden, Stockholm", "", new QualifiedCoordinates(59.32893, 18.06491, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //SWE Sweden, Stockholm 59.32893, 18.06491
    }

    public String collectSystemInformation() {

        String ReturnVal = "";
        try {
            PackageManager pm = getContext().getPackageManager();
            //Version
            PackageInfo pi = pm.getPackageInfo(getContext().getPackageName(), 0);
            String VersionName = pi.versionName;
            // Package name
            String PackageName = pi.packageName;
            // Device model
            putString(ConfigurationManager.PACKAGE_NAME, PackageName);

            int VersionCode = pi.versionCode;

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

        } catch (NameNotFoundException ex) {
            LoggerUtils.error("ConfigurationManager.collectSystemInformation error", ex);
        }

        return ReturnVal;
    }

    //User Manager candidates
    public boolean isUserLoggedIn() {
        return (getString(TWEET_AUTH_STATUS, "").equals(ON)
                || getString(FB_AUTH_STATUS, "").equals(ON)
                || getString(LN_AUTH_STATUS, "").equals(ON)
                || getString(GL_AUTH_STATUS, "").equals(ON)
                || getString(FS_AUTH_STATUS, "").equals(ON)
                //|| getString(GW_AUTH_STATUS, "").equals(ON)
                || getString(GMS_AUTH_STATUS, "").equals(ON));
    }

    public String getOAuthLoggedInUsername() {
        if (getString(FB_AUTH_STATUS, "").equals(ON)) {
            return getString(FB_USERNAME);
        } else if (getString(TWEET_AUTH_STATUS, "").equals(ON)) {
            return getString(TWEET_USERNAME);
        } else if (getString(LN_AUTH_STATUS, "").equals(ON)) {
            return getString(LN_USERNAME);
        } else if (getString(GL_AUTH_STATUS, "").equals(ON)) {
            return getString(GL_USERNAME);
        } else if (getString(FS_AUTH_STATUS, "").equals(ON)) {
            return getString(FS_USERNAME);
        } //else if (getString(GW_AUTH_STATUS, "").equals(ON)) {
            //return getString(GW_USERNAME);
        //}

        return null;
    }

    public List<String> getLoginItems(boolean withSuffix) {
        List<String> items = new ArrayList<String>();
        if (getString(FB_AUTH_STATUS, "").equals(OFF)) {
            if (withSuffix) {
                items.add(OAuthServiceFactory.getServiceName(Commons.FACEBOOK) + ";" + Commons.FACEBOOK);
            } else {
                items.add(OAuthServiceFactory.getServiceName(Commons.FACEBOOK));
            }
        }
        if (getString(FS_AUTH_STATUS, "").equals(OFF)) {
            if (withSuffix) {
                items.add(OAuthServiceFactory.getServiceName(Commons.FOURSQUARE) + ";" + Commons.FOURSQUARE);
            } else {
                items.add(OAuthServiceFactory.getServiceName(Commons.FOURSQUARE));
            }
        }
        if (getString(GL_AUTH_STATUS, "").equals(OFF)) {
            if (withSuffix) {
                items.add(OAuthServiceFactory.getServiceName(Commons.GOOGLE) + ";" + Commons.GOOGLE);
            } else {
                items.add(OAuthServiceFactory.getServiceName(Commons.GOOGLE));
            }
        }
        if (getString(GMS_AUTH_STATUS, "").equals(OFF)) {
            if (withSuffix) {
                items.add(GMS_WORLD + ";");
            } else {
                items.add(GMS_WORLD);
            }
        }
        if (getString(TWEET_AUTH_STATUS, "").equals(OFF)) {
            if (withSuffix) {
                items.add(OAuthServiceFactory.getServiceName(Commons.TWITTER) + ";" + Commons.TWITTER);
            } else {
                items.add(OAuthServiceFactory.getServiceName(Commons.TWITTER));
            }
        }
        if (getString(LN_AUTH_STATUS, "").equals(OFF)) {
            if (withSuffix) {
                items.add(OAuthServiceFactory.getServiceName(Commons.LINKEDIN) + ";" + Commons.LINKEDIN);
            } else {
                items.add(OAuthServiceFactory.getServiceName(Commons.LINKEDIN));
            }
        }
        return items;
    }

    public void resetUser() {
        putString(GMS_AUTH_STATUS, OFF);
        if (isUserLoggedIn()) {
        	setAppUser();
        } else {
        	putString(USERNAME, Commons.DEFAULT_USERNAME);
            putString(PASSWORD, Commons.DEFAULT_PASSWORD);
        }
    }

    public boolean isDefaultUser() {
        return (getString(USERNAME, "").equals(Commons.DEFAULT_USERNAME) || getString(USERNAME, "").equals(getString(APP_USER)));
    }

    public void setAppUser() {
        putString(USERNAME, getString(APP_USER));
        putString(PASSWORD, getString(APP_USER_PWD));
    }

    public void setMyPosUser() {
        putString(USERNAME, getString(MY_POS_USER));
        putString(PASSWORD, getString(APP_USER_PWD));
    }
    
    public boolean isMyPosUser() {
    	return getString(USERNAME).equals(getString(MY_POS_USER));
    }
}
