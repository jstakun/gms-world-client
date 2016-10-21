package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class LastFmReader extends AbstractSerialReader {

    @Override
	protected String getUri() {
		return "lastfmProvider";
	}
    
    @Override
    public String getLayerName(boolean formatted) {
		return Commons.LASTFM_LAYER;
	}
    
    @Override
	public boolean isEnabled() {
    	return false;
    }

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_LastFM_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.last_fm;
	}

	@Override
	public int getLargeIconResource() {
		return -1;
	}

	@Override
	public int getImageResource() {
		return R.drawable.lastfm_128;
	}
}
