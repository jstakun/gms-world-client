package com.jstakun.gms.android.landmarks;

import java.util.Locale;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class HotelsReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom) {
		super.init(latitude, longitude, zoom);    
		String l = Locale.getDefault().getLanguage();
		params.put("latitudeMin", Double.toString(latitude));
		params.put("longitudeMin", Double.toString(longitude));
		params.put("lang", l);
	}

	@Override
	protected String getUri() {
		return "hotelsProvider";
	}
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.HOTELS_LAYER;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Hotels_Combined_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.hotel;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.hotel_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.travel_img;
	}
	
	@Override
	public int getPriority() {
		return 4;
	}
}
