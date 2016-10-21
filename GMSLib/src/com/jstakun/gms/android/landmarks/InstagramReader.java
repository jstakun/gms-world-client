package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

public class InstagramReader extends AbstractSerialReader {
	@Override
	protected String getUri() {
		return "instagramProvider";
	}
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.INSTAGRAM_LAYER;
	}
	
	@Override
	public boolean isEnabled() {
    	return false;
    }

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Instagram_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.instagram_16;
	}

	@Override
	public int getLargeIconResource() {
		return -1;
	}

	@Override
	public int getImageResource() {
		return R.drawable.instagram_128;
	}
}
