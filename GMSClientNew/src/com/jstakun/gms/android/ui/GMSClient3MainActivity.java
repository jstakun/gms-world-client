package com.jstakun.gms.android.ui;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.List;

import org.osmdroid.util.GeoPoint;

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
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.config.Commons; 
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FavouritesDAO;
import com.jstakun.gms.android.data.FavouritesDbDataSource;
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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.AudioManager;
import android.media.SoundPool;
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
import android.view.KeyEvent;
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
	private GoogleRoutesOverlay routesCluster;
	
	private TextView statusBar;
    private View lvCloseButton, lvCallButton, lvCommentButton, 
            lvOpenButton, lvView, lvShareButton,
            thumbnailButton, lvCheckinButton, lvRouteButton, loadingImage;
    private ProgressBar loadingProgressBar;
	private NavigationDrawerFragment mNavigationDrawerFragment;
   
    private boolean appInitialized = false;
    
    private GoogleMap.OnCameraChangeListener mOnCameraChangeListener = new GoogleMap.OnCameraChangeListener() {
		
		@Override
		public void onCameraChange(CameraPosition position) {
			//check if zoom has changed
			int currentZoom = ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM);
			if (currentZoom != (int)position.zoom) {
				ConfigurationManager.getInstance().putInteger(ConfigurationManager.ZOOM, (int)position.zoom);
				markerCluster.cluster();
			}
			MapInfoView mapInfo = (MapInfoView) findViewById(R.id.info);
			mapInfo.setZoomLevel((int)position.zoom); 
			mapInfo.setMaxZoom((int)mMap.getMaxZoomLevel());
			mapInfo.setDrawDistance(false);
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
        
        loadingHandler = new LoadingHandler(this);
        
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        
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
        
        lvCheckinButton = findViewById(R.id.lvCheckinButton);
        lvCloseButton = findViewById(R.id.lvCloseButton);
        lvOpenButton = findViewById(R.id.lvOpenButton);
        lvCommentButton = findViewById(R.id.lvCommentButton);
        lvCallButton = findViewById(R.id.lvCallButton);
        lvRouteButton = findViewById(R.id.lvRouteButton);
        lvShareButton = findViewById(R.id.lvShareButton);
        thumbnailButton = findViewById(R.id.thumbnailButton);
        
        lvCheckinButton.setOnClickListener(this);
        lvCloseButton.setOnClickListener(this);
        lvOpenButton.setOnClickListener(this);
        lvCommentButton.setOnClickListener(this);
        lvCallButton.setOnClickListener(this);
        lvRouteButton.setOnClickListener(this);
        lvShareButton.setOnClickListener(this);
        thumbnailButton.setOnClickListener(this);
            	
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

        routesManager = ConfigurationManager.getInstance().getRoutesManager();

        if (routesManager == null) {
            LoggerUtils.debug("Creating RoutesManager...");
            routesManager = new RoutesManager();
            ConfigurationManager.getInstance().putObject("routesManager", routesManager);
        } 
        
        LatLng mapCenter = (LatLng) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, LatLng.class);
            
        if (mapCenter != null) {
        	initOnLocationChanged(mapCenter, 2);
        } else {
        	loadingHandler.sendEmptyMessageDelayed(PICK_LOCATION, ConfigurationManager.FIVE_SECONDS);
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
        
        //verify access token
        asyncTaskManager.executeGetTokenTask();

        Integer searchQueryResult = (Integer) ConfigurationManager.getInstance().removeObject(ConfigurationManager.SEARCH_QUERY_RESULT, Integer.class);
        if (searchQueryResult != null) {
        	int[] coordsE6 = intents.showSelectedLandmark(searchQueryResult, getMyPosition(), lvView, layerLoader, (int)mMap.getCameraPosition().zoom, null, projection);
            if (coordsE6 != null) {
            	animateTo(new LatLng(MathUtils.coordIntToDouble(coordsE6[0]),MathUtils.coordIntToDouble(coordsE6[1])));
            }
        } else if (landmarkManager != null && landmarkManager.getSeletedLandmarkUI() != null) {
            getSupportActionBar().hide();
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
        
        intents.startAutoCheckinBroadcast();
        
        if (markerCluster != null) {
        	markerCluster.loadAllMarkers();
        	routesCluster.loadAllRoutes();
        }
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
        if (mMap != null) {
        	ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, mMap.getCameraPosition().target);
        }
    }

    @Override
    public void onDestroy() {
    	LoggerUtils.debug("onDestroy");
        if (ConfigurationManager.getInstance().isClosing()) {
        	appInitialized = false;
        	intents.hardClose(layerLoader, routeRecorder, loadingHandler, null, (int)mMap.getCameraPosition().zoom, MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude));
        } else {
            intents.softClose((int)mMap.getCameraPosition().zoom, MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude));
            //ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, mMap.getCameraPosition().target);
        }
        AdsUtils.destroyAdView(this);
        System.gc();
    	super.onDestroy();
    }
    
    @Override
    public void onRestart() {
        super.onRestart();
        LoggerUtils.debug("onRestart");
        if (ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER) == ConfigurationManager.OSM_MAPS) {
            Intent intent = new Intent(this, GMSClient2OSMMainActivity.class);
            LatLng mapCenter = (LatLng) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, LatLng.class);
            if (mapCenter != null) {
            	GeoPoint center = new GeoPoint(MathUtils.coordDoubleToInt(mapCenter.latitude), MathUtils.coordDoubleToInt(mapCenter.longitude));
            	ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, center);
            }
            finish();
            startActivity(intent);
        } 
        
        if (mMap != null) {
        	int googleMapsType = ConfigurationManager.getInstance().getInt(ConfigurationManager.GOOGLE_MAPS_TYPE);

    	    LoggerUtils.debug("Google Maps type is " + googleMapsType);
    	    
    	    if (googleMapsType == 1) {
            	mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            	mMap.setTrafficEnabled(false);
            } else if (googleMapsType == 2) {
            	mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            	mMap.setTrafficEnabled(true);
            } else {
            	mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            	mMap.setTrafficEnabled(false);
            }
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
        	if (routes != null) {
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
        	}
            //

        	if (menu.findItem(R.id.shareScreenshot) != null) {
        		menu.findItem(R.id.shareScreenshot).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	}
        	if (menu.findItem(R.id.dataPacket) != null) {
        		menu.findItem(R.id.dataPacket).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	}
        	if (menu.findItem(R.id.reset) != null) {
        		menu.findItem(R.id.reset).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	}
        	if (menu.findItem(R.id.releaseNotes) != null) {
        		menu.findItem(R.id.releaseNotes).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	}
        	if (menu.findItem(R.id.config) != null) {
        		menu.findItem(R.id.config).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	}
        	
        	if (menu.findItem(R.id.login) != null) {
        		menu.findItem(R.id.login).setVisible(!ConfigurationManager.getUserManager().isUserLoggedInFully());
        	}
            
        	if (menu.findItem(R.id.register) != null) {
        		menu.findItem(R.id.register).setVisible(!ConfigurationManager.getUserManager().isUserLoggedInGMSWorld());
        	}
        	
        	mNavigationDrawerFragment.refreshDrawer(projection);
            
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
    	if (ConfigurationManager.getUserManager().isUserAllowedAction() || v == lvCloseButton) {
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
    	} else {
    		intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
    	}
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
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
                
                if (!appInitialized) {
                	initOnLocationChanged(new LatLng(lat, lng), 4);
                } else {
                	pickPositionAction(new LatLng(lat, lng), true, true);
                }
                landmarkManager.addLandmark(lat, lng, 0.0f, StringUtil.formatCommaSeparatedString(name), "", Commons.LOCAL_LAYER, true);
            } else if (resultCode == RESULT_CANCELED && !appInitialized) {
                ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
                intents.showInfoToast(Locale.getMessage(R.string.Pick_location_default, landmark.getName()));
                initOnLocationChanged(new LatLng(landmark.getQualifiedCoordinates().getLatitude(), landmark.getQualifiedCoordinates().getLongitude()), 5);
            } else if (resultCode == RESULT_CANCELED && intent != null && intent.hasExtra("message")) {
                String message = intent.getStringExtra("message");
                intents.showInfoToast(message);
            } else if (resultCode != RESULT_CANCELED) {
                intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
            	Status status = PlaceAutocomplete.getStatus(this, intent);
                intents.showInfoToast(status.getStatusMessage());
            	if (! appInitialized) {
            		ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
                    initOnLocationChanged(new LatLng(landmark.getQualifiedCoordinates().getLatitude(), landmark.getQualifiedCoordinates().getLongitude()), 6);
            	}
            }
        } else if (requestCode == IntentsHelper.INTENT_MULTILANDMARK) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                if (action.equals("load")) {
                    String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);
                    int id = Integer.parseInt(ids);
                    int[] coordsE6 = intents.showSelectedLandmark(id, getMyPosition(), lvView, layerLoader, (int)mMap.getCameraPosition().zoom, null, projection);
                    if (coordsE6 != null) {
                    	animateTo(new LatLng(MathUtils.coordIntToDouble(coordsE6[0]),MathUtils.coordIntToDouble(coordsE6[1])));;
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
                        pickPositionAction(new LatLng(l.getQualifiedCoordinates().getLatitude(), l.getQualifiedCoordinates().getLongitude()), true, true);
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
                FavouritesDbDataSource fdb = (FavouritesDbDataSource) ConfigurationManager.getInstance().getObject("FAVOURITESDB", FavouritesDbDataSource.class);
                FavouritesDAO fav = fdb.getLandmark(favouriteId);
                if (fav != null) {
                    pickPositionAction(new LatLng(fav.getLatitude(), fav.getLongitude()), true, false);
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
                    int[] coordsE6 = intents.showSelectedLandmark(id, getMyPosition(), lvView, layerLoader, (int)mMap.getCameraPosition().zoom, null, projection);
                    if (coordsE6 != null) {
                    	animateTo(new LatLng(MathUtils.coordIntToDouble(coordsE6[0]),MathUtils.coordIntToDouble(coordsE6[1])));
                    }
                }
            }
        } else {
            intents.processActivityResult(requestCode, resultCode, intent, getMyPosition(), new double[]{mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude}, loadingHandler, (int)mMap.getCameraPosition().zoom, layerLoader, projection);
        }

    }
    
	@Override
	public void onMapReady(GoogleMap map) {
		Log.d(this.getClass().getName(), "Google Map is ready!");
		this.mMap = map;
		this.projection = new GoogleLandmarkProjectionV2(mMap);
		
	    int googleMapsType = ConfigurationManager.getInstance().getInt(ConfigurationManager.GOOGLE_MAPS_TYPE);

	    LoggerUtils.debug("Google Maps type is " + googleMapsType);
	    
	    if (googleMapsType == 1) {
        	mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        	mMap.setTrafficEnabled(false);
        } else if (googleMapsType == 2) {
        	mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        	mMap.setTrafficEnabled(true);
        } else {
        	mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        	mMap.setTrafficEnabled(false);
        }
	    
	    mMap.getUiSettings().setZoomControlsEnabled(true);
	    mMap.setMyLocationEnabled(true);
	    mMap.setOnMyLocationButtonClickListener(this);
	    mMap.setOnCameraChangeListener(mOnCameraChangeListener);
	    
	    if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
	    	mMap.getUiSettings().setCompassEnabled(true);
	    }
	}  

	@Override
	public boolean onMyLocationButtonClick() {
		if (ConfigurationManager.getInstance().getLocation() != null) {
			showMyPositionAction(true);
		} else {
			intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
		}
		return true;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		//user location has changed	
		if (!appInitialized && !isFinishing()) {
			initOnLocationChanged(new LatLng(location.getLatitude(), location.getLongitude()), 3);
		}
		
		if (appInitialized && !isFinishing()) {
		
			ConfigurationManager.getInstance().setLocation(location);
		
			intents.addMyLocationLandmark(location);     
			intents.vibrateOnLocationUpdate();
			UserTracker.getInstance().sendMyLocation();
	    	
			if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
				showMyPositionAction(false);
				if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
					if (routeRecorder != null) {
	                	routeRecorder.addCoordinate(location.getLatitude(), location.getLongitude(), (float)location.getAltitude(), location.getAccuracy(), location.getSpeed(), location.getBearing());
					}
				}
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
			ConfigurationManager.getInstance().setLocation(location);
			initOnLocationChanged(new LatLng(location.getLatitude(), location.getLongitude()), 0);
		}
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

	@Override
	public void onConnectionSuspended(int reason) {
		//call logger
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
    			} 
            	return true;
        	} else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
        		int[] coordsE6 = intents.showLandmarkDetailsAction(getMyPosition(), lvView, layerLoader, (int)mMap.getCameraPosition().zoom, null, projection);
                if (coordsE6 != null) {
                	getSupportActionBar().hide();
                	animateTo(new LatLng(MathUtils.coordIntToDouble(coordsE6[0]),MathUtils.coordIntToDouble(coordsE6[1])));
                }
            	return true;
        	} else {
            	return super.onKeyDown(keyCode, event);
        	}
    	} else {
    		intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
    		return true;
    	}
	}
	
	protected boolean onMenuItemSelected(int itemId) {
		if (ConfigurationManager.getUserManager().isUserAllowedAction() || itemId == android.R.id.home || itemId == R.id.exit || itemId == R.id.login || itemId == R.id.register) {	
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
					takeScreenshot(true);
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
        	    	
        	if (!landmarkManager.isInitialized()) {
        		landmarkManager.initialize();
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
    	    	//might need to show toast that something went wrong
    	    	//intents.showInfoToast("Map initialization has failed. Please restart application!");
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
		if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
            ConfigurationManager.getInstance().setOn(ConfigurationManager.FOLLOW_MY_POSITION);
            String route = routeRecorder.startRecording();
            routesCluster.showRouteAction(route, true);
            if (layerLoader.isLoading()) {
                layerLoader.stopLoading();
            }
            List<ExtendedLandmark> myPosV = landmarkManager.getUnmodifableLayer(Commons.MY_POSITION_LAYER);
            if (!myPosV.isEmpty()) {
                ExtendedLandmark landmark = myPosV.get(0);
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
	
	private double[] getMyPosition() {
		return landmarkManager.getMyLocation(mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude);
    }

    private void clearMapAction() {
    	mMap.clear();
    	landmarkManager.clearLandmarkStore();
        routesManager.clearRoutesStore();
        intents.showInfoToast(Locale.getMessage(R.string.Maps_cleared));
    }
    
    private void showMyPositionAction(boolean loadLayers) {
        boolean isVisible = false;
        boolean clearLandmarks = false;
       
        Location myLoc = ConfigurationManager.getInstance().getLocation();
        LatLng myLocLatLng = new LatLng(myLoc.getLatitude(), myLoc.getLongitude());
        
        if (mMap.getProjection().getVisibleRegion().latLngBounds.contains(myLocLatLng)) {
            isVisible = true;
        }
        
        if (!isVisible) {
            hideLandmarkView();
            clearLandmarks = intents.isClearLandmarksRequired(projection, 
            		 MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), 
            		 MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude),
                     MathUtils.coordDoubleToInt(myLoc.getLatitude()), MathUtils.coordDoubleToInt(myLoc.getLongitude()));
        }
        
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocLatLng));
        } else {
            animateTo(myLocLatLng);
        }

        if (loadLayers && !isVisible) {
            markerCluster.clearMarkers();
            intents.loadLayersAction(true, null, clearLandmarks, true, layerLoader, myLoc.getLatitude(), 
            		myLoc.getLongitude(), (int)mMap.getCameraPosition().zoom, projection);
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
    
    private void showMapAndMarkers() {
    	if (!findViewById(R.id.mapContainer).isShown()) {
			findViewById(R.id.mapContainer).setVisibility(View.VISIBLE);
			findViewById(R.id.mapCanvasWidgetL).setVisibility(View.GONE);
		}
    	if ((lvView == null || !lvView.isShown()) && getSupportActionBar() != null) {
    		getSupportActionBar().show();
    	}
    	
	    markerCluster = new GoogleMarkerClusterOverlay(this, mMap, loadingHandler, landmarkManager);	
	    markerCluster.loadAllMarkers();
	    
	    routesCluster = new GoogleRoutesOverlay(mMap, landmarkManager, routesManager);
	    routesCluster.loadAllRoutes();
	    
	    if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
            String route = routeRecorder.getRouteLabel();
            if (route == null) {
                route = routeRecorder.startRecording();
            }

            if (route != null) {
            	routesCluster.showRouteAction(route, true);
            }

            messageStack.addMessage(Locale.getMessage(R.string.Routes_TrackMyPosOn), 10, -1, -1);
        }
	}
	
    private void takeScreenshot(boolean notify)
    {
    	if (!ConfigurationManager.getInstance().containsObject("screenshot_gms_" + StringUtil.formatCoordE2(mMap.getCameraPosition().target.latitude) + "_" + StringUtil.formatCoordE2(mMap.getCameraPosition().target.longitude), String.class) &&
    			!isFinishing()) {
    		
    		if (notify) {
    			intents.showInfoToast(Locale.getMessage(R.string.Task_started, Locale.getMessage(R.string.shareScreenshot)));
    		}
    		
    		try {
    			SoundPool soundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
                int shutterSound = soundPool.load(this, R.raw.camera_click, 0);
                int id = soundPool.play(shutterSound, 1f, 1f, 0, 0, 1);
                LoggerUtils.debug("Shutter sound played with id " + id);
    		} catch (Exception e) {
    			LoggerUtils.error("GMSClient3MainActivity.takeScreenshot exception", e);
    		}
    		
        	SnapshotReadyCallback callback = new SnapshotReadyCallback() {

				@Override
				public void onSnapshotReady(Bitmap screenshot) {
					LoggerUtils.debug("Google Map snapshot taken!");
				
					long loadingTime = 0; 
    				Long l = (Long) ConfigurationManager.getInstance().removeObject("LAYERS_LOADING_TIME_SEC", Long.class);
    				if (l != null) {
    					loadingTime = l.longValue();
    				}
    				int version = OsUtil.getSdkVersion();
    				int numOfLandmarks = landmarkManager.getAllLayersSize();
    				int limit = ConfigurationManager.getInstance().getInt(ConfigurationManager.LANDMARKS_PER_LAYER, 30);
    				String filename = "screenshot_time_" + loadingTime + "sec_sdk_v" + version + "_num_" + numOfLandmarks + "_l_" + limit + ".jpg";
    			
    				ByteArrayOutputStream out = new ByteArrayOutputStream();
                	screenshot.compress(Bitmap.CompressFormat.JPEG, 50, out);
                
					asyncTaskManager.executeImageUploadTask(out.toByteArray(), filename, mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude);
				    //TODO open dialog to share image
				}        	
        	};
        
        	mMap.snapshot(callback);
    	}
    }
    
	//auto generated placeholder
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
        			activity.showMapAndMarkers();
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
            		if (activity.markerCluster != null) {
            			activity.markerCluster.addMarkers((String)msg.obj); 
            		}
            	} else if (msg.what == LayerLoader.ALL_LAYERS_LOADED) {
            		if (activity.mMap != null) {
            			activity.takeScreenshot(false);
            		}	
            	} else if (msg.what == LayerLoader.FB_TOKEN_EXPIRED) {
            		activity.intents.showInfoToast(Locale.getMessage(R.string.Social_token_expired, "Facebook"));
            	} else if (msg.what == GoogleMarkerClusterOverlay.SHOW_LANDMARK_DETAILS) {
            		int[] coordsE6 = activity.intents.showLandmarkDetailsAction(activity.getMyPosition(), activity.lvView, activity.layerLoader, (int)activity.mMap.getCameraPosition().zoom, null, activity.projection);
                    if (coordsE6 != null) {
                    	activity.getSupportActionBar().hide();
                    	activity.animateTo(new LatLng(MathUtils.coordIntToDouble(coordsE6[0]),MathUtils.coordIntToDouble(coordsE6[1])));
                    }
            	} else if (msg.what == GoogleMarkerClusterOverlay.SHOW_LANDMARK_LIST) {
            		activity.intents.startMultiLandmarkIntent(activity.getMyPosition());
            		activity.animateTo(new LatLng(MathUtils.coordIntToDouble(msg.arg1), MathUtils.coordIntToDouble(msg.arg2)));
            	} else if (msg.what == AsyncTaskManager.SHOW_ROUTE_MESSAGE) {
            		activity.routesCluster.showRouteAction((String) msg.obj, true);
            	} else if (msg.obj != null) {
            		LoggerUtils.error("Unknown message received: " + msg.obj.toString());
            	}
        	}		
		}
	}
}
