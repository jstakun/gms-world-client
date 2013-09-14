package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.jstakun.gms.android.data.FavouritesDAO;
import com.jstakun.gms.android.data.FavouritesDbDataSource;
import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerLoader;
import com.jstakun.gms.android.location.LocationServicesManager;
import com.jstakun.gms.android.osm.maps.OsmInfoOverlay;
import com.jstakun.gms.android.osm.maps.OsmLandmarkOverlay;
import com.jstakun.gms.android.osm.maps.OsmLandmarkProjection;
import com.jstakun.gms.android.osm.maps.OsmMapsTypeSelector;
import com.jstakun.gms.android.osm.maps.OsmMyLocationOverlay;
import com.jstakun.gms.android.osm.maps.OsmRoutesOverlay;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.routes.RoutesManager;
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
import com.jstakun.gms.android.utils.ProjectionInterface;
import com.jstakun.gms.android.utils.ServicesUtils;
import com.jstakun.gms.android.utils.StringUtil;
import com.jstakun.gms.android.utils.UserTracker;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.Overlay;

public class GMSClientOSMMainActivity extends Activity implements OnClickListener {

    private static final int SHOW_MAP_VIEW = 0;
    private MapView mapView;
    private MapController mapController;
    private MyLocationOverlay myLocation;
    private OsmInfoOverlay infoOverlay;
    private LayerLoader layerLoader;
    private LandmarkManager landmarkManager;
    private MessageStack messageStack;
    private AsyncTaskManager asyncTaskManager;
    private RoutesManager routesManager;
    private RouteRecorder routeRecorder;
    private CheckinManager checkinManager;
    private CategoriesManager cm;
    private Intents intents;
    private DialogManager dialogManager;
    private TextView statusBar;
    private View lvCloseButton, lvCallButton, lvCommentButton,
            lvOpenButton, lvView, lvSendMailButton,
            newestButton, listButton, layersButton,
            lvActionButton, lvRouteButton, thumbnailButton, loadingImage;
    private long startingMillis;
    private boolean appInitialized = false;
    private boolean initLandmarkManager = false;
    //Handlers
    private Handler loadingHandler;
    private final Runnable gpsRunnable = new Runnable() {
        public void run() {
            GeoPoint location = LocationServicesManager.getMyLocation();
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
    //OnClickListener
    private DialogInterface.OnClickListener trackMyPosListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            String filename = followMyPositionAction();

            LocationServicesManager.enableCompass();

            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            if (filename != null) {
                dialogManager.showAlertDialog(AlertDialogBuilder.SAVE_ROUTE_DIALOG, null, new SpannableString(Locale.getMessage(R.string.Routes_Recording_Question, filename)));
            } else if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)
                    && !ServicesUtils.isGpsActive(ConfigurationManager.getInstance().getContext())) {
                dialogManager.showAlertDialog(AlertDialogBuilder.LOCATION_ERROR_DIALOG, null, null);
            }
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        LoggerUtils.debug("onCreate");
        LoggerUtils.debug("GMSClientMainActivity.onCreate called...");
        startingMillis = System.currentTimeMillis();

        UserTracker.getInstance().startSession(this);
        //TODO comment in production
        //UserTracker.getInstance().setDebug(true);
        //UserTracker.getInstance().setDryRun(true);
        //
        UserTracker.getInstance().trackActivity(getClass().getName());

        ConfigurationManager.getInstance().setContext(getApplicationContext());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        loadingHandler = new LoadingHandler(this);
        
        setContentView(R.layout.osmdroidcanvasview_1);
        mapView = (MapView) findViewById(R.id.mapCanvas);
        mapView.setMultiTouchControls(true);
        myLocation = new OsmMyLocationOverlay(this, mapView, loadingHandler);
        LocationServicesManager.initLocationServicesManager(this, loadingHandler, myLocation);
        infoOverlay = new OsmInfoOverlay(this);

        initComponents();
    }

    private void initComponents() {

        statusBar = (TextView) findViewById(R.id.statusBar);
        loadingImage = findViewById(R.id.loadingAnim);
        lvView = findViewById(R.id.lvView);

        lvActionButton = findViewById(R.id.lvActionButton);
        lvCloseButton = findViewById(R.id.lvCloseButton);
        lvOpenButton = findViewById(R.id.lvOpenButton);
        lvCommentButton = findViewById(R.id.lvCommentButton);
        lvCallButton = findViewById(R.id.lvCallButton);
        lvRouteButton = findViewById(R.id.lvCarRouteButton);
        lvSendMailButton = findViewById(R.id.lvSendMailButton);
        thumbnailButton = findViewById(R.id.thumbnailButton);

        lvActionButton.setOnClickListener(this);
        lvCloseButton.setOnClickListener(this);
        lvOpenButton.setOnClickListener(this);
        lvCommentButton.setOnClickListener(this);
        lvCallButton.setOnClickListener(this);
        lvRouteButton.setOnClickListener(this);
        lvSendMailButton.setOnClickListener(this);
        thumbnailButton.setOnClickListener(this);

        newestButton = findViewById(R.id.newestButton);
        listButton = findViewById(R.id.listButton);
        layersButton = findViewById(R.id.layersButton);

        newestButton.setOnClickListener(this);
        listButton.setOnClickListener(this);
        layersButton.setOnClickListener(this);

        mapController = mapView.getController();

        setBuiltInZoomControls(true);

        StatusBarLinearLayout bottomPanel = (StatusBarLinearLayout) findViewById(R.id.bottomPanel);
        ViewResizeListener viewResizeListener = new ViewResizeListener() {
            @Override
            public void onResize(int id, int xNew, int yNew, int xOld, int yOld) {
                infoOverlay.setFontSize(yNew);
            }
        };
        bottomPanel.setViewResizeListener(viewResizeListener);

        GeoPoint mapCenter = (GeoPoint) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, GeoPoint.class);

        if (mapCenter == null) {
            loadingHandler.postDelayed(gpsRunnable, ConfigurationManager.FIVE_SECONDS);
        }

        mapController.setZoom(ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM));

        landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
        if (landmarkManager == null) {
            LoggerUtils.debug("Creating LandmarkManager...");
            landmarkManager = new LandmarkManager();
            initLandmarkManager = true;
        }

        asyncTaskManager = (AsyncTaskManager) ConfigurationManager.getInstance().getObject("asyncTaskManager", AsyncTaskManager.class);
        if (asyncTaskManager == null) {
            LoggerUtils.debug("Creating AsyncTaskManager...");
            asyncTaskManager = new AsyncTaskManager(this, landmarkManager);
            ConfigurationManager.getInstance().putObject("asyncTaskManager", asyncTaskManager);

            //check if newer version available
            asyncTaskManager.executeNewVersionCheckTask();
        }

        intents = new Intents(this, landmarkManager, asyncTaskManager);

        checkinManager = new CheckinManager(asyncTaskManager, this);

        cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        if (cm == null || !cm.isInitialized()) {
            LoggerUtils.debug("Loading deal categories...");
            cm = new CategoriesManager();
            ConfigurationManager.getInstance().putObject(ConfigurationManager.DEAL_CATEGORIES, cm);
            asyncTaskManager.executeDealCategoryLoaderTask(cm, false);
        }

        dialogManager = new DialogManager(this, intents, asyncTaskManager, landmarkManager, checkinManager, trackMyPosListener);

        if (mapCenter != null) {
            initOnLocationChanged(mapCenter);
        } else {
            Runnable r = new Runnable() {
                public void run() {
                    if (!appInitialized) {
                        initOnLocationChanged(LocationServicesManager.getMyLocation());
                    }
                }
            };
            LocationServicesManager.runOnFirstFix(r);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LoggerUtils.debug("onResume");

        LocationServicesManager.enableMyLocation();

        OsmMapsTypeSelector.selectMapType(mapView, this);

        asyncTaskManager.setActivity(this);

        Integer searchQueryResult = (Integer) ConfigurationManager.getInstance().removeObject(ConfigurationManager.SEARCH_QUERY_RESULT, Integer.class);
        if (searchQueryResult != null) {
            showSelectedLandmark(searchQueryResult);
        } else if (landmarkManager != null && landmarkManager.getSeletedLandmarkUI() != null) {
            ExtendedLandmark landmark = landmarkManager.getSeletedLandmarkUI();
            intents.showLandmarkDetailsView(landmark, lvView, getMyPosition(), true);
        }

        Integer type = (Integer) ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
        if (type != null) {
            ArrayAdapter<?> arrayAdapter = null;
            if (type == AlertDialogBuilder.SHARE_INTENTS_DIALOG) {
                List<ResolveInfo> intentList = intents.getSendIntentsList();
                if (!intentList.isEmpty()) {
                    arrayAdapter = new IntentArrayAdapter(this, intentList);
                }
            } else if (type == AlertDialogBuilder.LOGIN_DIALOG) {
                if (!ConfigurationManager.getUserManager().isUserLoggedInFully()) {
                    arrayAdapter = new LoginArrayAdapter(this, ConfigurationManager.getUserManager().getLoginItems(false));
                }
            }
            dialogManager.showAlertDialog(type, arrayAdapter, null);
        }

        intents.onAppVersionChanged();

        if (ConfigurationManager.getInstance().removeObject(HelpActivity.HELP_ACTIVITY_SHOWN, String.class) != null) {
            GeoPoint mapCenter = (GeoPoint) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, GeoPoint.class);
            if (mapCenter == null) {
                loadingHandler.removeCallbacks(gpsRunnable);
                loadingHandler.post(gpsRunnable);
            }
        }
        
        intents.startAutoCheckinBroadcast();
    }

    @Override
    public void onPause() {
        super.onPause();
        LoggerUtils.debug("onPause");

        LocationServicesManager.disableMyLocation();

        if (dialogManager != null) {
            dialogManager.dismissDialog();
        }
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
        UserTracker.getInstance().stopSession();
    }

    @Override
    public void onDestroy() {
        LoggerUtils.debug("onDestroy");
        //if (!appAbort) {
        if (ConfigurationManager.getInstance().isClosing()) {
            hardClose();
        } else {
            softClose();
            ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, mapView.getMapCenter());
        }
        AdsUtils.destroyAdView(this);
        //}
        super.onDestroy();
        System.gc();
    }

    private void softClose() {
        ConfigurationManager.getInstance().putInteger(ConfigurationManager.ZOOM, mapView.getZoomLevel());
        ConfigurationManager.getInstance().putDouble(ConfigurationManager.LATITUDE, MathUtils.coordIntToDouble(mapView.getMapCenter().getLatitudeE6()));
        ConfigurationManager.getInstance().putDouble(ConfigurationManager.LONGITUDE, MathUtils.coordIntToDouble(mapView.getMapCenter().getLongitudeE6()));
        ConfigurationManager.getDatabaseManager().saveConfiguration(false);
    }

    private void hardClose() {
        if (layerLoader != null && layerLoader.isLoading()) {
            layerLoader.stopLoading();
        }

        loadingHandler.removeCallbacks(gpsRunnable);

        LocationServicesManager.disableMyLocation();

        ConfigurationManager.getInstance().setOn(ConfigurationManager.SEND_MY_POS_AT_STARTUP);
        ConfigurationManager.getInstance().putString(ConfigurationManager.LAST_STARTING_DATE, Long.toString(startingMillis));
        softClose();

        //SuggestionProviderUtil.clearHistory();

        IconCache.getInstance().clearAll();
        landmarkManager.clearLandmarkStore();
        asyncTaskManager.cancelAll();

        if (routeRecorder != null && ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
            String[] details = routeRecorder.saveRoute();
            if (details != null) {
                LoggerUtils.debug("Saving route: " + details[0]);
            }
        }

        ConfigurationManager.getDatabaseManager().closeAllDatabases();

        ConfigurationManager.getInstance().clearObjectCache();

        PersistenceManagerFactory.getFileManager().clearImageCache(System.currentTimeMillis() - DateTimeUtils.ONE_MONTH);
        
        HttpUtils.closeConnManager();
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
            } else {
                dialogManager.showAlertDialog(AlertDialogBuilder.EXIT_DIALOG, null, null);
            } //System.out.println("key back pressed in activity");
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

    private synchronized void initOnLocationChanged(GeoPoint location) {
        if (!appInitialized) {
            mapController.setCenter(location);
            softClose(); //save mapcenter coords

            if (initLandmarkManager) {
                UserTracker.getInstance().sendMyLocation();
                ConfigurationManager.getInstance().putObject("landmarkManager", landmarkManager);
                landmarkManager.initialize(ConfigurationManager.getDatabaseManager().getLandmarkDatabase());
            }

            addLandmarkOverlay();
            //must be on top of other overlays
            addOverlay(infoOverlay);
            if (LocationServicesManager.isGpsHardwarePresent()) {
                addOverlay(myLocation);
            }

            routesManager = ConfigurationManager.getInstance().getRoutesManager();

            if (routesManager == null) {
                LoggerUtils.debug("Creating RoutesManager...");
                routesManager = new RoutesManager();
                ConfigurationManager.getInstance().putObject("routesManager", routesManager);
            } else if (routesManager.getCount() > 0) {
                for (Iterator<String> i = routesManager.getRoutes().iterator(); i.hasNext();) {
                    addRoutesOverlay(i.next());
                }
            }

            messageStack = ConfigurationManager.getInstance().getMessageStack();

            if (messageStack == null) {
                LoggerUtils.debug("Creating MessageStack...");
                messageStack = new MessageStack(new LayersMessageCondition());
                ConfigurationManager.getInstance().putObject("messageStack", messageStack);
            }
            messageStack.setHandler(loadingHandler);
            routeRecorder = ConfigurationManager.getInstance().getRouteRecorder();
            if (routeRecorder == null) {
                LoggerUtils.debug("Creating RouteRecorder...");
                routeRecorder = new RouteRecorder(routesManager);
                ConfigurationManager.getInstance().putObject("routeRecorder", routeRecorder);
            }

            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
                String route = routeRecorder.getRouteLabel();
                if (route == null) {
                    route = routeRecorder.startRecording();
                }

                if (route != null) {
                    showRouteAction(route);
                }

                messageStack.addMessage(Locale.getMessage(R.string.Routes_TrackMyPosOn), 10, -1, -1);
            } //else {
            //    messageStack.addMessage(Locale.getMessage(R.string.Routes_TrackMyPosOff), 10, -1, -1);
            //}

            layerLoader = (LayerLoader) ConfigurationManager.getInstance().getObject("layerLoader", LayerLoader.class);
            
            if (layerLoader == null || landmarkManager.getLayerManager().isEmpty()) {
                LoggerUtils.debug("Creating LayerLoader...");
                layerLoader = new LayerLoader(landmarkManager, messageStack);
                ConfigurationManager.getInstance().putObject("layerLoader", layerLoader);
                if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
                    LoggerUtils.debug("Loading Layers...");
                    intents.loadLayersAction(true, null, false, true, layerLoader,
                            MathUtils.coordIntToDouble(mapView.getMapCenter().getLatitudeE6()),
                            MathUtils.coordIntToDouble(mapView.getMapCenter().getLongitudeE6()),
                            mapView.getZoomLevel());
                }
            } else {
                //load existing layers
                if (layerLoader.isLoading()) {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_VISIBLE);
                } else {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_GONE);
                }
                loadingHandler.sendEmptyMessage(MessageStack.STATUS_MESSAGE);
                postInvalidate();
            }

            layerLoader.setRepaintHandler(loadingHandler);
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
                loadingImage.setVisibility(View.GONE);
            }

            loadingHandler.sendEmptyMessage(SHOW_MAP_VIEW);

            appInitialized = true;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        LoggerUtils.debug("onNewIntent");
        Bundle extras = intent.getExtras();

        if (extras != null && extras.containsKey("notification") && extras.containsKey("delete")) {
            boolean delete = extras.getBoolean("delete");

            if (delete) {
                Integer taskId = extras.getInt("notification");
                //System.out.println("onNewIntent " + taskId + "----------------------------------");
                asyncTaskManager.cancelTask(taskId, true);
            }
        }
    }

    @Override
    public boolean onSearchRequested() {
        if (appInitialized) {
            intents.startSearchActivity(mapView.getLatitudeSpan(), mapView.getLongitudeSpan(),
                    mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6(), -1, false);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (appInitialized) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_menu, menu);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (ConfigurationManager.getInstance().isClosing()) {
            return false;
        } else {
            MenuItem routeRecording = menu.findItem(R.id.trackPos);
            MenuItem pauseRecording = menu.findItem(R.id.pauseRoute);
            MenuItem saveRoute = menu.findItem(R.id.saveRoute);
            MenuItem login = menu.findItem(R.id.login);

            if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
                routeRecording.setTitle(R.string.Routes_TrackMyPosStart);
                saveRoute.setVisible(false);
                pauseRecording.setVisible(false);
            } else {
                saveRoute.setVisible(true);
                pauseRecording.setVisible(true);
                routeRecording.setTitle(R.string.Routes_TrackMyPosStop);
                if (routeRecorder != null && routeRecorder.isPaused()) {
                    pauseRecording.setTitle(R.string.Routes_ResumeRecording);
                } else {
                    pauseRecording.setTitle(R.string.Routes_PauseRecording);
                }
            }

            login.setVisible(!ConfigurationManager.getUserManager().isUserLoggedInFully());

            return super.onPrepareOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        UserTracker.getInstance().trackEvent("MenuClicks", item.getTitle().toString(), "", 0);
        switch (item.getItemId()) {
            case R.id.settings:
                intents.startSettingsActivity(SettingsActivity.class);
                break;
            case R.id.search:
                onSearchRequested();
                break;
            case R.id.exit:
                dialogManager.showAlertDialog(AlertDialogBuilder.EXIT_DIALOG, null, null);
                break;
            case R.id.about:
                dialogManager.showAlertDialog(AlertDialogBuilder.INFO_DIALOG, null, null);
                break;
            case R.id.releaseNotes:
                intents.startHelpActivity();
                break;
            case R.id.login:
                if (!ConfigurationManager.getUserManager().isUserLoggedInFully()) {
                    dialogManager.showAlertDialog(AlertDialogBuilder.LOGIN_DIALOG, new LoginArrayAdapter(this, ConfigurationManager.getUserManager().getLoginItems(false)), null);
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Checkin_required_error));
                }
                break;
            case R.id.addLandmark:
                if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
                    intents.startAddLandmarkActivity();
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
                }
                break;
            case R.id.autocheckin:
                if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
                    intents.startAutoCheckinListActivity(getMyPosition());
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
                }
                break;
            case R.id.qrcheckin:
                if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
                    intents.startQrCodeCheckinActivity(getMyPosition());
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
                }
                break;
            case R.id.searchcheckin:
                if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
                    intents.startLocationCheckinActivity(getMyPosition());
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
                }
                break;
            case R.id.refreshLayers:
                intents.loadLayersAction(true, null, false, true, layerLoader,
                        MathUtils.coordIntToDouble(mapView.getMapCenter().getLatitudeE6()),
                        MathUtils.coordIntToDouble(mapView.getMapCenter().getLongitudeE6()),
                        mapView.getZoomLevel());
                break;
            case R.id.addLayer:
                intents.startAddLayerActivity();
                break;
            case R.id.showLayers:
                intents.startLayersListActivity();
                break;
            case R.id.clearMap:
                clearMapAction();
                break;
            case R.id.showMyPos:
                showMyPositionAction(true);
                break;
            case R.id.showMyLandmarks:
                intents.startMyLandmarksIntent(getMyPosition());
                break;
            case R.id.recentLandmarks:
                intents.startRecentLandmarksIntent(getMyPosition());
                break;
            case R.id.blogeo:
                if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
                    if (!landmarkManager.getUnmodifableLayer(Commons.MY_POSITION_LAYER).isEmpty()) {
                        intents.startBlogeoActivity();
                    } else {
                        intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
                    }
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
                }
                break;
            case R.id.friendsCheckins:
                if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)
                        || ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
                    intents.startFriendsCheckinsIntent(getMyPosition());
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Checkin_required_error));
                }
                break;
            case R.id.trackPos:
                dialogManager.showAlertDialog(AlertDialogBuilder.TRACK_MYPOS_DIALOG, null, null);
                break;
            case R.id.saveRoute:
                intents.saveRouteAction();
                break;
            case R.id.loadRoute:
                if (intents.startRouteLoadingActivity()) {
                    intents.showInfoToast(Locale.getMessage(R.string.Routes_NoRoutes));
                }
                break;
            case R.id.pauseRoute:
                routeRecorder.pause();
                if (routeRecorder.isPaused()) {
                    intents.showInfoToast(Locale.getMessage(R.string.Routes_PauseRecordingOn));
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Routes_PauseRecordingOff));
                }
                break;
            case R.id.loadPoiFile:
                if (intents.startFilesLoadingActivity()) {
                    intents.showInfoToast(Locale.getMessage(R.string.Files_NoFiles));
                }
                break;
            case R.id.socialNetworks:
                intents.startSocialListActivity();
                break;
            //case R.id.layers:
            //    intents.startLayersListActivity();
            //    break;
            case R.id.dataPacket:
                dialogManager.showAlertDialog(AlertDialogBuilder.PACKET_DATA_DIALOG, null, null);
                break;
            case R.id.pickMyPos:
                intents.startPickLocationActivity();
                break;
            case R.id.deals:
                if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
                    intents.startCategoryListActivity(mapView.getLatitudeSpan(), mapView.getLongitudeSpan(),
                            mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6(), -1, -1, DealCategoryListActivity.class);
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
                }
                break;
            case R.id.register:
                intents.startRegisterActivity();
                break;
            case R.id.newestLandmarks:
                final String[] excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER};
                intents.startNewestLandmarkIntent(getMyPosition(), excluded, AbstractLandmarkList.ORDER_BY_DATE_DESC, 2);
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

    public void onClick(View v) {
        if (v == lvCloseButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CloseSelectedLandmarkView", "", 0);
            lvView.setVisibility(View.GONE);
            landmarkManager.clearLandmarkOnFocusQueue();
            landmarkManager.setSelectedLandmark(null);
            landmarkManager.setSeletedLandmarkUI();
        } else if (v == lvCommentButton) {
            ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CommentSelectedLandmark", selectedLandmark.getLayer(), 0);
            intents.commentButtonPressedAction();
        } else if (v == lvActionButton) {
            ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
            if (selectedLandmark != null) {
                UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CheckinSelectedLandmark", selectedLandmark.getLayer(), 0);
                boolean authStatus = intents.checkAuthStatus(selectedLandmark);
                FavouritesDbDataSource fdb = (FavouritesDbDataSource) ConfigurationManager.getInstance().getObject("FAVOURITESDB", FavouritesDbDataSource.class);
                if (ConfigurationManager.getInstance().isOn(ConfigurationManager.AUTO_CHECKIN)
                        && !selectedLandmark.getLayer().equals(Commons.MY_POSITION_LAYER)
                        && authStatus && (fdb == null || !fdb.hasLandmark(selectedLandmark))) {
                    //dialogManager.showAlertDialog(AlertDialogBuilder.AUTO_CHECKIN_DIALOG, null, new SpannableString(Locale.getMessage(R.string.autoCheckinMessage, selectedLandmark.getName())));
                	checkinManager.checkinAction(true, false, selectedLandmark);
                } else if (authStatus) {
                    checkinManager.checkinAction(false, false, selectedLandmark);
                }
            } else {
                intents.showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
            }
        } else if (v == lvOpenButton || v == thumbnailButton) {
            ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenURLSelectedLandmark", selectedLandmark.getLayer(), 0);
            openButtonPressedAction(selectedLandmark);
        } else if (v == lvCallButton) {
            ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CallSelectedLandmark", selectedLandmark.getLayer(), 0);
            intents.startPhoneCallActivity(selectedLandmark);
        } else if (v == lvRouteButton) {
            ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowRouteSelectedLandmark", selectedLandmark.getLayer(), 0);
            if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
                asyncTaskManager.executeRouteServerLoadingTask(loadingHandler, true, selectedLandmark);
            } else {
                intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
            }
        } else if (v == newestButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowNewestLandmarks", "", 0);
            final String[] excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER, Commons.HOTWIRE_LAYER};
            intents.startNewestLandmarkIntent(getMyPosition(), excluded, AbstractLandmarkList.ORDER_BY_DATE_DESC, 7);
        } else if (v == listButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowVisibleLandmarks", "", 0);
            if (!lvView.isShown()) {
                intents.showNearbyLandmarks(getMyPosition(), new OsmLandmarkProjection(mapView), AbstractLandmarkList.ORDER_BY_DIST_ASC);
            }
        } else if (v == layersButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowLayersList", "", 0);
            intents.startLayersListActivity();
        } else if (v == lvSendMailButton) {
            ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShareSelectedLandmark", selectedLandmark.getLayer(), 0);
            intents.shareLandmarkAction(dialogManager);
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
        //super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == Intents.INTENT_PICKLOCATION) {
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
                    pickPositionAction(location, true, true);
                }
                landmarkManager.addLandmark(lat, lng, 0.0f, StringUtil.formatCommaSeparatedString(names), "", Commons.LOCAL_LAYER, true);
            } else if (resultCode == RESULT_CANCELED && intent != null && !appInitialized) {
                ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
                intents.showInfoToast(Locale.getMessage(R.string.Pick_location_default, landmark.getName()));
                GeoPoint location = new GeoPoint(landmark.getLatitudeE6(), landmark.getLongitudeE6());
                initOnLocationChanged(location);
            } else if (resultCode == RESULT_CANCELED && intent != null && intent.hasExtra("message") && intent.hasExtra("name")) {
                String name = intent.getStringExtra("name");
                String message = intent.getStringExtra("message");
                intents.showInfoToast(Locale.getMessage(R.string.Pick_location_failed_error, name, message));
            } else if (resultCode != RESULT_CANCELED) {
                intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
            }
        } else if (requestCode == Intents.INTENT_MULTILANDMARK) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);
                if (action.equals("load")) {
                    int id = Integer.parseInt(ids);
                    showSelectedLandmark(id);
                }
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
                        pickPositionAction(location, true, true);
                    }
                } else if (action.equals("delete")) {
                    //delete landmark
                    landmarkManager.deletePhoneLandmark(id);
                    intents.showInfoToast(Locale.getMessage(R.string.Landmark_deleted));
                }
            }
        } else if (requestCode == Intents.INTENT_AUTO_CHECKIN) {
            if (resultCode == RESULT_OK) {
                long favouriteId = intent.getLongExtra("favourite", 0);
                FavouritesDbDataSource fdb = (FavouritesDbDataSource) ConfigurationManager.getInstance().getObject("FAVOURITESDB", FavouritesDbDataSource.class);
                FavouritesDAO fav = fdb.getLandmark(favouriteId);
                if (fav != null) {
                    GeoPoint location = new GeoPoint(MathUtils.coordDoubleToInt(fav.getLatitude()), MathUtils.coordDoubleToInt(fav.getLongitude()));
                    pickPositionAction(location, true, false);
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
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
            intents.processActivityResult(requestCode, resultCode, intent, getMyPosition(), new double[]{MathUtils.coordIntToDouble(mapView.getMapCenter().getLatitudeE6()), MathUtils.coordIntToDouble(mapView.getMapCenter().getLongitudeE6())}, loadingHandler, mapView.getZoomLevel(), layerLoader);
        }
    }

    private void landmarkDetailsAction() {
        ExtendedLandmark selectedLandmark = landmarkManager.getLandmarkOnFocus();
        if (selectedLandmark != null) {
            if (!selectedLandmark.getLayer().equals(Commons.MULTI_LANDMARK)) {
                landmarkManager.setSeletedLandmarkUI();
            }

            if (selectedLandmark.getLayer().equals(Commons.MULTI_LANDMARK)) {
                intents.startMultiLandmarkIntent(getMyPosition(), AbstractLandmarkList.ORDER_BY_DIST_ASC);
            } else {
                intents.showLandmarkDetailsView(selectedLandmark, lvView, getMyPosition(), true);
                GeoPoint g = new GeoPoint(selectedLandmark.getLatitudeE6(), selectedLandmark.getLongitudeE6());
                mapController.animateTo(g);

                if (selectedLandmark.getLayer().equals(Commons.LOCAL_LAYER)) {
                    intents.loadLayersAction(true, null, false, true, layerLoader,
                            MathUtils.coordIntToDouble(mapView.getMapCenter().getLatitudeE6()),
                            MathUtils.coordIntToDouble(mapView.getMapCenter().getLongitudeE6()),
                            mapView.getZoomLevel());
                }
            }
        } else {
            LoggerUtils.debug(Locale.getMessage(R.string.Landmark_opening_error));
        }
    }

    private void openButtonPressedAction(ExtendedLandmark landmark) {
        intents.startLandmarkDetailsActivity(landmarkManager.getLandmarkURL(landmark), landmark.getName());
    }

    private void pickPositionAction(GeoPoint newCenter, boolean loadLayers, boolean clearMap) {
        mapController.setCenter(newCenter);
        if (loadLayers) {
            intents.loadLayersAction(true, null, clearMap, true, layerLoader,
                    MathUtils.coordIntToDouble(mapView.getMapCenter().getLatitudeE6()),
                    MathUtils.coordIntToDouble(mapView.getMapCenter().getLongitudeE6()),
                    mapView.getZoomLevel());
        }
    }

    private void showMyPositionAction(boolean loadLayers) {
        GeoPoint myLoc = LocationServicesManager.getMyLocation();
        if (myLoc != null) {
            boolean isVisible = false;
            boolean clearLandmarks = false;
            ProjectionInterface projection = new OsmLandmarkProjection(mapView);
            if (projection.isVisible(myLoc.getLatitudeE6(), myLoc.getLongitudeE6())) {
                isVisible = true;
            }
            if (!isVisible) {
                IGeoPoint mapCenter = mapView.getMapCenter();
                clearLandmarks = intents.isClearLandmarksRequired(projection, mapCenter.getLatitudeE6(), mapCenter.getLongitudeE6(),
                        myLoc.getLatitudeE6(), myLoc.getLongitudeE6());
            }

            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
                mapController.setCenter(myLoc);
            } else {
                mapController.animateTo(myLoc);
            }

            if (loadLayers && !isVisible) {
                mapController.setCenter(myLoc);
                intents.loadLayersAction(true, null, clearLandmarks, true, layerLoader,
                        MathUtils.coordIntToDouble(mapView.getMapCenter().getLatitudeE6()),
                        MathUtils.coordIntToDouble(mapView.getMapCenter().getLongitudeE6()),
                        mapView.getZoomLevel());
            }
        } else {
            intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
        }
    }

    private void updateGpsLocation(double lat, double lng, float altitude, float accuracy, float speed) {
        if (landmarkManager != null) {
            landmarkManager.addLandmark(lat, lng, altitude, Locale.getMessage(R.string.Your_Location), Long.toString(System.currentTimeMillis()), Commons.MY_POSITION_LAYER, false);
        }

        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
            showMyPositionAction(false);
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
                if (routeRecorder != null) {
                    routeRecorder.addCoordinate(lat, lng, altitude, accuracy, speed);
                }
            }
        }

        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.AUTO_CHECKIN)) {
            checkinManager.autoCheckin(lat, lng, false);
        }
    }

    private void setBuiltInZoomControls(boolean enable) {
        mapView.setBuiltInZoomControls(enable);
    }

    private void addOverlay(Overlay overlay) {
        mapView.getOverlays().add(overlay);
    }

    private void addLandmarkOverlay() {
        OsmLandmarkOverlay landmarkOverlay;
        if (LocationServicesManager.isGpsHardwarePresent()) {
            landmarkOverlay = new OsmLandmarkOverlay(this, landmarkManager, loadingHandler);
        } else {
            landmarkOverlay = new OsmLandmarkOverlay(this, landmarkManager, loadingHandler, new String[]{Commons.ROUTES_LAYER});
        }
        addOverlay(landmarkOverlay);
    }

    private void addRoutesOverlay(String routeName) {
        OsmRoutesOverlay routesOverlay = new OsmRoutesOverlay(mapView.getProjection(), this, routesManager, routeName);
        addOverlay(routesOverlay);
    }

    private void postInvalidate() {
        mapView.postInvalidate();
    }

    private double[] getMyPosition() {
        return landmarkManager.getMyPosition(mapView.getMapCenter().getLatitudeE6(),
                mapView.getMapCenter().getLongitudeE6());
    }

    private void showRouteAction(String routeKey) {
        LoggerUtils.debug("Adding route to view: " + routeKey);
        if (routesManager.containsRoute(routeKey)) {
            addRoutesOverlay(routeKey);
            if (!routeKey.startsWith(RouteRecorder.CURRENTLY_RECORDED)) {
                double[] locationCoords = routesManager.calculateRouteCenter(routeKey);
                GeoPoint newCenter = new GeoPoint(MathUtils.coordDoubleToInt(locationCoords[0]), MathUtils.coordDoubleToInt(locationCoords[1]));
                mapController.setCenter(newCenter);
                mapController.setZoom(routesManager.calculateRouteZoom(routeKey));
            }
            postInvalidate();
        }
    }

    private String followMyPositionAction() {
        if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
            ConfigurationManager.getInstance().setOn(ConfigurationManager.FOLLOW_MY_POSITION);
            String route = routeRecorder.startRecording();
            showRouteAction(route);
            if (layerLoader.isLoading()) {
                layerLoader.stopLoading();
            }
            List<ExtendedLandmark> myPosV = landmarkManager.getUnmodifableLayer(Commons.MY_POSITION_LAYER);
            if (!myPosV.isEmpty()) {
                ExtendedLandmark landmark = myPosV.get(0);
                ProjectionInterface projection = new OsmLandmarkProjection(mapView);
                if (projection.isVisible(landmark.getLatitudeE6(), landmark.getLongitudeE6())) {
                    showMyPositionAction(false);
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Routes_TrackMyPosOn));
                }
            } else {
                intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
            }
        } else if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
            ConfigurationManager.getInstance().setOff(ConfigurationManager.FOLLOW_MY_POSITION);
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
                String filename = routeRecorder.stopRecording();
                if (filename != null) {
                    return filename;
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Routes_TrackMyPosOff));
                }
            } else {
                intents.showInfoToast(Locale.getMessage(R.string.Routes_TrackMyPosOff));
            }
        }
        return null;
    }

    private void clearMapAction() {
        landmarkManager.clearLandmarkStore();
        routesManager.clearRoutesStore();
        postInvalidate();
        intents.showInfoToast(Locale.getMessage(R.string.Maps_cleared));
    }
    
    private static class LoadingHandler extends Handler {
    	
    	private WeakReference<GMSClientOSMMainActivity> parentActivity;
    	
    	public LoadingHandler(GMSClientOSMMainActivity parentActivity) {
    		this.parentActivity = new WeakReference<GMSClientOSMMainActivity>(parentActivity);
    	}
    	
        @Override
        public void handleMessage(Message msg) {
        	GMSClientOSMMainActivity activity = parentActivity.get();
        	if (activity != null && !activity.isFinishing()) {
        		if (msg.what == MessageStack.STATUS_MESSAGE) {
        			activity.statusBar.setText(activity.messageStack.getMessage());
        		} else if (msg.what == MessageStack.STATUS_VISIBLE) {
        			activity.loadingImage.setVisibility(View.VISIBLE);
        		} else if (msg.what == MessageStack.STATUS_GONE) {
        			activity.loadingImage.setVisibility(View.GONE);
        		} else if (msg.what == LayerLoader.LAYER_LOADED) {
        			activity.postInvalidate();
        		} else if (msg.what == LayerLoader.ALL_LAYERS_LOADED) {
        			if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER)) {
        				activity.asyncTaskManager.executeUploadImageTask(MathUtils.coordIntToDouble(activity.mapView.getMapCenter().getLatitudeE6()),
                            MathUtils.coordIntToDouble(activity.mapView.getMapCenter().getLongitudeE6()), activity.intents.takeScreenshot());
        			}
        		} else if (msg.what == LayerLoader.FB_TOKEN_EXPIRED) {
        			activity.intents.showInfoToast(Locale.getMessage(R.string.Social_token_expired, "Facebook"));
        		} else if (msg.what == OsmLandmarkOverlay.SHOW_LANDMARK_DETAILS) {
        			activity.landmarkDetailsAction();
        		} else if (msg.what == SHOW_MAP_VIEW) {
        			View loading = activity.findViewById(R.id.mapCanvasWidgetL);
        			View mapCanvas = activity.findViewById(R.id.mapCanvasWidgetM);
        			loading.setVisibility(View.GONE);
        			mapCanvas.setVisibility(View.VISIBLE);
        		} else if (msg.what == AsyncTaskManager.SHOW_ROUTE_MESSAGE) {
        			activity.showRouteAction((String)msg.obj);
        		} else if (msg.what == OsmMyLocationOverlay.UPDATE_LOCATION) {
        			Location location = (Location) msg.obj;
        			activity.updateGpsLocation(location.getLatitude(), location.getLongitude(), (float)location.getAltitude(), location.getAccuracy(), location.getSpeed());
        		}
        	}
        }
    }
}
