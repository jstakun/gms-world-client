package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;

/**
 *
 * @author jstakun
 */
public class LastFmReader extends AbstractSerialReader {

    @Override
	protected String getUri() {
		return "lastfmProvider";
	}
    
    @Override
	protected String getLayerName() {
		return Commons.LASTFM_LAYER;
	}
    
    @Override
	public boolean isEnabled() {
    	return false;
    }
}
