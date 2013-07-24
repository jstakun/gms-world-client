/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import android.app.Activity;
import android.widget.LinearLayout.LayoutParams;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.tapfortap.AdView;
import com.tapfortap.AdView.AdViewListener;
import com.tapfortap.AdView.Gender;
import com.tapfortap.TapForTap;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author jstakun
 */
public class TapForTapUtils {

    private static final SimpleDateFormat fbFormat = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat ggFormat = new SimpleDateFormat("yyyy-MM-dd");

    protected static void loadAd(final Activity activity) {
        TapForTap.setDefaultAppId(Commons.TAPFORTAP_ID);
        TapForTap.checkIn(activity);

        final AdView adView = (AdView) activity.findViewById(R.id.adView);
        //adView.setVisibility(View.GONE);
        adView.setListener(new AdViewListener() {
            public void didReceiveAd() {
                //System.out.println("AdViewListener.didReceiveAd() --------------------------------------------------");
                float scale = activity.getResources().getDisplayMetrics().density;
                adView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, (int)(50*scale)));
                //adView.setVisibility(View.VISIBLE);
            }
            public void didFailToReceiveAd(String reason) {
                //System.out.println("AdViewListener.didFailToReceiveAd(): " + reason);
            }
        });
            
        if (ConfigurationManager.getInstance().containsKey(ConfigurationManager.FB_GENDER)) {
            String gender = ConfigurationManager.getInstance().getString(ConfigurationManager.FB_GENDER);
            if (gender == null) {
                gender = ConfigurationManager.getInstance().getString(ConfigurationManager.GL_GENDER);
            }
            if (gender != null) {
                if (gender.toLowerCase().equals("male")) {
                    adView.setGender(Gender.MALE);
                } else if (gender.toLowerCase().equals("female")) {
                    adView.setGender(Gender.FEMALE);
                }
            }
        }
        if (ConfigurationManager.getInstance().getLocation()
                != null) {
            adView.setLocation(ConfigurationManager.getInstance().getLocation());
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
                try {
                    int age = getAge(birthday);
                    adView.setAge(age);
                } catch (Exception e) {
                }
            }
        }

        adView.loadAds();
    }

    protected static void destroyAdView(Activity activity) {
        AdView adView = (AdView) activity.findViewById(R.id.adView);
        adView.stopLoadingAds();
    }

    private static int getAge(Date dateOfBirth) {
        Calendar now = Calendar.getInstance();
        Calendar dob = Calendar.getInstance();
        dob.setTime(dateOfBirth);
        if (dob.after(now)) {
            throw new IllegalArgumentException("Can't be born in the future");
        }
        int age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (now.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }
}
