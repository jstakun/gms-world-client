package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.ProjectionInterface;
import com.jstakun.gms.android.utils.StringUtil;

/**
 *
 * @author jstakun
 */
public class PicasaReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		ProjectionInterface projection = (ProjectionInterface) ConfigurationManager.getInstance().getObject(ProjectionInterface.TAG, ProjectionInterface.class); 
		if (projection != null) {
			BoundingBox bbox = projection.getBoundingBox();
			if (bbox != null) {
				params.put("bbox", StringUtil.formatCoordE2(bbox.west) + "," + StringUtil.formatCoordE2(bbox.south) + "," +
								   StringUtil.formatCoordE2(bbox.east) + "," + StringUtil.formatCoordE2(bbox.north));
			}
		}	
	}

	@Override
	protected String getUri() {
		return "picasaProvider";
	}
}
