package com.jstakun.gms.android.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkFactory;
import com.openlapi.QualifiedCoordinates;

public class GMSClient3MainActivity extends ActionBarActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, 
                                                                         OnMapReadyCallback, OnClickListener, 
                                                                         GoogleMap.OnMyLocationButtonClickListener,
                                                                         GoogleApiClient.ConnectionCallbacks,
                                                                         GoogleApiClient.OnConnectionFailedListener,
                                                                         LocationListener {

	private static final int SHOW_MAP_VIEW = 0;
	private static final int PICK_LOCATION = 1;
	
	private Handler loadingHandler;
	private GoogleMarkerClusterOverlay markerCluster;
	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;
    
	private TextView statusBar;
    private View lvCloseButton, lvCallButton, lvCommentButton, mapButtons,
            lvOpenButton, lvView, lvShareButton, myLocationButton, nearbyLandmarksButton,
            thumbnailButton, lvCheckinButton, lvRouteButton, loadingImage;
    private ProgressBar loadingProgressBar;
   
	private NavigationDrawerFragment mNavigationDrawerFragment;
    private GoogleMap mMap;

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
        
        mapFragment.getMapAsync(this); 
        
        //LoggerUtils.debug("GMSClient3MainActivity.onCreate called...");
        
        //UserTracker.getInstance().trackActivity(getClass().getName());

        //ConfigurationManager.getInstance().setContext(getApplicationContext());
        //OsUtil.setDisplayType(getResources().getConfiguration());
        loadingHandler = new LoadingHandler(this);
       
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
        /*landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
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

        dialogManager = new DialogManager(this, intents, asyncTaskManager, landmarkManager, checkinManager, loadingHandler, trackMyPosListener);*/

        LatLng mapCenter = null; //(LatLng) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, LatLng.class);
            
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
        //LoggerUtils.debug("onResume");
    	mGoogleApiClient.connect();
    	mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        
        /*asyncTaskManager.setActivity(this);
        
        if (landmarkManager != null && landmarkManager.hasMyLocation() && ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
        	mapButtons.setVisibility(View.VISIBLE);
        }
        
        //verify access token
        asyncTaskManager.executeGetTokenTask();

        Integer searchQueryResult = (Integer) ConfigurationManager.getInstance().removeObject(ConfigurationManager.SEARCH_QUERY_RESULT, Integer.class);
        if (searchQueryResult != null) {
        	int[] coordsE6 = intents.showSelectedLandmark(searchQueryResult, getMyPosition(), lvView, layerLoader, mapView.getZoomLevel(), null, ProjectionFactory.getProjection(mapView, googleMapsView));
            if (coordsE6 != null) {
            	animateTo(coordsE6);
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
            IGeoPoint mapCenter = (IGeoPoint) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, IGeoPoint.class);
            if (mapCenter == null) {
                loadingHandler.removeCallbacks(gpsRunnable);
                loadingHandler.post(gpsRunnable);
            }
        }
        
        syncRoutesOverlays();
        
        intents.startAutoCheckinBroadcast(); */
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
        //show network status dialog
        /*Object networkStatus = ConfigurationManager.getInstance().getObject("NetworkStatus", Object.class);
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
        }*/
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
        	LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        
        /*if (dialogManager != null) {
            dialogManager.dismissDialog();
        }*/
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
    	/*LoggerUtils.debug("onDestroy");
        if (ConfigurationManager.getInstance().isClosing()) {
        	appInitialized = false;
        	intents.hardClose(layerLoader, routeRecorder, loadingHandler, gpsRunnable, mapView.getZoomLevel(), mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6());
        } else if (mapView.getMapCenter().getLatitudeE6() != 0 && mapView.getMapCenter().getLongitudeE6() != 0) {
            intents.softClose(mapView.getZoomLevel(), mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6());
            ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, mapView.getMapCenter());
        }
        AdsUtils.destroyAdView(this);
        System.gc();*/
    	super.onDestroy();
    }
    
    @Override
    public void onRestart() {
        super.onRestart();
        /*LoggerUtils.debug("onRestart");
        //when map provider is changing we need to restart activity
        if (mapProvider != ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER)) {
            Intent intent = getIntent();
            ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, mapView.getMapCenter());
            LocationServicesManager.disableMyLocation();
            finish();
            startActivity(intent);
        }*/
    }
    
    @Override
    public void onNewIntent(Intent intent) {
    	/*LoggerUtils.debug("onNewIntent");
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey("notification") && extras.containsKey("delete")) {
            boolean delete = extras.getBoolean("delete");
            if (delete) {
                Integer taskId = extras.getInt("notification");
                //System.out.println("onNewIntent " + taskId + "----------------------------------");
                asyncTaskManager.cancelTask(taskId, true);
            }
        }*/
    }
    
    @Override
    public boolean onSearchRequested() {
    	if (appInitialized) {
            //intents.startSearchActivity(mapView.getLatitudeSpan(), mapView.getLongitudeSpan(),
            //        mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6(), -1, false);
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
    	/*
    	 * if (ConfigurationManager.getInstance().isClosing()) {
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
            
            if (drawerLayout.isDrawerOpen(drawerLinearLayout)) {
            	NavigationDrawerExpandableListAdapter adapter = (NavigationDrawerExpandableListAdapter) drawerList.getExpandableListAdapter();
                adapter.rebuild(ProjectionFactory.getProjection(mapView, googleMapsView));
        	}
            
            return super.onPrepareOptionsMenu(menu);
        }
    	 */
    	return false;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	//don't implement
        return super.onOptionsItemSelected(item);
    }
    
    @Override
	public void onClick(View v) {		
    	/*
    	 * if (ConfigurationManager.getUserManager().isUserAllowedAction() || v == lvCloseButton || v == myLocationButton || v == nearbyLandmarksButton) {
    		if (v == myLocationButton) {
    			showMyPositionAction(true);
        	} else if (v == nearbyLandmarksButton) {
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
    	 */
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
                	//ExtendedLandmark defaultLocation = ConfigurationManager.getInstance().getDefaultCoordinate();
                	//name = defaultLocation.getName();
                	lat = 52.25; //defaultLocation.getQualifiedCoordinates().getLatitude();
                	lng = 20.95; //defaultLocation.getQualifiedCoordinates().getLongitude();
                }
                
                if (!appInitialized) {
                	initOnLocationChanged(new LatLng(lat, lng), 4);
                } else {
                	//pickPositionAction(location, true, true);
                }
                //landmarkManager.addLandmark(lat, lng, 0.0f, StringUtil.formatCommaSeparatedString(name), "", Commons.LOCAL_LAYER, true);
            } else if (resultCode == RESULT_CANCELED && !appInitialized) {
                //ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
                //intents.showInfoToast(Locale.getMessage(R.string.Pick_location_default, landmark.getName()));
                initOnLocationChanged(new LatLng(52.25, 20.95), 5);
            } else if (resultCode == RESULT_CANCELED && intent != null && intent.hasExtra("message")) {
                //String message = intent.getStringExtra("message");
                //intents.showInfoToast(message);
            } else if (resultCode != RESULT_CANCELED) {
                //intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
            	Status status = PlaceAutocomplete.getStatus(this, intent);
                //intents.showInfoToast(status.getStatusMessage());
            	if (! appInitialized) {
            		initOnLocationChanged(new LatLng(52.25, 20.95), 6);
            	}
            }
        }
    }
    
	@Override
	public void onMapReady(GoogleMap map) {
		Log.d(this.getClass().getName(), "Google Map is ready!");
		//loadingHandler.sendEmptyMessage(SHOW_MAP_VIEW);
		this.mMap = map;
		mMap.getUiSettings().setZoomControlsEnabled(true);
	    mMap.setMyLocationEnabled(true);
	    mMap.setOnMyLocationButtonClickListener(this);
	    mMap.setOnCameraChangeListener(mOnCameraChangeListener);
	    
	    markerCluster = new GoogleMarkerClusterOverlay(this, mMap);
	    
	    loadMarkers();
	}  
	
	@Override
	public boolean onMyLocationButtonClick() {
		Toast.makeText(this, "My location button clicked", Toast.LENGTH_SHORT).show();
		return false;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		//user location has changed	
		Toast.makeText(this, "New location received: " + location.getLatitude() + "," + location.getLongitude() + " from " + location.getProvider(), Toast.LENGTH_SHORT).show();
		if (! appInitialized) {
			initOnLocationChanged(new LatLng(location.getLatitude(), location.getLongitude()), 3);
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
		/*if (ConfigurationManager.getUserManager().isUserAllowedAction() || itemId == android.R.id.home || itemId == R.id.exit || itemId == R.id.login || itemId == R.id.register) {	
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
					MathUtils.coordIntToDouble(mapView.getMapCenter().getLatitudeE6()),
		            MathUtils.coordIntToDouble(mapView.getMapCenter().getLongitudeE6()),
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
		    			intents.startCategoryListActivity(mapView.getLatitudeSpan(), mapView.getLongitudeSpan(),
		                mapView.getMapCenter().getLatitudeE6(), mapView.getMapCenter().getLongitudeE6(), -1, -1);
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
		        		intents.showNearbyLandmarks(getMyPosition(), ProjectionFactory.getProjection(mapView, googleMapsView));
		    		}
		    		break;
				case R.id.shareScreenshot:
					asyncTaskManager.executeUploadImageTask(MathUtils.coordIntToDouble(mapView.getMapCenter().getLatitudeE6()),
		            MathUtils.coordIntToDouble(mapView.getMapCenter().getLongitudeE6()), true);
					break;
				case R.id.reset:
	            	dialogManager.showAlertDialog(AlertDialogBuilder.RESET_DIALOG, null, null);
	            	break;	
				default:
					return true;
		 	}
		} else {
			intents.showInfoToast(Locale.getMessage(R.string.Login_required_error));
		}*/
		Toast.makeText(this, "onMenuItemsSelected() clicked", Toast.LENGTH_SHORT).show();
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
        	    	
        	/*mapController.setCenter(location);
        	
        	if (!landmarkManager.isInitialized()) {
        		landmarkManager.initialize();
            }
            
            addLandmarkOverlay();

            if (myLocation != null) {
            	addOverlay(myLocation);
            }

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
                    LoggerUtils.debug("Loading Layers in " + location.getLatitude() + "," +  location.getLongitude());
                    intents.loadLayersAction(true, null, false, true, layerLoader, location.getLatitude(), location.getLongitude(),
                            mapView.getZoomLevel(), ProjectionFactory.getProjection(mapView, googleMapsView));
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
            }*/

            loadingProgressBar.setProgress(100);
            
            //layerLoader.setRepaintHandler(loadingHandler);
            //if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
            //    loadingImage.setVisibility(View.GONE);
            //}
            
            //mapController.setZoom(ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM));
    	    if (mMap != null) {
    	    	CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 8);
    	    	mMap.moveCamera(cameraUpdate);
    	    	loadingHandler.sendEmptyMessage(SHOW_MAP_VIEW);
    	    	appInitialized = true;
    	    } else {
    	    	//TODO show toast something went wrong
    	    }
        } 
    }
	
	private void hideLandmarkView() {
    	//lvView.setVisibility(View.GONE);
		//getSupportedActionBar().show();
		//landmarkManager.clearLandmarkOnFocusQueue();
		//landmarkManager.setSelectedLandmark(null);
		//landmarkManager.setSeletedLandmarkUI();
    }
	
	private void loadMarkers() {    
	    //load overlays here
	    List<ExtendedLandmark> default_locations = new ArrayList<ExtendedLandmark>();
	    long installed = System.currentTimeMillis();
	    for (int i=0;i<5;i++){
	    	default_locations.add(LandmarkFactory.getLandmark("United States, Los Angeles", "", new QualifiedCoordinates(34.052234, -118.243685, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //United States Los Angeles 34.052234,-118.243685
	    	default_locations.add(LandmarkFactory.getLandmark("United States, New York", "", new QualifiedCoordinates(40.71427, -74.00597, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //United States Los Angeles 34.052234,-118.243685
	    	default_locations.add(LandmarkFactory.getLandmark("United States, San Francisco", "", new QualifiedCoordinates(37.77493, -122.41942, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //United States Los Angeles 34.052234,-118.243685
	    	default_locations.add(LandmarkFactory.getLandmark("France, Paris", "", new QualifiedCoordinates(48.856918, 2.34121, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //France, Paris 48.856918, 2.34121 
	    	default_locations.add(LandmarkFactory.getLandmark("Germany, Berlin", "", new QualifiedCoordinates(52.516071, 13.37698, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Germany, Berlin 52.516071, 13.37698 
	    	default_locations.add(LandmarkFactory.getLandmark("Italy, Rome", "", new QualifiedCoordinates(41.901514, 12.460774, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Italy, Rome 41.901514, 12.460774
	    	default_locations.add(LandmarkFactory.getLandmark("Spain, Madrid", "", new QualifiedCoordinates(40.4203, -3.70577, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Spain, Madrid 40.4203,-3.70577, 
	    	default_locations.add(LandmarkFactory.getLandmark("Japan, Tokyo", "", new QualifiedCoordinates(35.689488, 139.691706, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Japan, Tokyo, 35.689488,139.691706 
	    	default_locations.add(LandmarkFactory.getLandmark("United Kingdom, London", "", new QualifiedCoordinates(51.506321, -0.12714, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //United Kingdom, London, 51.506321,-0.12714  
	    	default_locations.add(LandmarkFactory.getLandmark("India, Mumbai", "", new QualifiedCoordinates(19.076191, 72.875877, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //India, Mumbai, 19.076191,72.875877 
	    	default_locations.add(LandmarkFactory.getLandmark("China, Beijing", "", new QualifiedCoordinates(39.90403, 116.407526, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //China, Beijing 39.90403, 116.407526
	    	default_locations.add(LandmarkFactory.getLandmark("Poland, Warsaw", "", new QualifiedCoordinates(52.235352, 21.00939, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Poland, Warsaw, 52.235352,21.00939
	    	default_locations.add(LandmarkFactory.getLandmark("Canada, Toronto", "", new QualifiedCoordinates(43.64856, -79.38533, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Canada, Toronto, 43.64856,-79.38533
	    	default_locations.add(LandmarkFactory.getLandmark("Brazil, Sao Paolo", "", new QualifiedCoordinates(-23.548943, -46.638818, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //Brazil, Sao Paolo -23.548943,-46.638818,     
	    	default_locations.add(LandmarkFactory.getLandmark("Indonesia, Jakarta", "", new QualifiedCoordinates(-6.17144, 106.82782, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //IDN Indonesia, Jakarta -6.17144, 106.82782
	    	default_locations.add(LandmarkFactory.getLandmark("Thailand, Bangkok", "", new QualifiedCoordinates(13.75333, 100.504822, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //THA Thailand, Bangkok 13.75333, 100.504822
	    	default_locations.add(LandmarkFactory.getLandmark("Russia, Moscow", "", new QualifiedCoordinates(55.755786, 37.617633, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //RUS Russia, Moscow 55.755786, 37.617633
	    	default_locations.add(LandmarkFactory.getLandmark("Mexico, Mexico City", "", new QualifiedCoordinates(19.432608, -99.133208, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //MEX Mexico, Mexico City 19.432608, -99.133208
	    	default_locations.add(LandmarkFactory.getLandmark("Malaysia, Kuala Lumpur", "", new QualifiedCoordinates(3.15248, 101.71727, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //MYS Malaysia, Kuala Lumpur 3.15248, 101.71727
	    	default_locations.add(LandmarkFactory.getLandmark("Turkey, Istanbul", "", new QualifiedCoordinates(41.00527, 28.97696, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //TUR Turkey, Istanbul 41.00527, 28.97696
	    	default_locations.add(LandmarkFactory.getLandmark("Philippines, Manilia", "", new QualifiedCoordinates(14.5995124, 120.9842195, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //PHL Philippines, Manilia 14.5995124, 120.9842195
	    	default_locations.add(LandmarkFactory.getLandmark("Netherlands, Amsterdam", "", new QualifiedCoordinates(52.373119, 4.89319, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //NLD Netherlands, Amsterdam 52.373119, 4.89319
	    	default_locations.add(LandmarkFactory.getLandmark("Saudi Arabia, Riyadh", "", new QualifiedCoordinates(24.64732, 46.714581, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //SAU Saudi Arabia, Riyadh 24.64732, 46.714581
    		default_locations.add(LandmarkFactory.getLandmark("Portugal, Lisbon", "", new QualifiedCoordinates(38.7252993, -9.1500364, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //PRT Portugal, Lisbon 38.7252993, 9.1500364
    		default_locations.add(LandmarkFactory.getLandmark("Pakistan, Islamabad", "", new QualifiedCoordinates(33.718151, 73.060547, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //PAK Pakistan, Islamabad 33.718151, 73.060547
    		default_locations.add(LandmarkFactory.getLandmark("Sweden, Stockholm", "", new QualifiedCoordinates(59.32893, 18.06491, 0f, Float.NaN, Float.NaN), Commons.LOCAL_LAYER, installed)); //SWE Sweden, Stockholm 59.32893, 18.06491  	       	
	    }
    	markerCluster.addMarkers(default_locations);
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
            			Toast.makeText(activity, "Please select location you want to start with!", Toast.LENGTH_LONG).show();
            			try {
            				AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
            				.setTypeFilter(AutocompleteFilter.TYPE_FILTER_GEOCODE) //.TYPE_FILTER_NONE) //everything
            				.build();

            				Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN) //MODE_OVERLAY
            				.setFilter(typeFilter)
            				.build(activity);
            				activity.startActivityForResult(intent, PICK_LOCATION);
            			} catch (Exception e) {
            				//LoggerUtils.error("Intents.startPickLocationActivity() exception:", e);
            				//intent = new Intent(activity, PickLocationActivity.class);
            				activity.initOnLocationChanged(new LatLng(52.25, 20.95), 1);
            			}
            			//intents.startPickLocationActivity()
            		}
            	} /*else if (msg.what == MessageStack.STATUS_MESSAGE) {
        			activity.statusBar.setText(activity.messageStack.getMessage());
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
            		if (activity.mapProvider == ConfigurationManager.OSM_MAPS || activity.googleMapsView.canCoverCenter()) {
            			activity.asyncTaskManager.executeUploadImageTask(MathUtils.coordIntToDouble(activity.mapView.getMapCenter().getLatitudeE6()),
                            MathUtils.coordIntToDouble(activity.mapView.getMapCenter().getLongitudeE6()), false);
            		}	
            	} else if (msg.what == LayerLoader.FB_TOKEN_EXPIRED) {
            		activity.intents.showInfoToast(Locale.getMessage(R.string.Social_token_expired, "Facebook"));
            	} else if (msg.what == GoogleLandmarkOverlay.SHOW_LANDMARK_DETAILS || msg.what == OsmLandmarkOverlay.SHOW_LANDMARK_DETAILS || msg.what == OsmMarkerClusterOverlay.SHOW_LANDMARK_DETAILS) {
            		int[] coordsE6 = activity.intents.showLandmarkDetailsAction(activity.getMyPosition(), activity.lvView, activity.layerLoader, activity.mapView.getZoomLevel(), null, ProjectionFactory.getProjection(activity.mapView, activity.googleMapsView));
                    if (coordsE6 != null) {
                    	activity.animateTo(coordsE6);
                    }
            	} else if (msg.what == OsmMarkerClusterOverlay.SHOW_LANDMARK_LIST) {
                	activity.intents.startMultiLandmarkIntent(activity.getMyPosition());
            		activity.animateTo(new int[]{msg.arg1, msg.arg2});
            	} else if (msg.what == AsyncTaskManager.SHOW_ROUTE_MESSAGE) {
            		activity.showRouteAction((String) msg.obj);
            	} else if (LocationServicesManager.UPDATE_LOCATION == msg.what) {
                	Location location = (Location) msg.obj;
                	activity.updateLocation(location);
            	} else if (msg.obj != null) {
            		LoggerUtils.error("Unknown message received: " + msg.obj.toString());
            	}*/
        	}		
		}
	}
}
