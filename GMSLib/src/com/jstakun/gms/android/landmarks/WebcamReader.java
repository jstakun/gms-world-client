package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class WebcamReader extends AbstractSerialReader  {

    @Override
	protected String getUri() {
		return "webcamProvider";
	}
    
    @Override
    public String getLayerName(boolean formatted) {
		return Commons.WEBCAM_LAYER;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Travel_Webcams_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.webcam;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.webcam_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.webcam_128;
	}
	
	@Override
	public int getPriority() {
		return 14;
	}
}
