package com.jstakun.gms.android.google.maps;

import com.google.android.gms.maps.GoogleMap;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.LoggerUtils;

public class GoogleMapsV2TypeSelector {
	
	 public static void selectMapType(final GoogleMap mMap) {
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
