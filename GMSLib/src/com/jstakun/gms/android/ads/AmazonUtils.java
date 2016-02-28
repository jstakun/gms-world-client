package com.jstakun.gms.android.ads;

import android.app.Activity;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdListener;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.AdTargetingOptions;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.LoggerUtils;

public class AmazonUtils {
	
	protected static void loadAd(final Activity activity) {
		// For debugging purposes enable logging, but disable for production builds
        if (LoggerUtils.isDebug()) {
        	AdRegistration.enableLogging(true);
        } else {
        	AdRegistration.enableLogging(false);
        }
        // For debugging purposes flag all ad requests as tests, but set to false for production builds
        AdRegistration.enableTesting(false);
        
        AdLayout adView = (AdLayout)activity.findViewById(R.id.adView);
        adView.setListener(new AdListener() {

			@Override
			public void onAdCollapsed(Ad arg0) {
			}

			@Override
			public void onAdDismissed(Ad arg0) {
			}

			@Override
			public void onAdExpanded(Ad arg0) {
			}

			@Override
			public void onAdFailedToLoad(Ad arg0, AdError error) {
				LoggerUtils.error("Amazon Ad loading error " + error.getMessage());
			}

			@Override
			public void onAdLoaded(Ad arg0, AdProperties arg1) {
			}      	
        });
        
        try {
            AdRegistration.setAppKey(activity.getResources().getString(R.string.amzAppKey));
        } catch (Exception e) {
            LoggerUtils.error("AmazonUtils.loadAd() exception:", e);
        }
        
        AdTargetingOptions adOptions = new AdTargetingOptions();
        
        adOptions.enableGeoLocation(true);
        
        adView.loadAd(adOptions);
	}
	
	protected static void destroyAdView(Activity activity) {
		AdLayout adView = (AdLayout) activity.findViewById(R.id.adView);
		adView.destroy();
	}
}
