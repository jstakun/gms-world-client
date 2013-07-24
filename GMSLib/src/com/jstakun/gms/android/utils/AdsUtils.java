
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import com.jstakun.gms.android.config.ConfigurationManager;

import android.app.Activity;

/**
 *
 * @author jstakun
 */
public class AdsUtils {

    private static final int ADMOB = 0;
    private static final int TAPFORTAP = 1;
    //TODO set to one of above
    private static final int selected = ADMOB; //TAPFORTAP;

    public static void loadAd(Activity activity) {
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.SHOW_ADS)) {
            if (selected == TAPFORTAP) {
                TapForTapUtils.loadAd(activity);
            } else {
                AdMobUtils.loadAd(activity);
            }
        }
    }

    public static void destroyAdView(Activity activity) {
        if (selected == TAPFORTAP) {
            TapForTapUtils.destroyAdView(activity);
        } else {
            AdMobUtils.destroyAdView(activity);
        }
    }
}
