package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class MastercardAtmReader extends AbstractSerialReader {
	@Override
	protected String getUri() {
		return "atmProvider";
	}
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.MC_ATM_LAYER;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_MasterCard_ATMs_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.mastercard;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.mastercard_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.mastercard_128;
	}
	
	@Override
	public int getPriority() {
		return 8;
	}
}
