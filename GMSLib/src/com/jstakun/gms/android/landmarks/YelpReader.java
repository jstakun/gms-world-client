package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class YelpReader extends AbstractSerialReader{

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		int dist = radius * 1000;
		params.put("radius", Integer.toString(dist));
	}

	@Override
	protected String getUri() {
		return "yelpProvider";
	}
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.YELP_LAYER;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Yelp_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.yelp;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.yelp_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.yelp_128;
	}
	
	@Override
	public int getPriority() {
		return 5;
	}
}
