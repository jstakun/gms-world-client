
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
    private static final int ADMOB = 1;
    private static final int TAPFORTAP = 2;
    private static final int AMZADS = 3;
    
    public static void loadAd(Activity activity) {
    	int adsProvider = ConfigurationManager.getInstance().getInt(ConfigurationManager.ADS_PROVIDER, AMZADS);
        if (adsProvider != NONE && OsUtil.isDonutOrHigher()) {
            if (adsProvider == TAPFORTAP) {
                TapForTapUtils.loadAd(activity);
            } else if (adsProvider == AMZADS) {
                AmazonUtils.loadAd(activity);
            } else if (adsProvider == ADMOB)  {
                AdMobUtils.loadAd(activity);
            } 
        }
    }

    public static void destroyAdView(Activity activity) {
    	int adsProvider = ConfigurationManager.getInstance().getInt(ConfigurationManager.ADS_PROVIDER, AMZADS);
    	if (adsProvider != NONE && OsUtil.isDonutOrHigher()) {
    		if (adsProvider == TAPFORTAP) {
    			TapForTapUtils.destroyAdView(activity);
    		} else if (adsProvider == AMZADS) {
    			AmazonUtils.destroyAdView(activity);
    		} else if (adsProvider == ADMOB) {
    			AdMobUtils.destroyAdView(activity);
    		}
    	}
    }
}
