package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;

public class LandmarkDBReader implements LayerReader {

	@Override
	public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ?, ?> task) {
		landmarks.addAll(ConfigurationManager.getDatabaseManager().getLandmarkDatabase());
		return null;
	}

	@Override
	public void close() {
		
	}

	@Override
	public String[] getUrlPrefix() {
		return null;
	}

}
