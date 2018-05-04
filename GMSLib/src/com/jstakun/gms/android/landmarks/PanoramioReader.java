package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.StringUtil;

/**
 *
 * @author jstakun
 */

public class PanoramioReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom) {
		super.init(latitude, longitude, zoom);
		BoundingBox bbox = (BoundingBox) ConfigurationManager.getInstance().getObject(BoundingBox.BBOX, BoundingBox.class);
		if (bbox != null) {
			params.put("minx", StringUtil.formatCoordE2(bbox.west));
			params.put("miny", StringUtil.formatCoordE2(bbox.south));
			params.put("maxx", StringUtil.formatCoordE2(bbox.east));  
			params.put("maxy", StringUtil.formatCoordE2(bbox.north));	
		}
	}

	@Override
	protected String getUri() {
		return "panoramio2Provider";
	}
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.PANORAMIO_LAYER;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Panoramio_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.panoramio;
	}

	@Override
	public int getLargeIconResource() {
		return -1;
	}

	@Override
	public int getImageResource() {
		return R.drawable.panoramio_128;
	}
	
	@Override
	public int getPriority() {
		return 13;
	}
	
	@Override
	public boolean isEnabled() {
    	return false;
    }
}
