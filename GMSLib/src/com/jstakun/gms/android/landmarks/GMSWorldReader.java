package com.jstakun.gms.android.landmarks;

/**
 *
 * @author jstakun
 */
public class GMSWorldReader extends AbstractSerialReader {
    
	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		params.put("latitudeMin", Double.toString(latitude));
		params.put("longitudeMin", Double.toString(longitude));
	}
	
	@Override
	protected String getUri() {
		return "downloadLandmark";
	}
}
