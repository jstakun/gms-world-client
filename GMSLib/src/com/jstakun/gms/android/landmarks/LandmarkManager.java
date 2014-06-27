/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import com.devahead.util.objectpool.ObjectPool;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.data.LandmarkDbDataSource;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.deals.Category;
import com.jstakun.gms.android.deals.Deal;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.LayerPoint;
import com.jstakun.gms.android.utils.LayerPointFactory;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.MercatorUtils;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.ProjectionInterface;
import com.jstakun.gms.android.utils.SMSSender;
import com.jstakun.gms.android.utils.StringUtil;
import com.openlapi.QualifiedCoordinates;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 *
 * @author jstakun
 */
public class LandmarkManager {

    private Map<String, List<ExtendedLandmark>> landmarkStore = new ConcurrentHashMap<String, List<ExtendedLandmark>>();
    private LayerManager layerManager = null;
    private LandmarkPaintManager landmarkPaintManager = null;
    private ExtendedLandmark selectedLandmarkUI = null; //landmark selected by user in UI
    private int LAYER_LIMIT, TOTAL_LIMIT;
    private static final int COLOR_WHITE = Color.argb(128, 255, 255, 255); //white
    private static final int COLOR_LIGHT_SALMON = Color.argb(128, 255, 160, 122); //red Light Salmon
    private static final int COLOR_PALE_GREEN = Color.argb(128, 152, 251, 152); //Pale Green
    private static final int COLOR_DODGER_BLUE = Color.argb(128, 30, 144, 255); //Dodger Blue
    private static final int COLOR_YELLOW = Color.argb(128, 255, 215, 0); //yellow
    private ObjectPool pointsPool;
    private boolean initialized = false;

    public LandmarkManager() {
        layerManager = new LayerManager();
        landmarkPaintManager = new LandmarkPaintManager();
        LAYER_LIMIT = ConfigurationManager.getInstance().getInt(ConfigurationManager.LAYER_PAINT_LIMIT, 30);
        if (OsUtil.isIceCreamSandwichOrHigher()) {
            LAYER_LIMIT = 2 * LAYER_LIMIT;
        }

        TOTAL_LIMIT = ConfigurationManager.getInstance().getInt(ConfigurationManager.TOTAL_PAINT_LIMIT, 100);
        if (OsUtil.isIceCreamSandwichOrHigher()) {
            TOTAL_LIMIT = 3 * TOTAL_LIMIT;
        }

        pointsPool = new ObjectPool(new LayerPointFactory(), TOTAL_LIMIT + 1);
    }

    public void initialize(List<ExtendedLandmark> dbfile, String... layers) {
        initialized = true;
    	landmarkStore.put(Commons.LOCAL_LAYER, dbfile);
        layerManager.initialize(layers);
    }
    
    public boolean isInitialized() {
    	return initialized;
    }

    public String addLandmark(double latitude, double longitude, float altitude, String name, String details, String layer, boolean deduplicate) {
        try {
            ExtendedLandmark lm = createLandmark(latitude, longitude, altitude, name, details, layer);
            if (deduplicate && getUnmodifableLayer(layer).contains(lm)) {
                return Locale.getMessage(R.string.Landmark_exists_error);
            } else {
                return addLandmark(lm);
            }
        } catch (Exception e) {
            LoggerUtils.error("LandmarkManager.addLandmark exception", e);
            return Locale.getMessage(R.string.Landmark_add_error);
        }
    }

    public ExtendedLandmark createLandmark(double latitude, double longitude, float altitude, String name, String details, String layer) {
        String normalizedDetails = null;
        if (details != null) {
            normalizedDetails = details.replace('\n', ' ').replace('\r', ' ');
        }
        String normalizedName = name.replace('\n', ' ').replace('\r', ' ');
        QualifiedCoordinates qc = new QualifiedCoordinates(latitude, longitude, altitude, Float.NaN, Float.NaN);
        ExtendedLandmark lm = LandmarkFactory.getLandmark(normalizedName, normalizedDetails, qc, layer, System.currentTimeMillis());
        return lm;
    }

    public String addLandmark(ExtendedLandmark landmark) {
        String errorMessage = null;
        String layerName = landmark.getLayer();

        if (layerName.equals(Commons.MY_POSITION_LAYER)) {
            List<ExtendedLandmark> myPos = getLandmarkStoreLayer(Commons.MY_POSITION_LAYER);
            if (!myPos.isEmpty()) {
                myPos.clear();
            }
            myPos.add(landmark);
        } else if (layerName.equals(Commons.LOCAL_LAYER)) {
            List<ExtendedLandmark> layer = getLandmarkStoreLayer(Commons.LOCAL_LAYER);
            layer.add(landmark);
            //save landmark to sqlite database
            LandmarkDbDataSource db = (LandmarkDbDataSource) ConfigurationManager.getInstance().getObject("LANDMARKDB", LandmarkDbDataSource.class);
            long id = db.addLandmark(landmark);
            LoggerUtils.debug("Insterted new landmark to db: " + id);
            //
            addLandmarkToDynamicLayer(landmark);
        } else {
            List<ExtendedLandmark> layer = getLandmarkStoreLayer(layerName);
            layer.add(landmark);
            errorMessage = persistLandmark(landmark);
            String normalizedDetails = landmark.getDescription();
            if (normalizedDetails == null) {
            	normalizedDetails = "";
            }
            normalizedDetails += "<br/>" + Locale.getMessage(R.string.creation_date, DateTimeUtils.getDefaultDateTimeString(System.currentTimeMillis(), ConfigurationManager.getInstance().getCurrentLocale()));
            landmark.setDescription(normalizedDetails);
        }

        return errorMessage;
    }

    public void addLayer(String name, boolean extensible, boolean manageable, boolean enabled, boolean checkinable, boolean searchable, String smallIconPath, String largeIconPath, String desc, String formatted, List<ExtendedLandmark> layerLandmarks) {
        layerManager.addLayer(name, extensible, manageable, enabled, checkinable, searchable, smallIconPath, largeIconPath, desc, formatted);
        getLandmarkStoreLayer(name).addAll(layerLandmarks);
    }

    public int getLayerSize(String layerName) {
        Layer layer = layerManager.getLayer(layerName);
        if (layer != null && layer.getType() == LayerManager.LAYER_DYNAMIC) {
            return searchDynamicLayerCount(layerName);
        } else {
            return getLandmarkStoreLayer(layerName).size();
        }
    }

    public int getAllLayersSize() {
        int count = 0;
        for (Map.Entry<String, List<ExtendedLandmark>> layer : landmarkStore.entrySet()) {
            count += layer.getValue().size();
        }
        return count;
    }

    public int getLayerType(String layerName) {
        Layer layer = layerManager.getLayer(layerName);
        if (layer != null) {
            return layer.getType();
        }

        return -1;
    }

    public String[] getLayerUrlPrefix(String layerName) {
        Layer layer = layerManager.getLayer(layerName);
        if (layer != null && !layer.getLayerReader().isEmpty()) {
            return layer.getLayerReader().get(0).getUrlPrefix();
        }

        return null;
    }

    public List<ExtendedLandmark> getUnmodifableLayer(String layer) {
        return Collections.unmodifiableList(getLandmarkStoreLayer(layer));
    }

    public List<ExtendedLandmark> getLandmarkStoreLayer(String layer) {
        if (landmarkStore.containsKey(layer)) {
            return landmarkStore.get(layer);
        } else {
            List<ExtendedLandmark> landmarks = new CopyOnWriteArrayList<ExtendedLandmark>();
            landmarkStore.put(layer, landmarks);
            return landmarks;
        }
    }

    public void paintLandmarks(Canvas c, ProjectionInterface projection, int width, int height, String[] excluded, DisplayMetrics displayMetrics) {
        //long start = System.currentTimeMillis();
        //System.out.println("paintLandmarks");
        //Debug.startAllocCounting();

        int total_counter = 0;
        landmarkPaintManager.clearLandmarkDrawables();
        landmarkPaintManager.setSelectedLandmarkDrawable(null);
        List<LayerPoint> drawablePoints = new ArrayList<LayerPoint>();
        ExtendedLandmark selectedLandmark = getSeletedLandmarkUI();
        Function<ExtendedLandmark, LayerPoint> transformFunction = new ExtendedLandmarkToLayerPointFunction(projection);

        //draw recently opened landmarks
        List<ExtendedLandmark> recentlyOpenedLandmarks = new ArrayList<ExtendedLandmark>();
        List<ExtendedLandmark> recentlyOpenedLandmarksAll = landmarkPaintManager.getRecentlyOpenedLandmarks();
        List<Drawable> recentlyOpenedDrawables = new ArrayList<Drawable>();
        List<LayerPoint> layerPoints = new ArrayList<LayerPoint>();
        if (!recentlyOpenedLandmarksAll.isEmpty()) {
            layerPoints.addAll(Lists.transform(recentlyOpenedLandmarksAll, transformFunction));
        }
        int layersSize = layerPoints.size();

        for (int i = 0; i < layersSize; i++) {
            LayerPoint point = layerPoints.get(i);
            if (projection.isVisible(point)) {
                ExtendedLandmark landmark = recentlyOpenedLandmarksAll.get(i);
                boolean isMyPosLayer = landmark.getLayer().equals(Commons.MY_POSITION_LAYER);

                if (selectedLandmark == null || !selectedLandmark.equals(landmark)) {
                    recentlyOpenedLandmarks.add(landmark);
                    drawablePoints.add(point);
                    Drawable frame;

                    if (landmark.getCategoryId() != -1) {
                        int icon = LayerManager.getDealCategoryIcon(landmark.getLayer(), LayerManager.LAYER_ICON_LARGE, displayMetrics, landmark.getCategoryId());
                        frame = IconCache.getInstance().getLayerBitmap(icon, Integer.toString(landmark.getCategoryId()), COLOR_DODGER_BLUE, !isMyPosLayer, displayMetrics);
                    } else {
                        BitmapDrawable icon = LayerManager.getLayerIcon(landmark.getLayer(), LayerManager.LAYER_ICON_LARGE, displayMetrics, null);
                        frame = IconCache.getInstance().getLayerBitmap(icon, landmark.getLayer(), COLOR_DODGER_BLUE, !isMyPosLayer, displayMetrics);
                    }

                    if (frame != null) {
                    	int w = frame.getIntrinsicWidth() / 2;
                        frame.setBounds(point.x - w, point.y - frame.getIntrinsicHeight(), point.x + w, point.y);
                        recentlyOpenedDrawables.add(frame);
                    }
                } else {
                    pointsPool.freeObject(point);
                }
            } else {
                pointsPool.freeObject(point);
            }
        }

        //iterate over layers
        for (String key : Iterables.filter(layerManager.getLayers(), new LayerNotExcludedAndEnabledPredicate(excluded))) {
            List<ExtendedLandmark> layer = getLandmarkStoreLayer(key);
            boolean isMyPosLayer = key.equals(Commons.MY_POSITION_LAYER);
            int layer_counter = 0;
            layerPoints.clear();
            if (!layer.isEmpty()) {
                layerPoints.addAll(Lists.transform(layer, transformFunction));
            }
            int layerSize = layerPoints.size();

            for (int i = 0; i < layerSize; i++) {
                LayerPoint point = layerPoints.get(i);
                if (projection.isVisible(point)) {
                    ExtendedLandmark landmark = layer.get(i);
                    layer_counter++;
                    total_counter++;
                    if ((selectedLandmark == null || !selectedLandmark.equals(landmark))
                            && !recentlyOpenedLandmarks.contains(landmark) && (isMyPosLayer || !drawablePoints.contains(point))) {

                        drawablePoints.add(point);
                        int color = COLOR_WHITE;
                        if (landmark.isCheckinsOrPhotos()) {
                            color = COLOR_LIGHT_SALMON;
                        } else if (landmark.getRating() >= 0.85) {
                            color = COLOR_PALE_GREEN;
                        }

                        Drawable frame;
                        
                        if (landmark.getCategoryId() != -1) {
                            int icon = LayerManager.getDealCategoryIcon(key, LayerManager.LAYER_ICON_LARGE, displayMetrics, landmark.getCategoryId());
                            frame = IconCache.getInstance().getLayerBitmap(icon, Integer.toString(landmark.getCategoryId()), color, !isMyPosLayer, displayMetrics);
                        } else {
                            //if layer icon is loading, frame can't be cached
                            BitmapDrawable icon = LayerManager.getLayerIcon(key, LayerManager.LAYER_ICON_LARGE, displayMetrics, null);
                            frame = IconCache.getInstance().getLayerBitmap(icon, key, color, !isMyPosLayer, displayMetrics);
                        }

                        if (frame != null) {
                        	int w = frame.getIntrinsicWidth() / 2;
                            frame.setBounds(point.x - w, point.y - frame.getIntrinsicHeight(), point.x + w, point.y);
                            landmarkPaintManager.addLandmarkDrawable(frame);
                        }
                    } else {
                        pointsPool.freeObject(point);
                    }
                } else {
                    pointsPool.freeObject(point);
                }

                if (layer_counter >= LAYER_LIMIT || total_counter >= TOTAL_LIMIT) {
                    break;
                }
            }

            if (total_counter >= TOTAL_LIMIT) {
                break;
            }
        }

        //add recently opened drawables
        landmarkPaintManager.addAllLandmarkDrawable(recentlyOpenedDrawables);

        //draw selected landmark only when not in visible layer
        if (selectedLandmark != null && StringUtils.indexOfAny(selectedLandmark.getLayer(), excluded) < 0) {
            LayerPoint point = (LayerPoint) pointsPool.newObject();
            projection.toPixels(selectedLandmark.getLatitudeE6(), selectedLandmark.getLongitudeE6(), point);

            if (projection.isVisible(point)) {
                drawablePoints.add(point);
                Drawable frame;
                boolean isMyPosLayer = selectedLandmark.getLayer().equals(Commons.MY_POSITION_LAYER);

                if (selectedLandmark.getCategoryId() != -1) {
                    int icon = LayerManager.getDealCategoryIcon(selectedLandmark.getLayer(), LayerManager.LAYER_ICON_LARGE, displayMetrics, selectedLandmark.getCategoryId());
                    frame = IconCache.getInstance().getLayerBitmap(icon, selectedLandmark.getLayer(), COLOR_YELLOW, !isMyPosLayer, displayMetrics);
                } else {
                	BitmapDrawable icon = LayerManager.getLayerIcon(selectedLandmark.getLayer(), LayerManager.LAYER_ICON_LARGE, displayMetrics, null);
                    frame = IconCache.getInstance().getLayerBitmap(icon, selectedLandmark.getLayer(), COLOR_YELLOW, !isMyPosLayer, displayMetrics);
                }

                if (frame != null) {
                	int w = frame.getIntrinsicWidth() / 2;
                    frame.setBounds(point.x - w, point.y - frame.getIntrinsicHeight(), point.x + w, point.y);
                    landmarkPaintManager.setSelectedLandmarkDrawable(frame);
                }
            } else {
                pointsPool.freeObject(point);
            }
        }

        //free layer points for reuse
        for (LayerPoint point : drawablePoints) {
            pointsPool.freeObject(point);
        }

        //Debug.stopAllocCounting();
        //int ac = Debug.getThreadAllocCount();
        //System.out.println(ac + " ac");
    }

    public void getCheckinableLandmarks(List<LandmarkParcelable> checkinable, double lat, double lng) {
        final String[] excluded = new String[]{Commons.FACEBOOK_LAYER, Commons.FOURSQUARE_LAYER,
            Commons.FOURSQUARE_MERCHANT_LAYER, Commons.GOOGLE_PLACES_LAYER};

        List<ExtendedLandmark> landmarks = new ArrayList<ExtendedLandmark>();
        for (String key : Iterables.filter(layerManager.getLayers(), new LayerNotExcludedAndCheckinablePredicate(excluded))) {
            landmarks.addAll(getLandmarkStoreLayer(key));
        }

        if (!landmarks.isEmpty()) {
            Function<ExtendedLandmark, LandmarkParcelable> transformFunction = new ExtendedLandmarkToLandmarkParcelableFunction(lat, lng, ConfigurationManager.getInstance().getCurrentLocale());
            checkinable.addAll(Lists.transform(landmarks, transformFunction));
            landmarkPaintManager.setLandmarkOnFocusQueue(landmarks);
        } else {
            landmarkPaintManager.clearLandmarkOnFocusQueue();
        }
    }

    public void searchLandmarks(List<LandmarkParcelable> results, String searchTerm, String[] searchTermTokens, double lat, double lng, int searchType) {
        searchLandmarks(results, searchTerm, searchTermTokens, layerManager.getEnabledLayers(), lat, lng, searchType);
    }

    public void searchDeals(List<LandmarkParcelable> results, String searchTerm, String[] searchTermTokens, double lat, double lng, int searchType) {
        final List<String> included = Arrays.asList(new String[]{Commons.HOTWIRE_LAYER, Commons.COUPONS_LAYER,
                    Commons.GROUPON_LAYER, Commons.FOURSQUARE_MERCHANT_LAYER,
                    Commons.HOTELS_LAYER, Commons.LOCAL_LAYER, Commons.YELP_LAYER});
        searchLandmarks(results, searchTerm, searchTermTokens, included, lat, lng, searchType);
    }

    private void searchLandmarks(List<LandmarkParcelable> results, String searchTerm, String[] searchTermTokens, List<String> layerNames, double lat, double lng, int searchType) {
        //System.out.println("Starting method");

        Map<String, Thread> searchTasks = new HashMap<String, Thread>();
        Predicate<ExtendedLandmark> searchPredicate;

        if (searchTermTokens == null) {
        	searchTermTokens = StringUtils.split(searchTerm, " ");
        }
        
        if (searchType == -1) {
            searchType = ConfigurationManager.getInstance().getInt(ConfigurationManager.SEARCH_TYPE, ConfigurationManager.PHRASE_SEARCH);
        }
        
        if (searchType == ConfigurationManager.WORDS_SEARCH) {
            searchPredicate = new PhraseWordsSearchPredicate(searchTermTokens, false, searchTerm); //phrase words
        } else if (searchType == ConfigurationManager.FUZZY_SEARCH) {
            searchPredicate = new FuzzySearchPredicate(searchTermTokens, false, searchTerm); //fuzzy
        } else {
            //default exact phrase ConfigurationManager.PHRASE_SEARCH
            searchPredicate = new ExactPhraseSearchPredicate(searchTerm);
        }

        //System.out.println("Starting threads");

        List<ExtendedLandmark> landmarks = Collections.synchronizedList(new ArrayList<ExtendedLandmark>());

        for (String key : layerNames) {
            //System.out.println("Starting thread " + key);

            Thread searchLayerTask = new Thread(new SearchLayerTask(key, searchTermTokens, landmarks, searchPredicate, searchTasks));
            searchTasks.put(key, searchLayerTask);
            searchLayerTask.start();
        }

        //System.out.println("Started all threads");

        long s = System.currentTimeMillis();
        while (System.currentTimeMillis() - s < 30000) { //30 sec
            if (searchTasks.isEmpty()) {
                break;
            } else {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ie) {
                }
            }

            //System.out.println("Layers size " + searchTasks.size());
        }

        if (!landmarks.isEmpty()) {
            Function<ExtendedLandmark, LandmarkParcelable> transformFunction = new ExtendedLandmarkToLandmarkParcelableFunction(lat, lng, ConfigurationManager.getInstance().getCurrentLocale());
            results.addAll(Lists.transform(landmarks, transformFunction));
            landmarkPaintManager.setLandmarkOnFocusQueue(landmarks);
        } else {
            landmarkPaintManager.clearLandmarkOnFocusQueue();
        }
        //System.out.println("Found landmarks " + results.size());
    }

    public int searchDynamicLayerCount(String layerName) {
        return layerManager.getLayer(layerName).getCount();
    }

    private String persistLandmark(ExtendedLandmark landmark) {
        int persist = ConfigurationManager.getInstance().getInt(ConfigurationManager.PERSIST_METHOD);
        String errorMessage = null;

        if (persist == ConfigurationManager.PERSIST_SMS) {
            //Not used currently
            //System.out.println("Sending landmark via SMS");
            String username = ConfigurationManager.getUserManager().getLoggedInUsername();

            double[] coords = MercatorUtils.normalizeE6(new double[]{landmark.getQualifiedCoordinates().getLatitude(), landmark.getQualifiedCoordinates().getLongitude()});
            String text = " lat:" + coords[0] + FileManager.SEPARATOR_CHAR
                    + "lon:" + coords[1] + FileManager.SEPARATOR_CHAR
                    + "name:" + landmark.getName() + FileManager.SEPARATOR_CHAR
                    + "desc:" + landmark.getDescription() + FileManager.SEPARATOR_CHAR
                    + "user:" + username;

            try {
                SMSSender sender = new SMSSender(text);
                sender.sendSMS();
            } catch (Exception e) {
                LoggerUtils.error("LandmarkManager.persistLandmark exception ", e);
                errorMessage = "SMS sending error";
            }

            List<ExtendedLandmark> layer = getLandmarkStoreLayer(landmark.getLayer());
            layer.add(landmark);
        } else if (persist == ConfigurationManager.PERSIST_SERVER) {
            errorMessage = persistToServer(landmark, landmark.getLayer(), null, ConfigurationManager.getUserManager().getLoggedInUsername());
        } else if (persist == ConfigurationManager.PERSIST_LOCAL) {
            //Local store
            List<ExtendedLandmark> layer = getLandmarkStoreLayer(Commons.LOCAL_LAYER);
            layer.add(landmark);
            //System.out.println("Saving landmark to local store");
        }

        return errorMessage;
    }

    public LayerManager getLayerManager() {
        return layerManager;
    }

    public void clearLayer(String layer) {
        landmarkStore.remove(layer);
    }

    public void deleteLayer(String layerName) {
        Layer layer = layerManager.getLayer(layerName);

        if (layer != null) {
            landmarkStore.remove(layerName);
            layerManager.removeLayer(layerName);

            if (layer.getType() == LayerManager.LAYER_DYNAMIC) {
                layerManager.removeDynamicLayer(layerName);
            }
        }
    }

    public void deletePhoneLandmark(int id) {
        List<ExtendedLandmark> mypos = getLandmarkStoreLayer(Commons.MY_POSITION_LAYER);
        List<ExtendedLandmark> local = getLandmarkStoreLayer(Commons.LOCAL_LAYER);
        if (id == local.size() && mypos.size() > 0) {
            //mypos.remove(0);
            mypos.clear();
        } else if (id >= 0 && id < local.size()) {
            local.remove(id);
        }
    }

    public ExtendedLandmark getPhoneLandmark(int id) {
        List<ExtendedLandmark> mypos = getUnmodifableLayer(Commons.MY_POSITION_LAYER);
        List<ExtendedLandmark> local = getUnmodifableLayer(Commons.LOCAL_LAYER);
        ExtendedLandmark l = null;
        if (id == local.size() && mypos.size() > 0) {
            l = mypos.get(0);
        } else if (id >= 0 && id < local.size()) {
            l = local.get(id);
        }
        return l;
    }

    public void clearLandmarkStore() {
        for (Iterator<Map.Entry<String, List<ExtendedLandmark>>> i = landmarkStore.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, List<ExtendedLandmark>> entry = i.next();
            String key = entry.getKey();
            if (!(key.equals(Commons.LOCAL_LAYER) || key.equals(Commons.MY_POSITION_LAYER))) {
                i.remove();
            }
        }

        for (String key : getLayerManager().getLayers()) {
            getLayerManager().getLayer(key).setCount(0);
        }
        
        landmarkPaintManager.clearRecentlyOpenedLandmarks();
    }

    public String persistToServer(ExtendedLandmark landmark, String layer, String validityDate, String username) {

        String errorMessage;

        double[] coords = MercatorUtils.normalizeE6(new double[]{landmark.getQualifiedCoordinates().getLatitude(), landmark.getQualifiedCoordinates().getLongitude()});
        double alt = MercatorUtils.normalizeE6(landmark.getQualifiedCoordinates().getAltitude());
        String url = ConfigurationManager.getInstance().getServicesUrl() + "persistLandmark";
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("name", landmark.getName()));
        params.add(new BasicNameValuePair("description", landmark.getDescription()));
        params.add(new BasicNameValuePair("longitude", Double.toString(coords[1])));
        params.add(new BasicNameValuePair("latitude", Double.toString(coords[0])));
        params.add(new BasicNameValuePair("altitude", Double.toString(alt)));
        params.add(new BasicNameValuePair("radius", Integer.toString(DistanceUtils.radiusInKilometer())));

        if (StringUtils.isNotEmpty(username)) {
        	params.add(new BasicNameValuePair("username", username));
        } 

        if (StringUtils.isNotEmpty(layer)) {
            params.add(new BasicNameValuePair("layer", layer));
        }

        if (StringUtils.isNotEmpty(validityDate)) {
            params.add(new BasicNameValuePair("validityDate", validityDate));
        }

        String email = ConfigurationManager.getUserManager().getUserEmail();
        if (StringUtils.isNotEmpty(email)) {
            params.add(new BasicNameValuePair("email", email));
        }

        HttpUtils utils = new HttpUtils();
        utils.sendPostRequest(url, params, true);
        errorMessage = utils.getResponseCodeErrorMessage();

        //if (errorMessage == null) {
            //we are using bit.ly url shorter
        String lmUrl = null;
        String hash = utils.getHeader("hash");
        String key = utils.getHeader("key");
            
        if (hash != null) {
           lmUrl = ConfigurationManager.BITLY_URL + hash;
        } else if (key != null) {
           lmUrl = ConfigurationManager.SHOW_LANDMARK_URL + key;
        }
            
        if (key != null) {
           landmark.setServerKey(key);
        }

        if (lmUrl != null) {
           landmark.setUrl(lmUrl);
        }
        //}
        
        //System.out.println("Set values: " + key + " " + lmUrl);

        try {
            if (utils != null) {
                utils.close();
            }
        } catch (IOException ioe) {
        }

        return errorMessage;
    }

    public ExtendedLandmark findLandmarkById(int hashCode) {
    	for (String key : layerManager.getLayers()) {
    		for (ExtendedLandmark landmark : getLandmarkStoreLayer(key)) {
    			if (landmark.hashCode()==hashCode) {
    				return landmark;
    			}
    		}
    	}
    	return null;
    }
    
    public void findVisibleLandmarks(ProjectionInterface projection, boolean includeDisabled) {
        landmarkPaintManager.setSelectedLandmark(null, -1);
        List<ExtendedLandmark> newFocusQueue = new ArrayList<ExtendedLandmark>();
        Predicate<ExtendedLandmark> visibleLandmarkPredicate = new VisibleLandmarkPredicate(projection);
        List<String> i;

        if (includeDisabled) {
            i = layerManager.getLayers();
        } else {
            i = layerManager.getEnabledLayers();
        }

        for (String key : i) {
            newFocusQueue.addAll(Collections2.filter(getLandmarkStoreLayer(key), visibleLandmarkPredicate));
        }

        if (!newFocusQueue.isEmpty()) {
            landmarkPaintManager.setLandmarkOnFocusQueue(newFocusQueue);
        } else {
            landmarkPaintManager.clearLandmarkOnFocusQueue();
        }
    }

    public boolean findLandmarksInRadius(int x, int y, ProjectionInterface projection, boolean includeDisabled, DisplayMetrics metrics) {
        final int BOTTOM = (int) (48 * metrics.density);
        final int TOP = (int) (2 * metrics.density);
        final int LEFT = (int) (16 * metrics.density);
        final int RIGHT = LEFT;
        landmarkPaintManager.setSelectedLandmark(null, -1);
        List<ExtendedLandmark> newFocusQueue = new ArrayList<ExtendedLandmark>();
        List<String> keys;

        if (includeDisabled) {
            keys = layerManager.getLayers();
        } else {
            keys = layerManager.getEnabledLayers();
        }

        LandmarkInRadiusPredicate landmarkInRadiusPredicate = new LandmarkInRadiusPredicate(projection, x, y, LEFT, RIGHT, TOP, BOTTOM);

        for (String key : keys) {
            newFocusQueue.addAll(Collections2.filter(getLandmarkStoreLayer(key), landmarkInRadiusPredicate));
        }

        RoutesManager routesManager = ConfigurationManager.getInstance().getRoutesManager();
        if (routesManager != null) {
            newFocusQueue.addAll(Collections2.filter(routesManager.getBoundingRouteLandmarks(), landmarkInRadiusPredicate));
        }

        if (!newFocusQueue.isEmpty()) {
            landmarkPaintManager.setLandmarkOnFocusQueue(newFocusQueue);
        } else {
            landmarkPaintManager.clearLandmarkOnFocusQueue();
        }

        return !newFocusQueue.isEmpty();
    }

    public ExtendedLandmark findRecommendedLandmark(int category, int subcategory) {
        ExtendedLandmark recommendedBySubcat = null, recommendedByCat = null;
        double highestDiscountSubcat = 0.0, highestDiscountCat = 0.0,
                highestSaveSubcat = 0.0, highestSaveCat = 0.0;
        String recommendedDealShown = ConfigurationManager.getInstance().getString(ConfigurationManager.RECOMMENDED_DEALS_SHOWN);
        List<Integer> lastShownDeals = new ArrayList<Integer>();

        if (recommendedDealShown != null) {
            lastShownDeals = StringUtil.stringToLongArray(recommendedDealShown, ",");
        }
        for (String key : Iterables.filter(layerManager.getLayers(), new LayerExistsPredicate())) {
            for (ExtendedLandmark landmark : getUnmodifableLayer(key)) {
                Deal deal = landmark.getDeal();

                if (deal != null) {
                    RecommendedDealPredicate predicateSubcat = new RecommendedDealPredicate(lastShownDeals, highestDiscountSubcat, highestSaveSubcat, category, subcategory);

                    if (predicateSubcat.apply(landmark)) {
                        recommendedBySubcat = landmark;
                        highestDiscountSubcat = deal.getDiscount();
                        highestSaveSubcat = deal.getSave();
                    }

                    if (recommendedBySubcat == null) {
                        RecommendedDealPredicate predicateCat = new RecommendedDealPredicate(lastShownDeals, highestDiscountCat, highestSaveCat, category, -1);

                        if (predicateCat.apply(landmark)) {
                            recommendedByCat = landmark;
                            highestDiscountCat = deal.getDiscount();
                            highestSaveCat = deal.getSave();
                        }
                    }
                }
            }
        }

        if (recommendedBySubcat != null || recommendedByCat != null) {
            if (recommendedBySubcat != null) {
                lastShownDeals.add(recommendedBySubcat.hashCode());
            } else {
                lastShownDeals.add(recommendedByCat.hashCode());
            }
            ConfigurationManager.getInstance().putString(ConfigurationManager.RECOMMENDED_DEALS_SHOWN, StringUtil.getLastTokens(20, lastShownDeals));
        }

        if (recommendedBySubcat != null) {
            return recommendedBySubcat;
        } else {
            return recommendedByCat;
        }
    }

    public void findNewestLandmarks(List<LandmarkParcelable> newest, int minDays, String[] excluded, double lat, double lng) {
        long lastStartTime = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * minDays); //maximum landmark age

        //if (ConfigurationManager.getInstance().containsKey(ConfigurationManager.LAST_STARTING_DATE)) {
            long lastStartingDate = ConfigurationManager.getInstance().getLong(ConfigurationManager.LAST_STARTING_DATE);

            if (lastStartingDate > 0 && lastStartTime > lastStartingDate) {
                lastStartTime = lastStartingDate;
            }
        //}

        NewerThanDatePredicate newerThanDatePredicate = new NewerThanDatePredicate(lastStartTime);

        List<ExtendedLandmark> landmarks = new ArrayList<ExtendedLandmark>();

        for (String key : Iterables.filter(layerManager.getLayers(), new LayerNotExcludedPredicate(excluded))) {
            landmarks.addAll(Collections2.filter(getUnmodifableLayer(key), newerThanDatePredicate));
        }

        if (!landmarks.isEmpty()) {
            Function<ExtendedLandmark, LandmarkParcelable> transformFunction = new ExtendedLandmarkToLandmarkParcelableFunction(lat, lng, ConfigurationManager.getInstance().getCurrentLocale());
            newest.addAll(Lists.transform(landmarks, transformFunction));
            landmarkPaintManager.setLandmarkOnFocusQueue(landmarks);
        } else {
            landmarkPaintManager.clearLandmarkOnFocusQueue();
        }
    }

    public void findFriendsCheckinLandmarks(List<LandmarkParcelable> checkins, double lat, double lng) {
        FriendsCheckinsPredicate friendsCheckinsPredicate = new FriendsCheckinsPredicate();
        final String[] friendsLayers = new String[]{Commons.FOURSQUARE_LAYER, Commons.FACEBOOK_LAYER};
        List<ExtendedLandmark> landmarks = new ArrayList<ExtendedLandmark>();

        for (int i = 0; i < friendsLayers.length; i++) {
            landmarks.addAll(Collections2.filter(getLandmarkStoreLayer(friendsLayers[i]), friendsCheckinsPredicate));
        }

        if (!landmarks.isEmpty()) {
            Function<ExtendedLandmark, LandmarkParcelable> transformFunction = new ExtendedLandmarkToLandmarkParcelableFunction(lat, lng, ConfigurationManager.getInstance().getCurrentLocale());
            checkins.addAll(Lists.transform(landmarks, transformFunction));
            landmarkPaintManager.setLandmarkOnFocusQueue(landmarks);
        } else {
            landmarkPaintManager.clearLandmarkOnFocusQueue();
        }
    }

    public void findDealsOfTheDay(List<LandmarkParcelable> deals, String[] excluded, double lat, double lng) {
        DealsOfTheDayPredicate dealsOfTheDayPredicate = new DealsOfTheDayPredicate();
        List<ExtendedLandmark> landmarks = new ArrayList<ExtendedLandmark>();

        for (String key : Iterables.filter(layerManager.getLayers(), new LayerNotExcludedPredicate(excluded))) {
            landmarks.addAll(Collections2.filter(getLandmarkStoreLayer(key), dealsOfTheDayPredicate));
        }

        if (!landmarks.isEmpty()) {
            Function<ExtendedLandmark, LandmarkParcelable> transformFunction = new ExtendedLandmarkToLandmarkParcelableFunction(lat, lng, ConfigurationManager.getInstance().getCurrentLocale());
            deals.addAll(Lists.transform(landmarks, transformFunction));
            landmarkPaintManager.setLandmarkOnFocusQueue(landmarks);
        } else {
            landmarkPaintManager.clearLandmarkOnFocusQueue();
        }
    }

    public List<ExtendedLandmark> getLandmarkToFocusQueue() {
        return landmarkPaintManager.getLandmarkOnFocusQueue();
    }

    public ExtendedLandmark getLandmarkToFocusQueueSelectedLandmark(int id) {
        return landmarkPaintManager.getLandmarkOnFocusQueueSelectedLandmark(id);
    }

    public void addRecentlyOpenedLandmark(ExtendedLandmark landmark) {
        if (!landmark.getLayer().equals(Commons.MY_POSITION_LAYER)) {
            landmarkPaintManager.addRecentlyOpenedLandmark(landmark);
        }
    }

    public void getRecentlyOpenedLandmarks(List<LandmarkParcelable> recentlySelected, double lat, double lng) {
        List<ExtendedLandmark> landmarks = new ArrayList<ExtendedLandmark>();
        landmarks.addAll(landmarkPaintManager.getRecentlyOpenedLandmarks());
        if (!landmarks.isEmpty()) {
            Function<ExtendedLandmark, LandmarkParcelable> transformFunction = new ExtendedLandmarkToLandmarkParcelableFunction(lat, lng, ConfigurationManager.getInstance().getCurrentLocale());
            recentlySelected.addAll(Lists.transform(landmarks, transformFunction));
            landmarkPaintManager.setLandmarkOnFocusQueue(landmarks);
        } else {
            landmarkPaintManager.clearLandmarkOnFocusQueue();
        }
    }

    public void getMyLandmarks(List<LandmarkParcelable> myLandmarks, double lat, double lng) {
        List<ExtendedLandmark> landmarks = new ArrayList<ExtendedLandmark>();
        landmarks.addAll(getLandmarkStoreLayer(Commons.LOCAL_LAYER));
        List<ExtendedLandmark> mypos = getLandmarkStoreLayer(Commons.MY_POSITION_LAYER);
        if (!mypos.isEmpty()) {
            landmarks.add(mypos.get(0));
        }
        if (!landmarks.isEmpty()) {
            Function<ExtendedLandmark, LandmarkParcelable> transformFunction = new ExtendedLandmarkToLandmarkParcelableFunction(lat, lng, ConfigurationManager.getInstance().getCurrentLocale());
            myLandmarks.addAll(Lists.transform(landmarks, transformFunction));
            landmarkPaintManager.setLandmarkOnFocusQueue(landmarks);
        } else {
            landmarkPaintManager.clearLandmarkOnFocusQueue();
        }
    }

    public void getMultiLandmarks(List<LandmarkParcelable> multiLandmark, double lat, double lng) {
        List<ExtendedLandmark> landmarks = getLandmarkToFocusQueue();
        if (!landmarks.isEmpty()) {
            Function<ExtendedLandmark, LandmarkParcelable> transformFunction = new ExtendedLandmarkToLandmarkParcelableFunction(lat, lng, ConfigurationManager.getInstance().getCurrentLocale());
            //multiLandmark.addAll(Lists.transform(landmarks, transformFunction)); //could have null
            multiLandmark.addAll(Lists.newArrayList(Iterables.filter(Iterables.transform(landmarks, transformFunction), Predicates.notNull()))); 
        }
    }

    public void setSelectedLandmark(ExtendedLandmark landmark) {
        landmarkPaintManager.setSelectedLandmark(landmark, -1.0);
    }

    public void setSelectedLandmark(ExtendedLandmark landmark, double distanceFromCenter) {
        landmarkPaintManager.setSelectedLandmark(landmark, distanceFromCenter);
    }

    public boolean hasSelectedLandmark() {
        return landmarkPaintManager.hasSelectedLandmark();
    }

    public void clearLandmarkOnFocusQueue() {
        landmarkPaintManager.clearLandmarkOnFocusQueue();
    }

    public ExtendedLandmark getLandmarkOnFocus() {
        return landmarkPaintManager.getLandmarkOnFocus();
    }

    public ExtendedLandmark getSelectedLandmark() {
        return landmarkPaintManager.getSelectedLandmark();
    }

    public void setSeletedLandmarkUI() {
        selectedLandmarkUI = landmarkPaintManager.getSelectedLandmark();
    }

    public ExtendedLandmark getSeletedLandmarkUI() {
        return selectedLandmarkUI;
    }

    public int removeLandmark(ExtendedLandmark landmark) {
        String layer = landmark.getLayer();
        int response = 0;

        if (landmarkStore.containsKey(layer)) {
            List<ExtendedLandmark> layerVector = getLandmarkStoreLayer(layer);
            layerVector.remove(landmark);
        }

        if (layer.equals(Commons.LOCAL_LAYER)) {
            LandmarkDbDataSource db = (LandmarkDbDataSource) ConfigurationManager.getInstance().getObject("LANDMARKDB", LandmarkDbDataSource.class);
            response = db.deleteLandmark(landmark);
        }

        return response;
    }

    public void setLayerOnFocus(List<LandmarkParcelable> results, String layerName, boolean isDeal, double lat, double lng) {
        Layer layer = layerManager.getLayer(layerName);
        if (layer != null && layer.getType() == LayerManager.LAYER_DYNAMIC && !isDeal) {
            searchLandmarks(results, null, layer.getKeywords(), lat, lng, ConfigurationManager.FUZZY_SEARCH);
        } else if (layer != null && layer.getType() == LayerManager.LAYER_DYNAMIC && isDeal) {
            searchDeals(results, null, layer.getKeywords(), lat, lng, ConfigurationManager.FUZZY_SEARCH);
        } else if (landmarkStore.containsKey(layerName)) {
            List<ExtendedLandmark> landmarks = getLandmarkStoreLayer(layerName);
            if (!landmarks.isEmpty()) {
                Function<ExtendedLandmark, LandmarkParcelable> transformFunction = new ExtendedLandmarkToLandmarkParcelableFunction(lat, lng, ConfigurationManager.getInstance().getCurrentLocale());
                results.addAll(Lists.transform(landmarks, transformFunction));
                landmarkPaintManager.setLandmarkOnFocusQueue(landmarks);
            } else {
                landmarkPaintManager.clearLandmarkOnFocusQueue();
            }
        }
    }

    //deal categories
    public void selectCategoryLandmarks(List<LandmarkParcelable> landmarkParcelable, int categoryId, int subCategoryId, double lat, double lng) {
        //landmarkPaintManager.clearLandmarkOnFocusQueue();
        final List<String> included = Arrays.asList(new String[]{Commons.HOTWIRE_LAYER, Commons.COUPONS_LAYER,
                    Commons.GROUPON_LAYER, Commons.FOURSQUARE_MERCHANT_LAYER, Commons.HOTELS_LAYER, Commons.YELP_LAYER});

        List<ExtendedLandmark> categoryLandmarks = new ArrayList<ExtendedLandmark>();
        CategoryLandmarkPredicate categoryLandmarkPredicate = new CategoryLandmarkPredicate(categoryId, subCategoryId);

        for (String key : Iterables.filter(included, new LayerExistsPredicate())) {
            categoryLandmarks.addAll(Collections2.filter(getLandmarkStoreLayer(key), categoryLandmarkPredicate));
        }

        if (!categoryLandmarks.isEmpty()) {
            Function<ExtendedLandmark, LandmarkParcelable> transformFunction = new ExtendedLandmarkToLandmarkParcelableFunction(lat, lng, ConfigurationManager.getInstance().getCurrentLocale());
            landmarkParcelable.addAll(Lists.transform(categoryLandmarks, transformFunction));
            landmarkPaintManager.setLandmarkOnFocusQueue(categoryLandmarks);
        } else {
            landmarkPaintManager.clearLandmarkOnFocusQueue();
        }
    }

    public int selectCategoryLandmarksCount(int categoryId, int subCategoryId) {
        int count = 0;

        if (categoryId == CategoriesManager.CATEGORY_TRAVEL && (subCategoryId == CategoriesManager.SUBCATEGORY_HOTEL || subCategoryId == -1)) {
            count += getLayerSize(Commons.HOTWIRE_LAYER);
            count += getLayerSize(Commons.HOTELS_LAYER);
        }

        final List<String> included = Arrays.asList(new String[]{Commons.COUPONS_LAYER,
                    Commons.GROUPON_LAYER, Commons.FOURSQUARE_MERCHANT_LAYER, Commons.YELP_LAYER});
        CategoryLandmarkPredicate categoryLandmarkPredicate = new CategoryLandmarkPredicate(categoryId, subCategoryId);

        for (String key : Iterables.filter(included, new LayerExistsPredicate())) {
            count += Collections2.filter(getLandmarkStoreLayer(key), categoryLandmarkPredicate).size();
        }
        return count;
    }

    public boolean isCategoryLandmarksEmpty(int categoryId, int subCategoryId) {
        if (categoryId == CategoriesManager.CATEGORY_TRAVEL && (subCategoryId == CategoriesManager.SUBCATEGORY_HOTEL || subCategoryId == -1)) {
            if (getLayerSize(Commons.HOTWIRE_LAYER) > 0) {
                return false;
            } else if (getLayerSize(Commons.HOTELS_LAYER) > 0) {
                return false;
            }
        }

        final List<String> included = Arrays.asList(new String[]{Commons.COUPONS_LAYER,
                    Commons.GROUPON_LAYER, Commons.FOURSQUARE_MERCHANT_LAYER, Commons.YELP_LAYER});
        CategoryLandmarkPredicate categoryLandmarkPredicate = new CategoryLandmarkPredicate(categoryId, subCategoryId);

        for (String key : Iterables.filter(included, new LayerExistsPredicate())) {
            if (Collections2.filter(getLandmarkStoreLayer(key), categoryLandmarkPredicate).size() > 0) {
                return false;
            }
        }

        return true;
    }

    public List<Drawable> getLandmarkDrawables() {
        return landmarkPaintManager.getLandmarkDrawables();
    }

    public Drawable getSelectedLandmarkDrawable() {
        return landmarkPaintManager.getSelectedLandmarkDrawable();
    }

    public boolean hasMyLocation() {
    	return !getLandmarkStoreLayer(Commons.MY_POSITION_LAYER).isEmpty();
    }
    
    public double[] getMyLocation(int lat, int lon) {
        double latt, lonn;

        List<ExtendedLandmark> myPosV = getLandmarkStoreLayer(Commons.MY_POSITION_LAYER);
        if (!myPosV.isEmpty()) {
            ExtendedLandmark myPos = myPosV.get(0);
            latt = myPos.getQualifiedCoordinates().getLatitude();
            lonn = myPos.getQualifiedCoordinates().getLongitude();
        } else {
            latt = MathUtils.coordIntToDouble(lat);
            lonn = MathUtils.coordIntToDouble(lon);
        }

        return new double[]{latt, lonn};
    }

    public void findLandmarksInMonth(int year, int month, int[] daysWithLandmarks) {
        Calendar cal = Calendar.getInstance();

        for (String key : Iterables.filter(layerManager.getLayers(), new LayerExistsPredicate())) {
            for (ExtendedLandmark landmark : getUnmodifableLayer(key)) {
                if (landmark.isDeal()) {
                    cal.setTimeInMillis(landmark.getDeal().getEndDate());
                } else {
                    cal.setTimeInMillis(landmark.getCreationDate());
                }
                if (year == cal.get(Calendar.YEAR) && month == cal.get(Calendar.MONTH)) {
                    daysWithLandmarks[cal.get(Calendar.DAY_OF_MONTH) - 1] = 1;
                }
            }
        }
    }

    public void findLandmarksInDay(List<LandmarkParcelable> landmarkParcelable, int year, int month, int day, double lat, double lng) {
        List<ExtendedLandmark> dayLandmarks = new ArrayList<ExtendedLandmark>();
        
        if (year > 0 && month >= 0 && day > 0) {
        	Calendar selectedDay = Calendar.getInstance();
            selectedDay.set(year, month, day);
            Calendar landmarkDate = Calendar.getInstance();
            for (String key : Iterables.filter(layerManager.getLayers(), new LayerExistsPredicate())) {
                for (ExtendedLandmark landmark : getUnmodifableLayer(key)) {
                    if (landmark.isDeal()) {
                        landmarkDate.setTimeInMillis(landmark.getDeal().getEndDate());
                    } else {
                        landmarkDate.setTimeInMillis(landmark.getCreationDate());
                    }
                    //if (year == cal.get(Calendar.YEAR) && month == cal.get(Calendar.MONTH) && day == cal.get(Calendar.DAY_OF_MONTH)) {
                    //    dayLandmarks.add(landmark);
                    //}
                    if (selectedDay.get(Calendar.DAY_OF_YEAR) == landmarkDate.get(Calendar.DAY_OF_YEAR) &&
                    		selectedDay.get(Calendar.YEAR) == landmarkDate.get(Calendar.YEAR)) {
                    	dayLandmarks.add(landmark);
                    }
                }
            }
        }
        if (!dayLandmarks.isEmpty()) {
            Function<ExtendedLandmark, LandmarkParcelable> transformFunction = new ExtendedLandmarkToLandmarkParcelableFunction(lat, lng, ConfigurationManager.getInstance().getCurrentLocale());
            landmarkParcelable.addAll(Lists.transform(dayLandmarks, transformFunction));
            landmarkPaintManager.setLandmarkOnFocusQueue(dayLandmarks);
        } else {
            landmarkPaintManager.clearLandmarkOnFocusQueue();
        }
    }

    public void addLandmarkToDynamicLayer(ExtendedLandmark landmark) {
        List<String> dynamicLayers = layerManager.getDynamicLayers();
        for (String key : dynamicLayers) {
            Layer layer = layerManager.getLayer(key);
            Predicate<ExtendedLandmark> searchPredicate = new FuzzySearchPredicate(layer.getKeywords(), false, null);
            if (searchPredicate.apply(landmark)) {
                layer.incrementCount();
            }
        }
    }
    
    protected void addLandmarkListToDynamicLayer(Collection<ExtendedLandmark> landmarks) {
        if (!landmarks.isEmpty()) {
        	List<String> dynamicLayers = layerManager.getDynamicLayers();
        	for (String key : dynamicLayers) {
        		Layer layer = layerManager.getLayer(key);
        		Predicate<ExtendedLandmark> searchPredicate = new FuzzySearchPredicate(layer.getKeywords(), false, null);
        		for (ExtendedLandmark landmark : landmarks) {
        			if (searchPredicate.apply(landmark)) {
        				layer.incrementCount();
        			}
        		}
        	}
        }
    }

    public int countLandmarks(Category c) {
        if (c != null) {
            if (c.isCustom()) {
                return searchDynamicLayerCount(c.getCategory());
            } else {
                int categoryId = c.getCategoryID();
                int subCategoryId = c.getSubcategoryID();
                return selectCategoryLandmarksCount(categoryId, subCategoryId);
            }
        }

        return 0;
    }

    public boolean isLayerEmpty(Category c) {
        if (c != null) {
            if (c.isCustom()) {
                return (searchDynamicLayerCount(c.getCategory()) == 0);
            } else {
                int categoryId = c.getCategoryID();
                int subCategoryId = c.getSubcategoryID();
                return isCategoryLandmarksEmpty(categoryId, subCategoryId);
            }
        }

        return true;
    }

    //PREDICATES /////////////////////////////////////////////////////////////////////
    private class DealsOfTheDayPredicate implements Predicate<ExtendedLandmark> {

        public boolean apply(ExtendedLandmark input) {
            if (input.getDeal() != null) {
                return input.getDeal().isIsDealOfTheDay();
            } else {
                return false;
            }
        }
    }

    private class LayerNotExcludedAndEnabledPredicate implements Predicate<String> {

        private String[] excluded;

        public LayerNotExcludedAndEnabledPredicate(String[] excluded) {
            this.excluded = excluded;
        }

        public boolean apply(String layer) {
            return (landmarkStore.containsKey(layer) && layerManager.isLayerEnabled(layer) && StringUtils.indexOfAny(layer, excluded) < 0);
        }
    }

    private class LayerNotExcludedPredicate implements Predicate<String> {

        private String[] excluded;

        public LayerNotExcludedPredicate(String[] excluded) {
            this.excluded = excluded;
        }

        public boolean apply(String input) {
            return (landmarkStore.containsKey(input) && StringUtils.indexOfAny(input, excluded) < 0);
        }
    }

    private class LayerNotExcludedAndCheckinablePredicate implements Predicate<String> {

        private String[] excluded;

        public LayerNotExcludedAndCheckinablePredicate(String[] excluded) {
            this.excluded = excluded;
        }

        public boolean apply(String key) {
            return (landmarkStore.containsKey(key) && layerManager.isLayerCheckinable(key) && StringUtils.indexOfAny(key, excluded) < 0);
        }
    }

    private class NewerThanDatePredicate implements Predicate<ExtendedLandmark> {

        private long date;

        public NewerThanDatePredicate(long date) {
            this.date = date;
        }

        public boolean apply(ExtendedLandmark landmark) {
            return (landmark.getCreationDate() > date);
        }
    }

    private class VisibleLandmarkPredicate implements Predicate<ExtendedLandmark> {

        private ProjectionInterface projection;

        public VisibleLandmarkPredicate(ProjectionInterface projection) {
            this.projection = projection;
        }

        public boolean apply(ExtendedLandmark landmark) {
            return (projection.isVisible(landmark.getLatitudeE6(), landmark.getLongitudeE6()));
        }
    }

    private class SearchLayerTask implements Runnable {

        private String key;
        private String[] query;
        private List<ExtendedLandmark> results;
        private Predicate<ExtendedLandmark> searchPredicate;
        private Map<String, ?> searchTasks;
        private int query_length;

        public SearchLayerTask(String key, String[] query, List<ExtendedLandmark> results, Predicate<ExtendedLandmark> searchPredicate, Map<String, ?> searchTasks) {
            this.key = key;
            this.query = query;
            this.results = results;
            this.searchPredicate = searchPredicate;
            this.searchTasks = searchTasks;
            this.query_length = query.length;
        }

        public void run() {
            try {
                List<ExtendedLandmark> layer = getLandmarkStoreLayer(key);
                if (!layer.isEmpty()) {
                    boolean layerMatched = false;
                    for (int j = 0; j < query_length; j++) {
                        if (StringUtils.containsIgnoreCase(key, query[j])) {
                            results.addAll(layer);
                            layerMatched = true;
                            break;
                        }
                    }
                    if (!layerMatched) {
                        results.addAll(Collections2.filter(layer, searchPredicate));
                    }
                }
            } catch (Exception e) {
                LoggerUtils.error("SearchLayerTask.run() exception", e);
            } finally {
                searchTasks.remove(key);
            }
        }
    }

    private class CategoryLandmarkPredicate implements Predicate<ExtendedLandmark> {

        private int categoryId, subcategoryId;

        public CategoryLandmarkPredicate(int cat, int subcat) {
            this.categoryId = cat;
            this.subcategoryId = subcat;
        }

        public boolean apply(ExtendedLandmark l) {
            return ((l.getCategoryId() == categoryId && subcategoryId == -1) || (l.getSubCategoryId() == subcategoryId && subcategoryId != -1));
        }
    }

    private class FuzzySearchPredicate implements Predicate<ExtendedLandmark> {

        private String[] query_tokens;
        private CategoriesManager cm;
        private boolean searchCategories;
        private String searchTerm;

        public FuzzySearchPredicate(String[] query_tokens, boolean searchCategories, String searchTerm) {
            this.query_tokens = query_tokens;
            this.searchCategories = searchCategories;
            this.searchTerm = searchTerm;
            cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        }

        //check if landmarks contains any token
        public boolean apply(ExtendedLandmark landmark) {
            if (searchTerm != null && StringUtils.equalsIgnoreCase(searchTerm, landmark.getSearchTerm())) {
                landmark.setRevelance(99);
                return true;
            } else {
                String name = landmark.getName();
                String[] name_tokens = StringUtils.split(name, " ");
                int query_tokens_length = query_tokens.length;
                int name_tokens_length = name_tokens.length;
                for (int i = 0; i < name_tokens_length; i++) {
                    for (int j = 0; j < query_tokens_length; j++) {
                        if (StringUtils.equalsIgnoreCase(name_tokens[i], query_tokens[j])) {
                            //System.out.println("Found similar tokens: " + name_tokens[i] + "-" + query_tokens[j] + ", with score 100 " + name  + " " + landmark.getLayer());
                            landmark.setRevelance(100);
                            return true;
                        } else {
                            if (searchCategories) {
                                int catId = landmark.getCategoryId();
                                int subCatId = landmark.getSubCategoryId();
                                Category cat = cm.getCategory(catId);
                                if (catId != -1 && cat != null && StringUtils.containsIgnoreCase(cat.getCategory(), query_tokens[j])) {
                                    landmark.setRevelance(60);
                                }
                                Category subCat = cm.getSubCategory(catId, subCatId);
                                if (catId != -1 && subCatId != -1 && subCat != null && StringUtils.containsIgnoreCase(subCat.getSubcategory(), query_tokens[j])) {
                                    landmark.setRevelance(80);
                                }
                            }

                            int dist = StringUtils.getLevenshteinDistance(name_tokens[i], query_tokens[j]);
                            int percent = (int) ((1.0 - (double) dist / (double) Math.max(name_tokens[i].length(), query_tokens[j].length())) * 100.0);
                            if (percent > 60) {
                                //System.out.println("Found similar name tokens: " + name_tokens[i] + "-" + query_tokens[j] + ", with score " + percent + " " + name  + " " + landmark.getLayer());
                                landmark.setRevelance(percent);
                                return true;
                            }
                        }
                    }
                }

                String desc = landmark.getDescription();
                if (desc != null) {
                    String[] desc_tokens = StringUtils.split(desc, " ");
                    int desc_tokens_length = desc_tokens.length;
                    for (int i = 0; i < desc_tokens_length; i++) {
                        for (int j = 0; j < query_tokens_length; j++) {
                            if (StringUtils.equalsIgnoreCase(desc_tokens[i], query_tokens[j])) {
                                //System.out.println("Found similar desc tokens: " + desc_tokens[i] + "-" + query_tokens[j] + ", with score 100 " + name + " " + landmark.getLayer());
                                landmark.setRevelance(100);
                                return true;
                            } else {
                                int dist = StringUtils.getLevenshteinDistance(desc_tokens[i], query_tokens[j]);
                                int percent = (int) ((1.0 - (double) dist / (double) Math.max(desc_tokens[i].length(), query_tokens[j].length())) * 100.0);
                                if (percent > 60) {
                                    //System.out.println("Found similar tokens: " + desc_tokens[i] + "-" + query_tokens[j] + ", with score " + percent + " " + name  + " " + landmark.getLayer());
                                    landmark.setRevelance(percent);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

            landmark.setRevelance(0);
            return false;
        }
    }

    private class ExactPhraseSearchPredicate implements Predicate<ExtendedLandmark> {

        private String searchTerm;

        public ExactPhraseSearchPredicate(String searchTerm) {
            this.searchTerm = searchTerm;
        }

        public boolean apply(ExtendedLandmark landmark) {
            if (searchTerm != null && StringUtils.equalsIgnoreCase(searchTerm, landmark.getSearchTerm())) {
                landmark.setRevelance(99);
                return true;
            } else if (searchTerm != null && (StringUtils.containsIgnoreCase(landmark.getName(), searchTerm)
                    || StringUtils.containsIgnoreCase(landmark.getDescription(), searchTerm)
                    || StringUtils.containsIgnoreCase(landmark.getLayer(), searchTerm))) {
                landmark.setRevelance(100);
                return true;
            } else {
                landmark.setRevelance(0);
                return false;
            }
        }
    }

    private class PhraseWordsSearchPredicate implements Predicate<ExtendedLandmark> {

        private String[] query_tokens;
        private CategoriesManager cm;
        private boolean searchCategories;
        private String searchTerm;

        public PhraseWordsSearchPredicate(String[] query_tokens, boolean searchCategories, String searchTerm) {
            this.query_tokens = query_tokens;
            this.searchCategories = searchCategories;
            this.searchTerm = searchTerm;
            cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        }

        //check if landmarks contains all tokens
        public boolean apply(ExtendedLandmark landmark) {
            int revelance = 0;

            if (searchTerm != null && StringUtils.equalsIgnoreCase(searchTerm, landmark.getSearchTerm())) {
                revelance = 99;
            } else {
                String name = landmark.getName();
                String[] name_tokens = StringUtils.split(name, " ");
                String desc = landmark.getDescription();
                String[] desc_tokens = StringUtils.split(desc, " ");

                int query_tokens_length = query_tokens.length;
                int name_tokens_length = name_tokens.length;
                int desc_tokens_length = desc_tokens.length;

                for (int j = 0; j < query_tokens_length; j++) {
                    boolean tokenMatching = false;
                    for (int i = 0; i < name_tokens_length; i++) {
                        if (StringUtils.equalsIgnoreCase(name_tokens[i], query_tokens[j])) {
                            int percent = 100;
                            if (revelance == 0 || revelance < percent) {
                                revelance = percent;
                            }
                            tokenMatching = true;
                            break;
                        } else {
                            if (searchCategories) {
                                int catId = landmark.getCategoryId();
                                int subCatId = landmark.getSubCategoryId();
                                Category cat = cm.getCategory(catId);
                                if (catId != -1 && cat != null && StringUtils.containsIgnoreCase(cat.getCategory(), query_tokens[j])) {
                                    revelance = 60;
                                }
                                Category subCat = cm.getSubCategory(catId, subCatId);
                                if (catId != -1 && subCatId != -1 && subCat != null && StringUtils.containsIgnoreCase(subCat.getSubcategory(), query_tokens[j])) {
                                    revelance = 80;
                                }
                            }
                            int dist = StringUtils.getLevenshteinDistance(name_tokens[i], query_tokens[j]);
                            int percent = (int) ((1.0 - (double) dist / (double) Math.max(name_tokens[i].length(), query_tokens[j].length())) * 100.0);
                            if (percent > 60) {
                                //System.out.println("Found similar tokens: " + name_tokens[i] + "-" + query_tokens[j] + ", with score " + percent);
                                if (revelance == 0 || revelance < percent) {
                                    revelance = percent;
                                }
                                tokenMatching = true;
                                break;
                            }
                        }
                    }

                    if (!tokenMatching) {
                        for (int i = 0; i < desc_tokens_length; i++) {
                            if (StringUtils.equalsIgnoreCase(desc_tokens[i], query_tokens[j])) {
                                int percent = 100;
                                if (revelance == 0 || revelance < percent) {
                                    revelance = percent;
                                }
                                //tokenMatching = true;
                                break;
                            } else {
                                int dist = StringUtils.getLevenshteinDistance(desc_tokens[i], query_tokens[j]);
                                int percent = (int) ((1.0 - (double) dist / (double) Math.max(desc_tokens[i].length(), query_tokens[j].length())) * 100.0);
                                if (percent > 60) {
                                    //System.out.println("Found similar tokens: " + desc_tokens[i] + "-" + query_tokens[j] + ", with score " + percent);
                                    if (revelance == 0 || revelance < percent) {
                                        revelance = percent;
                                    }
                                    //tokenMatching = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (revelance == 0) {
                        landmark.setRevelance(0);
                        return false;
                    }
                }
            }

            landmark.setRevelance(revelance);
            return true;
        }
    }

    private class LandmarkInRadiusPredicate implements Predicate<ExtendedLandmark> {

        private ProjectionInterface projection;
        private int left, right, top, bottom, x, y;

        public LandmarkInRadiusPredicate(ProjectionInterface projection, int x, int y, int left, int right, int top, int bottom) {
            this.projection = projection;
            this.x = x;
            this.y = y;
            this.left = left;
            this.right = right;
            this.bottom = bottom;
            this.top = top;
        }

        public boolean apply(ExtendedLandmark landmark) {
            LayerPoint p = (LayerPoint) pointsPool.newObject();
            projection.toPixels(landmark.getLatitudeE6(), landmark.getLongitudeE6(), p);
            boolean result = false;
            if (x >= p.x - left && x <= p.x + right && y >= p.y - bottom && y <= p.y + top) {
                double distanceFromCenter = Math.sqrt((p.x - x) * (p.x - x) + (p.y - y) * (p.y - y));
                landmarkPaintManager.setSelectedLandmark(landmark, distanceFromCenter);
                result = true;
            }
            pointsPool.freeObject(p);
            return result;
        }
    }

    private class RecommendedDealPredicate implements Predicate<ExtendedLandmark> {

        private List<Integer> lastShownDeals;
        private double highestDiscount, highestSave;
        private int category, subcategory;

        public RecommendedDealPredicate(List<Integer> lastShownDeals, double highestDiscount, double highestSave, int category, int subcategory) {
            this.lastShownDeals = lastShownDeals;
            this.highestDiscount = highestDiscount;
            this.highestSave = highestSave;
            this.category = category;
            this.subcategory = subcategory;
        }

        public boolean apply(ExtendedLandmark landmark) {
            double discount = landmark.getDeal().getDiscount();

            return (landmark.getCategoryId() == category
                    && (subcategory == -1 || landmark.getSubCategoryId() == subcategory)
                    && ((discount < 100.0 && landmark.getDeal().getPrice() > 0.0 && discount > highestDiscount) || (discount == highestDiscount && landmark.getDeal().getSave() > highestSave && landmark.getDeal().getPrice() > 0.0))
                    && !lastShownDeals.contains(landmark.hashCode())
                    && DateTimeUtils.isAtMostNWeeksAgo(landmark.getCreationDate(), 1));
        }
    }

    private class LayerExistsPredicate implements Predicate<String> {

        public boolean apply(String layer) {
            return landmarkStore.containsKey(layer);
        }
    }

    private class FriendsCheckinsPredicate implements Predicate<ExtendedLandmark> {

        public boolean apply(ExtendedLandmark t) {
            return t.isCheckinsOrPhotos();
        }
    }
    
    private class ExtendedLandmarkToLandmarkParcelableFunction implements Function<ExtendedLandmark, LandmarkParcelable> {

        private int pos = -1;
        private double lat, lng;
        private java.util.Locale locale;

        public ExtendedLandmarkToLandmarkParcelableFunction(double lat, double lng, java.util.Locale locale) {
            this.lat = lat;
            this.lng = lng;
            this.locale = locale;
        }

        public LandmarkParcelable apply(ExtendedLandmark landmark) {
            pos++;
            return LandmarkParcelableFactory.getLandmarkParcelable(landmark, Integer.toString(pos), lat, lng, locale);
        }
    }

    private class ExtendedLandmarkToLayerPointFunction implements Function<ExtendedLandmark, LayerPoint> {

        private ProjectionInterface projection;

        public ExtendedLandmarkToLayerPointFunction(ProjectionInterface projection) {
            this.projection = projection;
        }

        public LayerPoint apply(ExtendedLandmark landmark) {
            LayerPoint point = (LayerPoint) pointsPool.newObject();
            projection.toPixels(landmark.getLatitudeE6(), landmark.getLongitudeE6(), point);
            return point;
        }
    }
}
