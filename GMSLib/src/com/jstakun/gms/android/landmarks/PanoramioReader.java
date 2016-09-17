package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.StringUtil;

/**
 *
 * @author jstakun
 */

public class PanoramioReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		BoundingBox bbox = (BoundingBox) ConfigurationManager.getInstance().getObject("bbox", BoundingBox.class);
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
}
