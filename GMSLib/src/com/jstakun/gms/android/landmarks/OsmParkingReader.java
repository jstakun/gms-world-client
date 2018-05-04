package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.GMSAsyncTask;

public class OsmParkingReader extends OsmReader {

	@Override
	public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, String layer, GMSAsyncTask<?, ? ,?> task) {
		init(latitude, longitude, zoom);
		params.put("amenity", "parking");
	    return parser.parse(getUrls(), 0, params, landmarks, task, true, layer, false);	
    }
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.OSM_PARKING_LAYER;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_OSM_Parkings_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.parking;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.parking_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.parking_img;
	}

}
