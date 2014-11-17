package com.jstakun.gms.android.landmarks;

import org.apache.http.message.BasicNameValuePair;

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
        	params.add(new BasicNameValuePair("minx", StringUtil.formatCoordE2(bbox.west)));
        	params.add(new BasicNameValuePair("miny", StringUtil.formatCoordE2(bbox.south)));
        	params.add(new BasicNameValuePair("maxx", StringUtil.formatCoordE2(bbox.east)));  
        	params.add(new BasicNameValuePair("maxy", StringUtil.formatCoordE2(bbox.north)));
        }
	}

	@Override
	protected String getUri() {
		return "panoramio2Provider";
	}
}
