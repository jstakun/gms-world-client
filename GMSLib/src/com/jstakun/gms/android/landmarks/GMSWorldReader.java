package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

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

	@Override
	public String getLayerName(boolean formatted) {
		return Commons.LM_SERVER_LAYER;
	}
	
	@Override
	public boolean isCheckinable() {
    	return true;
    }

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Public_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.globe16_new;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.globe24_new;
	}

	@Override
	public int getImageResource() {
		return R.drawable.discover_128;
	}
	
	@Override
	public int getPriority() {
		return 16;
	}
}
