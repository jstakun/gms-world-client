package com.jstakun.gms.android.landmarks;

import java.util.Locale;

import com.jstakun.gms.android.config.Commons;

/**
 *
 * @author jstakun
 */
public class FoursquareReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		params.put("lang", Locale.getDefault().getLanguage());
	}

	@Override
	protected String getUri() {
		return "foursquareProvider";
	}

	@Override
	protected String getLayerName() {
		return Commons.FOURSQUARE_LAYER;
	}
}
