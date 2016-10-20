package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;

/**
 *
 * @author jstakun
 */

public class EventfulReader extends AbstractSerialReader {

	@Override
	protected String getUri() {
		return "eventfulProvider";
	}

	@Override
	protected String getLayerName() {
		return Commons.EVENTFUL_LAYER;
	}
}
