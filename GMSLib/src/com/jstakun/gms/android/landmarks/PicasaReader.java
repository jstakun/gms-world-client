package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.StringUtil;

/**
 *
 * @author jstakun
 */
public class PicasaReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		BoundingBox bbox = (BoundingBox) ConfigurationManager.getInstance().getObject(BoundingBox.BBOX, BoundingBox.class);
		if (bbox != null) {
			params.put("bbox", StringUtil.formatCoordE2(bbox.west) + "," + StringUtil.formatCoordE2(bbox.south) + "," +
							   StringUtil.formatCoordE2(bbox.east) + "," + StringUtil.formatCoordE2(bbox.north));
		}	
	}

	@Override
	protected String getUri() {
		return "picasaProvider";
	}
	
	@Override
	protected String getLayerName() {
		return Commons.PICASA_LAYER;
	}
	
	@Override
	public boolean isEnabled() {
    	return false;
    }
}
