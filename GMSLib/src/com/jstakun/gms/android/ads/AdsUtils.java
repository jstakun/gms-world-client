
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ads;

import com.jstakun.gms.android.config.ConfigurationManager;

import android.app.Activity;

/**
 *
 * @author jstakun
 */
public class AdsUtils {

    private static final int ADMOB = 0;
    private static final int TAPFORTAP = 1;
    private static final int AMZADS = 2;
    //TODO set to one of above
    private static final int selected = AMZADS;

    public static void loadAd(Activity activity) {
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.SHOW_ADS)) {
            if (selected == TAPFORTAP) {
                TapForTapUtils.loadAd(activity);
            } else if (selected == AMZADS) {
                AmazonUtils.loadAd(activity);
            } else if (selected == ADMOB)  {
                AdMobUtils.loadAd(activity);
            }
        }
    }

    public static void destroyAdView(Activity activity) {
        if (selected == TAPFORTAP) {
            TapForTapUtils.destroyAdView(activity);
        } else if (selected == AMZADS) {
            AmazonUtils.destroyAdView(activity);
        } else if (selected == ADMOB) {
            AdMobUtils.destroyAdView(activity);
        }
    }
}
