package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.ProjectionInterface;
import com.jstakun.gms.android.utils.StringUtil;

/**
 *
 * @author jstakun
 */
public class OsmReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
        BoundingBox bbox = (BoundingBox) ConfigurationManager.getInstance().getObject(BoundingBox.BBOX, BoundingBox.class);
		if (bbox != null) {
			params.put("longitudeMin", StringUtil.formatCoordE2(bbox.west));
			params.put("latitudeMin", StringUtil.formatCoordE2(bbox.south));
			params.put("longitudeMax", StringUtil.formatCoordE2(bbox.east));  
			params.put("latitudeMax", StringUtil.formatCoordE2(bbox.north));
		}
    }
	
	@Override
	public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
		init(latitude, longitude, zoom, width, height);
		String amenity = "atm"; //Commons.OSM_ATM_LAYER
        if (layer.equals(Commons.OSM_PARKING_LAYER)){
            amenity = "parking";
        }
        params.put("amenity", amenity);
	    return parser.parse(getUrls(), 0, params, landmarks, task, true, layer, false);	
    }

	@Override
	protected String getUri() {
		return "osmProvider";
	}
}
