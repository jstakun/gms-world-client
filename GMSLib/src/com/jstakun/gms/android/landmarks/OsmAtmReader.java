package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.GMSAsyncTask;

public class OsmAtmReader extends OsmReader {

	@Override
	public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
		init(latitude, longitude, zoom, width, height);
		params.put("amenity", "atm");
	    return parser.parse(getUrls(), 0, params, landmarks, task, true, layer, false);	
    }
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.OSM_ATM_LAYER;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_OSM_ATMs_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.credit_card_16;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.credit_card_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.atm_128;
	}

}
