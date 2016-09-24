package com.jstakun.gms.android.ui.deals;

import java.lang.ref.WeakReference;
import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.google.maps.GoogleLandmarkOverlay;
import com.jstakun.gms.android.google.maps.GoogleLandmarkProjection;
import com.jstakun.gms.android.google.maps.GoogleMapsTypeSelector;
import com.jstakun.gms.android.google.maps.GoogleMyLocationOverlay;
import com.jstakun.gms.android.google.maps.GoogleRoutesOverlay;
import com.jstakun.gms.android.google.maps.ObservableMapView;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerLoader;
import com.jstakun.gms.android.location.LocationServicesManager;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.ui.AlertDialogBuilder;
import com.jstakun.gms.android.ui.AsyncTaskManager;
import com.jstakun.gms.android.ui.DialogManager;
import com.jstakun.gms.android.ui.HelpActivity;
import com.jstakun.gms.android.ui.IntentArrayAdapter;
import com.jstakun.gms.android.ui.IntentsHelper;
import com.jstakun.gms.android.ui.LandmarkListActivity;
import com.jstakun.gms.android.ui.MapInfoView;
import com.jstakun.gms.android.ui.ZoomChangeListener;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.MessageStack;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.ServicesUtils;
import com.jstakun.gms.android.utils.StringUtil;
import com.jstakun.gms.android.utils.UserTracker;

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
import android.widget.ProgressBar;
import android.widget.TextView;

public class DealMapActivity extends MapActivity implements OnClickListener {

    private static final int SHOW_MAP_VIEW = 0;
    
    private MapView mapView;
    private MapController mapController;
    private GoogleMyLocationOverlay myLocation;
    
    private DealOfTheDayDialog dealOfTheDayDialog;
    
    private TextView statusBar;
    private View lvCloseButton, lvCallButton, lvOpenButton, mapButtons,
            lvView, lvHotDealsButton, myLocationButton, nearbyLandmarksButton,
            newestButton, listButton, categoriesButton,
            lvShareButton, lvRouteButton, thumbnailButton, loadingImage;
    private ProgressBar loadingProgressBar;
    
    private boolean isStopped = false, appInitialized = false, isRouteDisplayed = false;
   
    //Handlers
    private Handler loadingHandler;
    private final Runnable gpsRunnable = new Runnable() {
        public void run() {
            GeoPoint location = myLocation.getMyLocation();
            if (location != null && !appInitialized) {  
                initOnLocationChanged(location);
            } else {
                if (ConfigurationManager.getInstance().isDefaultCoordinate()) {
                    //start only if helpactivity not on top
                    if (!ConfigurationManager.getInstance().containsObject(HelpActivity.HELP_ACTIVITY_SHOWN, String.class)) {
                    	IntentsHelper.getInstance().startPickLocationActivity();
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

        //UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        ConfigurationManager.getInstance().setContext(this);
        
        OsUtil.setDisplayType(getResources().getConfiguration()); 
        loadingHandler = new LoadingHandler(this);
        
        setContentView(R.layout.mapcanvasview);
        
        initComponents();
    }

    @Override
    protected boolean isRouteDisplayed() {
        return isRouteDisplayed;
    }

    private void initComponents() {

    	loadingProgressBar = (ProgressBar) findViewById(R.id.mapCanvasLoadingProgressBar);
    	loadingProgressBar.setProgress(25);
    	
    	statusBar = (TextView) findViewById(R.id.statusBar);
        loadingImage = findViewById(R.id.loadingAnim);
        lvView = findViewById(R.id.lvView);
        mapButtons = findViewById(R.id.mapButtons);

        lvCloseButton = findViewById(R.id.lvCloseButton);
        lvOpenButton = findViewById(R.id.lvOpenButton);
        lvShareButton = findViewById(R.id.lvShareButton);
        lvCallButton = findViewById(R.id.lvCallButton);
        lvRouteButton = findViewById(R.id.lvRouteButton);
        thumbnailButton = findViewById(R.id.thumbnailButton);
        nearbyLandmarksButton = findViewById(R.id.nearbyLandmarksButton);

        lvCloseButton.setOnClickListener(this);
        lvOpenButton.setOnClickListener(this);
        lvShareButton.setOnClickListener(this);
        lvCallButton.setOnClickListener(this);
        lvRouteButton.setOnClickListener(this);
        thumbnailButton.setOnClickListener(this);
        nearbyLandmarksButton.setOnClickListener(this);
        
        myLocationButton = findViewById(R.id.myLocationButton);
        newestButton = findViewById(R.id.newestButton);
        listButton = findViewById(R.id.listButton);
        categoriesButton = findViewById(R.id.layersButton);
        lvHotDealsButton = findViewById(R.id.hotDealsButton);

        newestButton.setOnClickListener(this);
        listButton.setOnClickListener(this);
        categoriesButton.setOnClickListener(this);
        lvHotDealsButton.setOnClickListener(this);
        myLocationButton.setOnClickListener(this);

        mapView = (MapView) findViewById(R.id.mapCanvas);
        mapView.setBuiltInZoomControls(true);

        ((ObservableMapView) mapView).setOnZoomChangeListener(new ZoomListener());
        
        myLocation = new GoogleMyLocationOverlay(this, mapView, loadingHandler, getResources().getDrawable(R.drawable.ic_maps_indicator_current_position));

        GeoPoint mapCenter = (GeoPoint) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, GeoPoint.class);

        if (mapCenter == null) {
            loadingHandler.postDelayed(gpsRunnable, ConfigurationManager.FIVE_SECONDS);
        }

        mapController = mapView.getController();
        mapController.setZoom(ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM));

        appInitialized = false;
        
        if (!CategoriesManager.getInstance().isInitialized()) {
            LoggerUtils.debug("Loading deal categories...");
            AsyncTaskManager.getInstance().executeDealCategoryLoaderTask(true);
        }

        if (mapCenter != null && mapCenter.getLatitudeE6() != 0 && mapCenter.getLongitudeE6() != 0) {
            initOnLocationChanged(mapCenter);
        } else {
            myLocation.runOnFirstFix(new Runnable() {
                public void run() {
                    if (!appInitialized) {
                        initOnLocationChanged(myLocation.getMyLocation());
                    }
                }
            });
        }
        
        loadingProgressBar.setProgress(50);
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
        MenuItem item = menu.findItem(R.id.mapMode);
        item.setVisible(false);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        UserTracker.getInstance().trackEvent("MenuClicks", item.getTitle().toString(), "", 0);
        switch (item.getItemId()) {
            case R.id.settings:
                IntentsHelper.getInstance().startSettingsActivity(SettingsActivity.class);
                break;
            case R.id.listMode:
                IntentsHelper.getInstance().startCategoryListActivity(mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6(), -1, -1);
                break;
            case R.id.pickMyPos:
                IntentsHelper.getInstance().startPickLocationActivity();
                break;
            case R.id.search:
                onSearchRequested();
                break;
            case R.id.refreshLayers:
                IntentsHelper.getInstance().loadLayersAction(true, null, false, false,
                        MathUtils.coordIntToDouble(mapView.getMapCenter().getLatitudeE6()),
                        MathUtils.coordIntToDouble(mapView.getMapCenter().getLongitudeE6()),
                        mapView.getZoomLevel(), new GoogleLandmarkProjection(mapView));
                break;
            case R.id.showMyLandmarks:
                IntentsHelper.getInstance().startMyLandmarksIntent(getMyLocation());
                break;
            case R.id.recentLandmarks:
                IntentsHelper.getInstance().startRecentLandmarksIntent(getMyLocation());
                break;
            case R.id.exit:
                DialogManager.getInstance().showExitAlertDialog(this);
                break;
            case R.id.about:
            	DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.INFO_DIALOG, null, null);
                break;
            case R.id.releaseNotes:
                IntentsHelper.getInstance().startHelpActivity();
                break;
            case R.id.showDoD:
                showRecommendedDeal(true);
                break;
            case R.id.reset:
            	DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.RESET_DIALOG, null, null);
            	break;
            case R.id.discoverPlaces:
                IntentsHelper.getInstance().startActionViewIntent(ConfigurationManager.getInstance().getString(ConfigurationManager.APP_URL));
                break;
            //case R.id.events:
                //IntentsHelper.getInstance().startCalendarActivity(getMyPosition());
                //break;
            case R.id.rateUs:
            	DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.RATE_US_DIALOG, null, null);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private synchronized void initOnLocationChanged(GeoPoint location) {
        if (!appInitialized && location != null) {
        	loadingProgressBar.setProgress(75);
            mapController.setCenter(location);
            
            if (!LandmarkManager.getInstance().isInitialized()) {
                //UserTracker.getInstance().sendMyLocation();
            	LandmarkManager.getInstance().initialize(Commons.LOCAL_LAYER, Commons.ROUTES_LAYER, Commons.MY_POSITION_LAYER, Commons.COUPONS_LAYER,
                		Commons.HOTELS_LAYER, Commons.GROUPON_LAYER, Commons.FOURSQUARE_MERCHANT_LAYER, Commons.YELP_LAYER);
            }
            
            GoogleLandmarkOverlay landmarkOverlay = new GoogleLandmarkOverlay(loadingHandler);
            mapView.getOverlays().add(landmarkOverlay);
            //must be on top of other overlays
            //mapView.getOverlays().add(infoOverlay);
            mapView.getOverlays().add(myLocation);

            MessageStack.getInstance().setHandler(loadingHandler);
            LayerLoader.getInstance().setRepaintHandler(loadingHandler);
            
            if (!LayerLoader.getInstance().isInitialized() && !LayerLoader.getInstance().isLoading()) {
            	LoggerUtils.debug("Loading Layers...");
                IntentsHelper.getInstance().loadLayersAction(true, null, false, false,
                        MathUtils.coordIntToDouble(location.getLatitudeE6()),
                        MathUtils.coordIntToDouble(location.getLongitudeE6()),
                        mapView.getZoomLevel(), new GoogleLandmarkProjection(mapView));
            } else {
            	//load existing layers
                if (LayerLoader.getInstance().isLoading()) {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_VISIBLE);
                } else {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_GONE);
                }
                loadingHandler.sendEmptyMessage(MessageStack.STATUS_MESSAGE);
                mapView.postInvalidate();
            }
            
            loadingProgressBar.setProgress(100);
             
            loadingHandler.sendEmptyMessage(SHOW_MAP_VIEW);
            
            appInitialized = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	IntentsHelper.getInstance().setActivity(this);
    	if (requestCode == IntentsHelper.INTENT_MULTILANDMARK) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);

                if (action.equals("load")) {
                    int id = Integer.parseInt(ids);
                    int[] coordsE6 = IntentsHelper.getInstance().showSelectedLandmark(id, getMyLocation(), lvView, mapView.getZoomLevel(), new GoogleLandmarkProjection(mapView));
                    if (coordsE6 != null) {
                    	animateTo(coordsE6);
                    }
                }
            }
        } else if (requestCode == IntentsHelper.INTENT_PICKLOCATION) {
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

                LandmarkManager.getInstance().addLandmark(lat, lng, 0.0f, StringUtil.formatCommaSeparatedString(names), "", Commons.LOCAL_LAYER, true);

            } else if (resultCode == RESULT_CANCELED && !appInitialized) {
                ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
                IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Pick_location_default, landmark.getName()));
                GeoPoint location = new GeoPoint(landmark.getLatitudeE6(), landmark.getLongitudeE6());
                initOnLocationChanged(location);
            } else if (resultCode == RESULT_CANCELED && intent.hasExtra("message")) {
                String message = intent.getStringExtra("message");
                IntentsHelper.getInstance().showInfoToast(message);
            } else if (resultCode != RESULT_CANCELED) { //if (!appInitialized) {
                IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
            }
        } else if (requestCode == IntentsHelper.INTENT_MYLANDMARKS) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);
                int id = Integer.parseInt(ids);

                if (action.equals("load")) {
                    ExtendedLandmark l = LandmarkManager.getInstance().getPhoneLandmark(id);
                    if (l != null) {
                        GeoPoint location = new GeoPoint(l.getLatitudeE6(), l.getLongitudeE6());
                        pickPositionAction(location, true, true, true);
                    }
                } else if (action.equals("delete")) {
                    //delete landmark
                	LandmarkManager.getInstance().deletePhoneLandmark(id);
                    IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Landmark_deleted));
                }
            }
        } else if (requestCode == IntentsHelper.INTENT_CALENDAR) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);

                if (action.equals("load")) {
                    int id = Integer.parseInt(ids);
                    int[] coordsE6 = IntentsHelper.getInstance().showSelectedLandmark(id, getMyLocation(), lvView, mapView.getZoomLevel(), new GoogleLandmarkProjection(mapView));
                    if (coordsE6 != null) {
                    	animateTo(coordsE6);
                    }
                }
            }
        } else {
            IntentsHelper.getInstance().processActivityResult(requestCode, resultCode, intent, getMyLocation(), null, null, -1, new GoogleLandmarkProjection(mapView));
        }
    }

    public void onClick(View v) {
    	if (v == myLocationButton) {
    		showMyPositionAction(true);
    	} else if (v == lvCloseButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CloseSelectedDealView", "", 0);
            hideLandmarkView();
        } else if (v == lvOpenButton || v == thumbnailButton) {
            ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getSeletedLandmarkUI();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenSelectedDealURL", selectedLandmark.getLayer(), 0);
            IntentsHelper.getInstance().openButtonPressedAction(LandmarkManager.getInstance().getSeletedLandmarkUI());
        } else if (v == lvCallButton) {
            ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getSeletedLandmarkUI();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CallSelectedDeal", selectedLandmark.getLayer(), 0);
            callButtonPressedAction(LandmarkManager.getInstance().getSeletedLandmarkUI());
        } else if (v == lvRouteButton) {
            ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getSeletedLandmarkUI();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowRouteSelectedDeal", selectedLandmark.getLayer(), 0);
            loadRoutePressedAction(LandmarkManager.getInstance().getSeletedLandmarkUI());
        } else if (v == newestButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowNewestDeals", "", 0);
            final String[] excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER, Commons.LOCAL_LAYER};
            IntentsHelper.getInstance().startNewestLandmarkIntent(getMyLocation(), excluded, 2);
        } else if (v == listButton || v == nearbyLandmarksButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowVisibleDeals", "", 0);
            IntentsHelper.getInstance().showNearbyLandmarks(getMyLocation(), new GoogleLandmarkProjection(mapView));
        } else if (v == categoriesButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowDealCategoriesList", "", 0);
            IntentsHelper.getInstance().startCategoryListActivity(mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6(), -1, -1);
        } else if (v == lvShareButton) {
            ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getSeletedLandmarkUI();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShareSelectedDeal", selectedLandmark.getLayer(), 0);
            sendMessageAction();
        } else if (v == lvHotDealsButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowHotDeals", "", 0);
            IntentsHelper.getInstance().startDealsOfTheDayIntent(getMyLocation(), null);
        }
    }

    @Override
    public boolean onSearchRequested() {
        if (appInitialized) {
            IntentsHelper.getInstance().startSearchActivity(mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6(), -1, true);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LoggerUtils.debug("onResume");
        isStopped = false;

        myLocation.enableMyLocation();

        GoogleMapsTypeSelector.selectMapType(mapView);

        AsyncTaskManager.getInstance().setContext(this);
        
        IntentsHelper.getInstance().setActivity(this);
        
        if (LandmarkManager.getInstance().hasMyLocation()){
        	mapButtons.setVisibility(View.VISIBLE);
        }
        
        AsyncTaskManager.getInstance().executeNewVersionCheckTask(this);
        //verify access token
        AsyncTaskManager.getInstance().executeGetTokenTask();
        
        Integer searchQueryResult = (Integer) ConfigurationManager.getInstance().removeObject(ConfigurationManager.SEARCH_QUERY_RESULT, Integer.class);
        if (searchQueryResult != null) {
        	int[] coordsE6 = IntentsHelper.getInstance().showSelectedLandmark(searchQueryResult, getMyLocation(), lvView, mapView.getZoomLevel(), new GoogleLandmarkProjection(mapView));
            if (coordsE6 != null) {
            	animateTo(coordsE6);
            }
        } else if (LandmarkManager.getInstance().getSeletedLandmarkUI() != null) {
            ExtendedLandmark landmark = LandmarkManager.getInstance().getSeletedLandmarkUI();
            IntentsHelper.getInstance().showLandmarkDetailsView(landmark, lvView, getMyLocation(), true);
        }

        if (ConfigurationManager.getInstance().containsObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class)) {
            int type = (Integer) ConfigurationManager.getInstance().getObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            IntentArrayAdapter arrayAdapter = null;
            if (type == AlertDialogBuilder.DEAL_OF_THE_DAY_DIALOG) {
                showRecommendedDeal(true);
            } else {
                if (type == AlertDialogBuilder.SHARE_INTENTS_DIALOG) {
                    List<ResolveInfo> intentList = IntentsHelper.getInstance().getSendIntentsList();
                    if (!intentList.isEmpty()) {
                        arrayAdapter = new IntentArrayAdapter(this, intentList);
                    }
                }
                DialogManager.getInstance().showAlertDialog(this, type, arrayAdapter, null);
            }
        }

        IntentsHelper.getInstance().onAppVersionChanged();

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
    }

    @Override
    public void onStart() {
        super.onStart();
        Object networkStatus = ConfigurationManager.getInstance().getObject("NetworkStatus", Object.class);
        boolean networkActive = ServicesUtils.isNetworkActive(this);
        if (networkStatus == null && !networkActive) {
        	DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.NETWORK_ERROR_DIALOG, null, null);
            ConfigurationManager.getInstance().putObject("NetworkStatus", new Object());
        }

        Object rateDialogStatus = ConfigurationManager.getInstance().getObject("rateDialogStatus", Object.class);
        if (rateDialogStatus == null && ConfigurationManager.getInstance().isOff(ConfigurationManager.APP_RATED) && networkActive) {
            int useCount = ConfigurationManager.getInstance().getInt(ConfigurationManager.USE_COUNT);
            //show rate us dialog
            if (useCount > 0 && (useCount % 10) == 0) {
            	DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.RATE_US_DIALOG, null, null);
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
        int type = DialogManager.getInstance().dismissDialog(this);
        if (type == AlertDialogBuilder.DEAL_OF_THE_DAY_DIALOG) {
            dealOfTheDayDialog.dismiss();
        } else if (dealOfTheDayDialog != null && dealOfTheDayDialog.isShowing()) {
            ConfigurationManager.getInstance().putObject(AlertDialogBuilder.OPEN_DIALOG, AlertDialogBuilder.DEAL_OF_THE_DAY_DIALOG);
            dealOfTheDayDialog.dismiss();
        }
        //UserTracker.getInstance().stopSession(this);
    }

    @Override
    public void onDestroy() {
    	LoggerUtils.debug("onDestroy");
        super.onDestroy();
        if (ConfigurationManager.getInstance().isClosing()) {
        	appInitialized = false;
            IntentsHelper.getInstance().hardClose(loadingHandler, gpsRunnable, mapView.getZoomLevel(), mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6());
        } else if (mapView.getMapCenter().getLatitudeE6() != 0 && mapView.getMapCenter().getLongitudeE6() != 0) {
        	IntentsHelper.getInstance().softClose(mapView.getZoomLevel(), mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6());
            ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, mapView.getMapCenter());
        }
        AdsUtils.destroyAdView(this);
        System.gc();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	UserTracker.getInstance().trackEvent("onKeyDown", "", "", 0);
    	//System.out.println("Key pressed in activity: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (lvView.isShown()) {
            	hideLandmarkView();
            } else {
            	DialogManager.getInstance().showExitAlertDialog(this);
            }
            //System.out.println("key back pressed in activity");
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
        	int[] coordsE6 = IntentsHelper.getInstance().showLandmarkDetailsAction(getMyLocation(), lvView, mapView.getZoomLevel(), new GoogleLandmarkProjection(mapView));
            if (coordsE6 != null) {
            	animateTo(coordsE6);
            }return true;
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
    
    private void hideLandmarkView() {
    	lvView.setVisibility(View.GONE);
    	LandmarkManager.getInstance().clearLandmarkOnFocusQueue();
    	LandmarkManager.getInstance().setSelectedLandmark(null);
    	LandmarkManager.getInstance().setSeletedLandmarkUI();
    }

    private double[] getMyLocation() {
    	return LandmarkManager.getInstance().getMyLocation(MathUtils.coordIntToDouble(mapView.getMapCenter().getLatitudeE6()), MathUtils.coordIntToDouble(mapView.getMapCenter().getLongitudeE6()));
    }

    private void loadRoutePressedAction(ExtendedLandmark landmark) {
    	AsyncTaskManager.getInstance().executeRouteServerLoadingTask(loadingHandler, false, landmark);
    }

    private void callButtonPressedAction(ExtendedLandmark landmark) {
        IntentsHelper.getInstance().startPhoneCallActivity(landmark);
    }

    private void sendMessageAction() {
        IntentsHelper.getInstance().shareLandmarkAction();
    }

    private void pickPositionAction(GeoPoint newCenter, boolean loadLayers, boolean animate, boolean clearMap) {
        mapController.setCenter(newCenter);
        if (loadLayers) {
            IntentsHelper.getInstance().loadLayersAction(true, null, clearMap, false, 
                    MathUtils.coordIntToDouble(mapView.getMapCenter().getLatitudeE6()),
                    MathUtils.coordIntToDouble(mapView.getMapCenter().getLongitudeE6()),
                    mapView.getZoomLevel(), new GoogleLandmarkProjection(mapView));
        }
    }

    private void showRouteAction(String routeKey) {
        if (RoutesManager.getInstance().containsRoute(routeKey)) {
            GoogleRoutesOverlay routeOverlay = new GoogleRoutesOverlay(this, routeKey);
            mapView.getOverlays().add(routeOverlay);
            isRouteDisplayed = true;
            mapView.postInvalidate();
        }
    }

    private void showMyPositionAction(boolean loadLayers) {
        GeoPoint g = myLocation.getMyLocation();
        if (g != null) {
            boolean isVisible = false;
            boolean clearLandmarks = false;

            GoogleLandmarkProjection projection = new GoogleLandmarkProjection(mapView);
            if (projection.isVisible(g.getLatitudeE6(), g.getLongitudeE6())) {
                isVisible = true;
            }

            if (!isVisible) {
            	hideLandmarkView();
            	GeoPoint mapCenter = mapView.getMapCenter();
                clearLandmarks = IntentsHelper.getInstance().isClearLandmarksRequired(projection, mapCenter.getLatitudeE6(), mapCenter.getLongitudeE6(),
                        g.getLatitudeE6(), g.getLongitudeE6());
            }

            mapController.animateTo(g);

            if (loadLayers && !isVisible) {
                //mapController.setCenter(g);
                IntentsHelper.getInstance().loadLayersAction(true, null, clearLandmarks, false,MathUtils.coordIntToDouble(g.getLatitudeE6()),
                        MathUtils.coordIntToDouble(g.getLongitudeE6()), mapView.getZoomLevel(), new GoogleLandmarkProjection(mapView));
            }
        } else {
            IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
        }
    }

    private void showRecommendedDeal(boolean forceToShow) {
        ExtendedLandmark recommended = (ExtendedLandmark) ConfigurationManager.getInstance().getObject("dod", ExtendedLandmark.class);

        if (recommended == null) {
            //System.out.println("Top subcategory stats: " + cm.getTopCategory() + " " + cm.getTopSubCategory() + " " + cm.getTopSubCategoryStats());
            if (CategoriesManager.getInstance().getTopSubCategoryStats() > ConfigurationManager.getInstance().getInt(ConfigurationManager.DEAL_RECOMMEND_CAT_STATS)
                    && (ConfigurationManager.getInstance().isOn(ConfigurationManager.SHOW_DEAL_OF_THE_DAY) || forceToShow)) {
                //System.out.println(cm.getTopCategory() + " " + cm.getTopSubCategory());
                recommended = LandmarkManager.getInstance().findRecommendedLandmark();
                if (recommended != null) {
                    ConfigurationManager.getInstance().putObject("dod", recommended);
                }
            }
        }

        if (recommended != null) {
        	LandmarkManager.getInstance().setSelectedLandmark(recommended);
            dealOfTheDayDialog = new DealOfTheDayDialog(this, recommended, getMyLocation(), loadingHandler, IntentsHelper.getInstance());
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
            IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.noDodAvailable));
        } else {
            //String status = "Recommended == null\n"
            //        + "SubCategoryStats: " + cm.getTopSubCategoryStats() + " " + cm.getTopCategory() + " " + cm.getTopSubCategory() + "\n"
            //        + "showDealOfTheDay: " + ConfigurationManager.getInstance().isOn(ConfigurationManager.SHOW_DEAL_OF_THE_DAY) + "\n"
            //        + "shownDeals: " + ConfigurationManager.getInstance().getString(ConfigurationManager.RECOMMENDED_DEALS_SHOWN)
            //comment out
            //IntentsHelper.getInstance().showInfoToast(status);
            //System.out.println(status);
        }
    }
    
    private void animateTo(int[] coordsE6) {
    	GeoPoint g = new GeoPoint(coordsE6[0], coordsE6[1]);
        mapController.animateTo(g);
    }
    
    private static class LoadingHandler extends Handler {
    	private WeakReference<DealMapActivity> parentActivity;
    	
    	public LoadingHandler(DealMapActivity parentActivity) {
    		this.parentActivity = new WeakReference<DealMapActivity>(parentActivity);
    	}
    	
        @Override
        public void handleMessage(Message msg) {
        	DealMapActivity activity = parentActivity.get();
        	if (activity != null && !activity.isFinishing()) {
        		if (msg.what == MessageStack.STATUS_MESSAGE) {
        			activity.statusBar.setText(MessageStack.getInstance().getMessage());
            	} else if (msg.what == MessageStack.STATUS_VISIBLE) {
            		activity.loadingImage.setVisibility(View.VISIBLE);
            	} else if (msg.what == MessageStack.STATUS_GONE) {
            		activity.loadingImage.setVisibility(View.GONE);
            	} else if (msg.what == DealOfTheDayDialog.OPEN) {
                	ExtendedLandmark recommended = (ExtendedLandmark) ConfigurationManager.getInstance().getObject("dod", ExtendedLandmark.class);
                	IntentsHelper.getInstance().openButtonPressedAction(recommended);
            	} else if (msg.what == DealOfTheDayDialog.CALL) {
                	ExtendedLandmark recommended = (ExtendedLandmark) ConfigurationManager.getInstance().getObject("dod", ExtendedLandmark.class);
                	activity.callButtonPressedAction(recommended);
            	} else if (msg.what == DealOfTheDayDialog.ROUTE) {
                	ExtendedLandmark recommended = (ExtendedLandmark) ConfigurationManager.getInstance().getObject("dod", ExtendedLandmark.class);
                	activity.loadRoutePressedAction(recommended);
            	} else if (msg.what == DealOfTheDayDialog.SEND_MAIL) {
            		activity.sendMessageAction();
            	} else if (msg.what == LayerLoader.LAYER_LOADED) {
            		activity.mapView.postInvalidate();
            	} else if (msg.what == LayerLoader.ALL_LAYERS_LOADED) {
            		activity.showRecommendedDeal(false);
            		if (activity.mapView.canCoverCenter()) {          			
            			AsyncTaskManager.getInstance().executeImageUploadTask(activity, MathUtils.coordIntToDouble(activity.mapView.getMapCenter().getLatitudeE6()),
                            MathUtils.coordIntToDouble(activity.mapView.getMapCenter().getLongitudeE6()), false);
            		}
            	} else if (msg.what == GoogleLandmarkOverlay.SHOW_LANDMARK_DETAILS) {
            		int[] coordsE6 = IntentsHelper.getInstance().showLandmarkDetailsAction(activity.getMyLocation(), activity.lvView, activity.mapView.getZoomLevel(), new GoogleLandmarkProjection(activity.mapView));
                    if (coordsE6 != null) {
                    	activity.animateTo(coordsE6);
                    }
            	} else if (msg.what == SHOW_MAP_VIEW) {
            		View loading = activity.findViewById(R.id.mapCanvasWidgetL);
            		loading.setVisibility(View.GONE);
                	View mapCanvas = activity.findViewById(R.id.mapCanvasWidgetM);
                	mapCanvas.setVisibility(View.VISIBLE);
                	//System.out.println("SHOW_MAP_VIEW done");
            	} else if (msg.what == AsyncTaskManager.SHOW_ROUTE_MESSAGE) {
            		activity.showRouteAction((String) msg.obj);
            	} else if (msg.what == LocationServicesManager.UPDATE_LOCATION) {
                	Location location = (Location) msg.obj;
                	IntentsHelper.getInstance().addMyLocationLandmark(location);
        			activity.mapButtons.setVisibility(View.VISIBLE);
                	IntentsHelper.getInstance().vibrateOnLocationUpdate();
                	UserTracker.getInstance().sendMyLocation();
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
			mapInfo.setZoomLevel(mapView.getZoomLevel());
			mapInfo.setMaxZoom(mapView.getMaxZoomLevel());
			mapInfo.postInvalidate();
		}
    }
}
