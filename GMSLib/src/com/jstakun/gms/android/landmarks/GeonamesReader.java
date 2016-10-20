package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;

/**
 *
 * @author jstakun
 */
public class GeonamesReader extends AbstractSerialReader {
	@Override
	protected String getUri() {
		return "geonamesProvider";
	}

	@Override
	protected String getLayerName() {
		return Commons.WIKIPEDIA_LAYER;
	}
}
