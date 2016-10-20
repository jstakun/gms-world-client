package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;

/**
 *
 * @author jstakun
 */
public class TwitterReader extends AbstractSerialReader {

    @Override
	protected String getUri() {
		return "twitterProvider";
	}
    
    @Override
	protected String getLayerName() {
		return Commons.TWITTER_LAYER;
	}
    
}
