package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class FlickrReader extends AbstractSerialReader {
	
	@Override
	protected String getUri() {
		return "flickrProvider";
	}

	@Override
	public String getLayerName(boolean formatted) {
		return Commons.FLICKR_LAYER;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Flickr_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.flickr;
	}

	@Override
	public int getLargeIconResource() {
		return -1;
	}

	@Override
	public int getImageResource() {
		return R.drawable.flickr_128;
	}
	
	@Override
	public int getPriority() {
		return 13;
	}
}
