package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class MeetupReader extends AbstractSerialReader {
    @Override
	protected String getUri() {
		return "meetupProvider";
	}
    
    @Override
    public String getLayerName(boolean formatted) {
		return Commons.MEETUP_LAYER;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Meetup_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.meetup;
	}

	@Override
	public int getLargeIconResource() {
		return -1;
	}

	@Override
	public int getImageResource() {
		return R.drawable.meetup_128;
	}
	
	@Override
	public int getPriority() {
		return 10;
	}
}
