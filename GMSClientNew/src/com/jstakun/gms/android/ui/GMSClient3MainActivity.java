package com.jstakun.gms.android.ui;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.List;

import org.osmdroid.util.GeoPoint;

import com.google.android.gms.common.api.Status;
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
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.FilenameFilterFactory;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.google.maps.GoogleLandmarkProjectionV2;
import com.jstakun.gms.android.google.maps.GoogleMapsV2TypeSelector;
import com.jstakun.gms.android.google.maps.GoogleMarkerClusterOverlay;
import com.jstakun.gms.android.google.maps.GoogleRoutesOverlay;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerLoader;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.location.GmsLocationServicesManager;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.service.RouteTracingService;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GMSClient3MainActivity extends ActionBarActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, 
                                                                         OnMapReadyCallback, OnClickListener, 
                                                                         GoogleMap.OnMyLocationButtonClickListener {

	private static final int SHOW_MAP_VIEW = 0;
	private static final int PICK_LOCATION = 1;
	
	private GoogleLandmarkProjectionV2 projection;
	private GoogleMarkerClusterOverlay markerCluster;
	private GoogleMap mMap;
	private GoogleRoutesOverlay routesCluster;
	
	private Handler loadingHandler;
	
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
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestFeature() must be called before adding content
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.gmsclient3_main);
         
        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
    
        // Set up the drawer.
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
        
        LoggerUtils.debug("GMSClient3MainActivity.onCreate called...");
        
        loadingHandler = new LoadingHandler(this);
        
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        
        mapFragment.getMapAsync(this); 
        
        UserTracker.getInstance().trackActivity(getClass().getName());
        
        OsUtil.setDisplayType(getResources().getConfiguration());
        
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
        	//actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            //actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getTitle());
            actionBar.hide();
        }
        
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
        
        if (!CategoriesManager.getInstance().isInitialized()) {
        	LoggerUtils.debug("Loading deal categories...");
            AsyncTaskManager.getInstance().executeDealCategoryLoaderTask(true);
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
        GmsLocationServicesManager.getInstance().enable(loadingHandler);
        
        AsyncTaskManager.getInstance().setContext(this);
        IntentsHelper.getInstance().setActivity(this);

        AsyncTaskManager.getInstance().executeNewVersionCheckTask(this);
        
        //verify access token
        AsyncTaskManager.getInstance().executeGetTokenTask();
        
        Integer searchQueryResult = (Integer) ConfigurationManager.getInstance().removeObject(ConfigurationManager.SEARCH_QUERY_RESULT, Integer.class);
        if (searchQueryResult != null) {
        	int[] coordsE6 = IntentsHelper.getInstance().showSelectedLandmark(searchQueryResult, getMyPosition(), lvView, (int)mMap.getCameraPosition().zoom, projection);
            if (coordsE6 != null) {
            	animateTo(new LatLng(MathUtils.coordIntToDouble(coordsE6[0]),MathUtils.coordIntToDouble(coordsE6[1])));
            }
        } else if (LandmarkManager.getInstance().getSeletedLandmarkUI() != null) {
            getSupportActionBar().hide();
            ExtendedLandmark landmark = LandmarkManager.getInstance().getSeletedLandmarkUI();
            IntentsHelper.getInstance().showLandmarkDetailsView(landmark, lvView, getMyPosition(), true);
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
            if (ConfigurationManager.getInstance().containsObject(ConfigurationManager.MAP_CENTER, LatLng.class)) {
                loadingHandler.removeMessages(PICK_LOCATION);
                loadingHandler.sendEmptyMessage(PICK_LOCATION);
            }
        }
        
        IntentsHelper.getInstance().startAutoCheckinBroadcast();
        
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
            DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.NETWORK_ERROR_DIALOG, null, null);
            ConfigurationManager.getInstance().putObject("NetworkStatus", new Object());
        }

		//show rate us dialog
        Object rateDialogStatus = ConfigurationManager.getInstance().getObject("rateDialogStatus", Object.class);
        if (rateDialogStatus == null && ConfigurationManager.getInstance().isOff(ConfigurationManager.APP_RATED) && networkActive) {
            int useCount = ConfigurationManager.getInstance().getInt(ConfigurationManager.USE_COUNT);
            if (useCount > 0 && (useCount % 10) == 0) {
                DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.RATE_US_DIALOG, null, null);
                ConfigurationManager.getInstance().putInteger(ConfigurationManager.USE_COUNT, useCount + 1);
                ConfigurationManager.getInstance().putObject("rateDialogStatus", new Object());
            }
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        GmsLocationServicesManager.getInstance().disable();
        DialogManager.getInstance().dismissDialog(this);
    }
    
    @Override
    protected void onStop() {
    	LoggerUtils.debug("onStop");
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
        	GmsLocationServicesManager.getInstance().disable();
        	IntentsHelper.getInstance().hardClose(loadingHandler, null, (int)mMap.getCameraPosition().zoom, MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude));
        } else if (mMap != null) {
            IntentsHelper.getInstance().softClose((int)mMap.getCameraPosition().zoom, MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude));
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
        	GoogleMapsV2TypeSelector.selectMapType(mMap);
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
            IntentsHelper.getInstance().startSearchActivity(MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), 
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

        	//if (menu.findItem(R.id.shareScreenshot) != null) {
        	//	menu.findItem(R.id.shareScreenshot).setVisible(ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));
        	//}
        	
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
    		final ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getSeletedLandmarkUI();
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
    	} else {
    		IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
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
                	LatLng mapCenter = new LatLng(lat, lng);
                	initOnLocationChanged(mapCenter, 4);
                } else {
                	pickPositionAction(new LatLng(lat, lng), true, true);
                }
                LandmarkManager.getInstance().addLandmark(lat, lng, 0.0f, StringUtil.formatCommaSeparatedString(name), "", Commons.LOCAL_LAYER, true);
            } else if (resultCode == RESULT_CANCELED && !appInitialized) {
                ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
                IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Pick_location_default, landmark.getName()));
                LatLng mapCenter = new LatLng(landmark.getQualifiedCoordinates().getLatitude(), landmark.getQualifiedCoordinates().getLongitude());
                initOnLocationChanged(mapCenter, 5);
            } else if (resultCode == RESULT_CANCELED && intent != null && intent.hasExtra("message")) {
                String message = intent.getStringExtra("message");
                IntentsHelper.getInstance().showInfoToast(message);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
            	Status status = PlaceAutocomplete.getStatus(this, intent);
                IntentsHelper.getInstance().showInfoToast(status.getStatusMessage());
            	if (! appInitialized) {
            		ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
            		LatLng mapCenter = new LatLng(landmark.getQualifiedCoordinates().getLatitude(), landmark.getQualifiedCoordinates().getLongitude());
            		initOnLocationChanged(mapCenter, 6);
            	}
            } else if (resultCode != RESULT_CANCELED) {
                IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
            } 
        } else if (requestCode == IntentsHelper.INTENT_MULTILANDMARK) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                if (action.equals("load")) {
                    String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);
                    int id = Integer.parseInt(ids);
                    int[] coordsE6 = IntentsHelper.getInstance().showSelectedLandmark(id, getMyPosition(), lvView, (int)mMap.getCameraPosition().zoom, projection);
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
                    ExtendedLandmark l = LandmarkManager.getInstance().getPhoneLandmark(id);
                    if (l != null) {
                        pickPositionAction(new LatLng(l.getQualifiedCoordinates().getLatitude(), l.getQualifiedCoordinates().getLongitude()), true, true);
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
                    pickPositionAction(new LatLng(fav.getLatitude(), fav.getLongitude()), true, false);
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
                    int[] coordsE6 = IntentsHelper.getInstance().showSelectedLandmark(id, getMyPosition(), lvView, (int)mMap.getCameraPosition().zoom, projection);
                    if (coordsE6 != null) {
                    	animateTo(new LatLng(MathUtils.coordIntToDouble(coordsE6[0]),MathUtils.coordIntToDouble(coordsE6[1])));
                    }
                }
            }
        } else {
        	if (mMap != null) {
        		IntentsHelper.getInstance().processActivityResult(requestCode, resultCode, intent, getMyPosition(), 
        				new double[]{mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude}, 
        				loadingHandler, (int)mMap.getCameraPosition().zoom, projection);
        	} else {
        		IntentsHelper.getInstance().processActivityResult(requestCode, resultCode, intent, getMyPosition(), 
        				new double[]{ConfigurationManager.getInstance().getDouble(ConfigurationManager.LATITUDE), ConfigurationManager.getInstance().getDouble(ConfigurationManager.LONGITUDE)}, 
        				loadingHandler, ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM), projection);
                
        	}
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
	    
	    if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
	    	mMap.getUiSettings().setCompassEnabled(true);
	    }
	    
	    if (appInitialized) {
	    	LatLng mapCenter = (LatLng) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, LatLng.class);
	        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mapCenter, ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM));
	    	mMap.moveCamera(cameraUpdate);
	    	loadingHandler.sendEmptyMessage(SHOW_MAP_VIEW);
	    }
	}  

	@Override
	public boolean onMyLocationButtonClick() {
		if (ConfigurationManager.getInstance().getLocation() != null) {
			showMyPositionAction(true);
		} else {
			IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.GPS_location_missing_error));
		}
		return true;
	}
	
	
	private void onLocationChanged() {
		Location location = ConfigurationManager.getInstance().getLocation();
		//user location has changed	
		if (!appInitialized && !isFinishing()) {
			initOnLocationChanged(new LatLng(location.getLatitude(), location.getLongitude()), 3);
		}
		
		if (appInitialized && !isFinishing()) {
		
			ConfigurationManager.getInstance().setLocation(location);
		
			IntentsHelper.getInstance().addMyLocationLandmark(location);     
			IntentsHelper.getInstance().vibrateOnLocationUpdate();
			UserTracker.getInstance().sendMyLocation();
	    	
			if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
				if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
					RouteRecorder.getInstance().addCoordinate(location.getLatitude(), location.getLongitude(), (float)location.getAltitude(), location.getAccuracy(), location.getSpeed(), location.getBearing());
					if (routesCluster != null) {
					   routesCluster.showRecordedRoute();
					}
				}
				showMyPositionAction(false);
			} 
	        
			if (ConfigurationManager.getInstance().isOn(ConfigurationManager.AUTO_CHECKIN)) {
				CheckinManager.getInstance().autoCheckin(location.getLatitude(), location.getLongitude(), false);
			}
		}
	}

	private void onConnected() {
		Location location = ConfigurationManager.getInstance().getLocation();
		if (location != null && !appInitialized) {
			LatLng mapCenter = new LatLng(location.getLatitude(), location.getLongitude());
			initOnLocationChanged(mapCenter, 0);
		}
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
    			} 
            	return true;
        	} else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
        		int[] coordsE6 = IntentsHelper.getInstance().showLandmarkDetailsAction(getMyPosition(), lvView, (int)mMap.getCameraPosition().zoom, projection);
                if (coordsE6 != null) {
                	getSupportActionBar().hide();
                	animateTo(new LatLng(MathUtils.coordIntToDouble(coordsE6[0]),MathUtils.coordIntToDouble(coordsE6[1])));
                }
            	return true;
        	} else {
            	return super.onKeyDown(keyCode, event);
        	}
    	} else {
    		IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
    		return true;
    	}
	}
	
	protected boolean onMenuItemSelected(int itemId) {
		if (ConfigurationManager.getUserManager().isUserAllowedAction() || itemId == android.R.id.home || itemId == R.id.exit || itemId == R.id.login || itemId == R.id.register) {	
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
						IntentsHelper.getInstance().startAutoCheckinListActivity(getMyPosition());
					} else {
						IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
					}
					break;
				case R.id.qrcheckin:
					if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
						IntentsHelper.getInstance().startQrCodeCheckinActivity(getMyPosition());
					} else {
						IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
					}
					break;
				case R.id.searchcheckin:
					if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
						IntentsHelper.getInstance().startLocationCheckinActivity(getMyPosition());
					} else {
						IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
					}
					break;
				case R.id.refreshLayers:
					IntentsHelper.getInstance().loadLayersAction(true, null, false, true,
					mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude,
		            (int)mMap.getCameraPosition().zoom, projection);
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
		    		IntentsHelper.getInstance().startMyLandmarksIntent(getMyPosition());
		    		break;
				case R.id.recentLandmarks:
		    		IntentsHelper.getInstance().startRecentLandmarksIntent(getMyPosition());
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
						IntentsHelper.getInstance().startFriendsCheckinsIntent(getMyPosition());
					} else {
						IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Checkin_required_error));
					}
					break;
				case R.id.trackPos:
					DialogManager.getInstance().showTrackMyPosAlertDialog(this, new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int id) {
				            trackMyPosAction();
				        }
				    });
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
				case R.id.dataPacket:
		    		DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.PACKET_DATA_DIALOG, null, null);
		    		break;
				case R.id.pickMyPos:
		    		IntentsHelper.getInstance().startPickLocationActivity();
		    		break;
				case R.id.deals:
		    		if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
		    			IntentsHelper.getInstance().startCategoryListActivity(MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude), -1, -1);
		    		} else {
		        		IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Login_required_error));
		    		}
		    		break;
				case R.id.register:
		    		IntentsHelper.getInstance().startRegisterActivity();
		    		break;
				case R.id.newestLandmarks:
					final String[] excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER};
					IntentsHelper.getInstance().startNewestLandmarkIntent(getMyPosition(), excluded, 2);
					break;
				case R.id.events:
		    		IntentsHelper.getInstance().startCalendarActivity(getMyPosition());
		    		break;
				case R.id.rateUs:
		    		DialogManager.getInstance().showAlertDialog(this,AlertDialogBuilder.RATE_US_DIALOG, null, null);
		    		break;
				case R.id.listLandmarks:
		    		if (!lvView.isShown()) {
		        		IntentsHelper.getInstance().showNearbyLandmarks(getMyPosition(), projection);
		    		}
		    		break;
				case R.id.shareScreenshot:
					takeScreenshot(true);
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

	private synchronized void initOnLocationChanged(LatLng location, int source) {
    	//System.out.println("4 --------------------------------");
    	if (!appInitialized && location != null) {
    		//System.out.println("4.1 --------------------------------");
        	loadingProgressBar.setProgress(75);
        	    	
        	if (!LandmarkManager.getInstance().isInitialized()) {
        		LandmarkManager.getInstance().initialize();
            }
            
        	MessageStack.getInstance().setHandler(loadingHandler);
        	LayerLoader.getInstance().setRepaintHandler(loadingHandler);
        	
        	if (!LayerLoader.getInstance().isInitialized() && !LayerLoader.getInstance().isLoading()) {
                if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
                    LoggerUtils.debug("Loading Layers in " + location.latitude + "," +  location.longitude);
                    int zoom = ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM);
                    if (mMap != null) {
                    	zoom = (int)mMap.getCameraPosition().zoom;
                    }
                    IntentsHelper.getInstance().loadLayersAction(true, null, false, true, location.latitude, location.longitude, zoom, projection);
                }
            } else {
                //load existing layers
                if (LayerLoader.getInstance().isLoading()) {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_VISIBLE);
                } else {
                    loadingHandler.sendEmptyMessage(MessageStack.STATUS_GONE);
                }
                loadingHandler.sendEmptyMessage(MessageStack.STATUS_MESSAGE);
            }

            loadingProgressBar.setProgress(100);
            
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
                loadingImage.setVisibility(View.GONE);
            }
            
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
	
	private void hideLandmarkView() {
    	lvView.setVisibility(View.GONE);
		getSupportActionBar().show();
		LandmarkManager.getInstance().clearLandmarkOnFocusQueue();
		LandmarkManager.getInstance().setSelectedLandmark(null);
		LandmarkManager.getInstance().setSeletedLandmarkUI();
    }
	
	private void animateTo(LatLng newLocation) {
		if (mMap != null) {
			CameraUpdate location = CameraUpdateFactory.newLatLng(newLocation);
			mMap.animateCamera(location);
		}
		
	}
	
	private String followMyPositionAction() {
		Intent routeTracingService = new Intent(this, RouteTracingService.class);	
		if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
            ConfigurationManager.getInstance().setOn(ConfigurationManager.FOLLOW_MY_POSITION);
            String route = RouteRecorder.getInstance().startRecording();
            startService(routeTracingService);
            routesCluster.showRouteAction(route, true);
            if (LayerLoader.getInstance().isLoading()) {
            	LayerLoader.getInstance().stopLoading();
            }
            List<ExtendedLandmark> myPosV = LandmarkManager.getInstance().getUnmodifableLayer(Commons.MY_POSITION_LAYER);
            if (!myPosV.isEmpty()) {
                ExtendedLandmark landmark = myPosV.get(0);
                if (projection.isVisible(landmark.getLatitudeE6(), landmark.getLongitudeE6())) {
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
            	//stopService(routeTracingService);
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
	
	private double[] getMyPosition() {
		if (mMap != null) {
			return LandmarkManager.getInstance().getMyLocation(mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude);
		} else {
			LatLng mapCenter = (LatLng) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, LatLng.class);
            if (mapCenter != null) {
            	return LandmarkManager.getInstance().getMyLocation(mapCenter.latitude, mapCenter.longitude);
            } else {
            	ExtendedLandmark l = ConfigurationManager.getInstance().getDefaultCoordinate();
            	return LandmarkManager.getInstance().getMyLocation(l.getQualifiedCoordinates().getLatitude(), l.getQualifiedCoordinates().getLongitude());
            }
		}  
    }

    private void clearMapAction() {
    	mMap.clear();
    	markerCluster.clearMarkers();
    	LandmarkManager.getInstance().clearLandmarkStore();
    	RoutesManager.getInstance().clearRoutesStore();
        IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Maps_cleared));
    }
    
    private void showMapAndMarkers() {
		if (!findViewById(R.id.mapContainer).isShown()) {
			findViewById(R.id.mapContainer).setVisibility(View.VISIBLE);
			findViewById(R.id.mapCanvasWidgetL).setVisibility(View.GONE);
		}
		if ((lvView == null || !lvView.isShown()) && getSupportActionBar() != null) {
			getSupportActionBar().show();
		}
		
	    markerCluster = new GoogleMarkerClusterOverlay(this, mMap, loadingHandler, this.getResources().getDisplayMetrics());	
	    markerCluster.loadAllMarkers();
	    
	    routesCluster = new GoogleRoutesOverlay(mMap, markerCluster, this.getResources().getDisplayMetrics().density);
	    routesCluster.loadAllRoutes();
	    
	    if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)) {
	        String route = RouteRecorder.getInstance().startRecording();
	        //Intent routeTracingService = new Intent(this, RouteTracingService.class);	
	        //startService(routeTracingService);
	        routesCluster.showRouteAction(route, true);
	
	        MessageStack.getInstance().addMessage(Locale.getMessage(R.string.Routes_TrackMyPosOn), 10, -1, -1);
	    }
	}

	private void showMyPositionAction(boolean loadLayers) {
        boolean isVisible = false;
        boolean clearLandmarks = false;
       
        Location myLoc = ConfigurationManager.getInstance().getLocation();
        LatLng myLocLatLng = new LatLng(myLoc.getLatitude(), myLoc.getLongitude());
        
        if (ConfigurationManager.getInstance().isOff(ConfigurationManager.RECORDING_ROUTE)) {
        
        	if (mMap.getProjection().getVisibleRegion().latLngBounds.contains(myLocLatLng)) {
        		isVisible = true;
        	}
        
        	if (!isVisible) {
        		hideLandmarkView();
        		clearLandmarks = IntentsHelper.getInstance().isClearLandmarksRequired(projection, MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), 
            		 MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude), MathUtils.coordDoubleToInt(myLoc.getLatitude()), MathUtils.coordDoubleToInt(myLoc.getLongitude()));
        	}
        	
        	if (loadLayers && !isVisible) {
        		if (clearLandmarks) {
            		markerCluster.clearMarkers();
            	}
                IntentsHelper.getInstance().loadLayersAction(true, null, clearLandmarks, true, myLoc.getLatitude(), myLoc.getLongitude(), (int)mMap.getCameraPosition().zoom, projection);
            }     
        }     
        animateTo(myLocLatLng);
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
            IntentsHelper.getInstance().loadLayersAction(true, null, clearMap, true,
                    mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude,
                    (int)mMap.getCameraPosition().zoom, projection);
        }
    }
    
    private void takeScreenshot(final boolean notify)
    {
    	if ((notify || !ConfigurationManager.getInstance().containsObject("screenshot_" + StringUtil.formatCoordE2(mMap.getCameraPosition().target.latitude) + "_" + StringUtil.formatCoordE2(mMap.getCameraPosition().target.longitude), Object.class)) && !isFinishing()) {   		
    		//if (notify) {
    		//	IntentsHelper.getInstance().showShortToast(Locale.getMessage(R.string.Task_started, Locale.getMessage(R.string.shareScreenshot)));
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
    				int numOfLandmarks = LandmarkManager.getInstance().getAllLayersSize();
    				int limit = ConfigurationManager.getInstance().getInt(ConfigurationManager.LANDMARKS_PER_LAYER, 30);
    				String filename = "screenshot_time_" + loadingTime + "sec_sdk_v" + version + "_num_" + numOfLandmarks + "_l_" + limit + ".jpg";
    			
    				ByteArrayOutputStream out = new ByteArrayOutputStream();
                	screenshot.compress(Bitmap.CompressFormat.JPEG, 80, out);
                
                	Uri uri = PersistenceManagerFactory.getFileManager().saveImageFile(screenshot, "screenshot.jpg");
                	
                	int id = soundPool.play(shutterSound, 1f, 1f, 0, 0, 1);
                    LoggerUtils.debug("Shutter sound played with id " + id);
                	
                    AsyncTaskManager.getInstance().executeImageUploadTask(out.toByteArray(), filename, mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude);
				    
					if (notify) {
						IntentsHelper.getInstance().shareImageAction(uri);
					}
				}        	
        	};
        
        	mMap.snapshot(callback);
    	} else if (notify) {
    		IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Share_screenshot_exists));
    	} else {
    		LoggerUtils.debug("Screenshot for current location has already been sent!");
    	}
    }
    
    private void trackMyPosAction() {
        String filename = followMyPositionAction();

        ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
        if (filename != null) {
            DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.SAVE_ROUTE_DIALOG, null, new SpannableString(Locale.getMessage(R.string.Routes_Recording_Question, filename)));
        } else if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION)
                && !ServicesUtils.isGpsActive(ConfigurationManager.getInstance().getContext())) {
            DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.LOCATION_ERROR_DIALOG, null, null);
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
            			IntentsHelper.getInstance().startPickLocationActivity();
            		}
            	} else if (msg.what == MessageStack.STATUS_MESSAGE) {
        			activity.statusBar.setText(MessageStack.getInstance().getMessage());
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
            		IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Social_token_expired, "Facebook"));
            	} else if (msg.what == GoogleMarkerClusterOverlay.SHOW_LANDMARK_DETAILS) {
            		int[] coordsE6 = IntentsHelper.getInstance().showLandmarkDetailsAction(activity.getMyPosition(), activity.lvView, (int)activity.mMap.getCameraPosition().zoom, activity.projection);
                    if (coordsE6 != null) {
                    	activity.getSupportActionBar().hide();
                    	activity.animateTo(new LatLng(MathUtils.coordIntToDouble(coordsE6[0]),MathUtils.coordIntToDouble(coordsE6[1])));
                    }
            	} else if (msg.what == GoogleMarkerClusterOverlay.SHOW_LANDMARK_LIST) {
            		IntentsHelper.getInstance().startMultiLandmarkIntent(activity.getMyPosition());
            		activity.animateTo(new LatLng(MathUtils.coordIntToDouble(msg.arg1), MathUtils.coordIntToDouble(msg.arg2)));
            	} else if (msg.what == AsyncTaskManager.SHOW_ROUTE_MESSAGE) {
            		activity.routesCluster.showRouteAction((String) msg.obj, true);
            	} else if (msg.what == GmsLocationServicesManager.GMS_CONNECTED) {
            		activity.onConnected();
            	} else if (msg.what == GmsLocationServicesManager.UPDATE_LOCATION) {
            		activity.onLocationChanged();
            	} else if (msg.obj != null) {
            		LoggerUtils.error("Unknown message received: " + msg.obj.toString());
            	} 
        	}		
		}
	}
}
