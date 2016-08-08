package com.jstakun.gms.android.ui;

import java.lang.ref.WeakReference;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.FilenameFilterFactory;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerLoader;
import com.jstakun.gms.android.location.LocationServicesManager;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.utils.LayersMessageCondition;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.MessageStack;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.ServicesUtils;
import com.jstakun.gms.android.utils.StringUtil;
import com.jstakun.gms.android.utils.UserTracker;

public class GMSClient3MainActivity extends ActionBarActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, 
                                                                         OnMapReadyCallback, OnClickListener, 
                                                                         GoogleMap.OnMyLocationButtonClickListener,
                                                                         GoogleApiClient.ConnectionCallbacks,
                                                                         GoogleApiClient.OnConnectionFailedListener,
                                                                         LocationListener {

	private static final int SHOW_MAP_VIEW = 0;
	private static final int PICK_LOCATION = 1;
	
	private LayerLoader layerLoader;
    private LandmarkManager landmarkManager;
    private MessageStack messageStack;
    private AsyncTaskManager asyncTaskManager;
    private RoutesManager routesManager;
    private RouteRecorder routeRecorder;
    private CheckinManager checkinManager;
    private CategoriesManager cm;
    private IntentsHelper intents;
    private DialogManager dialogManager;
    private GoogleLandmarkProjectionV2 projection;
	
	private Handler loadingHandler;
	private GoogleMarkerClusterOverlay markerCluster;
	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;
	private GoogleMap mMap;
	
	private TextView statusBar;
    private View lvCloseButton, lvCallButton, lvCommentButton, mapButtons,
            lvOpenButton, lvView, lvShareButton, myLocationButton, nearbyLandmarksButton,
            thumbnailButton, lvCheckinButton, lvRouteButton, loadingImage;
    private ProgressBar loadingProgressBar;
	private NavigationDrawerFragment mNavigationDrawerFragment;
   
    private boolean appInitialized = false;
    
    private GoogleMap.OnCameraChangeListener mOnCameraChangeListener = new GoogleMap.OnCameraChangeListener() {
		
		@Override
		public void onCameraChange(CameraPosition position) {
			//check if zoom has changed
			MapInfoView mapInfo = (MapInfoView) findViewById(R.id.info);
			mapInfo.setZoomLevel((int)position.zoom); 
			mapInfo.postInvalidate();
		}
	};    
	
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
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.gmsclient3_main);
         
        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
    
        // Set up the drawer.
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
        
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        
        loadingHandler = new LoadingHandler(this);
        
        mapFragment.getMapAsync(this); 
        
        LoggerUtils.debug("GMSClient3MainActivity.onCreate called...");
        
        UserTracker.getInstance().trackActivity(getClass().getName());

        ConfigurationManager.getInstance().setContext(getApplicationContext());
        OsUtil.setDisplayType(getResources().getConfiguration());
        
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
        	//actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            //actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getTitle());
            actionBar.hide();
        }
        
        buildGoogleApiClient();
        
        initComponents(savedInstanceState);      
    }

    private void initComponents(Bundle savedInstanceState) {
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

        checkinManager = new CheckinManager(asyncTaskManager, this);

        cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        if (cm == null || !cm.isInitialized()) {
            LoggerUtils.debug("Loading deal categories...");
            cm = new CategoriesManager();
            ConfigurationManager.getInstance().putObject(ConfigurationManager.DEAL_CATEGORIES, cm);
            asyncTaskManager.executeDealCategoryLoaderTask(cm, false);
        }

        dialogManager = new DialogManager(this, intents, asyncTaskManager, landmarkManager, checkinManager, loadingHandler, trackMyPosListener);

        LatLng mapCenter = (LatLng) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, LatLng.class);
            
        if (mapCenter != null) {
        	initOnLocationChanged(mapCenter, 2);
        } else {
        	loadingHandler.sendEmptyMessageDelayed(PICK_LOCATION, 5000);//ConfigurationManager.FIVE_SECONDS);
        }
        
        loadingProgressBar.setProgress(50);
    }
    
    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.mapContainer, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    @Override
    public void onResume() {
    	super.onResume();
        LoggerUtils.debug("onResume");
    	mGoogleApiClient.connect();
    	mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        
        asyncTaskManager.setActivity(this);
        
        if (landmarkManager != null && landmarkManager.hasMyLocation() && ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
        	mapButtons.setVisibility(View.VISIBLE);
        }
        
        //verify access token
        asyncTaskManager.executeGetTokenTask();

        Integer searchQueryResult = (Integer) ConfigurationManager.getInstance().removeObject(ConfigurationManager.SEARCH_QUERY_RESULT, Integer.class);
        if (searchQueryResult != null) {
        	int[] coordsE6 = intents.showSelectedLandmark(searchQueryResult, getMyPosition(), lvView, layerLoader, (int)mMap.getCameraPosition().zoom, null, projection);
            if (coordsE6 != null) {
            	animateTo(new LatLng(MathUtils.coordIntToDouble(coordsE6[0]),MathUtils.coordIntToDouble(coordsE6[1])));
            }
        } else if (landmarkManager != null && landmarkManager.getSeletedLandmarkUI() != null) {
            getActionBar().hide();
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
            if (ConfigurationManager.getInstance().containsObject(ConfigurationManager.MAP_CENTER, LatLng.class)) {
                loadingHandler.removeMessages(PICK_LOCATION);
                loadingHandler.sendEmptyMessage(PICK_LOCATION);
            }
        }
        
        syncRoutesOverlays();
        
        intents.startAutoCheckinBroadcast();
    }
    
    private void syncRoutesOverlays() {
    	
    	//TODO 
    	
    	/*int routesCount = 0;
    	if (routesManager != null) {
    	   routesCount = routesManager.getCount();
    	}
    	
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
    		for (Iterator<String> i = routesManager.getRoutes().iterator(); i.hasNext();) {
                addRoutesOverlay(i.next());
            }
    		isRouteDisplayed = true;
    	}*/
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
        //show network status dialog
        Object networkStatus = ConfigurationManager.getInstance().getObject("NetworkStatus", Object.class);
        boolean networkActive = ServicesUtils.isNetworkActive(this);
        if (networkStatus == null && !networkActive) {
            dialogManager.showAlertDialog(AlertDialogBuilder.NETWORK_ERROR_DIALOG, null, null);
            ConfigurationManager.getInstance().putObject("NetworkStatus", new Object());
        }

		//show rate us dialog
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
        if (mGoogleApiClient.isConnected()) {
        	LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        
        if (dialogManager != null) {
            dialogManager.dismissDialog();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
    	LoggerUtils.debug("onDestroy");
        if (ConfigurationManager.getInstance().isClosing()) {
        	appInitialized = false;
        	intents.hardClose(layerLoader, routeRecorder, loadingHandler, null, (int)mMap.getCameraPosition().zoom, MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude));
        } //else if (mapView.getMapCenter().getLatitudeE6() != 0 && mapView.getMapCenter().getLongitudeE6() != 0) {
        //    intents.softClose(mapView.getZoomLevel(), mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6());
        //    ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, mMap.getCameraPosition().target);
        //}
        AdsUtils.destroyAdView(this);
        System.gc();
    	super.onDestroy();
    }
    
    @Override
    public void onRestart() {
        super.onRestart();
        LoggerUtils.debug("onRestart");
        //when map provider is changing we need to restart activity
        if (ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER) == ConfigurationManager.OSM_MAPS ) {
            //TODO 
        	intents.showInfoToast("Shoud start osm map now. Not yet implemented");
        	//Intent intent = getIntent();
            //ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, mapView.getMapCenter());
            //LocationServicesManager.disableMyLocation();
            //finish();
            //startActivity(intent);
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
            intents.startSearchActivity(MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), 
            		MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude), -1, false);
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.main_menu_2, menu);
            return true;
        } else {
        	return super.onCreateOptionsMenu(menu);
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	if (ConfigurationManager.getInstance().isClosing()) {
            return false;
        } else {         
        	//if routes layer doesn't exists don't show routes menu
        	MenuItem routes = menu.findItem(R.id.routes);
        	if (landmarkManager.getLayerManager().containsLayer(Commons.ROUTES_LAYER)) {
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
        			if (routeRecorder != null && routeRecorder.isPaused()) {
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
            //

        	menu.findItem(R.id.shareScreenshot).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	menu.findItem(R.id.dataPacket).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	menu.findItem(R.id.reset).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	menu.findItem(R.id.releaseNotes).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	menu.findItem(R.id.config).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	      	
            menu.findItem(R.id.login).setVisible(!ConfigurationManager.getUserManager().isUserLoggedInFully());

            menu.findItem(R.id.register).setVisible(!ConfigurationManager.getUserManager().isUserLoggedInGMSWorld());
            
            //TODO
            //if (drawerLayout.isDrawerOpen(drawerLinearLayout)) {
            //	NavigationDrawerExpandableListAdapter adapter = (NavigationDrawerExpandableListAdapter) drawerList.getExpandableListAdapter();
            //    adapter.rebuild(ProjectionFactory.getProjection(mapView, googleMapsView));
        	//}
            
            return super.onPrepareOptionsMenu(menu);
        }   	 
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	//don't implement
        return super.onOptionsItemSelected(item);
    }
    
    @Override
	public void onClick(View v) {		
    	if (ConfigurationManager.getUserManager().isUserAllowedAction() || v == lvCloseButton || v == myLocationButton || v == nearbyLandmarksButton) {
    		if (v == nearbyLandmarksButton) {
        		intents.startLayersListActivity(true);
        	} else {
        		final ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
        		if (selectedLandmark != null) {
        			if (v == lvCloseButton) {
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CloseSelectedLandmarkView", selectedLandmark.getLayer(), 0);
        				hideLandmarkView();
        			} else if (v == lvCommentButton) {
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CommentSelectedLandmark", selectedLandmark.getLayer(), 0);
        				intents.commentButtonPressedAction();
        			} else if (v == lvCheckinButton) {
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CheckinSelectedLandmark", selectedLandmark.getLayer(), 0);
        				boolean authStatus = intents.checkAuthStatus(selectedLandmark);
        				if (authStatus) {
        					boolean addToFavourites = ConfigurationManager.getInstance().isOn(ConfigurationManager.AUTO_CHECKIN) && !selectedLandmark.getLayer().equals(Commons.MY_POSITION_LAYER);
        					checkinManager.checkinAction(addToFavourites, false, selectedLandmark);
        				}
        			} else if (v == lvOpenButton) { 
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenURLSelectedLandmark", selectedLandmark.getLayer(), 0);
        				intents.openButtonPressedAction(selectedLandmark);
        			} else if (v == thumbnailButton) {
        				if (intents.startStreetViewActivity(selectedLandmark)) {
        					UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenStreetView", selectedLandmark.getLayer(), 0);
        				} else {
        					UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenURLSelectedLandmark", selectedLandmark.getLayer(), 0);
            				intents.openButtonPressedAction(selectedLandmark);
        				}
        			} else if (v == lvCallButton) {
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CallSelectedLandmark", selectedLandmark.getLayer(), 0);
        				intents.startPhoneCallActivity(selectedLandmark);
        			} else if (v == lvRouteButton) {
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowRouteSelectedLandmark", selectedLandmark.getLayer(), 0);
        				if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
        					dialogManager.showAlertDialog(AlertDialogBuilder.ROUTE_DIALOG, null, null);
        				} else {
        					intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
        				}	
             		} else if (v == lvShareButton) {
        				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShareSelectedLandmark", selectedLandmark.getLayer(), 0);
        				intents.shareLandmarkAction(dialogManager);
        			} 
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
    	if (requestCode == PICK_LOCATION) {
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
                
                if (!appInitialized) {
                	initOnLocationChanged(new LatLng(lat, lng), 4);
                } else {
                	pickPositionAction(new LatLng(lat, lng), true, true);
                }
                landmarkManager.addLandmark(lat, lng, 0.0f, StringUtil.formatCommaSeparatedString(name), "", Commons.LOCAL_LAYER, true);
            } else if (resultCode == RESULT_CANCELED && !appInitialized) {
                ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
                intents.showInfoToast(Locale.getMessage(R.string.Pick_location_default, landmark.getName()));
                initOnLocationChanged(new LatLng(52.25, 20.95), 5);
            } else if (resultCode == RESULT_CANCELED && intent != null && intent.hasExtra("message")) {
                String message = intent.getStringExtra("message");
                intents.showInfoToast(message);
            } else if (resultCode != RESULT_CANCELED) {
                intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
            	Status status = PlaceAutocomplete.getStatus(this, intent);
                intents.showInfoToast(status.getStatusMessage());
            	if (! appInitialized) {
            		initOnLocationChanged(new LatLng(52.25, 20.95), 6);
            	}
            }
        }
    }
    
	@Override
	public void onMapReady(GoogleMap map) {
		Log.d(this.getClass().getName(), "Google Map is ready!");
		this.mMap = map;
		this.projection = new GoogleLandmarkProjectionV2(mMap);
		mMap.getUiSettings().setZoomControlsEnabled(true);
	    mMap.setMyLocationEnabled(true);
	    mMap.setOnMyLocationButtonClickListener(this);
	    mMap.setOnCameraChangeListener(mOnCameraChangeListener);
	    
	    markerCluster = new GoogleMarkerClusterOverlay(this, mMap, loadingHandler, landmarkManager);	    
	}  
	
	@Override
	public boolean onMyLocationButtonClick() {
		showMyPositionAction(true);
		return true;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		//user location has changed	
		Toast.makeText(this, "New location received: " + location.getLatitude() + "," + location.getLongitude() + " from " + location.getProvider(), Toast.LENGTH_SHORT).show();
		if (! appInitialized) {
			initOnLocationChanged(new LatLng(location.getLatitude(), location.getLongitude()), 3);
		} else {
			intents.addMyLocationLandmark(location);     
	    	intents.vibrateOnLocationUpdate();
	    	UserTracker.getInstance().sendMyLocation();
	    	
	    	if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
	        	mapButtons.setVisibility(View.GONE);
	        	showMyPositionAction(false);
	            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
	                if (routeRecorder != null) {
	                	routeRecorder.addCoordinate(location.getLatitude(), location.getLongitude(), (float)location.getAltitude(), location.getAccuracy(), location.getSpeed(), location.getBearing());
	                }
	            }
	        } else {
	        	mapButtons.setVisibility(View.VISIBLE);
	        	//postInvalidate();
	        }
	        
	        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.AUTO_CHECKIN)) {
	            checkinManager.autoCheckin(location.getLatitude(), location.getLongitude(), false);
	        }
		}
		
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		/*if (connectionResult.hasResolution()) {
			try {
	            // Start an Activity that tries to resolve the error
	            connectionResult.startResolutionForResult(this, 0);
	        } catch (IntentSender.SendIntentException e) {
	            //e.printStackTrace();
	        }
	    } else {
	        Log.i(getClass().getName(), "Location services connection failed with code " + connectionResult.getErrorCode());
	    }*/
	}

	@Override
    public void onConnected(Bundle bundle) {
		Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
		if (location != null && !appInitialized) {
			//Toast.makeText(this, "Last known location received: " + location.getLatitude() + "," + location.getLongitude() + " from " + location.getProvider(), Toast.LENGTH_SHORT).show();
			initOnLocationChanged(new LatLng(location.getLatitude(), location.getLongitude()), 0);
		}
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

	@Override
	public void onConnectionSuspended(int reason) {
		//call logger
	}
    
	protected boolean onMenuItemSelected(int itemId) {
		if (ConfigurationManager.getUserManager().isUserAllowedAction() || itemId == android.R.id.home || itemId == R.id.exit || itemId == R.id.login || itemId == R.id.register) {	
		 switch (itemId) {
				case R.id.settings:
					//TODO
					intents.showInfoToast("Settings activity will be added");
					//intents.startSettingsActivity(SettingsActivity.class);
					break;
				case R.id.search:
					onSearchRequested();
					break;
				case R.id.exit:
					dialogManager.showAlertDialog(AlertDialogBuilder.EXIT_DIALOG, null, null);
					break;
				//case android.R.id.home:
					//	dialogManager.showAlertDialog(AlertDialogBuilder.EXIT_DIALOG, null, null);
					//	break;
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
					mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude,
		            (int)mMap.getCameraPosition().zoom, projection);
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
		    		intents.startMyLandmarksIntent(getMyPosition());
		    		break;
				case R.id.recentLandmarks:
		    		intents.startRecentLandmarksIntent(getMyPosition());
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
		    		if (intents.startRouteFileLoadingActivity()) {
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
				case R.id.config:
					intents.startConfigurationViewerActivity();
					break;
				case R.id.socialNetworks:
		    		intents.startSocialListActivity();
		    		break;
				case R.id.dataPacket:
		    		dialogManager.showAlertDialog(AlertDialogBuilder.PACKET_DATA_DIALOG, null, null);
		    		break;
				case R.id.pickMyPos:
		    		intents.startPickLocationActivity();
		    		break;
				case R.id.deals:
		    		if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
		    			intents.startCategoryListActivity(MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude), -1, -1);
		    		} else {
		        		intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
		    		}
		    		break;
				case R.id.register:
		    		intents.startRegisterActivity();
		    		break;
				case R.id.newestLandmarks:
					final String[] excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER};
					intents.startNewestLandmarkIntent(getMyPosition(), excluded, 2);
					break;
				case R.id.events:
		    		intents.startCalendarActivity(getMyPosition());
		    		break;
				case R.id.rateUs:
		    		dialogManager.showAlertDialog(AlertDialogBuilder.RATE_US_DIALOG, null, null);
		    		break;
				case R.id.listLandmarks:
		    		if (!lvView.isShown()) {
		        		intents.showNearbyLandmarks(getMyPosition(), projection);
		    		}
		    		break;
				case R.id.shareScreenshot:
					asyncTaskManager.executeUploadImageTask(mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude, true);
					break;
				case R.id.reset:
	            	dialogManager.showAlertDialog(AlertDialogBuilder.RESET_DIALOG, null, null);
	            	break;	
				default:
					return true;
		 	}
		} else {
			intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
		}
		return true;
    }
	
	protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

	private synchronized void initOnLocationChanged(LatLng location, int source) {
    	//remove
    	//try {
    	//	intents.showInfoToast("Setting map center to " + location.getLatitudeE6() + "," + location.getLongitudeE6() + ", source: " + source + ", lm initialized: " + landmarkManager.isInitialized());
    	//} catch (Exception e) {
    	//}
    	//
    	//System.out.println("4 --------------------------------");
    	if (!appInitialized && location != null) {
    		//System.out.println("4.1 --------------------------------");
        	loadingProgressBar.setProgress(75);
        	    	
        	//mapController.setCenter(location);
        	
        	if (!landmarkManager.isInitialized()) {
        		landmarkManager.initialize();
            }
            
            //addLandmarkOverlay();

            //if (myLocation != null) {
            //	addOverlay(myLocation);
            //}

            routesManager = ConfigurationManager.getInstance().getRoutesManager();

            if (routesManager == null) {
                LoggerUtils.debug("Creating RoutesManager...");
                routesManager = new RoutesManager();
                ConfigurationManager.getInstance().putObject("routesManager", routesManager);
            } else {
            	syncRoutesOverlays();
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
                    LoggerUtils.debug("Loading Layers in " + location.latitude + "," +  location.longitude);
                    intents.loadLayersAction(true, null, false, true, layerLoader, location.latitude, location.longitude,
                            (int)mMap.getCameraPosition().zoom, projection);
                }
            } else {
                //load existing layers
                if (layerLoader.isLoading()) {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_VISIBLE);
                } else {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_GONE);
                }
                loadingHandler.sendEmptyMessage(MessageStack.STATUS_MESSAGE);
                //postInvalidate();
            }

            loadingProgressBar.setProgress(100);
            
            layerLoader.setRepaintHandler(loadingHandler);
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
                loadingImage.setVisibility(View.GONE);
            }
            
            if (mMap != null) {
    	    	CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM));
    	    	mMap.moveCamera(cameraUpdate);
    	    	loadingHandler.sendEmptyMessage(SHOW_MAP_VIEW);
    	    	appInitialized = true;
    	    } else {
    	    	//TODO show toast something went wrong
    	    	intents.showInfoToast("Map initialization has failed. Please restart appllcation!");
    	    }
        } 
    }
	
	private void hideLandmarkView() {
    	lvView.setVisibility(View.GONE);
		getSupportActionBar().show();
		landmarkManager.clearLandmarkOnFocusQueue();
		landmarkManager.setSelectedLandmark(null);
		landmarkManager.setSeletedLandmarkUI();
    }
	
	private void animateTo(LatLng newLocation) {
		if (mMap != null) {
			CameraUpdate location = CameraUpdateFactory.newLatLng(newLocation);
			mMap.animateCamera(location);
		}
		
	}
	
	private String followMyPositionAction() {
		intents.showInfoToast("Not implemented yet");
        /*if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
            ConfigurationManager.getInstance().setOn(ConfigurationManager.FOLLOW_MY_POSITION);
            String route = routeRecorder.startRecording();
            showRouteAction(route);
            if (layerLoader.isLoading()) {
                layerLoader.stopLoading();
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
                String filename = routeRecorder.stopRecording();
                if (filename != null) {
                    return filename;
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Routes_TrackMyPosOff));
                }
            } else {
                intents.showInfoToast(Locale.getMessage(R.string.Routes_TrackMyPosOff));
            }
        }*/
        return null;
    }
	
	private double[] getMyPosition() {
        return landmarkManager.getMyLocation(mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude);
    }

    private void showRouteAction(String routeKey) {
        //TODO
    	/*LoggerUtils.debug("Adding route to view: " + routeKey);
        if (routesManager.containsRoute(routeKey) && landmarkManager.getLayerManager().isLayerEnabled(Commons.ROUTES_LAYER)) {
            addRoutesOverlay(routeKey);
            isRouteDisplayed = true;
            if (!routeKey.startsWith(RouteRecorder.CURRENTLY_RECORDED)) {
                double[] locationCoords = routesManager.calculateRouteCenter(routeKey);
                IGeoPoint newCenter = new org.osmdroid.google.wrapper.GeoPoint(new GeoPoint(MathUtils.coordDoubleToInt(locationCoords[0]), MathUtils.coordDoubleToInt(locationCoords[1])));
                mapController.setCenter(newCenter);
                mapController.setZoom(routesManager.calculateRouteZoom(routeKey));
            }
            postInvalidate();
        }*/
    }

    private void clearMapAction() {
        landmarkManager.clearLandmarkStore();
        markerCluster.clearMarkers();
    	routesManager.clearRoutesStore();
        syncRoutesOverlays();
        intents.showInfoToast(Locale.getMessage(R.string.Maps_cleared));
    }
    
    private void showMyPositionAction(boolean loadLayers) {
        boolean isVisible = false;
        boolean clearLandmarks = false;
       
        /*if (projection.isVisible(myLoc.getLatitudeE6(), myLoc.getLongitudeE6())) {
            isVisible = true;
        }
        if (!isVisible) {
            hideLandmarkView();
            clearLandmarks = intents.isClearLandmarksRequired(projection, mapCenter.getLatitudeE6(), mapCenter.getLongitudeE6(),
                        myLoc.getLatitudeE6(), myLoc.getLongitudeE6());
        }*/

        if (loadLayers && !isVisible) {
            markerCluster.clearMarkers();
            intents.loadLayersAction(true, null, clearLandmarks, true, layerLoader, mMap.getCameraPosition().target.latitude, 
            		mMap.getCameraPosition().target.latitude, (int)mMap.getCameraPosition().zoom, projection);
        }
    }
    
    private void pickPositionAction(LatLng newCenter, boolean loadLayers, boolean clearMap) {
    	if (clearMap && markerCluster != null) {
    		markerCluster.clearMarkers();
    	}
    	if (mMap != null) {
	    	CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(newCenter);
	    	mMap.moveCamera(cameraUpdate);
	    } 
        if (loadLayers) {     	
            intents.loadLayersAction(true, null, clearMap, true, layerLoader,
                    mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude,
                    (int)mMap.getCameraPosition().zoom, projection);
        }
    }
	
	//autogenerated placeholder
    public static class PlaceholderFragment extends Fragment {
        
    	private static final String ARG_SECTION_NUMBER = "section_number";

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.gmsclient3_fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            //((GMSClient3MainActivity) activity).onSectionAttached(
            //        getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }  
	    
	private static class LoadingHandler extends Handler {
		
		private WeakReference<GMSClient3MainActivity> parentActivity;
    	
    	public LoadingHandler(GMSClient3MainActivity parentActivity) {
    		this.parentActivity = new WeakReference<GMSClient3MainActivity>(parentActivity);
    	}
		
		@Override
        public void handleMessage(Message msg) {
			GMSClient3MainActivity activity = parentActivity.get();
        	if (activity != null && !activity.isFinishing()) {
        		if (msg.what == SHOW_MAP_VIEW) {
        			if (!activity.findViewById(R.id.mapContainer).isShown()) {
        				Log.d(this.getClass().getName(), "Showing map view...");
            			activity.findViewById(R.id.mapContainer).setVisibility(View.VISIBLE);
        				activity.findViewById(R.id.mapCanvasWidgetL).setVisibility(View.GONE);
        			}
                	if (activity.lvView == null || !activity.lvView.isShown()) {
                		if (activity.getSupportActionBar() != null) {
                			activity.getSupportActionBar().show();
                		}	
                	}
            	} else if (msg.what == PICK_LOCATION) {
            		if (! activity.appInitialized) {
            			activity.intents.startPickLocationActivity();
            		}
            	} else if (msg.what == MessageStack.STATUS_MESSAGE) {
        			activity.statusBar.setText(activity.messageStack.getMessage());
            	} else if (msg.what == MessageStack.STATUS_VISIBLE && !ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
            		activity.loadingImage.setVisibility(View.VISIBLE);
            	} else if (msg.what == MessageStack.STATUS_GONE) {
            		activity.loadingImage.setVisibility(View.GONE);
            	} else if (msg.what == LayerLoader.LAYER_LOADED) {
            		activity.markerCluster.addMarkers((String)msg.obj); 
            		//activity.postInvalidate();
            	} else if (msg.what == LayerLoader.ALL_LAYERS_LOADED) {
            		//TODO
            		//if (activity.mapProvider == ConfigurationManager.OSM_MAPS || activity.googleMapsView.canCoverCenter()) {
            		//	activity.asyncTaskManager.executeUploadImageTask(MathUtils.coordIntToDouble(activity.mapView.getMapCenter().getLatitudeE6()),
                    //        MathUtils.coordIntToDouble(activity.mapView.getMapCenter().getLongitudeE6()), false);
            		//}	
            	} else if (msg.what == LayerLoader.FB_TOKEN_EXPIRED) {
            		activity.intents.showInfoToast(Locale.getMessage(R.string.Social_token_expired, "Facebook"));
            	} else if (msg.what == GoogleMarkerClusterOverlay.SHOW_LANDMARK_DETAILS) {
            		int[] coordsE6 = activity.intents.showLandmarkDetailsAction(activity.getMyPosition(), activity.lvView, activity.layerLoader, (int)activity.mMap.getCameraPosition().zoom, null, activity.projection);
                    if (coordsE6 != null) {
                    	activity.animateTo(new LatLng(MathUtils.coordIntToDouble(coordsE6[0]),MathUtils.coordIntToDouble(coordsE6[1])));
                    }
            	} else if (msg.what == GoogleMarkerClusterOverlay.SHOW_LANDMARK_LIST) {
            		activity.intents.startMultiLandmarkIntent(activity.getMyPosition());
            		activity.animateTo(new LatLng(MathUtils.coordIntToDouble(msg.arg1), MathUtils.coordIntToDouble(msg.arg2)));
            	} else if (msg.what == AsyncTaskManager.SHOW_ROUTE_MESSAGE) {
            		activity.showRouteAction((String) msg.obj);
            	} else if (msg.obj != null) {
            		LoggerUtils.error("Unknown message received: " + msg.obj.toString());
            	}
        	}		
		}
	}
}
