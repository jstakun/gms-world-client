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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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
	
	private Handler loadingHandler;
	private GoogleMarkerClusterOverlay markerCluster;
	private GoogleApiClient mGoogleApiClient;
    
	private TextView statusBar;
    private View lvCloseButton, lvCallButton, lvCommentButton, mapButtons,
            lvOpenButton, lvView, lvShareButton, myLocationButton, nearbyLandmarksButton,
            thumbnailButton, lvCheckinButton, lvRouteButton, loadingImage;
    private ProgressBar loadingProgressBar;
   
	private NavigationDrawerFragment mNavigationDrawerFragment;
    private GoogleMap mMap;

    private GoogleMap.OnCameraChangeListener mOnCameraChangeListener = new GoogleMap.OnCameraChangeListener() {
		
		@Override
		public void onCameraChange(CameraPosition position) {
			//check if zoom has changed
			MapInfoView mapInfo = (MapInfoView) findViewById(R.id.info);
			mapInfo.setZoomLevel((int)position.zoom); 
			//mapInfo.postInvalidate();
		}
	};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        
        loadingHandler = new LoadingHandler(this);
        
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
        	actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(getTitle());
            actionBar.hide();
        }
        
        Log.d(this.getClass().getName(), "Waiting for map to get ready...");
        loadingHandler.sendEmptyMessageDelayed(SHOW_MAP_VIEW, 5000);
    }

    private void initComponents(Bundle savedInstanceState) {
    	
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
    }
    
    @Override
    public void onStart() {
        super.onStart();
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
    
    @Override
    public void onRestart() {
        super.onRestart();
    }
    
    @Override
    public void onNewIntent(Intent intent) {
    }
    
    @Override
    public boolean onSearchRequested() {
    	return false;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.main_menu_2, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	return false;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
    
    @Override
	public void onClick(View v) {		
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    }
    
	@Override
	public void onMapReady(GoogleMap map) {
		Log.d(this.getClass().getName(), "Google Map is ready!");
		loadingHandler.sendEmptyMessage(SHOW_MAP_VIEW);
		this.mMap = map;
		buildGoogleApiClient();
	    mMap.getUiSettings().setZoomControlsEnabled(true);
	    mMap.setMyLocationEnabled(true);
	    mMap.setOnMyLocationButtonClickListener(this);
	    mMap.setOnCameraChangeListener(mOnCameraChangeListener);
	    
	    LatLng latLng = new LatLng(52.25, 20.95);
	    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 8);
	    mMap.moveCamera(cameraUpdate);
	    
	    loadMarkers();
	}  
	
	@Override
	public boolean onMyLocationButtonClick() {
		return false;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		//user location has changed	
		Toast.makeText(this, "Location new received: " + location.getLatitude() + "," + location.getLongitude() + " from " + location.getProvider(), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult result) {
		//call logger
	}

	@Override
    public void onConnected(Bundle bundle) {
		LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

	@Override
	public void onConnectionSuspended(int reason) {
		//call logger
	}
    
	protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

	
	private void loadMarkers() {    
	    markerCluster = new GoogleMarkerClusterOverlay(this, mMap);
	    
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
            	}
        	}
			
		}
	}
}
