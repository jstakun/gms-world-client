package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;

public class FreebaseReader extends AbstractSerialReader {

	@Override
	protected String getUri() {
		return "freebaseProvider";
	}

	@Override
	protected String getLayerName() {
		return Commons.FREEBASE_LAYER;
	}

	@Override
	public boolean isEnabled() {
    	return false;
    }
}
