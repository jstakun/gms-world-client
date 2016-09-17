package com.jstakun.gms.android.landmarks;

import java.util.Locale;

/**
 *
 * @author jstakun
 */
public class HotelsReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);    
		String l = Locale.getDefault().getLanguage();
		params.put("latitudeMin", Double.toString(latitude));
		params.put("longitudeMin", Double.toString(longitude));
		params.put("lang", l);
	}

	@Override
	protected String getUri() {
		return "hotelsProvider";
	}
}
