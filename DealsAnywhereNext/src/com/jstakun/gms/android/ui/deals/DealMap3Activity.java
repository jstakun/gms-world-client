package com.jstakun.gms.android.ui.deals;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.List;

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
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.google.maps.GoogleLandmarkProjectionV2;
import com.jstakun.gms.android.google.maps.GoogleMapsV2TypeSelector;
import com.jstakun.gms.android.google.maps.GoogleMarkerClusterOverlay;
import com.jstakun.gms.android.google.maps.GoogleRoutesOverlay;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerLoader;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.ui.AlertDialogBuilder;
import com.jstakun.gms.android.ui.AsyncTaskManager;
import com.jstakun.gms.android.ui.DialogManager;
import com.jstakun.gms.android.ui.HelpActivity;
import com.jstakun.gms.android.ui.IntentArrayAdapter;
import com.jstakun.gms.android.ui.IntentsHelper;
import com.jstakun.gms.android.ui.LandmarkListActivity;
import com.jstakun.gms.android.ui.MapInfoView;
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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableString;
import android.view.KeyEvent;
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

public class DealMap3Activity extends ActionBarActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks,
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
    private CategoriesManager cm;
    private IntentsHelper intents;
    private DialogManager dialogManager;
    private DealOfTheDayDialog dealOfTheDayDialog;
    
    private TextView statusBar;
    private View lvCloseButton, lvCallButton, lvOpenButton,
                 lvView, lvShareButton, lvRouteButton,
                 thumbnailButton, loadingImage;
    private ProgressBar loadingProgressBar;
    private NavigationDrawerFragment mNavigationDrawerFragment;
    
    private GoogleLandmarkProjectionV2 projection;
	private GoogleMarkerClusterOverlay markerCluster;
	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;
	private GoogleMap mMap;
	private GoogleRoutesOverlay routesCluster;
	
    
    private boolean appInitialized = false;
    
    private Handler loadingHandler;
    
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//requestFeature() must be called before adding content
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_deal_map);
        
		mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
		
		loadingHandler = new LoadingHandler(this);
        
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        
        mapFragment.getMapAsync(this); 
        
        LoggerUtils.debug("DealMap3Activity.onCreate called...");
        
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
        
        loadingProgressBar = (ProgressBar) findViewById(R.id.mapCanvasLoadingProgressBar);
    	loadingProgressBar.setProgress(25);
    	
    	statusBar = (TextView) findViewById(R.id.statusBar);
        loadingImage = findViewById(R.id.loadingAnim);
        lvView = findViewById(R.id.lvView);
         
        lvCloseButton = findViewById(R.id.lvCloseButton);
        lvOpenButton = findViewById(R.id.lvOpenButton);
        lvShareButton = findViewById(R.id.lvShareButton);
        lvCallButton = findViewById(R.id.lvCallButton);
        lvRouteButton = findViewById(R.id.lvRouteButton);
        thumbnailButton = findViewById(R.id.thumbnailButton);
        
        lvCloseButton.setOnClickListener(this);
        lvOpenButton.setOnClickListener(this);
        lvShareButton.setOnClickListener(this);
        lvCallButton.setOnClickListener(this);
        lvRouteButton.setOnClickListener(this);
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
            LoggerUtils.debug("Initializing AsyncTaskManager...");
            asyncTaskManager = new AsyncTaskManager(this, landmarkManager);
            ConfigurationManager.getInstance().putObject("asyncTaskManager", asyncTaskManager);
            //check if newer version available
            asyncTaskManager.executeNewVersionCheckTask();           
        }
        
        intents = new IntentsHelper(this, landmarkManager, asyncTaskManager);

        cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        if (cm == null || !cm.isInitialized()) {
            LoggerUtils.debug("Loading deal categories...");
            cm = new CategoriesManager();
            ConfigurationManager.getInstance().putObject(ConfigurationManager.DEAL_CATEGORIES, cm);
            asyncTaskManager.executeDealCategoryLoaderTask(cm, true);
        }

        dialogManager = new DialogManager(this, intents, asyncTaskManager, landmarkManager, null, null);
        
        ((LoadingHandler) loadingHandler).setDialogManager(dialogManager);
        
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
		fragmentManager.beginTransaction().replace(R.id.mapContainer, PlaceholderFragment.newInstance(position + 1))
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
    public void onRestart() {
        super.onRestart();
        LoggerUtils.debug("onRestart");
        if (mMap != null) {
        	GoogleMapsV2TypeSelector.selectMapType(mMap);
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
        int type = dialogManager.dismissDialog();
        if (type == AlertDialogBuilder.DEAL_OF_THE_DAY_DIALOG) {
            dealOfTheDayDialog.dismiss();
        } else if (dealOfTheDayDialog != null && dealOfTheDayDialog.isShowing()) {
            ConfigurationManager.getInstance().putObject(AlertDialogBuilder.OPEN_DIALOG, AlertDialogBuilder.DEAL_OF_THE_DAY_DIALOG);
            dealOfTheDayDialog.dismiss();
        }
    }
	
	@Override
    public void onDestroy() {
    	LoggerUtils.debug("onDestroy");
        if (ConfigurationManager.getInstance().isClosing()) {
        	appInitialized = false;
        	intents.hardClose(layerLoader, loadingHandler, null, (int)mMap.getCameraPosition().zoom, MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude));
        } else if (mMap != null) {
            intents.softClose((int)mMap.getCameraPosition().zoom, MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude));
        }
        AdsUtils.destroyAdView(this);
        System.gc();
    	super.onDestroy();
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
        	//if (menu.findItem(R.id.shareScreenshot) != null) {
        	//	menu.findItem(R.id.shareScreenshot).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	//}
        	
        	if (menu.findItem(R.id.reset) != null) {
        		menu.findItem(R.id.reset).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	}
        	if (menu.findItem(R.id.releaseNotes) != null) {
        		menu.findItem(R.id.releaseNotes).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	}
        	if (menu.findItem(R.id.config) != null) {
        		menu.findItem(R.id.config).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
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
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	UserTracker.getInstance().trackEvent("onKeyDown", "", "", 0);
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
    	
	}
	
	protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

	private synchronized void initOnLocationChanged(LatLng location, int source) {
    	//System.out.println("4 --------------------------------");
    	if (!appInitialized && location != null) {
    		//System.out.println("4.1 --------------------------------");
        	loadingProgressBar.setProgress(75);
        	    	
        	if (!landmarkManager.isInitialized()) {
        		landmarkManager.initialize(Commons.LOCAL_LAYER, Commons.ROUTES_LAYER, Commons.MY_POSITION_LAYER, Commons.COUPONS_LAYER,
                		Commons.HOTELS_LAYER, Commons.GROUPON_LAYER, Commons.FOURSQUARE_MERCHANT_LAYER, Commons.YELP_LAYER);
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
                ConfigurationManager.getInstance().putObject("layerLoader", layerLoader);
                LoggerUtils.debug("Loading Layers in " + location.latitude + "," +  location.longitude);
                int zoom = ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM);
                if (mMap != null) {
                    zoom = (int)mMap.getCameraPosition().zoom;
                }
                intents.loadLayersAction(true, null, false, false, layerLoader, location.latitude, location.longitude, zoom, projection);               
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
            
            appInitialized = true;
            
            if (mMap != null) {
    	    	CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM));
    	    	mMap.moveCamera(cameraUpdate);
    	    	loadingHandler.sendEmptyMessage(SHOW_MAP_VIEW);
    	    } else {
    	    	ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, location);
    	    }
        } 
    }
	
	protected boolean onMenuItemSelected(int itemId) {
		switch (itemId) {
            case R.id.hotDeals:
                intents.startDealsOfTheDayIntent(getMyPosition(), null);
                break;
            case R.id.newestDeals:
                final String[] excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER, Commons.LOCAL_LAYER};
                intents.startNewestLandmarkIntent(getMyPosition(), excluded, 2);
                break;
            case R.id.nearbyDeals:
                if (!lvView.isShown()) {
                    intents.showNearbyLandmarks(getMyPosition(), projection);
                }
                break;
            case R.id.settings:
                intents.startSettingsActivity(SettingsActivity.class);
                break;
            case R.id.listMode:
                intents.startCategoryListActivity(MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude), -1, -1);
                break;
            case R.id.pickMyPos:
                intents.startPickLocationActivity();
                break;
            case R.id.search:
                onSearchRequested();
                break;
            case R.id.refreshLayers:
                intents.loadLayersAction(true, null, false, false, layerLoader,
                		mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude,
    		            (int)mMap.getCameraPosition().zoom, projection);
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
            case R.id.config:
				intents.startConfigurationViewerActivity();
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
                intents.startActionViewIntent(ConfigurationManager.getInstance().getString("lmUrl"));
                break;
            case R.id.events:
                intents.startCalendarActivity(getMyPosition());
                break;
            case R.id.rateUs:
                dialogManager.showAlertDialog(AlertDialogBuilder.RATE_US_DIALOG, null, null);
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
        return true;
	}
	
	private double[] getMyPosition() {
		if (mMap != null) {
			return landmarkManager.getMyLocation(mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude);
		} else {
			LatLng mapCenter = (LatLng) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, LatLng.class);
            if (mapCenter != null) {
            	return landmarkManager.getMyLocation(mapCenter.latitude, mapCenter.longitude);
            } else {
            	ExtendedLandmark l = ConfigurationManager.getInstance().getDefaultCoordinate();
            	return landmarkManager.getMyLocation(l.getQualifiedCoordinates().getLatitude(), l.getQualifiedCoordinates().getLongitude());
            }
		}  
    }
	
	private void animateTo(LatLng newLocation) {
		if (mMap != null) {
			CameraUpdate location = CameraUpdateFactory.newLatLng(newLocation);
			mMap.animateCamera(location);
		}
		
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
        	clearLandmarks = intents.isClearLandmarksRequired(projection, MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), 
            		 MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude), MathUtils.coordDoubleToInt(myLoc.getLatitude()), MathUtils.coordDoubleToInt(myLoc.getLongitude()));
        }
        	
        if (loadLayers && !isVisible) {
        	if (clearLandmarks) {
        		markerCluster.clearMarkers();
        	}
            intents.loadLayersAction(true, null, clearLandmarks, false, layerLoader, myLoc.getLatitude(), myLoc.getLongitude(), (int)mMap.getCameraPosition().zoom, projection);
        }
        
        animateTo(myLocLatLng);
    }
	
	private void hideLandmarkView() {
    	lvView.setVisibility(View.GONE);
		getSupportActionBar().show();
		landmarkManager.clearLandmarkOnFocusQueue();
		landmarkManager.setSelectedLandmark(null);
		landmarkManager.setSeletedLandmarkUI();
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
            if (appInitialized) {
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
                LatLng location = new LatLng(recommended.getLatitudeE6(), recommended.getLongitudeE6());
                pickPositionAction(location, false, false);
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
    
    private void pickPositionAction(LatLng newCenter, boolean loadLayers, boolean clearMap) {
    	if (clearMap && markerCluster != null) {
    		markerCluster.clearMarkers();
    	}
    	if (mMap != null) {
	    	CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(newCenter);
	    	mMap.moveCamera(cameraUpdate);
	    } 
        if (loadLayers) {     	
            intents.loadLayersAction(true, null, clearMap, false, layerLoader,
                    mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude,
                    (int)mMap.getCameraPosition().zoom, projection);
        }
    }
    
    private void callButtonPressedAction(ExtendedLandmark landmark) {
        intents.startPhoneCallActivity(landmark);
    }

    private void sendMessageAction() {
        intents.shareLandmarkAction(dialogManager);
    }
    
    private void showMapAndMarkers() {
    	if (!findViewById(R.id.mapContainer).isShown()) {
			findViewById(R.id.mapContainer).setVisibility(View.VISIBLE);
			findViewById(R.id.mapCanvasWidgetL).setVisibility(View.GONE);
		}
    	if ((lvView == null || !lvView.isShown()) && getSupportActionBar() != null) {
    		getSupportActionBar().show();
    	}
    	
	    markerCluster = new GoogleMarkerClusterOverlay(this, mMap, loadingHandler, landmarkManager, this.getResources().getDisplayMetrics());	
	    markerCluster.loadAllMarkers();
	    
	    routesCluster = new GoogleRoutesOverlay(mMap, landmarkManager, routesManager, markerCluster, this.getResources().getDisplayMetrics().density);
	    routesCluster.loadAllRoutes();
	}
    
    private void takeScreenshot(final boolean notify)
    {
    	if ((notify || !ConfigurationManager.getInstance().containsObject("screenshot_" + StringUtil.formatCoordE2(mMap.getCameraPosition().target.latitude) + "_" + StringUtil.formatCoordE2(mMap.getCameraPosition().target.longitude), Object.class)) && !isFinishing()) {   		
    		//if (notify) {
    		//	intents.showShortToast(Locale.getMessage(R.string.Task_started, Locale.getMessage(R.string.shareScreenshot)));
    		//}   		
    		final SoundPool soundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
            final int shutterSound = soundPool.load(this, R.raw.camera_click, 0);
    		
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
                	screenshot.compress(Bitmap.CompressFormat.JPEG, 80, out);
                
                	Uri uri = PersistenceManagerFactory.getFileManager().saveImageFile(screenshot, "screenshot.jpg");
                	
                	int id = soundPool.play(shutterSound, 1f, 1f, 0, 0, 1);
                    LoggerUtils.debug("Shutter sound played with id " + id);
                	
					asyncTaskManager.executeImageUploadTask(out.toByteArray(), filename, mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude);
				    
					if (notify) {
						intents.shareImageAction(uri);
					}
				}        	
        	};
        
        	mMap.snapshot(callback);
    	} else if (notify) {
    		intents.showInfoToast(Locale.getMessage(R.string.Share_screenshot_exists));
    	} else {
    		LoggerUtils.debug("Screenshot for current location has already been sent!");
    	}
    }
    
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == IntentsHelper.INTENT_MULTILANDMARK) {
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
        } else if (requestCode == IntentsHelper.INTENT_PICKLOCATION) {
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
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
            	Status status = PlaceAutocomplete.getStatus(this, intent);
                intents.showInfoToast(status.getStatusMessage());
            	if (! appInitialized) {
            		ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
                    initOnLocationChanged(new LatLng(landmark.getQualifiedCoordinates().getLatitude(), landmark.getQualifiedCoordinates().getLongitude()), 6);
            	}
            } else if (resultCode != RESULT_CANCELED) {
                intents.showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
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
        	if (mMap != null) {
        		intents.processActivityResult(requestCode, resultCode, intent, getMyPosition(), 
        				new double[]{mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude}, 
        				loadingHandler, (int)mMap.getCameraPosition().zoom, layerLoader, projection);
        	} else {
        		intents.processActivityResult(requestCode, resultCode, intent, getMyPosition(), 
        				new double[]{ConfigurationManager.getInstance().getDouble(ConfigurationManager.LATITUDE), ConfigurationManager.getInstance().getDouble(ConfigurationManager.LONGITUDE)}, 
        				loadingHandler, ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM), layerLoader, projection);
                
        	}
        }
    }

	@Override
	public void onLocationChanged(Location location) {
		if (!appInitialized && !isFinishing()) {
			initOnLocationChanged(new LatLng(location.getLatitude(), location.getLongitude()), 3);
		}
		
		if (appInitialized && !isFinishing()) {
			ConfigurationManager.getInstance().setLocation(location);		
			intents.addMyLocationLandmark(location);     
			intents.vibrateOnLocationUpdate();
			UserTracker.getInstance().sendMyLocation();	   
		}		
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {		
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
	public void onConnectionSuspended(int reasonCode) {
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
	public void onClick(View v) {
		ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
    	if (selectedLandmark != null) {
    		if (v == lvCloseButton) {
    			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CloseSelectedDealView", "", 0);
    			hideLandmarkView();
    		} else if (v == lvOpenButton) {
    			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenSelectedDealURL", selectedLandmark.getLayer(), 0);
    			intents.openButtonPressedAction(landmarkManager.getSeletedLandmarkUI());
    		} else if (v == thumbnailButton) {
    			if (intents.startStreetViewActivity(selectedLandmark)) {
    				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenStreetView", selectedLandmark.getLayer(), 0);
    			} else {
    				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenURLSelectedLandmark", selectedLandmark.getLayer(), 0);
        			intents.openButtonPressedAction(selectedLandmark);
    			}
    		} else if (v == lvCallButton) {
    			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CallSelectedDeal", selectedLandmark.getLayer(), 0);
    			callButtonPressedAction(landmarkManager.getSeletedLandmarkUI());
    		} else if (v == lvRouteButton) {
    			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowRouteSelectedDeal", selectedLandmark.getLayer(), 0);
    			dialogManager.showAlertDialog(AlertDialogBuilder.ROUTE_DIALOG, null, null);
    		} else if (v == lvShareButton) {
    			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShareSelectedDeal", selectedLandmark.getLayer(), 0);
    			sendMessageAction();
    		}
    	} else {
    		intents.showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
    	}	
	}

	@Override
	public void onMapReady(GoogleMap map) {
		LoggerUtils.debug("Google Map is ready!");
		this.mMap = map;
		this.projection = new GoogleLandmarkProjectionV2(mMap);
		
		GoogleMapsV2TypeSelector.selectMapType(mMap);
		
	    mMap.getUiSettings().setZoomControlsEnabled(true);
	    mMap.setMyLocationEnabled(true);
	    mMap.setOnMyLocationButtonClickListener(this);
	    mMap.setOnCameraChangeListener(mOnCameraChangeListener);
	    
	    if (appInitialized) {
	    	LatLng mapCenter = (LatLng) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, LatLng.class);
	        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mapCenter, ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM));
	    	mMap.moveCamera(cameraUpdate);
	    	loadingHandler.sendEmptyMessage(SHOW_MAP_VIEW);
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
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_deal_map, container, false);
			return rootView;
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
		}
	}
	
	private static class LoadingHandler extends Handler {
		
		 private WeakReference<DealMap3Activity> parentActivity;
	     private WeakReference<DialogManager> dialogManager;
	    	
	     public LoadingHandler(DealMap3Activity parentActivity) {
	    	this.parentActivity = new WeakReference<DealMap3Activity>(parentActivity);
	     }
	    	
	     public void setDialogManager(DialogManager dialogManager) {
	     	this.dialogManager = new WeakReference<DialogManager>(dialogManager); 
	     }
	     
		 @Override
         public void handleMessage(Message msg) {
			DealMap3Activity activity = parentActivity.get();
        	if (activity != null && !activity.isFinishing()) {
        		if (msg.what == MessageStack.STATUS_MESSAGE) {
        			activity.statusBar.setText(activity.messageStack.getMessage());
        		} else if (msg.what == MessageStack.STATUS_VISIBLE) {
        			activity.loadingImage.setVisibility(View.VISIBLE);
        		} else if (msg.what == MessageStack.STATUS_GONE) {
        			activity.loadingImage.setVisibility(View.GONE);
        		} else if (msg.what == DealOfTheDayDialog.OPEN) {
        			ExtendedLandmark recommended = (ExtendedLandmark) ConfigurationManager.getInstance().getObject("dod", ExtendedLandmark.class);
        			activity.intents.openButtonPressedAction(recommended);
        		} else if (msg.what == DealOfTheDayDialog.CALL) {
        			ExtendedLandmark recommended = (ExtendedLandmark) ConfigurationManager.getInstance().getObject("dod", ExtendedLandmark.class);
        			activity.callButtonPressedAction(recommended);
        		} else if (msg.what == DealOfTheDayDialog.ROUTE) {
        			dialogManager.get().showAlertDialog(AlertDialogBuilder.ROUTE_DIALOG, null, new SpannableString("dod"));
        		} else if (msg.what == DealOfTheDayDialog.SEND_MAIL) {
        			activity.sendMessageAction();
        		} else if (msg.what == LayerLoader.LAYER_LOADED) {
        			String layerKey = (String)msg.obj;
        			if (activity.markerCluster != null) {	
        				int count = activity.markerCluster.addMarkers(layerKey);
            			LoggerUtils.debug(count + " markers from layer " + layerKey + " stored in cluster.");
            		} else {
            			LoggerUtils.debug("Layer " + layerKey + " but marker cluster not yet initialized!");
            		}
        		} else if (msg.what == LayerLoader.ALL_LAYERS_LOADED) {
        			activity.showRecommendedDeal(false);
        			if (activity.mMap != null) {
            			activity.takeScreenshot(false);
            		}	
        		} else if (msg.what == GoogleMarkerClusterOverlay.SHOW_LANDMARK_DETAILS) {
            		int[] coordsE6 = activity.intents.showLandmarkDetailsAction(activity.getMyPosition(), activity.lvView, activity.layerLoader, (int)activity.mMap.getCameraPosition().zoom, null, activity.projection);
                    if (coordsE6 != null) {
                    	activity.getSupportActionBar().hide();
                    	activity.animateTo(new LatLng(MathUtils.coordIntToDouble(coordsE6[0]),MathUtils.coordIntToDouble(coordsE6[1])));
                    }
            	} else if (msg.what == GoogleMarkerClusterOverlay.SHOW_LANDMARK_LIST) {
            		activity.intents.startMultiLandmarkIntent(activity.getMyPosition());
            		activity.animateTo(new LatLng(MathUtils.coordIntToDouble(msg.arg1), MathUtils.coordIntToDouble(msg.arg2)));
            	} else if (msg.what == SHOW_MAP_VIEW) {
        			activity.showMapAndMarkers();
        		} else if (msg.what == AsyncTaskManager.SHOW_ROUTE_MESSAGE) {
        			activity.routesCluster.showRouteAction((String) msg.obj, true);
        		} else if (msg.obj != null) {
            		LoggerUtils.error("Unknown message received: " + msg.obj.toString());
            	}
        	}
		}
	}
}
