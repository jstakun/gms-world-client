package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

public class YouTubeReader extends AbstractSerialReader {

	@Override
	protected String getUri() {
		return "youTubeProvider";
	}
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.YOUTUBE_LAYER;
	}
	
	@Override
	public int getDescriptionResource() {
		return R.string.Layer_YouTube_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.youtube_icon;
	}

	@Override
	public int getLargeIconResource() {
		return -1;
	}

	@Override
	public int getImageResource() {
		return R.drawable.youtube_128;
	}
	
	@Override
	public int getPriority() {
		return 15;
	}
}
