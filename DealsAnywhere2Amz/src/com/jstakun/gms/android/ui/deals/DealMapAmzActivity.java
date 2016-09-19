package com.jstakun.gms.android.ui.deals;

import java.lang.ref.WeakReference;
import java.util.List;

import com.amazon.geo.maps.GeoPoint;
import com.amazon.geo.maps.MapActivity;
import com.amazon.geo.maps.MapController;
import com.amazon.geo.maps.MapView;
import com.amazon.geo.maps.Overlay;
import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.amz.maps.AmzLandmarkOverlay;
import com.jstakun.gms.android.amz.maps.AmzLandmarkProjection;
import com.jstakun.gms.android.amz.maps.AmzMyLocationOverlay;
import com.jstakun.gms.android.amz.maps.AmzRoutesOverlay;
import com.jstakun.gms.android.amz.maps.ObservableMapView;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerLoader;
import com.jstakun.gms.android.location.SkyhookUtils;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.ui.AlertDialogBuilder;
import com.jstakun.gms.android.ui.AsyncTaskManager;
import com.jstakun.gms.android.ui.DialogManager;
import com.jstakun.gms.android.ui.HelpActivity;
import com.jstakun.gms.android.ui.IntentArrayAdapter;
import com.jstakun.gms.android.ui.IntentsHelper;
import com.jstakun.gms.android.ui.LandmarkListActivity;
import com.jstakun.gms.android.ui.MapInfoView;
import com.jstakun.gms.android.ui.NavigationDrawerListItem;
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
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DealMapAmzActivity extends MapActivity implements OnClickListener {

    private static final int SHOW_MAP_VIEW = 0;
    
    private MapView mapView;
    private MapController mapController;
    private SkyhookUtils skyhook;
    
    private DealOfTheDayDialog dealOfTheDayDialog;
    
    private TextView statusBar;
    private View lvCloseButton, lvCallButton, lvOpenButton, mapButtons, nearbyLandmarksButton,
            lvView, lvShareButton, lvRouteButton, myLocationButton,
            thumbnailButton, loadingImage;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private ProgressBar loadingProgressBar;
    
    private boolean appAbort = false, isStopped = false, appInitialized = false, isRouteDisplayed = false;
    
    //Handlers
    private Handler loadingHandler;
    
    private final Runnable gpsRunnable = new Runnable() {
        public void run() {
            GeoPoint location = getMyLocation();
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

        if (!appAbort) {
        	ConfigurationManager.getInstance().setContext(this);
            skyhook = new SkyhookUtils(this, loadingHandler);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
            OsUtil.setDisplayType(getResources().getConfiguration());
            getActionBar().hide();

            setContentView(R.layout.mapcanvasview_amz);
            
            loadingHandler = new LoadingHandler(this);

            initComponents();
        }
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
        myLocationButton = findViewById(R.id.myLocationButton);
        nearbyLandmarksButton = findViewById(R.id.nearbyLandmarksButton);

        lvCloseButton.setOnClickListener(this);
        lvOpenButton.setOnClickListener(this);
        lvShareButton.setOnClickListener(this);
        lvCallButton.setOnClickListener(this);
        lvRouteButton.setOnClickListener(this);
        thumbnailButton.setOnClickListener(this);
        myLocationButton.setOnClickListener(this);
        nearbyLandmarksButton.setOnClickListener(this);

        mapView = (MapView) findViewById(R.id.mapCanvas);
        mapView.setBuiltInZoomControls(true);
        //set this to solve path painting issue
        mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        drawerList.setAdapter(new NavigationDrawerListAdapter(this, R.layout.drawerrow_parent));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());
        
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout,  R.drawable.ic_drawer, 
                R.string.app_name,  /* "open drawer" description for accessibility */
                R.string.app_name  /* "close drawer" description for accessibility */
                ) {
        	public void onDrawerClosed(View view) {
            	super.onDrawerClosed(view);
            	invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
            	super.onDrawerOpened(drawerView);
            	invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
       
        ((ObservableMapView) mapView).setOnZoomChangeListener(new ZoomListener());
        
        GeoPoint mapCenter = (GeoPoint) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, GeoPoint.class);

        if (mapCenter == null) {
            loadingHandler.postDelayed(gpsRunnable, ConfigurationManager.FIVE_SECONDS);
        }

        //amzMapsView.getOverlays().add(myLocation);

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
            skyhook.runOnFirstFix(new Runnable() {
                public void run() {
                    if (!appInitialized) {
                        initOnLocationChanged(getMyLocation());
                    }
                }
            });
        }
        
        loadingProgressBar.setProgress(50);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu_2, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	if (drawerLayout.isDrawerOpen(drawerList)) {
    		NavigationDrawerListAdapter adapter = (NavigationDrawerListAdapter) drawerList.getAdapter();
    		adapter.rebuild();
    	}
    	return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (drawerToggle.onOptionsItemSelected(item)) {
    		return true;
        } else {
        	UserTracker.getInstance().trackEvent("MenuClicks", item.getTitle().toString(), "", 0);
        	return onMenuOptionSelected(item.getItemId());
        }
    }

	private boolean onMenuOptionSelected(int itemId) {
		switch (itemId) {
            case R.id.hotDeals:
                IntentsHelper.getInstance().startDealsOfTheDayIntent(getMyPosition(), null);
                break;
            case R.id.newestDeals:
                final String[] excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER, Commons.LOCAL_LAYER};
                IntentsHelper.getInstance().startNewestLandmarkIntent(getMyPosition(), excluded, 2);
                break;
            case R.id.nearbyDeals:
                if (!lvView.isShown()) {
                    IntentsHelper.getInstance().showNearbyLandmarks(getMyPosition(), new AmzLandmarkProjection(mapView));
                }
                break;
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
                        mapView.getZoomLevel(), new AmzLandmarkProjection(mapView));
                break;
            case R.id.showMyLandmarks:
                IntentsHelper.getInstance().startMyLandmarksIntent(getMyPosition());
                break;
            case R.id.recentLandmarks:
                IntentsHelper.getInstance().startRecentLandmarksIntent(getMyPosition());
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
            case R.id.discoverPlaces:
                IntentsHelper.getInstance().startActionViewIntent(ConfigurationManager.getInstance().getString(ConfigurationManager.APP_URL));
                break;
            case R.id.events:
                IntentsHelper.getInstance().startCalendarActivity(getMyPosition());
                break;
            case R.id.rateUs:
            	DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.RATE_US_DIALOG, null, null);
                break;
            case R.id.reset:
            	DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.RESET_DIALOG, null, null);
            	break;     
            default:
                return true;
        }
        return true;
	}

    private synchronized void initOnLocationChanged(GeoPoint location) {
        if (!appInitialized && location != null) {
        	loadingProgressBar.setProgress(75);
            mapController.setCenter(location);
            IntentsHelper.getInstance().softClose(mapView.getZoomLevel(), mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6());; //save mapcenter coords

            if (!LandmarkManager.getInstance().isInitialized()) {
            	LandmarkManager.getInstance().initialize(Commons.LOCAL_LAYER, Commons.ROUTES_LAYER, Commons.MY_POSITION_LAYER, Commons.COUPONS_LAYER,
                		Commons.HOTELS_LAYER, Commons.GROUPON_LAYER, Commons.FOURSQUARE_MERCHANT_LAYER, Commons.YELP_LAYER);
            }

            AmzLandmarkOverlay landmarkOverlay = new AmzLandmarkOverlay(loadingHandler, new String[]{Commons.ROUTES_LAYER});
            mapView.getOverlays().add(landmarkOverlay);
            
            //must be on top of other overlays
            //mapView.getOverlays().add(infoOverlay);

            MessageStack.getInstance().setHandler(loadingHandler);
            LayerLoader.getInstance().setRepaintHandler(loadingHandler);
            
            if (!LayerLoader.getInstance().isInitialized() && !LayerLoader.getInstance().isLoading()) {
                LoggerUtils.debug("Loading Layers...");
                IntentsHelper.getInstance().loadLayersAction(true, null, false, false, 
                        MathUtils.coordIntToDouble(location.getLatitudeE6()),
                        MathUtils.coordIntToDouble(location.getLongitudeE6()),
                        mapView.getZoomLevel(), new AmzLandmarkProjection(mapView));
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
        if (requestCode == IntentsHelper.INTENT_MULTILANDMARK) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);

                if (action.equals("load")) {
                    int id = Integer.parseInt(ids);
                    int[] coordsE6 = IntentsHelper.getInstance().showSelectedLandmark(id, getMyPosition(), lvView, mapView.getZoomLevel(), new AmzLandmarkProjection(mapView));
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
            } else if (resultCode != RESULT_CANCELED) { 
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
                    int[] coordsE6 = IntentsHelper.getInstance().showSelectedLandmark(id, getMyPosition(), lvView, mapView.getZoomLevel(), new AmzLandmarkProjection(mapView));
                    if (coordsE6 != null) {
                    	animateTo(coordsE6);
                    }
                }
            }
        } else {
            IntentsHelper.getInstance().processActivityResult(requestCode, resultCode, intent, getMyPosition(), null, null, -1, new AmzLandmarkProjection(mapView));
        }
    }

    public void onClick(View v) {
    	if (v == myLocationButton) {
    		showMyPositionAction(true);
    	} else if (v == nearbyLandmarksButton) {
    		IntentsHelper.getInstance().showNearbyLandmarks(getMyPosition(), new AmzLandmarkProjection(mapView));
    	} else {
    		ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getSeletedLandmarkUI();
    		if (selectedLandmark != null) {
    			if (v == lvCloseButton) {
    				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CloseSelectedDealView", "", 0);
    				hideLandmarkView();
				} else if (v == lvOpenButton || v == thumbnailButton) {
					UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenSelectedDealURL", selectedLandmark.getLayer(), 0);
					IntentsHelper.getInstance().openButtonPressedAction(LandmarkManager.getInstance().getSeletedLandmarkUI());
				} else if (v == lvCallButton) {
					UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CallSelectedDeal", selectedLandmark.getLayer(), 0);
					callButtonPressedAction(LandmarkManager.getInstance().getSeletedLandmarkUI());
				} else if (v == lvRouteButton) {
					UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowRouteSelectedDeal", selectedLandmark.getLayer(), 0);
					loadRoutePressedAction(LandmarkManager.getInstance().getSeletedLandmarkUI());
				} else if (v == lvShareButton) {
					UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShareSelectedDeal", selectedLandmark.getLayer(), 0);
					sendMessageAction();
				}
    		} else {
    			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
    		}
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

        //myLocation.enableMyLocation();
        skyhook.enableMyLocation();

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
        	int[] coordsE6 = IntentsHelper.getInstance().showSelectedLandmark(searchQueryResult, getMyPosition(), lvView, mapView.getZoomLevel(), new AmzLandmarkProjection(mapView));
            if (coordsE6 != null) {
            	animateTo(coordsE6);
            }
        } else if (LandmarkManager.getInstance().getSeletedLandmarkUI() != null) {
            getActionBar().hide();
            ExtendedLandmark landmark = LandmarkManager.getInstance().getSeletedLandmarkUI();
            IntentsHelper.getInstance().showLandmarkDetailsView(landmark, lvView, getMyPosition(), true);
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
        LoggerUtils.debug("onPause");
        //myLocation.disableMyLocation();
        skyhook.disableMyLocation();
        try {
            super.onPause();
        } catch (Exception e) {
            LoggerUtils.error("DealMapActivity.onPause exception:", e);
        }
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
        super.onDestroy();
        LoggerUtils.debug("onDestroy");
        if (!appAbort) {
            if (ConfigurationManager.getInstance().isClosing()) {
            	appInitialized = false;
                IntentsHelper.getInstance().hardClose(loadingHandler, gpsRunnable, mapView.getZoomLevel(), mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6());
            } else if (mapView.getMapCenter().getLatitudeE6() != 0 && mapView.getMapCenter().getLongitudeE6() != 0) {
            	IntentsHelper.getInstance().softClose(mapView.getZoomLevel(), mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6());
                ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, mapView.getMapCenter());
            }
            AdsUtils.destroyAdView(this);
        }
        System.gc();
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        drawerToggle.onConfigurationChanged(newConfig);
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
        	int[] coordsE6 = IntentsHelper.getInstance().showLandmarkDetailsAction(getMyPosition(), lvView, mapView.getZoomLevel(), new AmzLandmarkProjection(mapView));
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
        getActionBar().show();
        LandmarkManager.getInstance().clearLandmarkOnFocusQueue();
        LandmarkManager.getInstance().setSelectedLandmark(null);
        LandmarkManager.getInstance().setSeletedLandmarkUI();
    }
    
    protected double[] getMyPosition() {
        return LandmarkManager.getInstance().getMyLocation(mapView.getMapCenter().getLatitudeE6(),
                mapView.getMapCenter().getLongitudeE6());
    }

    protected void loadRoutePressedAction(ExtendedLandmark landmark) {
    	AsyncTaskManager.getInstance().executeRouteServerLoadingTask(loadingHandler, false, landmark);
    }

    protected void callButtonPressedAction(ExtendedLandmark landmark) {
        IntentsHelper.getInstance().startPhoneCallActivity(landmark);
    }

    //protected void openButtonPressedAction(ExtendedLandmark landmark) {
    //    IntentsHelper.getInstance().startLandmarkDetailsActivity(landmarkManager.getLandmarkURL(landmark), landmark.getName());
    //}

    protected void sendMessageAction() {
        List<ResolveInfo> intentList = IntentsHelper.getInstance().getSendIntentsList();
        if (!intentList.isEmpty()) {
            IntentArrayAdapter arrayAdapter = new IntentArrayAdapter(this, intentList);
            DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.SHARE_INTENTS_DIALOG, arrayAdapter, null);
        } else {
            IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Share_no_share_apps));
        }
    }

    private void pickPositionAction(GeoPoint newCenter, boolean loadLayers, boolean animate, boolean clearMap) {
        mapController.setCenter(newCenter);
        if (loadLayers) {
            IntentsHelper.getInstance().loadLayersAction(true, null, clearMap, false,
                    MathUtils.coordIntToDouble(mapView.getMapCenter().getLatitudeE6()),
                    MathUtils.coordIntToDouble(mapView.getMapCenter().getLongitudeE6()),
                    mapView.getZoomLevel(), new AmzLandmarkProjection(mapView));
        }
    }

    private void showRouteAction(String routeKey) {
        if (RoutesManager.getInstance().containsRoute(routeKey)) {
            List<Overlay> mapOverlays = mapView.getOverlays();
            AmzRoutesOverlay routeOverlay = new AmzRoutesOverlay(this, routeKey);
            mapOverlays.add(routeOverlay);
            isRouteDisplayed = true;
            mapView.postInvalidate();
        }
    }

    private GeoPoint getMyLocation() {
        /*if (myLocation != null) {
         return myLocation.getMyLocation();
         } else {
         return null;
         }*/
        Location location = ConfigurationManager.getInstance().getLocation();
        if (location != null) {
            return new GeoPoint(MathUtils.coordDoubleToInt(location.getLatitude()),
                    MathUtils.coordDoubleToInt(location.getLongitude()));
        } else {
            return null;
        }
    }

    private void showMyPositionAction(boolean loadLayers) {
        GeoPoint g = getMyLocation();
        if (g != null) {
            boolean isVisible = false;
            boolean clearLandmarks = false;

            AmzLandmarkProjection projection = new AmzLandmarkProjection(mapView);
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
                IntentsHelper.getInstance().loadLayersAction(true, null, clearLandmarks, false, MathUtils.coordIntToDouble(g.getLatitudeE6()),
                        MathUtils.coordIntToDouble(g.getLongitudeE6()),mapView.getZoomLevel(), new AmzLandmarkProjection(mapView));
            }
        } else {
            IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
        }
    }
    
    private void updateLocation(Location location) {
    	IntentsHelper.getInstance().addMyLocationLandmark(location);
		mapButtons.setVisibility(View.VISIBLE);
		IntentsHelper.getInstance().vibrateOnLocationUpdate();
        UserTracker.getInstance().sendMyLocation();
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
            dealOfTheDayDialog = new DealOfTheDayDialog(this, recommended);
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
        } //else {
        //comment out
        //IntentsHelper.getInstance().showInfoToast("Recommended == null\n"
        //        + "SubCategoryStats: " + cm.getTopSubCategoryStats() + " " + cm.getTopCategory() + " " + cm.getTopSubCategory() + "\n"
        //        + "showDealOfTheDay: " + ConfigurationManager.getInstance().isOn(ConfigurationManager.SHOW_DEAL_OF_THE_DAY) + "\n"
        //        + "shownDeals: " + ConfigurationManager.getInstance().getString(ConfigurationManager.RECOMMENDED_DEALS_SHOWN));
        //}
    }
    
    private void animateTo(int[] coordsE6) {
    	GeoPoint g = new GeoPoint(coordsE6[0], coordsE6[1]);
        mapController.animateTo(g);
    }
    
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        	NavigationDrawerListItem listItem = (NavigationDrawerListItem) drawerList.getAdapter().getItem(position);
        	UserTracker.getInstance().trackEvent("NavigationDrawerClicks", listItem.getName(), "", 0);
        	drawerList.setItemChecked(position, true);
            drawerLayout.closeDrawer(drawerList);
            onMenuOptionSelected((int)id);
        }
    }
    
    private static class LoadingHandler extends Handler {
    	private WeakReference<DealMapAmzActivity> parentActivity;
    	
    	public LoadingHandler(DealMapAmzActivity parentActivity) {
    		this.parentActivity = new WeakReference<DealMapAmzActivity>(parentActivity);
    	}
    	
        @Override
        public void handleMessage(Message msg) {
        	DealMapAmzActivity activity = parentActivity.get();
        	if (activity != null && !activity.isFinishing()) {
        		if (msg.what == MessageStack.STATUS_MESSAGE) {
        			activity.statusBar.setText(MessageStack.getInstance().getMessage());
                } else if (msg.what == MessageStack.STATUS_VISIBLE) {
                	activity.loadingImage.setVisibility(View.VISIBLE);
                } else if (msg.what == MessageStack.STATUS_GONE) {
                	activity.loadingImage.setVisibility(View.GONE);
                } else if (msg.what == LayerLoader.LAYER_LOADED) {
                	activity.mapView.postInvalidate();
                } else if (msg.what == LayerLoader.ALL_LAYERS_LOADED) {
                	activity.showRecommendedDeal(false);
                	if (activity.mapView.canCoverCenter()) {       			
                		AsyncTaskManager.getInstance().executeImageUploadTask(activity, MathUtils.coordIntToDouble(activity.mapView.getMapCenter().getLatitudeE6()),
                                MathUtils.coordIntToDouble(activity.mapView.getMapCenter().getLongitudeE6()), false);
                	}
                } else if (msg.what == AmzLandmarkOverlay.SHOW_LANDMARK_DETAILS) {
                	int[] coordsE6 = IntentsHelper.getInstance().showLandmarkDetailsAction(activity.getMyPosition(), activity.lvView, activity.mapView.getZoomLevel(), new AmzLandmarkProjection(activity.mapView));
                    if (coordsE6 != null) {
                    	activity.animateTo(coordsE6);
                    }
                } else if (msg.what == SHOW_MAP_VIEW) {
                    View loading = activity.findViewById(R.id.mapCanvasWidgetL);
                    loading.setVisibility(View.GONE);
                    View mapCanvas = activity.findViewById(R.id.mapCanvasWidgetM);
                    mapCanvas.setVisibility(View.VISIBLE);
                    if (activity.lvView == null || !activity.lvView.isShown()) {
                    	activity.getActionBar().show();
                    }
                } else if (msg.what == AsyncTaskManager.SHOW_ROUTE_MESSAGE) {
                	activity.showRouteAction((String)msg.obj);
                } else if (msg.what == AmzMyLocationOverlay.UPDATE_LOCATION) {
                    Location location = (Location) msg.obj;
                    activity.updateLocation(location);
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
