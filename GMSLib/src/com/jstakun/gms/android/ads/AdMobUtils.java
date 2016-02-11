package com.jstakun.gms.android.ads;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import android.app.Activity;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DateTimeUtils;

/**
 *
 * @author jstakun
 */
public class AdMobUtils {

    private static AdRequest adReq = null;
    private static final String fbFormat = "yyyyMMdd";
    private static final String ggFormat = "yyyy-MM-dd";

    protected synchronized static void loadAd(Activity activity) {
        AdView adView = (AdView) activity.findViewById(R.id.adView);
        if (adReq == null) {
        	AdRequest.Builder adReqBuilder = new AdRequest.Builder();
            //adReq.addTestDevice(AdRequest.TEST_EMULATOR);
            if (ConfigurationManager.getInstance().containsKey(ConfigurationManager.FB_GENDER)) {
                String gender = ConfigurationManager.getInstance().getString(ConfigurationManager.FB_GENDER);
                if (gender == null) {
                    gender = ConfigurationManager.getInstance().getString(ConfigurationManager.GL_GENDER);
                }
                if (gender != null) {
                    if (StringUtils.equalsIgnoreCase(gender, "male")) {
                        adReqBuilder.setGender(AdRequest.GENDER_MALE);
                    } else if (StringUtils.equalsIgnoreCase(gender, "female")) {
                        adReqBuilder.setGender(AdRequest.GENDER_FEMALE);
                    }
                }
            }

            if (ConfigurationManager.getInstance().getLocation() != null) {
                adReqBuilder.setLocation(ConfigurationManager.getInstance().getLocation());
            }

            if (ConfigurationManager.getInstance().containsKey(ConfigurationManager.FB_BIRTHDAY)) {
                Date birthday = null;
                String birthdayStr = ConfigurationManager.getInstance().getString(ConfigurationManager.FB_BIRTHDAY);
                if (birthdayStr != null) {
                    try {
                        birthday = DateTimeUtils.parseDate(fbFormat, birthdayStr);
                    } catch (Exception e) {
                    }
                }

                if (birthday == null) {
                    birthdayStr = ConfigurationManager.getInstance().getString(ConfigurationManager.GL_BIRTHDAY);
                    if (birthdayStr != null) {
                        try {
                            birthday = DateTimeUtils.parseDate(ggFormat, birthdayStr);
                        } catch (Exception e) {
                        }
                    }
                }

                if (birthday != null) {
                    adReqBuilder.setBirthday(birthday);
                }
            }
            adReq = adReqBuilder.build();
        }

        adView.loadAd(adReq);
    }

    protected static void destroyAdView(Activity activity) {
        AdView adView = (AdView) activity.findViewById(R.id.adView);

        if (adView != null) {
            //WebView.destroy() called while still attached!
        	final ViewGroup viewGroup = (ViewGroup) adView.getParent();
            if (viewGroup != null)
            {
            	viewGroup.removeView(adView);
            }
        	adView.destroy();
        }
    }
}
