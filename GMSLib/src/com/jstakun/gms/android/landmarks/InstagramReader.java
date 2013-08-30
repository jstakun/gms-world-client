package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;

public class InstagramReader extends AbstractSerialReader {

	@Override
	public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ?, ?> task) {
		init(latitude, longitude, zoom, width, height);
		
		String url = ConfigurationManager.getInstance().getServicesUrl() + "instagramProvider?" +
				"lat=" + coords[0] + "&lng=" + coords[1] + "&radius=" + radius  +
				"&limit=" + limit + "&display=" + display + "&format=bin";
		
		return parser.parse(url, landmarks, task, true);
	}

}
