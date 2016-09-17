package com.jstakun.gms.android.landmarks;

/**
 *
 * @author jstakun
 */
public class YelpReader extends AbstractSerialReader{

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		int dist = radius * 1000;
		params.remove(0); //remove default radius parameter
		params.put("radius", Integer.toString(dist));
	}

	@Override
	protected String getUri() {
		return "yelpProvider";
	}
}
