package com.jstakun.gms.android.landmarks;

import java.util.Locale;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class GooglePlacesReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom) {
		super.init(latitude, longitude, zoom);
		String lang = Locale.getDefault().getLanguage();
		params.put("language", lang);
	}

	@Override
	protected String getUri() {
		return "googlePlacesProvider";
	}
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.GOOGLE_PLACES_LAYER;
	}
	
	@Override
	public boolean isCheckinable() {
    	return true;
    }

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Google_Places_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.google_icon;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.google_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.google_places_128;
	}
	
	@Override
	public int getPriority() {
		return 6;
	}
}
