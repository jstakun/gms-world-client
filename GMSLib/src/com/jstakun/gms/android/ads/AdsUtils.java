package com.jstakun.gms.android.ads;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.OsUtil;

import android.app.Activity;

/**
 *
 * @author jstakun
 */
public class AdsUtils {

	private static final int NONE = 0;
    //private static final int ADMOB = 1;
    private static final int TAPFORTAP = 2;
    private static final int AMZADS = 3;
    
    
    
    public static void loadAd(Activity activity) {
    	AdsProvider adsProvider = getAdsProvider();
    	if (adsProvider != null) {
    		adsProvider.loadAd(activity);
    	}
    }

    public static void destroyAdView(Activity activity) {
    	AdsProvider adsProvider = getAdsProvider();
    	if (adsProvider != null) {
    		adsProvider.destroyAdView(activity);
    	}
    }
    
    private static AdsProvider getAdsProvider() {
    	int adsProvider = ConfigurationManager.getInstance().getInt(ConfigurationManager.ADS_PROVIDER, AMZADS);
    	if (adsProvider != NONE && OsUtil.isDonutOrHigher()) {
    		if (adsProvider == TAPFORTAP) {
    			return AdsFactory.getTapForTapInstance();
    		} else if (adsProvider == AMZADS) {
    			return AdsFactory.getAmazonInstance();
    		} else {
    			return null;
    		}
    	} else {
    		return null;
    	}
    }
}
