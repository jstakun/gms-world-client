package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;

/**
 *
 * @author jstakun
 */
public class FlickrReader extends AbstractSerialReader {
	
	@Override
	protected String getUri() {
		return "flickrProvider";
	}

	@Override
	protected String getLayerName() {
		return Commons.FLICKR_LAYER;
	}
}
