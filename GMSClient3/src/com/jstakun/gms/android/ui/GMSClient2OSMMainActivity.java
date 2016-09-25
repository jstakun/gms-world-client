package com.jstakun.gms.android.ui;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.api.IMyLocationOverlay;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FavouritesDAO;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.FilenameFilterFactory;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerLoader;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.location.LocationServicesManager;
import com.jstakun.gms.android.osm.maps.ObservableMapView;
import com.jstakun.gms.android.osm.maps.OsmLandmarkOverlay;
import com.jstakun.gms.android.osm.maps.OsmLandmarkProjection;
import com.jstakun.gms.android.osm.maps.OsmMapsTypeSelector;
import com.jstakun.gms.android.osm.maps.OsmMarkerClusterOverlay;
import com.jstakun.gms.android.osm.maps.OsmMyLocationNewOverlay;
import com.jstakun.gms.android.osm.maps.OsmRoutesOverlay;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.MessageStack;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.ProjectionInterface;
import com.jstakun.gms.android.utils.ServicesUtils;
import com.jstakun.gms.android.utils.StringUtil;
import com.jstakun.gms.android.utils.UserTracker;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.SpannableString;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GMSClient2OSMMainActivity extends Activity implements OnClickListener {

    private static final int SHOW_MAP_VIEW = 0;
    
    private MapView mapView;
    private IMapController mapController;
    private IMyLocationOverlay myLocation;
    private OsmMarkerClusterOverlay markerCluster;
    
    private TextView statusBar;
    private View lvCloseButton, lvCallButton, lvCommentButton, mapButtons,
            lvOpenButton, lvView, lvShareButton, myLocationButton, nearbyLandmarksButton,
            lvCheckinButton, lvRouteButton, thumbnailButton, loadingImage;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private LinearLayout drawerLinearLayout;
    private ExpandableListView drawerList;
    private ProgressBar loadingProgressBar;
    
    private int mapProvider;
    private boolean appInitialized = false;
    
    private  Handler loadingHandler;
    
    private final Runnable gpsRunnable = new Runnable() {
        public void run() {
            GeoPoint location = LocationServicesManager.getMyLocation();
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
    //OnClickListener
    private DialogInterface.OnClickListener trackMyPosListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            trackMyPosAction();
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        LoggerUtils.debug("onCreate");
        LoggerUtils.debug("GMSClient2OSMMainActivity.onCreate called...");
    
        UserTracker.getInstance().trackActivity(getClass().getName());

        ConfigurationManager.getInstance().setContext(this);
        
        mapProvider = ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        OsUtil.setDisplayType(getResources().getConfiguration());
        getActionBar().hide();
        
        loadingHandler = new LoadingHandler(this);
        
        LoggerUtils.debug("Map provider is " + mapProvider);

        setContentView(R.layout.osmdroidcanvasview_2);
        
        mapView = (MapView) findViewById(R.id.mapCanvas);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        //set this to solve path painting issue
        mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        ((ObservableMapView)mapView).setOnZoomChangeListener(new ZoomListener());

        myLocation = new OsmMyLocationNewOverlay(this, mapView, loadingHandler);
        LocationServicesManager.initLocationServicesManager(this, loadingHandler, myLocation);
        
        initComponents();
        
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
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        //getActionBar().setHomeButtonEnabled(true);
        
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        
        drawerLinearLayout = (LinearLayout) findViewById(R.id.left_drawer_view);
        
        drawerList = (ExpandableListView) findViewById(R.id.left_drawer);
        drawerList.setAdapter(new NavigationDrawerExpandableListAdapter(this));
        drawerList.setOnGroupClickListener(new DrawerOnGroupClickListener());
        drawerList.setOnChildClickListener(new DrawerOnChildClickListener());
        drawerList.setGroupIndicator(null);
        
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.drawable.ic_drawer,
                R.string.app_name,  /* "open drawer" description for accessibility */
                R.string.app_name  /* "close drawer" description for accessibility */
                ) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu(); 
            	for (int i=0;i<drawerList.getExpandableListAdapter().getGroupCount();i++) {
            		if (drawerList.isGroupExpanded(i)) {
            			drawerList.collapseGroup(i);	
            			TextView textView = (TextView) drawerList.getChildAt(i);
            			textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_bullet, 0, 0, 0);
            		}
        		}
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        mapController = mapView.getController();

        GeoPoint mapCenter = (GeoPoint) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, GeoPoint.class);

        if (mapCenter == null) {
            loadingHandler.postDelayed(gpsRunnable, ConfigurationManager.FIVE_SECONDS);
        }

        mapController.setZoom(ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM));

        appInitialized = false;
        
        if (!CategoriesManager.getInstance().isInitialized()) {
            LoggerUtils.debug("Loading deal categories...");
            AsyncTaskManager.getInstance().executeDealCategoryLoaderTask(true);
        }

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

        OsmMapsTypeSelector.selectMapType(mapView, this);

        AsyncTaskManager.getInstance().setContext(this);
        
        IntentsHelper.getInstance().setActivity(this);
        
        //check if myloc is available
        if (LandmarkManager.getInstance().hasMyLocation() && ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
        	mapButtons.setVisibility(View.VISIBLE);
        }
        
        AsyncTaskManager.getInstance().executeNewVersionCheckTask(this);
        
        //verify access token
        AsyncTaskManager.getInstance().executeGetTokenTask();
        
        Integer searchQueryResult = (Integer) ConfigurationManager.getInstance().removeObject(ConfigurationManager.SEARCH_QUERY_RESULT, Integer.class);
        if (searchQueryResult != null) {
        	int[] coordsE6 = IntentsHelper.getInstance().showSelectedLandmark(searchQueryResult, getMyLocation(), lvView, mapView.getZoomLevel(), new OsmLandmarkProjection(mapView));
            if (coordsE6 != null) {
            	animateTo(coordsE6);
            }
        } else if (LandmarkManager.getInstance().getSeletedLandmarkUI() != null) {
            getActionBar().hide();
            ExtendedLandmark landmark = LandmarkManager.getInstance().getSeletedLandmarkUI();
            IntentsHelper.getInstance().showLandmarkDetailsView(landmark, lvView, getMyLocation(), true);
        }

        Integer type = (Integer) ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
        if (type != null) {
            ArrayAdapter<?> arrayAdapter = null;
            if (type == AlertDialogBuilder.SHARE_INTENTS_DIALOG) {
                List<ResolveInfo> intentList = IntentsHelper.getInstance().getSendIntentsList();
                if (!intentList.isEmpty()) {
                    arrayAdapter = new IntentArrayAdapter(this, intentList);
                }
            } else if (type == AlertDialogBuilder.LOGIN_DIALOG) {
                if (!ConfigurationManager.getUserManager().isUserLoggedInFully()) {
                    arrayAdapter = new LoginArrayAdapter(this, ConfigurationManager.getUserManager().getLoginItems(false));
                }
            }
            DialogManager.getInstance().showAlertDialog(this, type, arrayAdapter, null);
        }

        IntentsHelper.getInstance().onAppVersionChanged();

        if (ConfigurationManager.getInstance().removeObject(HelpActivity.HELP_ACTIVITY_SHOWN, String.class) != null) {
            GeoPoint mapCenter = (GeoPoint) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, GeoPoint.class);
            if (mapCenter == null) {
                loadingHandler.removeCallbacks(gpsRunnable);
                loadingHandler.post(gpsRunnable);
            }
        }
        
        if (markerCluster != null && mapProvider == ConfigurationManager.OSM_MAPS) {
        	markerCluster.clearMarkers();
        	markerCluster.loadAllMarkers(mapView);
        }
        
        syncRoutesOverlays();
        
        IntentsHelper.getInstance().startAutoCheckinBroadcast();
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentsHelper.getInstance().setActivity(this);
        IntentsHelper.getInstance().showStatusDialogs();
    }

    @Override
    public void onPause() {
        super.onPause();
        LoggerUtils.debug("onPause");

        LocationServicesManager.disableMyLocation();

        DialogManager.getInstance().dismissDialog(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
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
    public void onRestart() {
        super.onRestart();
        LoggerUtils.debug("onRestart");
        //this activity works only with osm maps
        //if (mapProvider != ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER)) {
        //    Intent intent = getIntent();
        //    ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, mapView.getMapCenter());
        //    LocationServicesManager.disableMyLocation();
        //    finish();
        //    startActivity(intent);
        //}
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private void hideLandmarkView() {
    	lvView.setVisibility(View.GONE);
		getActionBar().show();
		LandmarkManager.getInstance().clearLandmarkOnFocusQueue();
		LandmarkManager.getInstance().setSelectedLandmark(null);
		LandmarkManager.getInstance().setSeletedLandmarkUI();
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
    				DialogManager.getInstance().showExitAlertDialog(this);
    			} //System.out.println("key back pressed in activity");
    			return true;
    		} else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
    			int[] coordsE6 = IntentsHelper.getInstance().showLandmarkDetailsAction(getMyLocation(), lvView, mapView.getZoomLevel(), new OsmLandmarkProjection(mapView));
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
    	}  else {
    		IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
    		return true;
  	  	}
    }

    private synchronized void initOnLocationChanged(final GeoPoint location) {
        if (!appInitialized && location != null) {
        	loadingProgressBar.setProgress(75);
        	
        	mapController.setCenter(location);
        	
            if (!LandmarkManager.getInstance().isInitialized()) {
                //UserTracker.getInstance().sendMyLocation();
                LandmarkManager.getInstance().initialize();
            }

            addLandmarkOverlay();
            if (LocationServicesManager.isGpsHardwarePresent()) {
                addOverlay(myLocation);
            }

            syncRoutesOverlays();
            
            MessageStack.getInstance().setHandler(loadingHandler);
            
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
                String route  = RouteRecorder.getInstance().startRecording();
                showRouteAction(route);
                MessageStack.getInstance().addMessage(Locale.getMessage(R.string.Routes_TrackMyPosOn), 10, -1, -1);
            } 

            LayerLoader.getInstance().setRepaintHandler(loadingHandler);
            
            if (!LayerLoader.getInstance().isInitialized() && !LayerLoader.getInstance().isLoading()) {
                if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
                	LoggerUtils.debug("Loading Layers in " + location.getLatitude() + "," +  location.getLongitude());
                    IntentsHelper.getInstance().loadLayersAction(true, null, false, true, location.getLatitude(),
                    		location.getLongitude(), mapView.getZoomLevel(), new OsmLandmarkProjection(mapView));
                }
            } else {
                //load existing layers
                if (LayerLoader.getInstance().isLoading()) {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_VISIBLE);
                } else {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_GONE);
                }
                loadingHandler.sendEmptyMessage(MessageStack.STATUS_MESSAGE);
                postInvalidate();
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
                AsyncTaskManager.getInstance().cancelTask(taskId, true);
            }
        }
    }

    @Override
    public boolean onSearchRequested() {
        if (appInitialized) {
            IntentsHelper.getInstance().startSearchActivity(mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6(), -1, false);
            return true;
        } else {
            return false;
        }
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
        if (ConfigurationManager.getInstance().isClosing()) {
            return false;
        } else {
        	MenuItem routes = menu.findItem(R.id.routes);
        	if (LayerManager.getInstance().containsLayer(Commons.ROUTES_LAYER)) {
        		routes.setVisible(true);
        		MenuItem routeRecording = menu.findItem(R.id.trackPos);
            	MenuItem pauseRecording = menu.findItem(R.id.pauseRoute);
            	MenuItem saveRoute = menu.findItem(R.id.saveRoute);
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
            	if (FileManager.getInstance().isFolderEmpty(FileManager.getInstance().getRoutesFolderPath(), FilenameFilterFactory.getFilenameFilter("kml"))) {    
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
            
            if (drawerLayout.isDrawerOpen(drawerLinearLayout)) {
            	NavigationDrawerExpandableListAdapter adapter = (NavigationDrawerExpandableListAdapter) drawerList.getExpandableListAdapter();
            	adapter.rebuild(new OsmLandmarkProjection(mapView));
        	}

            return super.onPrepareOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (appInitialized) {
        	if (drawerToggle.onOptionsItemSelected(item)) {
        		return true;
            } else {
            	UserTracker.getInstance().trackEvent("MenuClicks", item.getTitle().toString(), "", 0);
                return onMenuItemSelected(item.getItemId());
            }
        } else {
        	return true;
        }
    }

	private boolean onMenuItemSelected(int itemId) {
		if (ConfigurationManager.getUserManager().isUserAllowedAction() || !ConfigurationManager.getUserManager().isItemRestricted(itemId)) {	
			switch (itemId) {
		    	case R.id.settings:
		    		IntentsHelper.getInstance().startSettingsActivity(SettingsActivity.class);
		    		break;
		    	case R.id.search:
		    		onSearchRequested();
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
		    	case R.id.login:
		    		if (!ConfigurationManager.getUserManager().isUserLoggedInFully()) {
		    			DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.LOGIN_DIALOG, new LoginArrayAdapter(this, ConfigurationManager.getUserManager().getLoginItems(false)), null);
		    		} else {
		    			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.loginFull));
		    		}
		    		break;
		    	case R.id.addLandmark:
		    		if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
		    			IntentsHelper.getInstance().startAddLandmarkActivity();
		    		} else {
		    			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
		    		}
		    		break;
		    	case R.id.autocheckin:
		    		if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
		    			IntentsHelper.getInstance().startAutoCheckinListActivity(getMyLocation());
		    		} else {
		    			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
		    		}
		    		break;
		    	case R.id.qrcheckin:
		    		if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
		    			IntentsHelper.getInstance().startQrCodeCheckinActivity(getMyLocation());
		    		} else {
		    			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
		    		}
		    		break;
		    	case R.id.searchcheckin:
		    		if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
		    			IntentsHelper.getInstance().startLocationCheckinActivity(getMyLocation());
		    		} else {
		    			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
		    		}
		    		break;
		    	case R.id.refreshLayers:
		    		IntentsHelper.getInstance().loadLayersAction(true, null, false, true,
		                mapView.getMapCenter().getLatitude(), mapView.getMapCenter().getLongitude(),
		                mapView.getZoomLevel(), new OsmLandmarkProjection(mapView));
		    		break;
		    	case R.id.addLayer:
		    		IntentsHelper.getInstance().startAddLayerActivity();
		    		break;
		    	case R.id.showLayers:
		    		IntentsHelper.getInstance().startLayersListActivity(false);
		    		break;
		    	case R.id.clearMap:
		    		clearMapAction();
		    		break;
		    	case R.id.showMyLandmarks:
		    		IntentsHelper.getInstance().startMyLandmarksIntent(getMyLocation());
		    		break;
		    	case R.id.recentLandmarks:
		    		IntentsHelper.getInstance().startRecentLandmarksIntent(getMyLocation());
		    		break;
		    	case R.id.blogeo:
		    		if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
		    			if (LandmarkManager.getInstance().hasMyLocation()) {
		    				IntentsHelper.getInstance().startBlogeoActivity();
		    			} else {
		    				IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
		    			}
		    		} else {
		    			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
		    		}
		    		break;
		    	case R.id.friendsCheckins:
		    		if (ConfigurationManager.getUserManager().isFriendSocialLoggedIn()) {
		    			IntentsHelper.getInstance().startFriendsCheckinsIntent(getMyLocation());
		    		} else {
		    			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Checkin_required_error));
		    		}
		    		break;
		    	case R.id.trackPos:
		    		DialogManager.getInstance().showTrackMyPosAlertDialog(this, trackMyPosListener);
		    		break;
		    	case R.id.saveRoute:
		    		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
		    			DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.SAVE_ROUTE_DIALOG, null, null);
			        } else if (ConfigurationManager.getInstance().isOff(ConfigurationManager.RECORDING_ROUTE)) {
			            IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Routes_TrackMyPosStopped));
			        }
		    		break;
		    	case R.id.loadRoute:
		    		if (IntentsHelper.getInstance().startRouteFileLoadingActivity()) {
		    			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Routes_NoRoutes));
		    		}
		    		break;
		    	case R.id.pauseRoute:
		    		RouteRecorder.getInstance().pause();
		    		if (RouteRecorder.getInstance().isPaused()) {
		    			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Routes_PauseRecordingOn));
		    		} else {
		    			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Routes_PauseRecordingOff));
		    		}
		    		break;
		    	case R.id.loadPoiFile:
		    		if (IntentsHelper.getInstance().startFilesLoadingActivity()) {
		    			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Files_NoFiles));
		    		}
		    		break;
		    	case R.id.config:
					IntentsHelper.getInstance().startConfigurationViewerActivity();
					break;	
		    	case R.id.socialNetworks:
		    		IntentsHelper.getInstance().startSocialListActivity();
		    		break;
		    	//case R.id.layers:
		    		//IntentsHelper.getInstance().startLayersListActivity();
		    		//break;
		    	case R.id.dataPacket:
		    		DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.PACKET_DATA_DIALOG, null, null);
		    		break;
		    	case R.id.pickMyPos:
		    		IntentsHelper.getInstance().startPickLocationActivity();
		    		break;
		    	case R.id.deals:
		    		if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
		    			IntentsHelper.getInstance().startCategoryListActivity(mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6(), -1, -1);
		    		} else {
		    			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
		    		}
		    		break;
		    	case R.id.register:
		    		IntentsHelper.getInstance().startRegisterActivity();
		    		break;
		    	case R.id.newestLandmarks:
		    		final String[] excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER};
		    		IntentsHelper.getInstance().startNewestLandmarkIntent(getMyLocation(), excluded, 2);
		    		break;
		    	case R.id.events:
		    		IntentsHelper.getInstance().startCalendarActivity(getMyLocation());
		    		break;
		    	case R.id.rateUs:
		    		DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.RATE_US_DIALOG, null, null);
		    		break;
		    	case R.id.listLandmarks:
		    		if (!lvView.isShown()) {
		    			IntentsHelper.getInstance().showNearbyLandmarks(getMyLocation(), new OsmLandmarkProjection(mapView));
		    		}
		    		break;
		    	case R.id.shareScreenshot:
		    		AsyncTaskManager.getInstance().executeImageUploadTask(this, mapView.getMapCenter().getLatitude(), mapView.getMapCenter().getLongitude(), true);
		    		break;    
		    	case R.id.reset:
		    		DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.RESET_DIALOG, null, null);
	            	break;	
		    	default:
		    		return true;
			}
        } else {
        	 IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
     	}
		return true;
	}

    public void onClick(View v) {
    	if (ConfigurationManager.getUserManager().isUserAllowedAction() || v == lvCloseButton || v == myLocationButton || v == nearbyLandmarksButton) {
    		if (v == myLocationButton) {
    			showMyPositionAction(true);
    		} else if (v == nearbyLandmarksButton) {
    			IntentsHelper.getInstance().startLayersListActivity(true);
        	} else {
        		ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getSeletedLandmarkUI();
        		if (selectedLandmark != null) {
        			if (v == lvCloseButton) {
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CloseSelectedLandmarkView", selectedLandmark.getLayer(), 0);
        				hideLandmarkView();
        			} else if (v == lvCommentButton) {
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CommentSelectedLandmark", selectedLandmark.getLayer(), 0);
        				IntentsHelper.getInstance().commentButtonPressedAction();
        			} else if (v == lvCheckinButton) {
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CheckinSelectedLandmark", selectedLandmark.getLayer(), 0);
        				boolean authStatus = IntentsHelper.getInstance().checkAuthStatus(selectedLandmark);
        				if (authStatus) {
        					boolean addToFavourites = ConfigurationManager.getInstance().isOn(ConfigurationManager.AUTO_CHECKIN) && !selectedLandmark.getLayer().equals(Commons.MY_POSITION_LAYER);
        					CheckinManager.getInstance().checkinAction(addToFavourites, false, selectedLandmark);
        				}
        			} else if (v == lvOpenButton) {
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenURLSelectedLandmark", selectedLandmark.getLayer(), 0);
        				IntentsHelper.getInstance().openButtonPressedAction(selectedLandmark);
        			} else if (v == thumbnailButton) {
        				if (IntentsHelper.getInstance().startStreetViewActivity(selectedLandmark)) {
        					UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenStreetView", selectedLandmark.getLayer(), 0);
        				} else {
        					UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenURLSelectedLandmark", selectedLandmark.getLayer(), 0);
            				IntentsHelper.getInstance().openButtonPressedAction(selectedLandmark);
        				}
        			} else if (v == lvCallButton) {
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CallSelectedLandmark", selectedLandmark.getLayer(), 0);
        				IntentsHelper.getInstance().startPhoneCallActivity(selectedLandmark);
        			} else if (v == lvRouteButton) {
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowRouteSelectedLandmark", selectedLandmark.getLayer(), 0);
        				if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
        					DialogManager.getInstance().showRouteAlertDialog(this, null, loadingHandler);
        				} else {
        					IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
        				}	
        			} else if (v == lvShareButton) {
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShareSelectedLandmark", selectedLandmark.getLayer(), 0);
        				IntentsHelper.getInstance().shareLandmarkAction();
        			}
        		} else {
        			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
        		}
        	}	
    	} else {
       	 	IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
    	}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	IntentsHelper.getInstance().setActivity(this);
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
            	} else {
            		Place place = PlaceAutocomplete.getPlace(this, intent);
            		name = place.getName().toString();
            		lat = place.getLatLng().latitude;
            		lng = place.getLatLng().longitude;
            	}
                
                if (lat == null || lng == null || name == null) {
                	ExtendedLandmark defaultLocation = ConfigurationManager.getInstance().getDefaultCoordinate();
                    name = defaultLocation.getName();
                    lat = defaultLocation.getQualifiedCoordinates().getLatitude();
                    lng = defaultLocation.getQualifiedCoordinates().getLongitude();
                }
                	
                GeoPoint location = new GeoPoint(MathUtils.coordDoubleToInt(lat), MathUtils.coordDoubleToInt(lng));
                if (!appInitialized) {
                	initOnLocationChanged(location);
                } else {
                	pickPositionAction(location, true, true);
                }
                LandmarkManager.getInstance().addLandmark(lat, lng, 0.0f, StringUtil.formatCommaSeparatedString(name), "", Commons.LOCAL_LAYER, true);
                
            } else if (resultCode == RESULT_CANCELED && intent != null && !appInitialized) {
                ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
                IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Pick_location_default, landmark.getName()));
                GeoPoint location = new GeoPoint(landmark.getLatitudeE6(), landmark.getLongitudeE6());
                initOnLocationChanged(location);
            } else if (resultCode == RESULT_CANCELED && intent != null && intent.hasExtra("message")) {
                String message = intent.getStringExtra("message");
                IntentsHelper.getInstance().showInfoToast(message);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
            	Status status = PlaceAutocomplete.getStatus(this, intent);
                IntentsHelper.getInstance().showInfoToast(status.getStatusMessage());
            } else if (resultCode != RESULT_CANCELED) {
                IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
            } 
        } else if (requestCode == IntentsHelper.INTENT_MULTILANDMARK) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);
                if (action.equals("load")) {
                    int id = Integer.parseInt(ids);
                    int[] coordsE6 = IntentsHelper.getInstance().showSelectedLandmark(id, getMyLocation(), lvView, mapView.getZoomLevel(), new OsmLandmarkProjection(mapView));
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
                    ExtendedLandmark l = LandmarkManager.getInstance().getPhoneLandmark(id);
                    if (l != null) {
                        GeoPoint location = new GeoPoint(l.getLatitudeE6(), l.getLongitudeE6());
                        pickPositionAction(location, true, true);
                    }
                } else if (action.equals("delete")) {
                    //delete landmark
                    LandmarkManager.getInstance().deletePhoneLandmark(id);
                    IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Landmark_deleted));
                }
            }
        } else if (requestCode == IntentsHelper.INTENT_AUTO_CHECKIN) {
            if (resultCode == RESULT_OK) {
                int favouriteId = intent.getIntExtra("favourite", 0);
                FavouritesDAO fav = ConfigurationManager.getDatabaseManager().getFavouritesDatabase().getLandmark(favouriteId);
                if (fav != null) {
                    GeoPoint location = new GeoPoint(MathUtils.coordDoubleToInt(fav.getLatitude()), MathUtils.coordDoubleToInt(fav.getLongitude()));
                    pickPositionAction(location, true, false);
                } else {
                    IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
                }
            }
        } else if (requestCode == IntentsHelper.INTENT_CALENDAR) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);

                if (action.equals("load")) {
                    int id = Integer.parseInt(ids);
                    int[] coordsE6 = IntentsHelper.getInstance().showSelectedLandmark(id, getMyLocation(), lvView, mapView.getZoomLevel(), new OsmLandmarkProjection(mapView));
                    if (coordsE6 != null) {
                    	animateTo(coordsE6);
                    }
                }
            }
        } else {
            IntentsHelper.getInstance().processActivityResult(requestCode, resultCode, intent, getMyLocation(), new double[]{mapView.getMapCenter().getLatitude(), mapView.getMapCenter().getLongitude()}, loadingHandler, mapView.getZoomLevel(), new OsmLandmarkProjection(mapView));
        }
    }

    private void pickPositionAction(GeoPoint newCenter, boolean loadLayers, boolean clearMap) {
    	if (clearMap && markerCluster != null) {
    		markerCluster.clearMarkers();
    	}
        mapController.setCenter(newCenter);
        if (loadLayers) {
        	IntentsHelper.getInstance().loadLayersAction(true, null, clearMap, true, 
                    mapView.getMapCenter().getLatitude(), mapView.getMapCenter().getLongitude(),
                    mapView.getZoomLevel(), new OsmLandmarkProjection(mapView));
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
            	hideLandmarkView();
        		IGeoPoint mapCenter = mapView.getMapCenter();
                clearLandmarks = IntentsHelper.getInstance().isClearLandmarksRequired(projection, mapCenter.getLatitudeE6(), mapCenter.getLongitudeE6(),
                        myLoc.getLatitudeE6(), myLoc.getLongitudeE6());
            }

            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
                mapController.setCenter(myLoc);
            } else {
                mapController.animateTo(myLoc);
            }

            if (loadLayers && !isVisible) {
            	if (clearLandmarks && mapProvider == ConfigurationManager.OSM_MAPS && markerCluster != null) {
            		markerCluster.clearMarkers();
            	}
                IntentsHelper.getInstance().loadLayersAction(true, null, clearLandmarks, true, myLoc.getLatitude(), myLoc.getLongitude(), mapView.getZoomLevel(), projection);
            }
        } else {
            IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
        }
    }

    private void updateLocation(Location l) {
    	if (appInitialized) {
    		IntentsHelper.getInstance().addMyLocationLandmark(l);
        	IntentsHelper.getInstance().vibrateOnLocationUpdate();
        	UserTracker.getInstance().sendMyLocation();
    	
        	if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
        		mapButtons.setVisibility(View.GONE);
        		showMyPositionAction(false);
            	if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
            		RouteRecorder.getInstance().addCoordinate(l);
            	}
        	} else {
        		mapButtons.setVisibility(View.VISIBLE);
        	}

        	if (ConfigurationManager.getInstance().isOn(ConfigurationManager.AUTO_CHECKIN)) {
        		CheckinManager.getInstance().autoCheckin(l.getLatitude(), l.getLongitude(), false);
        	}
    	}
    }

    private void addOverlay(Object overlay) {
        mapView.getOverlays().add((org.osmdroid.views.overlay.Overlay) overlay);
    }

    private void addLandmarkOverlay() {
        markerCluster = new OsmMarkerClusterOverlay(this, loadingHandler);
    	addOverlay(markerCluster);
    }

    private void addRoutesOverlay(String routeName) {
        OsmRoutesOverlay routesOverlay = new OsmRoutesOverlay(mapView, this, routeName, markerCluster);
        addOverlay(routesOverlay);
        //add first & last route point to marker cluster
        List<ExtendedLandmark> routePoints = RoutesManager.getInstance().getRoute(routeName);
        if (routePoints.size() > 1) {
            markerCluster.addMarker(routePoints.get(0), mapView);
            markerCluster.addMarker(routePoints.get(routePoints.size()-1), mapView);
        }
    }
    
    private void syncRoutesOverlays() {
    	int routesCount = RoutesManager.getInstance().getCount(); 	
    	int routesOverlaysCount = 0;
    	
    	for (Iterator<org.osmdroid.views.overlay.Overlay> iter = ((org.osmdroid.views.MapView) mapView).getOverlays().listIterator(); iter.hasNext();) {
            if (iter.next() instanceof OsmRoutesOverlay) {
            	routesOverlaysCount++;
            }
        }         
        
    	
    	boolean isRoutesEnabled = LayerManager.getInstance().isLayerEnabled(Commons.ROUTES_LAYER);
    		
    	if ((routesCount == 0 || !isRoutesEnabled) && routesOverlaysCount > 0) {
    		for (Iterator<org.osmdroid.views.overlay.Overlay> iter = ((org.osmdroid.views.MapView) mapView).getOverlays().listIterator(); iter.hasNext();) {
    				if (iter.next() instanceof OsmRoutesOverlay) {
    					iter.remove();
    				}
    		}         
    	} else if (routesCount > 0 && isRoutesEnabled && routesOverlaysCount == 0) {
    		for (String routeKey: RoutesManager.getInstance().getRoutes()) {
                addRoutesOverlay(routeKey);
            }
    	}
    }

    private void postInvalidate() {
        mapView.postInvalidate();
    }

    private double[] getMyLocation() {
    	return LandmarkManager.getInstance().getMyLocation(mapView.getMapCenter().getLatitude(), mapView.getMapCenter().getLongitude());
    }

    private void showRouteAction(String routeKey) {
        LoggerUtils.debug("Adding route to view: " + routeKey);
        if (RoutesManager.getInstance().containsRoute(routeKey) && LayerManager.getInstance().isLayerEnabled(Commons.ROUTES_LAYER)) {
            addRoutesOverlay(routeKey);
            if (!routeKey.startsWith(RouteRecorder.CURRENTLY_RECORDED)) {
            	double[] locationAndZoom = RoutesManager.getInstance().calculateRouteCenterAndZoom(routeKey);
                GeoPoint newCenter = new GeoPoint(MathUtils.coordDoubleToInt(locationAndZoom[0]), MathUtils.coordDoubleToInt(locationAndZoom[1]));
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
            List<ExtendedLandmark> myPosV = LandmarkManager.getInstance().getUnmodifableLayer(Commons.MY_POSITION_LAYER);
            if (!myPosV.isEmpty()) {
                ExtendedLandmark landmark = myPosV.get(0);
                ProjectionInterface projection = new OsmLandmarkProjection(mapView);
                if (!projection.isVisible(landmark.getLatitudeE6(), landmark.getLongitudeE6())) {
                    showMyPositionAction(false);
                } else {
                    IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Routes_TrackMyPosOn));
                }
            } else {
                IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
            }
        } else if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
            ConfigurationManager.getInstance().setOff(ConfigurationManager.FOLLOW_MY_POSITION);
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
                String filename = RouteRecorder.getInstance().stopRecording();
                if (filename != null) {
                    return filename;
                } else {
                    IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Routes_TrackMyPosOff));
                }
            } else {
                IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Routes_TrackMyPosOff));
            }
        }
        return null;
    }

    private void clearMapAction() {
    	LandmarkManager.getInstance().clearLandmarkStore();
        if (markerCluster != null) {
    		markerCluster.clearMarkers();
    	}
        RoutesManager.getInstance().clearRoutesStore();
        syncRoutesOverlays();
        postInvalidate();
        IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Maps_cleared));
    }
    
    private void animateTo(int[] coordsE6) {
    	GeoPoint g = new GeoPoint(coordsE6[0], coordsE6[1]);
        mapController.animateTo(g);
    }
    
    private void trackMyPosAction() {
    	String filename = followMyPositionAction();

        LocationServicesManager.enableCompass();

        ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
        if (filename != null) {
            DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.SAVE_ROUTE_DIALOG, null, new SpannableString(Locale.getMessage(R.string.Routes_Recording_Question, filename)));
        } else if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)
                && !ServicesUtils.isGpsActive(ConfigurationManager.getInstance().getContext())) {
            DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.LOCATION_ERROR_DIALOG, null, null);
        }
    }
    
    private class DrawerOnGroupClickListener implements ExpandableListView.OnGroupClickListener {

		@Override
		public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
			TextView textView = (TextView)v;
			UserTracker.getInstance().trackEvent("NavigationDrawerClicks", textView.getText().toString(), "", 0);
        	if (groupPosition == 0 || groupPosition == 3 || groupPosition == 4 || groupPosition == 5) {
        		drawerLayout.closeDrawer(drawerLinearLayout);
        		onMenuItemSelected((int)id);
        		return true;
        	} else if (groupPosition == 1 || groupPosition == 2) {
        		if (parent.isGroupExpanded(groupPosition)) {
        			textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_bullet, 0, 0, 0);
        		} else {
        		    textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_bullet_down, 0, 0, 0);
        		}
    	        return false;
        	} else {
        		return true;
        	}
		}   	
    }
    
    private class DrawerOnChildClickListener implements ExpandableListView.OnChildClickListener {

		@Override
		public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
			TextView textView = (TextView) parent.getChildAt(groupPosition);
			if (groupPosition == 1 || groupPosition == 2) {
				textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_bullet, 0, 0, 0);		
			}
			UserTracker.getInstance().trackEvent("NavigationDrawerClicks", textView.getText().toString(), "", 0);
        	drawerLayout.closeDrawer(drawerLinearLayout);
    		onMenuItemSelected((int)id);
			return true;
		}
    	
    }
    
    private static class LoadingHandler extends Handler {
    	
    	private WeakReference<GMSClient2OSMMainActivity> parentActivity;
    	
    	public LoadingHandler(GMSClient2OSMMainActivity parentActivity) {
    		this.parentActivity = new WeakReference<GMSClient2OSMMainActivity>(parentActivity);
    	}
    	
    	@Override
        public void handleMessage(Message msg) {
    		GMSClient2OSMMainActivity activity = parentActivity.get();
        	if (activity != null && !activity.isFinishing()) {
        		if (msg.what == MessageStack.STATUS_MESSAGE) {
        			activity.statusBar.setText(MessageStack.getInstance().getMessage());
            	} else if (msg.what == MessageStack.STATUS_VISIBLE && !ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
            		activity.loadingImage.setVisibility(View.VISIBLE);
            	} else if (msg.what == MessageStack.STATUS_GONE) {
            		activity.loadingImage.setVisibility(View.GONE);
            	} else if (msg.what == LayerLoader.LAYER_LOADED) {
            		if (activity.mapProvider == ConfigurationManager.OSM_MAPS) {
            			activity.markerCluster.addMarkers((String)msg.obj, (org.osmdroid.views.MapView)activity.mapView);
            		} 
            		activity.postInvalidate();
            	} else if (msg.what == LayerLoader.ALL_LAYERS_LOADED) {
            		//TODO check if mapView has loaded map tiles activity.mapView.getTileProvider()
            		//activity.mapView.getOverlayManager().getTilesOverlay()
            		//activity.mapView.getTileProvider().getMapTile(arg0)
            		AsyncTaskManager.getInstance().executeImageUploadTask(activity, activity.mapView.getMapCenter().getLatitude(),
                            activity.mapView.getMapCenter().getLongitude(), false);
            	} else if (msg.what == LayerLoader.FB_TOKEN_EXPIRED) {
            		IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Social_token_expired, "Facebook"));
            	} else if (msg.what == OsmLandmarkOverlay.SHOW_LANDMARK_DETAILS || msg.what == OsmMarkerClusterOverlay.SHOW_LANDMARK_DETAILS) {
            		int[] coordsE6 = IntentsHelper.getInstance().showLandmarkDetailsAction(activity.getMyLocation(), activity.lvView, activity.mapView.getZoomLevel(), new OsmLandmarkProjection(activity.mapView));
                    if (coordsE6 != null) {
                    	activity.animateTo(coordsE6);
                    }
            	} else if (msg.what == OsmMarkerClusterOverlay.SHOW_LANDMARK_LIST) {
                	IntentsHelper.getInstance().startMultiLandmarkIntent(activity.getMyLocation());
            		activity.animateTo(new int[]{msg.arg1, msg.arg2});
            	} else if (msg.what == SHOW_MAP_VIEW) {
                	View loading = activity.findViewById(R.id.mapCanvasWidgetL);
                	loading.setVisibility(View.GONE);
                	View mapCanvas = activity.findViewById(R.id.mapCanvasWidgetM);
                	mapCanvas.setVisibility(View.VISIBLE);
                	if (activity.lvView == null || !activity.lvView.isShown()) {
                		activity.getActionBar().show();
                	}
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
			mapInfo.setZoomLevel(mapView.getZoomLevel());
            mapInfo.setMaxZoom(mapView.getMaxZoomLevel());
            mapInfo.setDistance(distance);
            mapInfo.postInvalidate();
		}
    }		
}
