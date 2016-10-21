package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.StringUtil;

/**
 *
 * @author jstakun
 */
public class PicasaReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		BoundingBox bbox = (BoundingBox) ConfigurationManager.getInstance().getObject(BoundingBox.BBOX, BoundingBox.class);
		if (bbox != null) {
			params.put("bbox", StringUtil.formatCoordE2(bbox.west) + "," + StringUtil.formatCoordE2(bbox.south) + "," +
							   StringUtil.formatCoordE2(bbox.east) + "," + StringUtil.formatCoordE2(bbox.north));
		}	
	}

	@Override
	protected String getUri() {
		return "picasaProvider";
	}
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.PICASA_LAYER;
	}
	
	@Override
	public boolean isEnabled() {
    	return false;
    }

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Picasa_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.picasa_icon;
	}

	@Override
	public int getLargeIconResource() {
		return -1;
	}

	@Override
	public int getImageResource() {
		return R.drawable.picasa_128;
	}
}
