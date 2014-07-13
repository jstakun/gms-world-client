package com.jstakun.gms.android.landmarks;

import java.util.Locale;

import org.apache.http.message.BasicNameValuePair;

/**
 *
 * @author jstakun
 */
public class FoursquareReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		params.add(new BasicNameValuePair("lang", Locale.getDefault().getLanguage()));
	}

	@Override
	protected String getUri() {
		return "foursquareProvider";
	}
}
