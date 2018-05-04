package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.StringUtil;

/**
 *
 * @author jstakun
 */
public abstract class OsmReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom) {
		super.init(latitude, longitude, zoom);
        BoundingBox bbox = (BoundingBox) ConfigurationManager.getInstance().getObject(BoundingBox.BBOX, BoundingBox.class);
		if (bbox != null) {
			params.put("longitudeMin", StringUtil.formatCoordE2(bbox.west));
			params.put("latitudeMin", StringUtil.formatCoordE2(bbox.south));
			params.put("longitudeMax", StringUtil.formatCoordE2(bbox.east));  
			params.put("latitudeMax", StringUtil.formatCoordE2(bbox.north));
		}
    }

	@Override
	protected String getUri() {
		return "osmProvider";
	}
	
	@Override
	public int getPriority() {
		return 9;
	}
}
