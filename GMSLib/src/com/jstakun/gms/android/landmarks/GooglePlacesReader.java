package com.jstakun.gms.android.landmarks;

import java.util.Locale;

/**
 *
 * @author jstakun
 */
public class GooglePlacesReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		String lang = Locale.getDefault().getLanguage();
		params.put("language", lang);
	}

	@Override
	protected String getUri() {
		return "googlePlacesProvider";
	}
}
