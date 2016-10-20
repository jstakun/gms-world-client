package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.utils.GMSAsyncTask;

public class OsmTaxiReader extends OsmReader {

	@Override
	public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
		init(latitude, longitude, zoom, width, height);
		params.put("amenity", "taxi");
	    return parser.parse(getUrls(), 0, params, landmarks, task, true, layer, false);	
    }
	
	@Override
	protected String getLayerName() {
		return Commons.OSM_TAXI_LAYER;
	}

}
