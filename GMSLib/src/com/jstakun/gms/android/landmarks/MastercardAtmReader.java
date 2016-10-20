package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;

/**
 *
 * @author jstakun
 */
public class MastercardAtmReader extends AbstractSerialReader {
	@Override
	protected String getUri() {
		return "atmProvider";
	}
	
	@Override
	protected String getLayerName() {
		return Commons.MC_ATM_LAYER;
	}
}
