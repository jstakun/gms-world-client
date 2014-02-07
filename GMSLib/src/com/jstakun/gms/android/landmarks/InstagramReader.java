package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;


public class InstagramReader extends AbstractSerialReader {
	@Override
	protected String getUrl() {
		return ConfigurationManager.getInstance().getServerUrl() + "instagramProvider";
	}
}
