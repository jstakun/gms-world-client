package com.jstakun.gms.android.ui.deals;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.google.maps.GoogleInfoOverlay;
import com.jstakun.gms.android.google.maps.GoogleLandmarkOverlay;
import com.jstakun.gms.android.google.maps.GoogleLandmarkProjection;
import com.jstakun.gms.android.google.maps.GoogleMapsTypeSelector;
import com.jstakun.gms.android.google.maps.GoogleMyLocationOverlay;
import com.jstakun.gms.android.google.maps.GoogleRoutesOverlay;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerLoader;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.ui.AbstractLandmarkList;
import com.jstakun.gms.android.ui.AlertDialogBuilder;
import com.jstakun.gms.android.ui.AsyncTaskManager;
import com.jstakun.gms.android.ui.DialogManager;
import com.jstakun.gms.android.ui.HelpActivity;
import com.jstakun.gms.android.ui.IntentArrayAdapter;
import com.jstakun.gms.android.ui.Intents;
import com.jstakun.gms.android.ui.LandmarkListActivity;
import com.jstakun.gms.android.ui.StatusBarLinearLayout;
import com.jstakun.gms.android.ui.ViewResizeListener;
import com.jstakun.gms.android.utils.AdsUtils;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.LayersMessageCondition;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.MessageStack;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.ServicesUtils;
import com.jstakun.gms.android.utils.StringUtil;
import com.jstakun.gms.android.utils.UserTracker;
import java.util.List;

public class DealMap2Activity extends MapActivity implements OnClickListener {

    private static final int SHOW_MAP_VIEW = 0;
    private MapView googleMapsView;
    private MapController mapController;
    private GoogleMyLocationOverlay myLocation;
    private LayerLoader layerLoader;
    private LandmarkManager landmarkManager;
    private MessageStack messageStack;
    private AsyncTaskManager asyncTaskManager;
    private RoutesManager routesManager;
    private CategoriesManager cm;
    private Intents intents;
    private DialogManager dialogManager;
    private DealOfTheDayDialog dealOfTheDayDialog;
    private TextView statusBar;
    private View lvCloseButton, lvCallButton, lvOpenButton,
            lvView, lvSendMailButton, lvRouteButton,
            thumbnailButton, loadingImage;
    private long startingMillis;
    private boolean isStopped = false;
    private boolean initLandmarkManager = false;
    private boolean appInitialized = false;
    private boolean isRouteDisplayed = false;
    private GoogleInfoOverlay infoOverlay;
    //Handlers
    private final Handler loadingHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == MessageStack.STATUS_MESSAGE) {
                statusBar.setText(messageStack.getMessage());
            } else if (msg.what == MessageStack.STATUS_VISIBLE) {
                loadingImage.setVisibility(View.VISIBLE);
            } else if (msg.what == MessageStack.STATUS_GONE) {
                loadingImage.setVisibility(View.GONE);
            } else if (msg.what == DealOfTheDayDialog.OPEN) {
                ExtendedLandmark recommended = (ExtendedLandmark) ConfigurationManager.getInstance().getObject("dod", ExtendedLandmark.class);
                openButtonPressedAction(recommended);
            } else if (msg.what == DealOfTheDayDialog.CALL) {
                ExtendedLandmark recommended = (ExtendedLandmark) ConfigurationManager.getInstance().getObject("dod", ExtendedLandmark.class);
                callButtonPressedAction(recommended);
            } else if (msg.what == DealOfTheDayDialog.ROUTE) {
                ExtendedLandmark recommended = (ExtendedLandmark) ConfigurationManager.getInstance().getObject("dod", ExtendedLandmark.class);
                loadRoutePressedAction(recommended);
            } else if (msg.what == DealOfTheDayDialog.SEND_MAIL) {
                sendMessageAction();
            } else if (msg.what == LayerLoader.LAYER_LOADED) {
                googleMapsView.postInvalidate();
            } else if (msg.what == LayerLoader.ALL_LAYERS_LOADED) {
                showRecommendedDeal(false);
                if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER)) {
                    asyncTaskManager.executeUploadImageTask(MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLatitudeE6()),
                            MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLongitudeE6()), intents.takeScreenshot());
                }
            } else if (msg.what == GoogleLandmarkOverlay.SHOW_LANDMARK_DETAILS) {
                landmarkDetailsAction();
            } else if (msg.what == SHOW_MAP_VIEW) {
                View loading = findViewById(R.id.loadingWidgetP);
                View mapCanvas = findViewById(R.id.mapCanvasWidgetM);
                loading.setVisibility(View.GONE);
                mapCanvas.setVisibility(View.VISIBLE);
                if (lvView == null || !lvView.isShown()) {
                    getActionBar().show();
                }
            } else if (msg.what == AsyncTaskManager.SHOW_ROUTE_MESSAGE) {
                showRouteAction((String)msg.obj);
            } else if (msg.what == GoogleMyLocationOverlay.UPDATE_LOCATION) {
                Location location = (Location) msg.obj;
                if (landmarkManager != null) {
                    landmarkManager.addLandmark(location.getLatitude(), location.getLongitude(), (float)location.getAltitude(), Locale.getMessage(R.string.Your_Location), Long.toString(System.currentTimeMillis()), Commons.MY_POSITION_LAYER, false);
                }
            }
        }
    };
    private final Runnable gpsRunnable = new Runnable() {
        public void run() {
            GeoPoint location = getMyLocation();
            if (location != null && !appInitialized) {
                initOnLocationChanged(location);
            } else {
                if (ConfigurationManager.getInstance().isDefaultCoordinate()) {
                    //start only if helpactivity not on top
                    if (!ConfigurationManager.getInstance().containsObject(HelpActivity.HELP_ACTIVITY_SHOWN, String.class)) {
                        intents.startPickLocationActivity();
                    }
                } else if (!appInitialized) {
                    double lat = ConfigurationManager.getInstance().getDouble(ConfigurationManager.LATITUDE);
                    double lng = ConfigurationManager.getInstance().getDouble(ConfigurationManager.LONGITUDE);
                    GeoPoint loc = new GeoPoint(MathUtils.coordDoubleToInt(lat), MathUtils.coordDoubleToInt(lng));
                    initOnLocationChanged(loc);
                }
            }
        }
    };
    
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        ConfigurationManager.getInstance().setContext(getApplicationContext());

        startingMillis = System.currentTimeMillis();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        OsUtil.setDisplayType(getResources().getConfiguration());
        getActionBar().hide();

        setContentView(R.layout.mapcanvasview);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        initComponents();
    }

    @Override
    protected boolean isRouteDisplayed() {
        return isRouteDisplayed;
    }

    private void initComponents() {

        statusBar = (TextView) findViewById(R.id.statusBar);
        loadingImage = findViewById(R.id.loadingAnim);
        lvView = findViewById(R.id.lvView);

        lvCloseButton = findViewById(R.id.lvCloseButton);
        lvOpenButton = findViewById(R.id.lvOpenButton);
        lvSendMailButton = findViewById(R.id.lvSendMailButton);
        lvCallButton = findViewById(R.id.lvCallButton);
        lvRouteButton = findViewById(R.id.lvCarRouteButton);
        thumbnailButton = findViewById(R.id.thumbnailButton);

        lvCloseButton.setOnClickListener(this);
        lvOpenButton.setOnClickListener(this);
        lvSendMailButton.setOnClickListener(this);
        lvCallButton.setOnClickListener(this);
        lvRouteButton.setOnClickListener(this);
        thumbnailButton.setOnClickListener(this);

        googleMapsView = (MapView) findViewById(R.id.mapCanvas);
        googleMapsView.setBuiltInZoomControls(true);

        infoOverlay = new GoogleInfoOverlay();

        StatusBarLinearLayout bottomPanel = (StatusBarLinearLayout) findViewById(R.id.bottomPanel);
        ViewResizeListener viewResizeListener = new ViewResizeListener() {
            @Override
            public void onResize(int id, int xNew, int yNew, int xOld, int yOld) {
                infoOverlay.setFontSize(yNew);
            }
        };
        bottomPanel.setViewResizeListener(viewResizeListener);

        myLocation = new GoogleMyLocationOverlay(this, googleMapsView, loadingHandler, getResources().getDrawable(R.drawable.ic_maps_indicator_current_position));

        mapController = googleMapsView.getController();
        mapController.setZoom(ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM));

        landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
        if (landmarkManager == null) {
            LoggerUtils.debug("Creating LandmarkManager...");
            landmarkManager = new LandmarkManager();
            initLandmarkManager = true;
        }

        asyncTaskManager = (AsyncTaskManager) ConfigurationManager.getInstance().getObject("asyncTaskManager", AsyncTaskManager.class);
        if (asyncTaskManager == null) {
            LoggerUtils.debug("Initializing AsyncTaskManager...");
            asyncTaskManager = new AsyncTaskManager(this, landmarkManager);
            ConfigurationManager.getInstance().putObject("asyncTaskManager", asyncTaskManager);
            //check if newer version available
            asyncTaskManager.executeNewVersionCheckTask();
        }

        intents = new Intents(this, landmarkManager, asyncTaskManager);

        cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        if (cm == null || !cm.isInitialized()) {
            LoggerUtils.debug("Loading deal categories...");
            cm = new CategoriesManager();
            ConfigurationManager.getInstance().putObject(ConfigurationManager.DEAL_CATEGORIES, cm);
            asyncTaskManager.executeDealCategoryLoaderTask(cm, true);
        }

        dialogManager = new DialogManager(this, intents, asyncTaskManager, landmarkManager, null, null);

        GeoPoint mapCenter = (GeoPoint) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, GeoPoint.class);

        if (mapCenter != null) {
            initOnLocationChanged(mapCenter);
        } else {
            loadingHandler.postDelayed(gpsRunnable, ConfigurationManager.FIVE_SECONDS);
            myLocation.runOnFirstFix(new Runnable() {
                public void run() {
                    if (!appInitialized) {
                        initOnLocationChanged(getMyLocation());
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        UserTracker.getInstance().trackEvent("MenuClicks", item.getTitle().toString(), "", 0);
        switch (item.getItemId()) {
            case R.id.hotDeals:
                intents.startDealsOfTheDayIntent(getMyPosition(), null);
                break;
            case R.id.newestDeals:
                final String[] excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER, Commons.HOTWIRE_LAYER, Commons.LOCAL_LAYER};
                intents.startNewestLandmarkIntent(getMyPosition(), excluded, AbstractLandmarkList.ORDER_BY_CAT_STATS, 2);
                break;
            case R.id.nearbyDeals:
                if (!lvView.isShown()) {
                    intents.showNearbyLandmarks(getMyPosition(), new GoogleLandmarkProjection(googleMapsView), AbstractLandmarkList.ORDER_BY_CAT_STATS);
                }
                break;
            case R.id.settings:
                intents.startSettingsActivity(SettingsActivity.class);
                break;
            case R.id.listMode:
                intents.startCategoryListActivity(googleMapsView.getLatitudeSpan(), googleMapsView.getLongitudeSpan(),
                        googleMapsView.getMapCenter().getLatitudeE6(), googleMapsView.getMapCenter().getLongitudeE6(), -1, -1, DealCategoryListActivity.class);
                break;
            case R.id.pickMyPos:
                intents.startPickLocationActivity();
                break;
            case R.id.showMyPos:
                showMyPositionAction(true);
                break;
            case R.id.search:
                onSearchRequested();
                break;
            case R.id.refreshLayers:
                intents.loadLayersAction(true, null, false, false, layerLoader,
                        MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLatitudeE6()),
                        MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLongitudeE6()),
                        googleMapsView.getZoomLevel());
                break;
            case R.id.showMyLandmarks:
                intents.startMyLandmarksIntent(getMyPosition());
                break;
            case R.id.recentLandmarks:
                intents.startRecentLandmarksIntent(getMyPosition());
                break;
            case R.id.exit:
                dialogManager.showAlertDialog(AlertDialogBuilder.EXIT_DIALOG, null, null);
                break;
            case android.R.id.home:
                dialogManager.showAlertDialog(AlertDialogBuilder.EXIT_DIALOG, null, null);
                break;
            case R.id.about:
                dialogManager.showAlertDialog(AlertDialogBuilder.INFO_DIALOG, null, null);
                break;
            case R.id.releaseNotes:
                intents.startHelpActivity();
                break;
            case R.id.showDoD:
                showRecommendedDeal(true);
                break;
            case R.id.discoverPlaces:
                intents.startActionViewIntent(ConfigurationManager.LM_MARKET_URL);
                break;
            case R.id.events:
                intents.startCalendarActivity(getMyPosition());
                break;
            case R.id.rateUs:
                dialogManager.showAlertDialog(AlertDialogBuilder.RATE_US_DIALOG, null, null);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private synchronized void initOnLocationChanged(GeoPoint location) {
        if (!appInitialized) {
            mapController.setCenter(location);
            if (initLandmarkManager) {
                UserTracker.getInstance().sendMyLocation();
                ConfigurationManager.getInstance().putObject("landmarkManager", landmarkManager);
                landmarkManager.initialize(ConfigurationManager.getInstance().getLandmarkDatabase(), Commons.LOCAL_LAYER, Commons.ROUTES_LAYER, Commons.MY_POSITION_LAYER, Commons.COUPONS_LAYER,
                		Commons.HOTELS_LAYER, Commons.GROUPON_LAYER, Commons.FOURSQUARE_MERCHANT_LAYER, Commons.YELP_LAYER);
            }

            GoogleLandmarkOverlay landmarkOverlay = new GoogleLandmarkOverlay(landmarkManager, loadingHandler);//, new String[]{LayerManager.ROUTES_LAYER});
            googleMapsView.getOverlays().add(landmarkOverlay);
            //must be on top of other overlays
            googleMapsView.getOverlays().add(infoOverlay);
            googleMapsView.getOverlays().add(myLocation);
            
            routesManager = ConfigurationManager.getInstance().getRoutesManager();
            if (routesManager == null) {
                LoggerUtils.debug("Creating RoutesManager...");
                routesManager = new RoutesManager();
                ConfigurationManager.getInstance().putObject("routesManager", routesManager);
            }

            messageStack = ConfigurationManager.getInstance().getMessageStack();
            if (messageStack == null) {
                LoggerUtils.debug("Creating MessageStack...");
                messageStack = new MessageStack(new LayersMessageCondition());
                ConfigurationManager.getInstance().putObject("messageStack", messageStack);
            }
            messageStack.setHandler(loadingHandler);

            layerLoader = (LayerLoader) ConfigurationManager.getInstance().getObject("layerLoader", LayerLoader.class);
            if (layerLoader == null || landmarkManager.getLayerManager().isEmpty()) {
                LoggerUtils.debug("Creating LayerLoader...");
                layerLoader = new LayerLoader(landmarkManager, messageStack);
                LoggerUtils.debug("Loading Layers...");
                intents.loadLayersAction(true, null, false, false, layerLoader,
                        MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLatitudeE6()),
                        MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLongitudeE6()),
                        googleMapsView.getZoomLevel());
                ConfigurationManager.getInstance().putObject("layerLoader", layerLoader);
            } else {
                //load existing layers
                if (layerLoader.isLoading()) {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_VISIBLE);
                } else {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_GONE);
                }
                loadingHandler.sendEmptyMessage(MessageStack.STATUS_MESSAGE);
                googleMapsView.postInvalidate();
            }
            layerLoader.setRepaintHandler(loadingHandler);

            loadingHandler.sendEmptyMessage(SHOW_MAP_VIEW);
            appInitialized = true;
        }
    }

    private void showSelectedLandmark(int id) {
        if (id >= 0) {
            ExtendedLandmark selectedLandmark = landmarkManager.getLandmarkToFocusQueueSelectedLandmark(id);
            if (selectedLandmark != null) {
                landmarkManager.setSelectedLandmark(selectedLandmark);
                landmarkManager.clearLandmarkOnFocusQueue();
                landmarkDetailsAction();
            } else {
                intents.showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
            }
        } else {
            intents.showInfoToast(Locale.getMessage(R.string.Landmark_search_empty_result));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == Intents.INTENT_MULTILANDMARK) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);

                if (action.equals("load")) {
                    int id = Integer.parseInt(ids);
                    showSelectedLandmark(id);
                }
            }
        } else if (requestCode == Intents.INTENT_PICKLOCATION) {
            if (resultCode == RESULT_OK) {
                String lats = intent.getStringExtra("lat");
                String lngs = intent.getStringExtra("lng");
                String names = intent.getStringExtra("name");
                double lat = Double.parseDouble(lats);
                double lng = Double.parseDouble(lngs);

                GeoPoint location = new GeoPoint(MathUtils.coordDoubleToInt(lat), MathUtils.coordDoubleToInt(lng));
                if (!appInitialized) {
                    initOnLocationChanged(location);
                } else {
                    pickPositionAction(location, true, false, true);
                }

                landmarkManager.addLandmark(lat, lng, 0.0f, StringUtil.formatCommaSeparatedString(names), "", Commons.LOCAL_LAYER, true);

            } else if (resultCode == RESULT_CANCELED && !appInitialized) {
                //String name = intent.getStringExtra("name");
                //String message = intent.getStringExtra("message");
                //intents.showInfoToast(Locale.getMessage(R.string.Pick_location_failed_error, name, message));
                ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
                intents.showInfoToast(Locale.getMessage(R.string.Pick_location_default, landmark.getName()));
                GeoPoint location = new GeoPoint(landmark.getLatitudeE6(), landmark.getLongitudeE6());
                initOnLocationChanged(location);
            } else if (resultCode == RESULT_CANCELED && intent.hasExtra("message") && intent.hasExtra("name")) {
                String name = intent.getStringExtra("name");
                String message = intent.getStringExtra("message");
                intents.showInfoToast(Locale.getMessage(R.string.Pick_location_failed_error, name, message));
            } else if (resultCode != RESULT_CANCELED) { //if (!appInitialized) {
                intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
            }
        } else if (requestCode == Intents.INTENT_MYLANDMARKS) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);
                int id = Integer.parseInt(ids);

                if (action.equals("load")) {
                    ExtendedLandmark l = landmarkManager.getPhoneLandmark(id);
                    if (l != null) {
                        GeoPoint location = new GeoPoint(l.getLatitudeE6(), l.getLongitudeE6());
                        pickPositionAction(location, true, true, true);
                    }
                } else if (action.equals("delete")) {
                    //delete landmark
                    landmarkManager.deletePhoneLandmark(id);
                    intents.showInfoToast(Locale.getMessage(R.string.Landmark_deleted));
                }
            }
        } else if (requestCode == Intents.INTENT_CALENDAR) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);

                if (action.equals("load")) {
                    int id = Integer.parseInt(ids);
                    showSelectedLandmark(id);
                }
            }
        } else {
            intents.processActivityResult(requestCode, resultCode, intent, getMyPosition(), null, null, -1, null);
        }
    }

    public void onClick(View v) {
        if (v == lvCloseButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CloseSelectedDealView", "", 0);
            lvView.setVisibility(View.GONE);
            getActionBar().show();
            landmarkManager.clearLandmarkOnFocusQueue();
            landmarkManager.setSelectedLandmark(null);
            landmarkManager.setSeletedLandmarkUI();
        } else if (v == lvOpenButton || v == thumbnailButton) {
            ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenSelectedDealURL", selectedLandmark.getLayer(), 0);
            openButtonPressedAction(landmarkManager.getSeletedLandmarkUI());
        } else if (v == lvCallButton) {
            ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CallSelectedDeal", selectedLandmark.getLayer(), 0);
            callButtonPressedAction(landmarkManager.getSeletedLandmarkUI());
        } else if (v == lvRouteButton) {
            ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowRouteSelectedDeal", selectedLandmark.getLayer(), 0);
            loadRoutePressedAction(landmarkManager.getSeletedLandmarkUI());
        } else if (v == lvSendMailButton) {
            ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShareSelectedDeal", selectedLandmark.getLayer(), 0);
            sendMessageAction();
        }
    }

    @Override
    public boolean onSearchRequested() {
        if (appInitialized) {
            intents.startSearchActivity(googleMapsView.getLatitudeSpan(), googleMapsView.getLongitudeSpan(),
                    googleMapsView.getMapCenter().getLatitudeE6(), googleMapsView.getMapCenter().getLongitudeE6(), -1, true);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LoggerUtils.debug("onResume");

        //skyhook.enableMyLocation();
        myLocation.enableMyLocation();

        isStopped = false;

        GoogleMapsTypeSelector.selectMapType(googleMapsView);

        asyncTaskManager.setActivity(this);

        Integer searchQueryResult = (Integer) ConfigurationManager.getInstance().removeObject(ConfigurationManager.SEARCH_QUERY_RESULT, Integer.class);
        if (searchQueryResult != null) {
            showSelectedLandmark(searchQueryResult);
        } else if (landmarkManager != null && landmarkManager.getSeletedLandmarkUI() != null) {
            getActionBar().hide();
            ExtendedLandmark landmark = landmarkManager.getSeletedLandmarkUI();
            intents.showLandmarkDetailsView(landmark, lvView, getMyPosition(), true);
        }

        if (ConfigurationManager.getInstance().containsObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class)) {
            int type = (Integer) ConfigurationManager.getInstance().getObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            IntentArrayAdapter arrayAdapter = null;
            if (type == AlertDialogBuilder.DEAL_OF_THE_DAY_DIALOG) {
                showRecommendedDeal(true);
            } else {
                if (type == AlertDialogBuilder.SHARE_INTENTS_DIALOG) {
                    List<ResolveInfo> intentList = intents.getSendIntentsList();
                    if (!intentList.isEmpty()) {
                        arrayAdapter = new IntentArrayAdapter(this, intentList);
                    }
                }
                dialogManager.showAlertDialog(type, arrayAdapter, null);
            }
        }

        intents.checkAppVersion();

        if (ConfigurationManager.getInstance().removeObject(HelpActivity.HELP_ACTIVITY_SHOWN, String.class) != null) {
            GeoPoint mapCenter = (GeoPoint) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, GeoPoint.class);
            if (mapCenter == null) {
                loadingHandler.removeCallbacks(gpsRunnable);
                loadingHandler.post(gpsRunnable);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LoggerUtils.debug("onPause");
        myLocation.disableMyLocation();
        //skyhook.disableMyLocation();
    }

    @Override
    public void onStart() {
        super.onStart();
        Object networkStatus = ConfigurationManager.getInstance().getObject("NetworkStatus", Object.class);
        boolean networkActive = ServicesUtils.isNetworkActive(this);
        if (networkStatus == null && !networkActive) {
            dialogManager.showAlertDialog(AlertDialogBuilder.NETWORK_ERROR_DIALOG, null, null);
            ConfigurationManager.getInstance().putObject("NetworkStatus", new Object());
        }

        Object rateDialogStatus = ConfigurationManager.getInstance().getObject("rateDialogStatus", Object.class);
        if (rateDialogStatus == null && ConfigurationManager.getInstance().isOff(ConfigurationManager.APP_RATED) && networkActive) {
            int useCount = ConfigurationManager.getInstance().getInt(ConfigurationManager.USE_COUNT);
            //show rate us dialog
            if (useCount > 0 && (useCount % 10) == 0) {
                dialogManager.showAlertDialog(AlertDialogBuilder.RATE_US_DIALOG, null, null);
                ConfigurationManager.getInstance().putInteger(ConfigurationManager.USE_COUNT, useCount + 1);
                ConfigurationManager.getInstance().putObject("rateDialogStatus", new Object());
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LoggerUtils.debug("onStop");
        isStopped = true;
        int type = dialogManager.dismissDialog();
        if (type == AlertDialogBuilder.DEAL_OF_THE_DAY_DIALOG) {
            dealOfTheDayDialog.dismiss();
        } else if (dealOfTheDayDialog != null && dealOfTheDayDialog.isShowing()) {
            ConfigurationManager.getInstance().putObject(AlertDialogBuilder.OPEN_DIALOG, AlertDialogBuilder.DEAL_OF_THE_DAY_DIALOG);
            dealOfTheDayDialog.dismiss();
        }
        UserTracker.getInstance().stopSession();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LoggerUtils.debug("onDestroy");
        //if (!appAbort) {
        if (ConfigurationManager.getInstance().isClosing()) {
            hardClose();
        } else {
            softClose();
            ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, googleMapsView.getMapCenter());
        }
        AdsUtils.destroyAdView(this);
        //}
        System.gc();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //System.out.println("Key pressed in activity: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (lvView.isShown()) {
                landmarkManager.clearLandmarkOnFocusQueue();
                landmarkManager.setSelectedLandmark(null);
                landmarkManager.setSeletedLandmarkUI();
                lvView.setVisibility(View.GONE);
                getActionBar().show();
            } else {
                dialogManager.showAlertDialog(AlertDialogBuilder.EXIT_DIALOG, null, null);
            }
            //System.out.println("key back pressed in activity");
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            try {
                landmarkDetailsAction();
            } catch (Exception e) {
                LoggerUtils.error("GMSClientMainActivity.onKeyDown error", e);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_8) { //key *
            mapController.zoomIn();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_0) {
            mapController.zoomOut();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void softClose() {
        ConfigurationManager.getInstance().putInteger(ConfigurationManager.ZOOM, googleMapsView.getZoomLevel());
        ConfigurationManager.getInstance().putDouble(ConfigurationManager.LATITUDE, MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLatitudeE6()));
        ConfigurationManager.getInstance().putDouble(ConfigurationManager.LONGITUDE, MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLongitudeE6()));
        ConfigurationManager.getInstance().saveConfiguration(false);
    }

    private void hardClose() {
        if (layerLoader != null && layerLoader.isLoading()) {
            layerLoader.stopLoading();
        }

        loadingHandler.removeCallbacks(gpsRunnable);

        ConfigurationManager.getInstance().putString(ConfigurationManager.LAST_STARTING_DATE, Long.toString(startingMillis));
        softClose();

        //SuggestionProviderUtil.clearHistory();

        HttpUtils.closeConnManager();
        IconCache.getInstance().clearAll();
        landmarkManager.clearLandmarkStore();

        ConfigurationManager.getInstance().closeAllDatabases();

        ConfigurationManager.getInstance().clearObjectCache();

        PersistenceManagerFactory.getFileManager().clearImageCache(System.currentTimeMillis() - DateTimeUtils.ONE_MONTH);
    }

    private void landmarkDetailsAction() {
        ExtendedLandmark selectedLandmark = landmarkManager.getLandmarkOnFocus();

        if (selectedLandmark != null) {
            if (!selectedLandmark.getLayer().equals(Commons.MULTI_LANDMARK)) {
                landmarkManager.setSeletedLandmarkUI();
            }

            if (selectedLandmark.getLayer().equals(Commons.MULTI_LANDMARK)) {
                //System.out.println("2 -----------------------------------------");
                intents.startMultiLandmarkIntent(getMyPosition(), AbstractLandmarkList.ORDER_BY_CAT_STATS);
            } else {
                UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowSelectedDealView", selectedLandmark.getLayer(), selectedLandmark.getCategoryId());
                cm.addSubCategoryStats(selectedLandmark.getCategoryId(), selectedLandmark.getSubCategoryId());
                getActionBar().hide();
                intents.showLandmarkDetailsView(selectedLandmark, lvView, getMyPosition(), true);
                GeoPoint g = new GeoPoint(selectedLandmark.getLatitudeE6(), selectedLandmark.getLongitudeE6());
                mapController.animateTo(g);

                if (selectedLandmark.getLayer().equals(Commons.LOCAL_LAYER)) {
                    intents.loadLayersAction(true, null, false, false, layerLoader,
                            MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLatitudeE6()),
                            MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLongitudeE6()),
                            googleMapsView.getZoomLevel());
                }
            }
        } else {
            LoggerUtils.debug(Locale.getMessage(R.string.Landmark_opening_error));
        }
    }

    private GeoPoint getMyLocation() {
        Location location = ConfigurationManager.getInstance().getLocation();
        if (location != null) {
            return new GeoPoint(MathUtils.coordDoubleToInt(location.getLatitude()),
                    MathUtils.coordDoubleToInt(location.getLongitude()));
        } else {
            //return null;
            return myLocation.getMyLocation();
        }
    }

    private double[] getMyPosition() {
        return landmarkManager.getMyPosition(googleMapsView.getMapCenter().getLatitudeE6(),
                googleMapsView.getMapCenter().getLongitudeE6());
    }

    private void loadRoutePressedAction(ExtendedLandmark landmark) {
        asyncTaskManager.executeRouteServerLoadingTask(loadingHandler, false, landmark);
    }

    private void callButtonPressedAction(ExtendedLandmark landmark) {
        intents.startPhoneCallActivity(landmark);
    }

    private void openButtonPressedAction(ExtendedLandmark landmark) {
        intents.startLandmarkDetailsActivity(landmarkManager.getLandmarkURL(landmark), landmark.getName());
    }

    private void sendMessageAction() {
        intents.shareLandmarkAction(dialogManager);
    }

    private void pickPositionAction(GeoPoint newCenter, boolean loadLayers, boolean animate, boolean clearMap) {
        mapController.setCenter(newCenter);
        if (loadLayers) {
            intents.loadLayersAction(true, null, clearMap, false, layerLoader,
                    MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLatitudeE6()),
                    MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLongitudeE6()),
                    googleMapsView.getZoomLevel());
        }
    }

    private void showRouteAction(String routeKey) {
        if (routesManager.containsRoute(routeKey)) {
            GoogleRoutesOverlay routeOverlay = new GoogleRoutesOverlay(this, routesManager, routeKey);
            googleMapsView.getOverlays().add(routeOverlay);
            isRouteDisplayed = true;
            googleMapsView.postInvalidate();
        }
    }

    private void showMyPositionAction(boolean loadLayers) {
        GeoPoint g = getMyLocation();
        if (g != null) {
            boolean isVisible = false;
            boolean clearLandmarks = false;

            GoogleLandmarkProjection projection = new GoogleLandmarkProjection(googleMapsView);
            if (projection.isVisible(g.getLatitudeE6(), g.getLongitudeE6())) {
                isVisible = true;
            }

            if (!isVisible) {
                GeoPoint mapCenter = googleMapsView.getMapCenter();

                clearLandmarks = intents.isClearLandmarksRequired(projection, mapCenter.getLatitudeE6(), mapCenter.getLongitudeE6(),
                        g.getLatitudeE6(), g.getLongitudeE6());
            }

            mapController.animateTo(g);

            if (loadLayers && !isVisible) {
                mapController.setCenter(g);
                intents.loadLayersAction(true, null, clearLandmarks, false, layerLoader,
                        MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLatitudeE6()),
                        MathUtils.coordIntToDouble(googleMapsView.getMapCenter().getLongitudeE6()),
                        googleMapsView.getZoomLevel());
            }
        } else {
            intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
        }
    }

    private void showRecommendedDeal(boolean forceToShow) {
        ExtendedLandmark recommended = (ExtendedLandmark) ConfigurationManager.getInstance().getObject("dod", ExtendedLandmark.class);

        if (recommended == null) {
            //System.out.println("Top subcategory stats: " + cm.getTopCategory() + " " + cm.getTopSubCategory() + " " + cm.getTopSubCategoryStats());
            if (cm.getTopSubCategoryStats() > ConfigurationManager.getInstance().getInt(ConfigurationManager.DEAL_RECOMMEND_CAT_STATS)
                    && (ConfigurationManager.getInstance().isOn(ConfigurationManager.SHOW_DEAL_OF_THE_DAY) || forceToShow)) {
                //System.out.println(cm.getTopCategory() + " " + cm.getTopSubCategory());
                recommended = landmarkManager.findRecommendedLandmark(cm.getTopCategory(), cm.getTopSubCategory());
                if (recommended != null) {
                    ConfigurationManager.getInstance().putObject("dod", recommended);
                }
            }
        }

        if (recommended != null) {
            landmarkManager.setSelectedLandmark(recommended);
            dealOfTheDayDialog = new DealOfTheDayDialog(this, recommended, getMyPosition(), loadingHandler, intents);
            ConfigurationManager.getInstance().putObject(AlertDialogBuilder.OPEN_DIALOG, AlertDialogBuilder.DEAL_OF_THE_DAY_DIALOG);
            if (!isStopped) {
                dealOfTheDayDialog.show();
            }
            if (!forceToShow) {
                try {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    //300 millis
                    v.vibrate(300);
                } catch (Exception e) {
                }
            } else {
                GeoPoint location = new GeoPoint(recommended.getLatitudeE6(), recommended.getLongitudeE6());
                pickPositionAction(location, false, false, false);
            }
        } else if (forceToShow) {
            //System.out.println("recommended == null");
            intents.showInfoToast(Locale.getMessage(R.string.noDodAvailable));
        } //else {
        //comment out
        //intents.showInfoToast("Recommended == null\n"
        //        + "SubCategoryStats: " + cm.getTopSubCategoryStats() + " " + cm.getTopCategory() + " " + cm.getTopSubCategory() + "\n"
        //        + "showDealOfTheDay: " + ConfigurationManager.getInstance().isOn(ConfigurationManager.SHOW_DEAL_OF_THE_DAY) + "\n"
        //        + "shownDeals: " + ConfigurationManager.getInstance().getString(ConfigurationManager.RECOMMENDED_DEALS_SHOWN));
        //}
    }
}
