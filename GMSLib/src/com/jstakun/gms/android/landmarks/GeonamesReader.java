package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class GeonamesReader extends AbstractSerialReader {
	@Override
	protected String getUri() {
		return "geonamesProvider";
	}

	@Override
	public String getLayerName(boolean formatted) {
		return Commons.WIKIPEDIA_LAYER;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Wikipedia_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.wikipedia;
	}

	@Override
	public int getLargeIconResource() {
		return -1;
	}

	@Override
	public int getImageResource() {
		return R.drawable.wikipedia_128;
	}
	
	@Override
	public int getPriority() {
		return 12;
	}
}
