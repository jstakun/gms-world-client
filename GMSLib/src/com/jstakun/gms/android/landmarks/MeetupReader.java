package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;

/**
 *
 * @author jstakun
 */
public class MeetupReader extends AbstractSerialReader {
    @Override
	protected String getUri() {
		return "meetupProvider";
	}
    
    @Override
	protected String getLayerName() {
		return Commons.MEETUP_LAYER;
	}
}
