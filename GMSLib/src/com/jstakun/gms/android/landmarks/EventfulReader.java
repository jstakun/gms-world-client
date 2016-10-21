package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */

public class EventfulReader extends AbstractSerialReader {

	@Override
	protected String getUri() {
		return "eventfulProvider";
	}

	@Override
	public String getLayerName(boolean formatted) {
		return Commons.EVENTFUL_LAYER;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Eventful_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.eventful;
	}

	@Override
	public int getLargeIconResource() {
		return -1;
	}

	@Override
	public int getImageResource() {
		return R.drawable.eventful_128;
	}
	
	@Override
	public int getPriority() {
		return 10;
	}
}
