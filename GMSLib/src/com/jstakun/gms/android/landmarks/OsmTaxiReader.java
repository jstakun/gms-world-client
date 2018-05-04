package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.GMSAsyncTask;

public class OsmTaxiReader extends OsmReader {

	@Override
	public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, String layer, GMSAsyncTask<?, ? ,?> task) {
		init(latitude, longitude, zoom);
		params.put("amenity", "taxi");
	    return parser.parse(getUrls(), 0, params, landmarks, task, true, layer, false);	
    }
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.OSM_TAXI_LAYER;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_OSM_Taxi_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.taxi16;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.taxi24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.taxi128;
	}

}
