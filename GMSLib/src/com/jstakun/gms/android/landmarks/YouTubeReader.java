package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;

public class YouTubeReader extends AbstractSerialReader {

	@Override
	protected String getUrl() {
		return ConfigurationManager.getInstance().getServicesUrl() + "youTubeProvider";
	}
}
