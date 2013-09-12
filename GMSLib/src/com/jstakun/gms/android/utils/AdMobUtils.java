/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import android.app.Activity;
import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author jstakun
 */
public class AdMobUtils {

    private static AdRequest adReq = null;
    private static final SimpleDateFormat fbFormat = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat ggFormat = new SimpleDateFormat("yyyy-MM-dd");

    protected static void loadAd(Activity activity) {
        AdView adView = (AdView) activity.findViewById(R.id.adView);
        if (adReq == null) {
            adReq = new AdRequest();
            //adReq.addTestDevice(AdRequest.TEST_EMULATOR);
            if (ConfigurationManager.getInstance().containsKey(ConfigurationManager.FB_GENDER)) {
                String gender = ConfigurationManager.getInstance().getString(ConfigurationManager.FB_GENDER);
                if (gender == null) {
                    gender = ConfigurationManager.getInstance().getString(ConfigurationManager.GL_GENDER);
                }
                if (gender != null) {
                    if (gender.toLowerCase().equals("male")) {
                        adReq.setGender(AdRequest.Gender.MALE);
                    } else if (gender.toLowerCase().equals("female")) {
                        adReq.setGender(AdRequest.Gender.FEMALE);
                    }
                }
            }

            if (ConfigurationManager.getInstance().getLocation() != null) {
                adReq.setLocation(ConfigurationManager.getInstance().getLocation());
            }

            if (ConfigurationManager.getInstance().containsKey(ConfigurationManager.FB_BIRTHDAY)) {
                Date birthday = null;
                String birthdayStr = ConfigurationManager.getInstance().getString(ConfigurationManager.FB_BIRTHDAY);
                if (birthdayStr != null) {
                    try {
                        birthday = fbFormat.parse(birthdayStr);
                    } catch (Exception e) {
                    }
                }

                if (birthday == null) {
                    birthdayStr = ConfigurationManager.getInstance().getString(ConfigurationManager.GL_BIRTHDAY);
                    if (birthdayStr != null) {
                        try {
                            birthday = ggFormat.parse(birthdayStr);
                        } catch (Exception e) {
                        }
                    }
                }

                if (birthday != null) {
                    adReq.setBirthday(birthday);
                }
            }
        }

        adView.loadAd(adReq);
    }

    protected static void destroyAdView(Activity activity) {
        AdView adView = (AdView) activity.findViewById(R.id.adView);

        if (adView != null) {
            adView.destroy();
        }
    }
}
