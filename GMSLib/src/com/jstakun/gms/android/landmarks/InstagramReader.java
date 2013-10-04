package com.jstakun.gms.android.landmarks;

import java.util.List;

import org.apache.http.client.utils.URLEncodedUtils;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;

public class InstagramReader extends AbstractSerialReader {

	@Override
	public String readLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ?, ?> task) {
		String url = ConfigurationManager.getInstance().getServicesUrl() + "instagramProvider?" + URLEncodedUtils.format(params, "UTF-8");
		return parser.parse(url, landmarks, task, true, null);
	}
}
