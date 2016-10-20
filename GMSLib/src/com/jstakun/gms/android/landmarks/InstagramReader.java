package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;

public class InstagramReader extends AbstractSerialReader {
	@Override
	protected String getUri() {
		return "instagramProvider";
	}
	
	@Override
	protected String getLayerName() {
		return Commons.INSTAGRAM_LAYER;
	}
	
	@Override
	public boolean isEnabled() {
    	return false;
    }
}
