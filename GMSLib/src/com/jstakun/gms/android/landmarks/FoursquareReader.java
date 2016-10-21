package com.jstakun.gms.android.landmarks;

import java.util.Locale;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

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
	public String getLayerName(boolean formatted) {
		return Commons.FOURSQUARE_LAYER;
	}
	
	@Override
	public boolean isCheckinable() {
    	return true;
    }

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Foursquare_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.foursquare;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.foursquare_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.foursquare_128;
	}
	
	@Override
	public int getPriority() {
		return 3;
	}
}
