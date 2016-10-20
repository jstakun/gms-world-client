package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;

/**
 *
 * @author jstakun
 */
public class ExpediaReader extends AbstractSerialReader {
    
	@Override
	protected String getUri() {
		return "expediaProvider";
	}

	@Override
	protected String getLayerName() {
		return Commons.EXPEDIA_LAYER;
	}
	
	@Override
	public boolean isEnabled() {
    	return false;
    }
}
