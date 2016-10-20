package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;

public class YouTubeReader extends AbstractSerialReader {

	@Override
	protected String getUri() {
		return "youTubeProvider";
	}
	
	@Override
	protected String getLayerName() {
		return Commons.YOUTUBE_LAYER;
	}
}
