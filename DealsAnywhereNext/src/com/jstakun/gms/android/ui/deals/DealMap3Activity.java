package com.jstakun.gms.android.ui.deals;

import java.lang.ref.WeakReference;

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
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.google.maps.GoogleLandmarkProjectionV2;
import com.jstakun.gms.android.google.maps.GoogleMapsV2TypeSelector;
import com.jstakun.gms.android.google.maps.GoogleMarkerClusterOverlay;
import com.jstakun.gms.android.google.maps.GoogleRoutesOverlay;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerLoader;
import com.jstakun.gms.android.location.AndroidDevice;
import com.jstakun.gms.android.location.GmsLocationServicesManager;
import com.jstakun.gms.android.ui.AlertDialogBuilder;
import com.jstakun.gms.android.ui.AsyncTaskManager;
import com.jstakun.gms.android.ui.DialogManager;
import com.jstakun.gms.android.ui.HelpActivity;
import com.jstakun.gms.android.ui.IntentsHelper;
import com.jstakun.gms.android.ui.LandmarkListActivity;
import com.jstakun.gms.android.ui.MapInfoView;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.MessageStack;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.StringUtil;
import com.jstakun.gms.android.utils.UserTracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
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
																   GoogleMap.OnMyLocationButtonClickListener {

	private static final int SHOW_MAP_VIEW = 0;
	private static final int PICK_LOCATION = 1;
	private static final int PERMISSION_ACCESS_LOCATION = 0;
	private static final int PERMISSION_CALL_PHONE = 1;
	
	private DealOfTheDayDialog dealOfTheDayDialog;
    
    private TextView statusBar;
    private View lvCloseButton, lvCallButton, lvOpenButton,
                 lvView, lvShareButton, lvRouteButton,
                 thumbnailButton, loadingImage;
    private ProgressBar loadingProgressBar;
    private NavigationDrawerFragment mNavigationDrawerFragment;
    
    private GoogleLandmarkProjectionV2 projection;
	private GoogleMarkerClusterOverlay markerCluster;
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
        
        ConfigurationManager.getInstance().setContext(this);
        
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
		fragmentManager.beginTransaction().replace(R.id.mapContainer, PlaceholderFragment.newInstance(position + 1))
				.commit();
	}
	
	@Override
    public void onResume() {
    	super.onResume();
        LoggerUtils.debug("onResume");
        
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || 
        	    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        	ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ACCESS_LOCATION);
        } else {
        	GmsLocationServicesManager.getInstance().enable(loadingHandler);
        }
        
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

        IntentsHelper.getInstance().showStatusDialogs();

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
    }
	
	@Override
    public void onPause() {
        super.onPause();
        GmsLocationServicesManager.getInstance().disable();
        
        DialogManager.getInstance().dismissDialog(this);
    }
	
	@Override
    protected void onStop() {
        super.onStop();
        if (mMap != null) {
        	ConfigurationManager.getInstance().putObject(ConfigurationManager.MAP_CENTER, mMap.getCameraPosition().target);
        }
        int type = DialogManager.getInstance().dismissDialog(this);
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
    	super.onDestroy();
    	if (ConfigurationManager.getInstance().isClosing()) {
        	appInitialized = false;
        	IntentsHelper.getInstance().hardClose(loadingHandler, null, (int)mMap.getCameraPosition().zoom, MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude));
        } else if (mMap != null) {
            IntentsHelper.getInstance().softClose((int)mMap.getCameraPosition().zoom, MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude));
        }
        AdsUtils.destroyAdView(this);
        System.gc();  	
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
    	
	}
	
	private synchronized void initOnLocationChanged(LatLng location, int source) {
    	//System.out.println("4 --------------------------------");
    	if (!appInitialized && location != null) {
    		//System.out.println("4.1 --------------------------------");
        	loadingProgressBar.setProgress(75);
        	    	
        	if (!LandmarkManager.getInstance().isInitialized()) {
        		LandmarkManager.getInstance().initialize(Commons.LOCAL_LAYER, Commons.ROUTES_LAYER, Commons.MY_POSITION_LAYER, Commons.COUPONS_LAYER,
                		Commons.HOTELS_LAYER, Commons.GROUPON_LAYER, Commons.FOURSQUARE_MERCHANT_LAYER, Commons.YELP_LAYER);
            }
            
        	MessageStack.getInstance().setHandler(loadingHandler);
        	LayerLoader.getInstance().setRepaintHandler(loadingHandler);
            
            if (!LayerLoader.getInstance().isLoading() && !LayerLoader.getInstance().isInitialized()) {
                LoggerUtils.debug("Loading Layers in " + location.latitude + "," +  location.longitude);
                int zoom = ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM);
                if (mMap != null) {
                    zoom = (int)mMap.getCameraPosition().zoom;
                }
                IntentsHelper.getInstance().loadLayersAction(true, null, false, false, location.latitude, location.longitude, zoom, projection);               
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
                IntentsHelper.getInstance().startDealsOfTheDayIntent(getMyPosition(), null);
                break;
            case R.id.newestDeals:
                final String[] excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER, Commons.LOCAL_LAYER};
                IntentsHelper.getInstance().startNewestLandmarkIntent(getMyPosition(), excluded, 2);
                break;
            case R.id.nearbyDeals:
                if (!lvView.isShown()) {
                    IntentsHelper.getInstance().showNearbyLandmarks(getMyPosition(), projection);
                }
                break;
            case R.id.settings:
                IntentsHelper.getInstance().startSettingsActivity(SettingsActivity.class);
                break;
            case R.id.listMode:
                IntentsHelper.getInstance().startCategoryListActivity(MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude), -1, -1);
                break;
            case R.id.pickMyPos:
                IntentsHelper.getInstance().startPickLocationActivity();
                break;
            case R.id.search:
                onSearchRequested();
                break;
            case R.id.refreshLayers:
                IntentsHelper.getInstance().loadLayersAction(true, null, false, false,
                		mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude,
    		            (int)mMap.getCameraPosition().zoom, projection);
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
            case R.id.config:
				IntentsHelper.getInstance().startConfigurationViewerActivity();
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
                IntentsHelper.getInstance().startActionViewIntent(ConfigurationManager.getInstance().getString("lmUrl"));
                break;
            case R.id.events:
                IntentsHelper.getInstance().startCalendarActivity(getMyPosition());
                break;
            case R.id.rateUs:
                DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.RATE_US_DIALOG, null, null);
                break;
            case R.id.shareScreenshot:
				takeScreenshot(true, this);
				break;
			case R.id.reset:
            	DialogManager.getInstance().showAlertDialog(this, AlertDialogBuilder.RESET_DIALOG, null, null);
            	break;              	
            default:
                return true;
        }
        return true;
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
        	clearLandmarks = IntentsHelper.getInstance().isClearLandmarksRequired(projection, MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.latitude), 
            		 MathUtils.coordDoubleToInt(mMap.getCameraPosition().target.longitude), MathUtils.coordDoubleToInt(myLoc.getLatitude()), MathUtils.coordDoubleToInt(myLoc.getLongitude()));
        }
        	
        if (loadLayers && !isVisible) {
        	if (clearLandmarks) {
        		markerCluster.clearMarkers();
        	}
            IntentsHelper.getInstance().loadLayersAction(true, null, clearLandmarks, false, myLoc.getLatitude(), myLoc.getLongitude(), (int)mMap.getCameraPosition().zoom, projection);
        }
        
        animateTo(myLocLatLng);
    }
	
	private void hideLandmarkView() {
    	lvView.setVisibility(View.GONE);
		getSupportActionBar().show();
		LandmarkManager.getInstance().clearLandmarkOnFocusQueue();
		LandmarkManager.getInstance().setSelectedLandmark(null);
		LandmarkManager.getInstance().setSeletedLandmarkUI();
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
            dealOfTheDayDialog = new DealOfTheDayDialog(this, recommended, getMyPosition(), loadingHandler, IntentsHelper.getInstance());
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
            IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.noDodAvailable));
        } //else {
        //comment out
        //IntentsHelper.getInstance().showInfoToast("Recommended == null\n"
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
            IntentsHelper.getInstance().loadLayersAction(true, null, clearMap, false,
                    mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude,
                    (int)mMap.getCameraPosition().zoom, projection);
        }
    }
    
    private void callButtonPressedAction(ExtendedLandmark landmark) {
        IntentsHelper.getInstance().startPhoneCallActivity(landmark);
    }

    private void sendMessageAction() {
        IntentsHelper.getInstance().shareLandmarkAction();
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
	}
    
    private void takeScreenshot(final boolean notify, final Context context) {
    	if (mMap != null) {
    		SnapshotReadyCallback callback = new SnapshotReadyCallback() {
    			@Override
    			public void onSnapshotReady(Bitmap screenshot) {
    				LoggerUtils.debug("Google Map snapshot taken!");
    				AsyncTaskManager.getInstance().executeImageUploadTask(context, screenshot, mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude, notify);
    			}        	
    		};
    		mMap.snapshot(callback);
    	}
    }
    
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentsHelper.getInstance().setActivity(this);
    	if (requestCode == IntentsHelper.INTENT_MULTILANDMARK) {
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
                LandmarkManager.getInstance().addLandmark(lat, lng, 0.0f, StringUtil.formatCommaSeparatedString(name), "", Commons.LOCAL_LAYER, true);
            } else if (resultCode == RESULT_CANCELED && !appInitialized) {
                ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
                IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Pick_location_default, landmark.getName()));
                initOnLocationChanged(new LatLng(landmark.getQualifiedCoordinates().getLatitude(), landmark.getQualifiedCoordinates().getLongitude()), 5);
            } else if (resultCode == RESULT_CANCELED && intent != null && intent.hasExtra("message")) {
                String message = intent.getStringExtra("message");
                IntentsHelper.getInstance().showInfoToast(message);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
            	Status status = PlaceAutocomplete.getStatus(this, intent);
                IntentsHelper.getInstance().showInfoToast(status.getStatusMessage());
            	if (! appInitialized) {
            		ExtendedLandmark landmark = ConfigurationManager.getInstance().getDefaultCoordinate();
                    initOnLocationChanged(new LatLng(landmark.getQualifiedCoordinates().getLatitude(), landmark.getQualifiedCoordinates().getLongitude()), 6);
            	}
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
                        pickPositionAction(new LatLng(l.getQualifiedCoordinates().getLatitude(), l.getQualifiedCoordinates().getLongitude()), true, true);
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

	private void onLocationChanged() {
		Location location = ConfigurationManager.getInstance().getLocation();
		
		if (!appInitialized && !isFinishing()) {
			initOnLocationChanged(new LatLng(location.getLatitude(), location.getLongitude()), 3);
		}
		
		if (appInitialized && !isFinishing() && AndroidDevice.isBetterLocation(location, ConfigurationManager.getInstance().getLocation())) {
			IntentsHelper.getInstance().addMyLocationLandmark(location);     
			IntentsHelper.getInstance().vibrateOnLocationUpdate();
			UserTracker.getInstance().sendMyLocation();	   
		}		
	}

	private void onConnected() {
		Location location = ConfigurationManager.getInstance().getLocation();
		if (location != null && !appInitialized) {
			initOnLocationChanged(new LatLng(location.getLatitude(), location.getLongitude()), 0);
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

	@Override
	public void onClick(View v) {
		ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getSeletedLandmarkUI();
    	if (selectedLandmark != null) {
    		if (v == lvCloseButton) {
    			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CloseSelectedDealView", "", 0);
    			hideLandmarkView();
    		} else if (v == lvOpenButton) {
    			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenSelectedDealURL", selectedLandmark.getLayer(), 0);
    			IntentsHelper.getInstance().openButtonPressedAction(LandmarkManager.getInstance().getSeletedLandmarkUI());
    		} else if (v == thumbnailButton) {
    			if (IntentsHelper.getInstance().startStreetViewActivity(selectedLandmark)) {
    				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenStreetView", selectedLandmark.getLayer(), 0);
    			} else {
    				UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".OpenURLSelectedLandmark", selectedLandmark.getLayer(), 0);
        			IntentsHelper.getInstance().openButtonPressedAction(selectedLandmark);
    			}
    		} else if (v == lvCallButton) {
    			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CallSelectedDeal", selectedLandmark.getLayer(), 0);
    			if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
    	        	ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, PERMISSION_CALL_PHONE);
    	        } else {
    	        	IntentsHelper.getInstance().startPhoneCallActivity(selectedLandmark);
    	        }
    		} else if (v == lvRouteButton) {
    			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowRouteSelectedDeal", selectedLandmark.getLayer(), 0);
                DialogManager.getInstance().showRouteAlertDialog(this, null, loadingHandler);
    		} else if (v == lvShareButton) {
    			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShareSelectedDeal", selectedLandmark.getLayer(), 0);
    			sendMessageAction();
    		}
    	} else {
    		IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
    	}	
	}

	@Override
	public void onMapReady(GoogleMap map) {
		LoggerUtils.debug("Google Map is ready!");
		this.mMap = map;
		this.projection = new GoogleLandmarkProjectionV2(mMap);
		
		GoogleMapsV2TypeSelector.selectMapType(mMap);
		
	    mMap.getUiSettings().setZoomControlsEnabled(true);
	    
	    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || 
		    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
		    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ACCESS_LOCATION);
	    } else {
		    mMap.setMyLocationEnabled(true);
		}
	    
	    mMap.setOnMyLocationButtonClickListener(this);
	    mMap.setOnCameraChangeListener(mOnCameraChangeListener);
	    
	    if (appInitialized) {
	    	LatLng mapCenter = (LatLng) ConfigurationManager.getInstance().getObject(ConfigurationManager.MAP_CENTER, LatLng.class);
	        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mapCenter, ConfigurationManager.getInstance().getInt(ConfigurationManager.ZOOM));
	    	mMap.moveCamera(cameraUpdate);
	    	loadingHandler.sendEmptyMessage(SHOW_MAP_VIEW);
	    }
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
	    switch (requestCode) {	
	    	case PERMISSION_ACCESS_LOCATION:
	    		if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
	    			GmsLocationServicesManager.getInstance().enable(loadingHandler);
	    			mMap.setMyLocationEnabled(true);
	    		}
	    		break;
	    	case PERMISSION_CALL_PHONE:
	    		IntentsHelper.getInstance().startPhoneCallActivity(LandmarkManager.getInstance().getSeletedLandmarkUI());
	    		break;	
	    	default:	
	    		break; 	
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
	     	
	     public LoadingHandler(DealMap3Activity parentActivity) {
	    	this.parentActivity = new WeakReference<DealMap3Activity>(parentActivity);
	     }
	    	
	     @Override
         public void handleMessage(Message msg) {
			DealMap3Activity activity = parentActivity.get();
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
                    DialogManager.getInstance().showRouteAlertDialog(activity, new SpannableString("dod"), this);
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
        			activity.takeScreenshot(false, activity);
        		} else if (msg.what == GoogleMarkerClusterOverlay.SHOW_LANDMARK_DETAILS) {
            		int[] coordsE6 = IntentsHelper.getInstance().showLandmarkDetailsAction(activity.getMyPosition(), activity.lvView, (int)activity.mMap.getCameraPosition().zoom, activity.projection);
                    if (coordsE6 != null) {
                    	activity.getSupportActionBar().hide();
                    	activity.animateTo(new LatLng(MathUtils.coordIntToDouble(coordsE6[0]),MathUtils.coordIntToDouble(coordsE6[1])));
                    }
            	} else if (msg.what == GoogleMarkerClusterOverlay.SHOW_LANDMARK_LIST) {
            		IntentsHelper.getInstance().startMultiLandmarkIntent(activity.getMyPosition());
            		activity.animateTo(new LatLng(MathUtils.coordIntToDouble(msg.arg1), MathUtils.coordIntToDouble(msg.arg2)));
            	} else if (msg.what == SHOW_MAP_VIEW) {
        			activity.showMapAndMarkers();
        		} else if (msg.what == AsyncTaskManager.SHOW_ROUTE_MESSAGE) {
        			activity.routesCluster.showRouteAction((String) msg.obj, true);
        		} else if (msg.obj != null) {
            		LoggerUtils.error("Unknown message received: " + msg.obj.toString());
            	} else if (msg.what == GmsLocationServicesManager.GMS_CONNECTED) {
            		activity.onConnected();
            	} else if (msg.what == GmsLocationServicesManager.UPDATE_LOCATION) {
            		activity.onLocationChanged();
            	} 
        	}
		}
	}
}
