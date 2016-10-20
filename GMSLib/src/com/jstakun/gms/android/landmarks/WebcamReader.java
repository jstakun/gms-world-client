package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;

/**
 *
 * @author jstakun
 */
public class WebcamReader extends AbstractSerialReader  {

    @Override
	protected String getUri() {
		return "webcamProvider";
	}
    
    @Override
	protected String getLayerName() {
		return Commons.WEBCAM_LAYER;
	}
}
