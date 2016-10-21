package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class ExpediaReader extends AbstractSerialReader {
    
	@Override
	protected String getUri() {
		return "expediaProvider";
	}

	@Override
	public String getLayerName(boolean formatted) {
		return Commons.EXPEDIA_LAYER;
	}
	
	@Override
	public boolean isEnabled() {
    	return false;
    }

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Expedia_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.expedia;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.expedia_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.expedia_128;
	}
}
