package com.jstakun.gms.android.ads;

import android.app.Activity;

public interface AdsProvider {

	public void loadAd(Activity activity);
	
	public void destroyAdView(Activity activity);
}
