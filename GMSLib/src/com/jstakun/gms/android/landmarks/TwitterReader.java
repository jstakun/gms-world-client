package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class TwitterReader extends AbstractSerialReader {

    @Override
	protected String getUri() {
		return "twitterProvider";
	}
    
    @Override
    public String getLayerName(boolean formatted) {
		return Commons.TWITTER_LAYER;
	}
    
    @Override
	public int getDescriptionResource() {
		return R.string.Layer_Twitter_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.twitter;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.twitter_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.twitter_128;
	}
	
	@Override
	public int getPriority() {
		return 7;
	}
}
