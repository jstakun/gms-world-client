package com.jstakun.gms.android.ui;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.api.IMapView;
import org.osmdroid.api.IMyLocationOverlay;

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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FavouritesDAO;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.FilenameFilterFactory;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.google.maps.GoogleLandmarkOverlay;
import com.jstakun.gms.android.google.maps.GoogleMapsTypeSelector;
import com.jstakun.gms.android.google.maps.GoogleRoutesOverlay;
import com.jstakun.gms.android.google.maps.ObservableMapView;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerLoader;
import com.jstakun.gms.android.location.LocationServicesManager;
import com.jstakun.gms.android.osm.maps.OsmLandmarkOverlay;
import com.jstakun.gms.android.osm.maps.OsmMapsTypeSelector;
import com.jstakun.gms.android.osm.maps.OsmMyLocationNewOverlay;
import com.jstakun.gms.android.osm.maps.OsmRoutesOverlay;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.MessageStack;
import com.jstakun.gms.android.utils.ProjectionInterface;
import com.jstakun.gms.android.utils.ServicesUtils;
import com.jstakun.gms.android.utils.StringUtil;
import com.jstakun.gms.android.utils.UserTracker;
 
public class GMSClientMainActivity extends MapActivity implements OnClickListener {

    private static final int SHOW_MAP_VIEW = 0;
    private IMapView mapView;
    private IMapController mapController;
    private IMyLocationOverlay myLocation;
    private MapView googleMapsView;
    private LandmarkManager landmarkManager;
    private AsyncTaskManager asyncTaskManager;
    private IntentsHelper intents;
    private DialogManager dialogManager;
    private TextView statusBar;
    private View lvCloseButton, lvCallButton, lvCommentButton, mapButtons,
            lvOpenButton, lvView, lvShareButton, myLocationButton, nearbyLandmarksButton,
            newestButton, listButton, layersButton,
            lvCheckinButton, lvRouteButton, thumbnailButton, loadingImage;
    private ProgressBar loadingProgressBar;
    private int mapProvider;
    private boolean appInitialized = false, isRouteDisplayed = false;
    //Handlers
    private Handler loadingHandler;
    private final Runnable gpsRunnable = new Runnable() {
        public void run() {
            IGeoPoint location = LocationServicesManager.getMyLocation();
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
                    initOnLocationChanged(new org.osmdroid.google.wrapper.GeoPoint(loc));
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
        
        //UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        ConfigurationManager.getInstance().setContext(getApplicationContext());
        mapProvider = ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        loadingHandler = new LoadingHandler(this);

        LoggerUtils.debug("Map provider is " + mapProvider);

        if (mapProvider == ConfigurationManager.OSM_MAPS) {
            setContentView(R.layout.osmdroidcanvasview);
            mapView = (IMapView) findViewById(R.id.mapCanvas);
            ((org.osmdroid.views.MapView) mapView).setMultiTouchControls(true);
            ((com.jstakun.gms.android.osm.maps.ObservableMapView) mapView).setOnZoomChangeListener(new ZoomListener());
            myLocation = new OsmMyLocationNewOverlay(this.getApplicationContext(), (org.osmdroid.views.MapView) mapView, loadingHandler);
        } else {
            //default view is Google
            setContentView(R.layout.googlemapscanvasview);
            googleMapsView = (MapView) findViewById(R.id.mapCanvas);
            ((ObservableMapView)googleMapsView).setOnZoomChangeListener(new ZoomListener());
            mapView = new org.osmdroid.google.wrapper.MapView(googleMapsView);
            myLocation = new GoogleIMyLocationOverlay(this, googleMapsView, loadingHandler, getResources().getDrawable(R.drawable.ic_maps_indicator_current_position));
        }

        LocationServicesManager.initLocationServicesManager(this, loadingHandler, myLocation);

        initComponents();
        //}
    }

    private void initComponents() {

    	loadingProgressBar = (ProgressBar) findViewById(R.id.mapCanvasLoadingProgressBar);
    	loadingProgressBar.setProgress(25);
    	
    	statusBar = (TextView) findViewById(R.id.statusBar);
        loadingImage = findViewById(R.id.loadingAnim);
        lvView = findViewById(R.id.lvView);
        mapButtons = findViewById(R.id.mapButtons);

        lvCheckinButton = findViewById(R.id.lvCheckinButton);
        lvCloseButton = findViewById(R.id.lvCloseButton);
        lvOpenButton = findViewById(R.id.lvOpenButton);
        lvCommentButton = findViewById(R.id.lvCommentButton);
        lvCallButton = findViewById(R.id.lvCallButton);
        lvRouteButton = findViewById(R.id.lvRouteButton);
        lvShareButton = findViewById(R.id.lvShareButton);
        thumbnailButton = findViewById(R.id.thumbnailButton);
        myLocationButton = findViewById(R.id.myLocationButton);
        nearbyLandmarksButton = findViewById(R.id.nearbyLandmarksButton);

        lvCheckinButton.setOnClickListener(this);
        lvCloseButton.setOnClickListener(this);
        lvOpenButton.setOnClickListener(this);
        lvCommentButton.setOnClickListener(this);
        lvCallButton.setOnClickListener(this);
        lvRouteButton.setOnClickListener(this);
        lvShareButton.setOnClickListener(this);
        thumbnailButton.setOnClickListener(this);
        myLocationButton.setOnClickListener(this);
        nearbyLandmarksButton.setOnClickListener(this);

        newestButton = findViewById(R.id.newestButton);
        listButton = findViewById(R.id.listButton);
        layersButton = findViewById(R.id.layersButton);

        newestButton.setOnClickListener(this);
        listButton.setOnClickListener(this);
        layersButton.setOnClickListener(this);

        mapController = mapView.getController();

        setBuiltInZoomControls(true);

        IGeoPoint mapCenter = (IGeoPoint) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, IGeoPoint.class);

        if (mapCenter == null) {
            loadingHandler.postDelayed(gpsRunnable, ConfigurationManager.FIVE_SECONDS);
        }

        mapController.setZoom(ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM));

        appInitialized = false;
        landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
        if (landmarkManager == null) {
            LoggerUtils.debug("Creating LandmarkManager...");
            landmarkManager = new LandmarkManager();
            ConfigurationManager.getInstance().putObject("landmarkManager", landmarkManager);
        } 

        asyncTaskManager = (AsyncTaskManager) ConfigurationManager.getInstance().getObject("asyncTaskManager", AsyncTaskManager.class);
        if (asyncTaskManager == null) {
            LoggerUtils.debug("Creating AsyncTaskManager...");
            asyncTaskManager = new AsyncTaskManager(this, landmarkManager);
            ConfigurationManager.getInstance().putObject("asyncTaskManager", asyncTaskManager);
            //check if newer version available
            asyncTaskManager.executeNewVersionCheckTask();
        }

        intents = new IntentsHelper(this, landmarkManager, asyncTaskManager);

        if (!CategoriesManager.getInstance().isInitialized()) {
            LoggerUtils.debug("Loading deal categories...");
            asyncTaskManager.executeDealCategoryLoaderTask(true);
        }

        dialogManager = new DialogManager(this, intents, asyncTaskManager, landmarkManager, trackMyPosListener);

        if (mapCenter != null && mapCenter.getLatitudeE6() != 0 && mapCenter.getLongitudeE6() != 0) {
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
        
        loadingProgressBar.setProgress(50);
    }

    @Override
    public void onResume() {
        super.onResume();
        LoggerUtils.debug("onResume");

        LocationServicesManager.enableMyLocation();

        if (mapProvider == ConfigurationManager.GOOGLE_MAPS) {
            GoogleMapsTypeSelector.selectMapType(googleMapsView);
        } else if (mapProvider == ConfigurationManager.OSM_MAPS) {
            OsmMapsTypeSelector.selectMapType((org.osmdroid.views.MapView) mapView, this);
        }

        asyncTaskManager.setActivity(this);
        
        if (landmarkManager != null && landmarkManager.hasMyLocation() && ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
        	mapButtons.setVisibility(View.VISIBLE);
        }
        
        //verify access token
        asyncTaskManager.executeGetTokenTask();
        
        Integer searchQueryResult = (Integer) ConfigurationManager.getInstance().removeObject(ConfigurationManager.SEARCH_QUERY_RESULT, Integer.class);
        if (searchQueryResult != null) {
        	int[] coordsE6 = intents.showSelectedLandmark(searchQueryResult, getMyLocation(), lvView, mapView.getZoomLevel(), ProjectionFactory.getProjection(mapView, googleMapsView));
            if (coordsE6 != null) {
            	animateTo(coordsE6);
            }
        } else if (landmarkManager != null && landmarkManager.getSeletedLandmarkUI() != null) {
            ExtendedLandmark landmark = landmarkManager.getSeletedLandmarkUI();
            intents.showLandmarkDetailsView(landmark, lvView, getMyLocation(), true);
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
            IGeoPoint mapCenter = (IGeoPoint) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, IGeoPoint.class);
            if (mapCenter == null) {
                loadingHandler.removeCallbacks(gpsRunnable);
                loadingHandler.post(gpsRunnable);
            }
        }
        
        syncRoutesOverlays();
        
        intents.startAutoCheckinBroadcast();
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
    public void onPause() {
        super.onPause();
        LoggerUtils.debug("onPause");

        LocationServicesManager.disableMyLocation();

        if (dialogManager != null) {
            dialogManager.dismissDialog();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //UserTracker.getInstance().stopSession(this);
    }

    @Override
    public void onDestroy() {
        LoggerUtils.debug("onDestroy");
        //if (!appAbort) {
        if (ConfigurationManager.getInstance().isClosing()) {
        	appInitialized = false;
            intents.hardClose(loadingHandler, gpsRunnable, mapView.getZoomLevel(), mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6());
        } else if (mapView.getMapCenter().getLatitudeE6() != 0 && mapView.getMapCenter().getLongitudeE6() != 0) {
        	intents.softClose(mapView.getZoomLevel(), mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6());
            ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, mapView.getMapCenter());
        }
        AdsUtils.destroyAdView(this);
        //}
        super.onDestroy();
        System.gc();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        LoggerUtils.debug("onRestart");
        if (mapProvider != ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER)) {
            Intent intent = getIntent();
            ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, mapView.getMapCenter());
            LocationServicesManager.disableMyLocation();
            finish();
            startActivity(intent);
        }
    }

    private void hideLandmarkView() {
    	landmarkManager.clearLandmarkOnFocusQueue();
		landmarkManager.setSelectedLandmark(null);
		landmarkManager.setSeletedLandmarkUI();
		lvView.setVisibility(View.GONE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	UserTracker.getInstance().trackEvent("onKeyDown", "", "", 0);
    	if (ConfigurationManager.getUserManager().isUserAllowedAction() || keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {		
    		//System.out.println("Key pressed in activity: " + keyCode);
    		if (keyCode == KeyEvent.KEYCODE_BACK) {
    			if (lvView.isShown()) {
    				hideLandmarkView();
    			} else {
    				dialogManager.showAlertDialog(AlertDialogBuilder.EXIT_DIALOG, null, null);
    			} //System.out.println("key back pressed in activity");
    			return true;
    		} else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
    			int[] coordsE6 = intents.showLandmarkDetailsAction(getMyLocation(), lvView, mapView.getZoomLevel(), ProjectionFactory.getProjection(mapView, googleMapsView));
    			if (coordsE6 != null) {
    				animateTo(coordsE6);
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
    	} else {
    		intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
    		return true;
  	  	}   
    }

    private void initOnLocationChanged(IGeoPoint location) {
        if (!appInitialized && location != null) {
        	loadingProgressBar.setProgress(75);
        	mapController.setCenter(location);

        	if (!landmarkManager.isInitialized()) {
                //UserTracker.getInstance().sendMyLocation();
                landmarkManager.initialize();
            }

            addLandmarkOverlay();
            //must be on top of other overlays
            //addOverlay(infoOverlay);
            addOverlay(myLocation);
            
            syncRoutesOverlays();
            
            MessageStack.getInstance().setHandler(loadingHandler);
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
                String route = RouteRecorder.getInstance().startRecording();
                showRouteAction(route);
                MessageStack.getInstance().addMessage(Locale.getMessage(R.string.Routes_TrackMyPosOn), 10, -1, -1);
            } 

            LayerLoader.getInstance().setRepaintHandler(loadingHandler);
            
            if (!LayerLoader.getInstance().isInitialized() || !LayerLoader.getInstance().isLoading()) {
               LoggerUtils.debug("Creating LayerLoader...");
                if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
                	LoggerUtils.debug("Loading Layers in " + location.getLatitude() + "," +  location.getLongitude());
                    intents.loadLayersAction(true, null, false, true, location.getLatitude(), location.getLongitude(),
                            mapView.getZoomLevel(), ProjectionFactory.getProjection(mapView, googleMapsView));
                }
            } else {
                //load existing layers
                if (LayerLoader.getInstance().isLoading()) {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_VISIBLE);
                } else {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_GONE);
                }
                loadingHandler.sendEmptyMessage(MessageStack.STATUS_MESSAGE);
                //postInvalidate();
            }

            loadingProgressBar.setProgress(100);
            
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
            intents.startSearchActivity(mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6(), -1, false);
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
        	MenuItem routes = menu.findItem(R.id.routes);
        	if (landmarkManager.getLayerManager().containsLayer(Commons.ROUTES_LAYER)) {
        		routes.setVisible(true);
            	MenuItem routeRecording = menu.findItem(R.id.trackPos);
        		MenuItem pauseRecording = menu.findItem(R.id.pauseRoute);
        		MenuItem saveRoute = menu.findItem(R.id.saveRoute);
        		menu.findItem(R.id.events).setVisible(false);
        		MenuItem loadRoute = menu.findItem(R.id.loadRoute);

        		if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
        			routeRecording.setTitle(R.string.Routes_TrackMyPosStart);
        			saveRoute.setVisible(false);
        			pauseRecording.setVisible(false);
        		} else {
        			saveRoute.setVisible(true);
        			pauseRecording.setVisible(true);
        			routeRecording.setTitle(R.string.Routes_TrackMyPosStop);
        			if (RouteRecorder.getInstance().isPaused()) {
        				pauseRecording.setTitle(R.string.Routes_ResumeRecording);
        			} else {
        				pauseRecording.setTitle(R.string.Routes_PauseRecording);
        			}
        		}
        		if (PersistenceManagerFactory.getFileManager().isFolderEmpty(FileManager.getRoutesFolderPath(), FilenameFilterFactory.getFilenameFilter("kml"))) {    
        			loadRoute.setVisible(false);
        		} else {
        			loadRoute.setVisible(true);
        		}
        	} else {
        		routes.setVisible(false);	
        	}

        	menu.findItem(R.id.shareScreenshot).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	menu.findItem(R.id.dataPacket).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	menu.findItem(R.id.reset).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	menu.findItem(R.id.releaseNotes).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	menu.findItem(R.id.config).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	      	
            menu.findItem(R.id.login).setVisible(!ConfigurationManager.getUserManager().isUserLoggedInFully());

            menu.findItem(R.id.register).setVisible(!ConfigurationManager.getUserManager().isUserLoggedInGMSWorld());
            
            return super.onPrepareOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        UserTracker.getInstance().trackEvent("MenuClicks", item.getTitle().toString(), "", 0);
        int itemId = item.getItemId();
    	if (ConfigurationManager.getUserManager().isUserAllowedAction() || itemId == R.id.exit || itemId == R.id.login || itemId == R.id.register) {	
    	  switch (itemId) {
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
                    intents.showInfoToast(Locale.getMessage(R.string.loginFull));
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
                    intents.startAutoCheckinListActivity(getMyLocation());
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
                }
                break;
            case R.id.qrcheckin:
                if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
                    intents.startQrCodeCheckinActivity(getMyLocation());
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
                }
                break;
            case R.id.searchcheckin:
                if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
                    intents.startLocationCheckinActivity(getMyLocation());
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
                }
                break;
            case R.id.refreshLayers:
                intents.loadLayersAction(true, null, false, true, 
                        mapView.getMapCenter().getLatitude(), mapView.getMapCenter().getLongitude(),
                        mapView.getZoomLevel(), ProjectionFactory.getProjection(mapView, googleMapsView));
                break;
            case R.id.addLayer:
                intents.startAddLayerActivity();
                break;
            case R.id.showLayers:
                intents.startLayersListActivity(false);
                break;
            case R.id.clearMap:
                clearMapAction();
                break;
            case R.id.showMyLandmarks:
                intents.startMyLandmarksIntent(getMyLocation());
                break;
            case R.id.recentLandmarks:
                intents.startRecentLandmarksIntent(getMyLocation());
                break;
            case R.id.blogeo:
                if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
                    if (landmarkManager.hasMyLocation()) {
                        intents.startBlogeoActivity();
                    } else {
                        intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
                    }
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
                }
                break;
            case R.id.friendsCheckins:
                if (ConfigurationManager.getUserManager().isFriendSocialLoggedIn()) {
                    intents.startFriendsCheckinsIntent(getMyLocation());
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Checkin_required_error));
                }
                break;
            case R.id.trackPos:
                dialogManager.showAlertDialog(AlertDialogBuilder.TRACK_MYPOS_DIALOG, null, null);
                break;
            case R.id.saveRoute:
            	if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
		        	dialogManager.showAlertDialog(AlertDialogBuilder.SAVE_ROUTE_DIALOG, null, null);
		        } else if (ConfigurationManager.getInstance().isOff(ConfigurationManager.RECORDING_ROUTE)) {
		            intents.showInfoToast(Locale.getMessage(R.string.Routes_TrackMyPosStopped));
		        }
                break;
            case R.id.loadRoute:
                if (intents.startRouteFileLoadingActivity()) {
                    intents.showInfoToast(Locale.getMessage(R.string.Routes_NoRoutes));
                }
                break;
            case R.id.pauseRoute:
            	RouteRecorder.getInstance().pause();
                if (RouteRecorder.getInstance().isPaused()) {
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
            case R.id.config:
				intents.startConfigurationViewerActivity();
				break;
			case R.id.dataPacket:
                dialogManager.showAlertDialog(AlertDialogBuilder.PACKET_DATA_DIALOG, null, null);
                break;
            case R.id.pickMyPos:
                intents.startPickLocationActivity();
                break;
            case R.id.deals:
                if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
                    intents.startCategoryListActivity(mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6(), -1, -1);
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
                }
                break;
            case R.id.register:
                intents.startRegisterActivity();
                break;
            case R.id.newestLandmarks:
                final String[] excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER};
                intents.startNewestLandmarkIntent(getMyLocation(), excluded, 2);
                break;
            //case R.id.events:
                //intents.startCalendarActivity(getMyPosition());
                //break;
            case R.id.rateUs:
                dialogManager.showAlertDialog(AlertDialogBuilder.RATE_US_DIALOG, null, null);
                break;
            case R.id.listLandmarks:
	    		if (!lvView.isShown()) {
	        		intents.showNearbyLandmarks(getMyLocation(), ProjectionFactory.getProjection(mapView, googleMapsView));
	    		}
	    		break;    
            case R.id.shareScreenshot:
            	asyncTaskManager.executeImageUploadTask(mapView.getMapCenter().getLatitude(), mapView.getMapCenter().getLongitude(), true);
            	break;    
            case R.id.reset:
            	dialogManager.showAlertDialog(AlertDialogBuilder.RESET_DIALOG, null, null);
            	break;	
            default:
                return super.onOptionsItemSelected(item);
          }
    	} else {
    		intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
    	}
        return true;
    }

    @Override
    protected boolean isRouteDisplayed() {
        return isRouteDisplayed;
    }

    public void onClick(View v) {
    	if (ConfigurationManager.getUserManager().isUserAllowedAction() || v == lvCloseButton || v == myLocationButton || v == nearbyLandmarksButton) {	
    		if (v == myLocationButton) {
    			showMyPositionAction(true);
      	  	} else if (v == nearbyLandmarksButton) {
      	  		intents.startLayersListActivity(true);		
        	} else {
      	  		ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();	  
      	  		if (selectedLandmark != null) {
      	  			if (v == lvCloseButton) {
      	  				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CloseSelectedLandmarkView", "", 0);
      	  			    hideLandmarkView();
      	  			} else if (v == lvCommentButton) {
      	  				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CommentSelectedLandmark", selectedLandmark.getLayer(), 0);
      	  				intents.commentButtonPressedAction();
      	  			} else if (v == lvCheckinButton) {
      	  				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CheckinSelectedLandmark", selectedLandmark.getLayer(), 0);
      	  				boolean authStatus = intents.checkAuthStatus(selectedLandmark);
      	  				if (authStatus) {
      	  					boolean addToFavourites = ConfigurationManager.getInstance().isOn(ConfigurationManager.AUTO_CHECKIN) && !selectedLandmark.getLayer().equals(Commons.MY_POSITION_LAYER);
      	  					CheckinManager.getInstance().checkinAction(addToFavourites, false, selectedLandmark);
      	  				} 
      	  			} else if (v == lvOpenButton || v == thumbnailButton) {
      	  				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenURLSelectedLandmark", selectedLandmark.getLayer(), 0);
      	  				intents.openButtonPressedAction(selectedLandmark);
      	  			} else if (v == lvCallButton) {
      	  				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CallSelectedLandmark", selectedLandmark.getLayer(), 0);
      	  				intents.startPhoneCallActivity(selectedLandmark);
      	  			} else if (v == lvRouteButton) {
      	  				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowRouteSelectedLandmark", selectedLandmark.getLayer(), 0);
      	  				if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
      	  					asyncTaskManager.executeRouteServerLoadingTask(loadingHandler, false, selectedLandmark);
      	  				} else {
      	  					intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
      	  				}
      	  			} else if (v == lvShareButton) {
      	  				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShareSelectedLandmark", selectedLandmark.getLayer(), 0);
      	  				intents.shareLandmarkAction(dialogManager);
      	  			}
      	  		} else if (v == newestButton) {
      	  			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowNewestLandmarks", "", 0);
      	  			final String[] excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER};
      	  			intents.startNewestLandmarkIntent(getMyLocation(), excluded, 7);
      	  		} else if (v == listButton) {
      	  			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowVisibleLandmarks", "", 0);
      	  			if (!lvView.isShown()) {
      	  				intents.showNearbyLandmarks(getMyLocation(), ProjectionFactory.getProjection(mapView, googleMapsView));
      	  			}
      	  		} else if (v == layersButton) {
      	  			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowLayersList", "", 0);
      	  			intents.startLayersListActivity(false);
      	  		} else {
      	  			intents.showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
      	  		}
      	  	} 
    	} else {
       		intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
    	}
    }  

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == IntentsHelper.INTENT_PICKLOCATION) {
            if (resultCode == RESULT_OK) {
            	Double lat = null, lng = null;
            	String name = null;
            	if (intent.hasExtra("name") && intent.hasExtra("lat") && intent.hasExtra("lng")) {
                	String lats = intent.getStringExtra("lat");
                    String lngs = intent.getStringExtra("lng");
                    lat = Double.parseDouble(lats);
                    lng = Double.parseDouble(lngs);                   
                    name = intent.getStringExtra("name");
            	} 
            	if (lat == null || lng == null || name == null) {
                	ExtendedLandmark defaultLocation = ConfigurationManager.getInstance().getDefaultCoordinate();
                    name = defaultLocation.getName();
                    lat = defaultLocation.getQualifiedCoordinates().getLatitude();
                    lng = defaultLocation.getQualifiedCoordinates().getLongitude();
                }
                GeoPoint location = new GeoPoint(MathUtils.coordDoubleToInt(lat), MathUtils.coordDoubleToInt(lng));
                if (!appInitialized) {
                    initOnLocationChanged(new org.osmdroid.google.wrapper.GeoPoint(location));
                } else {
                    pickPositionAction(location, true, true);
                }
                landmarkManager.addLandmark(lat, lng, 0.0f, StringUtil.formatCommaSeparatedString(name), "", Commons.LOCAL_LAYER, true);
            } else if (resultCode == RESULT_CANCELED && !appInitialized) {
                ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
                intents.showInfoToast(Locale.getMessage(R.string.Pick_location_default, landmark.getName()));
                GeoPoint location = new GeoPoint(landmark.getLatitudeE6(), landmark.getLongitudeE6());
                initOnLocationChanged(new org.osmdroid.google.wrapper.GeoPoint(location));
            } else if (resultCode == RESULT_CANCELED && intent.hasExtra("message")) {
                String message = intent.getStringExtra("message");
                intents.showInfoToast(message);
            } else if (resultCode != RESULT_CANCELED) {
                intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
            }
        } else if (requestCode == IntentsHelper.INTENT_MULTILANDMARK) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);
                if (action.equals("load")) {
                    int id = Integer.parseInt(ids);
                    int[] coordsE6 = intents.showSelectedLandmark(id, getMyLocation(), lvView, mapView.getZoomLevel(), ProjectionFactory.getProjection(mapView, googleMapsView));
                    if (coordsE6 != null) {
                    	animateTo(coordsE6);
                    }
                }
            }
        } else if (requestCode == IntentsHelper.INTENT_MYLANDMARKS) {
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
        } else if (requestCode == IntentsHelper.INTENT_AUTO_CHECKIN) {
            if (resultCode == RESULT_OK) {
                int favouriteId = intent.getIntExtra("favourite", 0);
                FavouritesDAO fav = ConfigurationManager.getDatabaseManager().getFavouritesDatabase().getLandmark(favouriteId);
                if (fav != null) {
                    GeoPoint location = new GeoPoint(MathUtils.coordDoubleToInt(fav.getLatitude()),
                            MathUtils.coordDoubleToInt(fav.getLongitude()));
                    pickPositionAction(location, true, false);
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
                }
            }
        } else if (requestCode == IntentsHelper.INTENT_CALENDAR) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);

                if (action.equals("load")) {
                    int id = Integer.parseInt(ids);
                    int[] coordsE6 = intents.showSelectedLandmark(id, getMyLocation(), lvView, mapView.getZoomLevel(), ProjectionFactory.getProjection(mapView, googleMapsView));
                    if (coordsE6 != null) {
                    	animateTo(coordsE6);
                    }
                }
            }
        } else {
            intents.processActivityResult(requestCode, resultCode, intent, getMyLocation(), new double[]{mapView.getMapCenter().getLatitude(), mapView.getMapCenter().getLongitude()}, loadingHandler, mapView.getZoomLevel(), ProjectionFactory.getProjection(mapView, googleMapsView));
        }
    }

    private void pickPositionAction(GeoPoint newCenter, boolean loadLayers, boolean clearMap) {
        mapController.setCenter(new org.osmdroid.google.wrapper.GeoPoint(newCenter));
        if (loadLayers) {
            intents.loadLayersAction(true, null, clearMap, true, 
                    mapView.getMapCenter().getLatitude(), mapView.getMapCenter().getLongitude(),
                    mapView.getZoomLevel(), ProjectionFactory.getProjection(mapView, googleMapsView));
        }
    }

    private void showMyPositionAction(boolean loadLayers) {
        IGeoPoint myLoc = LocationServicesManager.getMyLocation();
        if (myLoc != null) {
            boolean isVisible = false;
            boolean clearLandmarks = false;
            ProjectionInterface projection = ProjectionFactory.getProjection(mapView, googleMapsView);
            if (projection.isVisible(myLoc.getLatitudeE6(), myLoc.getLongitudeE6())) {
                isVisible = true;
            }
            if (!isVisible) {
            	hideLandmarkView();
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
                intents.loadLayersAction(true, null, clearLandmarks, true, myLoc.getLatitude(), myLoc.getLongitude(), mapView.getZoomLevel(), projection);
            }
        } else {
            intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
        }
    }

    private void updateLocation(Location l) {
    	intents.addMyLocationLandmark(l);       
        intents.vibrateOnLocationUpdate();
        UserTracker.getInstance().sendMyLocation();
    	
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
        	mapButtons.setVisibility(View.GONE);
        	showMyPositionAction(false);
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
            	RouteRecorder.getInstance().addCoordinate(l.getLatitude(), l.getLongitude(), (float)l.getAltitude(), l.getAccuracy(), l.getSpeed(), l.getBearing());
            }
        } else {
        	mapButtons.setVisibility(View.VISIBLE);
            postInvalidate();
        }

        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.AUTO_CHECKIN)) {
        	CheckinManager.getInstance().autoCheckin(l.getLatitude(), l.getLongitude(), false);
        }
    }

    private void setBuiltInZoomControls(boolean enable) {
        if (mapProvider == ConfigurationManager.OSM_MAPS) {
            ((org.osmdroid.views.MapView) mapView).setBuiltInZoomControls(enable);
        } else {
            googleMapsView.setBuiltInZoomControls(enable);
        }
    }

    private void addOverlay(Object overlay) {
        if (mapProvider == ConfigurationManager.OSM_MAPS) {
            ((org.osmdroid.views.MapView) mapView).getOverlays().add((org.osmdroid.views.overlay.Overlay) overlay);
        } else {
            googleMapsView.getOverlays().add((com.google.android.maps.Overlay) overlay);
        }
    }

    private void addLandmarkOverlay() {
        if (mapProvider == ConfigurationManager.OSM_MAPS) {
        	 OsmLandmarkOverlay landmarkOverlay = null;
        	 if (LocationServicesManager.isGpsHardwarePresent()) {
                 landmarkOverlay = new OsmLandmarkOverlay(this, landmarkManager, loadingHandler);
             } else {
                 landmarkOverlay = new OsmLandmarkOverlay(this, landmarkManager, loadingHandler, new String[]{Commons.ROUTES_LAYER});
             }
            addOverlay(landmarkOverlay);
        } else {
            GoogleLandmarkOverlay landmarkOverlay = null;
            if (LocationServicesManager.isGpsHardwarePresent()) {
            	landmarkOverlay = new GoogleLandmarkOverlay(landmarkManager, loadingHandler);
            } else {
            	landmarkOverlay = new GoogleLandmarkOverlay(landmarkManager, loadingHandler, new String[]{Commons.ROUTES_LAYER});
            }
            addOverlay(landmarkOverlay);
        }
    }

    private void addRoutesOverlay(String routeName) {
        if (mapProvider == ConfigurationManager.OSM_MAPS) {
            OsmRoutesOverlay routesOverlay = new OsmRoutesOverlay((org.osmdroid.views.MapView) mapView, this, routeName);
            addOverlay(routesOverlay);
        } else {
            GoogleRoutesOverlay routesOverlay = new GoogleRoutesOverlay(this, routeName);
            addOverlay(routesOverlay);
        }
    }
    
    private void syncRoutesOverlays() {
    	int routesCount = RoutesManager.getInstance().getCount();
    	int routesOverlaysCount = 0;
    	
    	if (mapProvider == ConfigurationManager.OSM_MAPS) {
            for (Iterator<org.osmdroid.views.overlay.Overlay> iter = ((org.osmdroid.views.MapView) mapView).getOverlays().listIterator(); iter.hasNext();) {
            	if (iter.next() instanceof OsmRoutesOverlay) {
            		routesOverlaysCount++;
            	}
            }         
        } else {
            for (Iterator<com.google.android.maps.Overlay> iter = googleMapsView.getOverlays().listIterator(); iter.hasNext();) {
            	if (iter.next() instanceof GoogleRoutesOverlay) {
            		routesOverlaysCount++;
            	}
            }
        }
    	
    	boolean isRoutesEnabled = landmarkManager.getLayerManager().isLayerEnabled(Commons.ROUTES_LAYER);
    		
    	if ((routesCount == 0 || !isRoutesEnabled) && routesOverlaysCount > 0) {
    		if (mapProvider == ConfigurationManager.OSM_MAPS) {
    			for (Iterator<org.osmdroid.views.overlay.Overlay> iter = ((org.osmdroid.views.MapView) mapView).getOverlays().listIterator(); iter.hasNext();) {
    				if (iter.next() instanceof OsmRoutesOverlay) {
    					iter.remove();
    				}
    			}         
    		} else {
    			for (Iterator<com.google.android.maps.Overlay> iter = googleMapsView.getOverlays().listIterator(); iter.hasNext();) {
    				if (iter.next() instanceof GoogleRoutesOverlay) {
    					iter.remove();
    				}
    			}
    		}
    	} else if (routesCount > 0 && isRoutesEnabled && routesOverlaysCount == 0) {
    		for (String routeKey: RoutesManager.getInstance().getRoutes()) {
                addRoutesOverlay(routeKey);
            }
    		isRouteDisplayed = true;
    	}
    }

    private void postInvalidate() {
        if (mapView instanceof org.osmdroid.views.MapView) {
            ((org.osmdroid.views.MapView) mapView).postInvalidate();
        } else {
            googleMapsView.postInvalidate();
        }
    }

    private double[] getMyLocation() {
    	return landmarkManager.getMyLocation(mapView.getMapCenter().getLatitude(), mapView.getMapCenter().getLongitude());
    }

    private void showRouteAction(String routeKey) {
        LoggerUtils.debug("Adding route to view: " + routeKey);
        if (RoutesManager.getInstance().containsRoute(routeKey) && landmarkManager.getLayerManager().isLayerEnabled(Commons.ROUTES_LAYER)) {
            addRoutesOverlay(routeKey);
            isRouteDisplayed = true;
            if (!routeKey.startsWith(RouteRecorder.CURRENTLY_RECORDED)) {
                double[] locationAndZoom = RoutesManager.getInstance().calculateRouteCenterAndZoom(routeKey);
                IGeoPoint newCenter = new org.osmdroid.google.wrapper.GeoPoint(new GeoPoint(MathUtils.coordDoubleToInt(locationAndZoom[0]), MathUtils.coordDoubleToInt(locationAndZoom[1])));
                mapController.setCenter(newCenter);
                mapController.setZoom((int)locationAndZoom[2]);
            }
            postInvalidate();
        }
    }

    private String followMyPositionAction() {
        if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
            ConfigurationManager.getInstance().setOn(ConfigurationManager.FOLLOW_MY_POSITION);
            String route = RouteRecorder.getInstance().startRecording();
            showRouteAction(route);
            if (LayerLoader.getInstance().isLoading()) {
            	LayerLoader.getInstance().stopLoading();
            }
            List<ExtendedLandmark> myPosV = landmarkManager.getUnmodifableLayer(Commons.MY_POSITION_LAYER);
            if (!myPosV.isEmpty()) {
                ExtendedLandmark landmark = myPosV.get(0);
                ProjectionInterface projection = ProjectionFactory.getProjection(mapView, googleMapsView);
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
                String filename = RouteRecorder.getInstance().stopRecording();
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
        RoutesManager.getInstance().clearRoutesStore();
        syncRoutesOverlays();
        postInvalidate();
        intents.showInfoToast(Locale.getMessage(R.string.Maps_cleared));
    }
    
    private void animateTo(int[] coordsE6) {
    	GeoPoint g = new GeoPoint(coordsE6[0], coordsE6[1]);
        mapController.animateTo(new org.osmdroid.google.wrapper.GeoPoint(g));
    }
    
    private static class LoadingHandler extends Handler {
    	
        private WeakReference<GMSClientMainActivity> parentActivity;
    	
    	public LoadingHandler(GMSClientMainActivity parentActivity) {
    		this.parentActivity = new WeakReference<GMSClientMainActivity>(parentActivity);
    	}
    	
        @Override
        public void handleMessage(Message msg) {
        	GMSClientMainActivity activity = parentActivity.get();
        	if (activity != null && !activity.isFinishing()) {
        		if (msg.what == MessageStack.STATUS_MESSAGE) {
        			activity.statusBar.setText(MessageStack.getInstance().getMessage());
            	} else if (msg.what == MessageStack.STATUS_VISIBLE && !ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
            		activity.loadingImage.setVisibility(View.VISIBLE);
            	} else if (msg.what == MessageStack.STATUS_GONE) {
            		activity.loadingImage.setVisibility(View.GONE);
            	} else if (msg.what == LayerLoader.LAYER_LOADED) {
            		activity.postInvalidate();
            	} else if (msg.what == LayerLoader.ALL_LAYERS_LOADED) {
            		if (activity.mapProvider == ConfigurationManager.OSM_MAPS || activity.googleMapsView.canCoverCenter()) {
                		activity.asyncTaskManager.executeImageUploadTask(activity.mapView.getMapCenter().getLatitude(),
                            activity.mapView.getMapCenter().getLongitude(), false);
            		}	
            	} else if (msg.what == LayerLoader.FB_TOKEN_EXPIRED) {
            		activity.intents.showInfoToast(Locale.getMessage(R.string.Social_token_expired, "Facebook"));
            	} else if (msg.what == GoogleLandmarkOverlay.SHOW_LANDMARK_DETAILS || msg.what == OsmLandmarkOverlay.SHOW_LANDMARK_DETAILS) {
            		int[] coordsE6 = activity.intents.showLandmarkDetailsAction(activity.getMyLocation(), activity.lvView, activity.mapView.getZoomLevel(), ProjectionFactory.getProjection(activity.mapView, activity.googleMapsView));
                    if (coordsE6 != null) {
                    	activity.animateTo(coordsE6);
                    }
            	} else if (msg.what == SHOW_MAP_VIEW) {
                	View loading = activity.findViewById(R.id.mapCanvasWidgetL);
                	View mapCanvas = activity.findViewById(R.id.mapCanvasWidgetM);
                	loading.setVisibility(View.GONE);
                	mapCanvas.setVisibility(View.VISIBLE);
            	} else if (msg.what == AsyncTaskManager.SHOW_ROUTE_MESSAGE) {
            		activity.showRouteAction((String) msg.obj);
            	} else if (msg.what == LocationServicesManager.UPDATE_LOCATION) {
            		Location location = (Location) msg.obj;
                	activity.updateLocation(location);
            	} else if (msg.obj != null) {
            		LoggerUtils.error("Unknown message received: " + msg.obj.toString());
            	}
        	}
        }
    }
    
    private class ZoomListener implements ZoomChangeListener {

		@Override
		public void onZoom(int oldZoom, int currentZoom, float distance) {
			MapInfoView mapInfo = (MapInfoView) findViewById(R.id.info);
			
			mapInfo.setDistance(distance);
		    
			if (mapProvider == ConfigurationManager.OSM_MAPS) {
				mapInfo.setZoomLevel(mapView.getZoomLevel());
	            mapInfo.setMaxZoom(mapView.getMaxZoomLevel());
	        } else {
	        	mapInfo.setZoomLevel(googleMapsView.getZoomLevel());
	            mapInfo.setMaxZoom(googleMapsView.getMaxZoomLevel());
	        }
			
			mapInfo.postInvalidate();
		}
    }
}
