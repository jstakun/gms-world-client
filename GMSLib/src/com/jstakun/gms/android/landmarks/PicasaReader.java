package com.jstakun.gms.android.landmarks;

import org.apache.http.message.BasicNameValuePair;

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
		BoundingBox bbox = (BoundingBox) ConfigurationManager.getInstance().getObject("bbox", BoundingBox.class);
        if (bbox != null) {
        	params.add(new BasicNameValuePair("bbox", StringUtil.formatCoordE2(bbox.west) + "," + StringUtil.formatCoordE2(bbox.south) + "," +
                StringUtil.formatCoordE2(bbox.east) + "," + StringUtil.formatCoordE2(bbox.north)));
        }
	}

	@Override
	protected String getUri() {
		return "picasaProvider";
	}
}
