/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ads;

import java.util.Calendar;
import java.util.Date;

import android.app.Activity;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.tapfortap.Banner;
import com.tapfortap.TapForTap;
import com.tapfortap.TapForTap.Gender;

/**
 *
 * @author jstakun
 */
public class TapForTapUtils {

    private static final String fbFormat = "yyyyMMdd";
    private static final String ggFormat = "yyyy-MM-dd";

    protected static void loadAd(final Activity activity) {
    	TapForTap.enableTapForTap();
    	
        final Banner adView = (Banner) activity.findViewById(R.id.adView);
        adView.setListener(new Banner.BannerListener() {
            @Override
            public void bannerOnReceive(Banner Banner) {
            	//System.out.println("BannerOnReceive");
            }

            @Override
            public void bannerOnFail(Banner Banner, String s, Throwable t) {
            	//System.out.println("BannerOnFail");
            	if (TapForTap.isEnabled()) {
            		LoggerUtils.error("TapForTapUtils.BannerOnFail() exception:", t);
            	}
            }

            @Override
            public void bannerOnTap(Banner Banner) {
            	//System.out.println("BannerOnTap");
            }
        });
        
        if (ConfigurationManager.getInstance().containsKey(ConfigurationManager.FB_GENDER)) {
            String gender = ConfigurationManager.getInstance().getString(ConfigurationManager.FB_GENDER);
            if (gender == null) {
                gender = ConfigurationManager.getInstance().getString(ConfigurationManager.GL_GENDER);
            }
            if (gender != null) {
                if (gender.toLowerCase(java.util.Locale.US).equals("male")) {
                    TapForTap.setGender(Gender.MALE);
                } else if (gender.toLowerCase(java.util.Locale.US).equals("female")) {
                	TapForTap.setGender(Gender.FEMALE);
                }
            }
        }
        if (ConfigurationManager.getInstance().getLocation() != null) {
        	TapForTap.setLocation(ConfigurationManager.getInstance().getLocation());
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
                try {
                    int yob = getYearOfBirth(birthday);
                    TapForTap.setYearOfBirth(yob);
                } catch (Exception e) {
                }
            }
        }
    }

    protected static void destroyAdView(Activity activity) {
        //Banner adView = (Banner) activity.findViewById(R.id.adView);     
    	TapForTap.disableTapForTap();
    }

    /*private static int getAge(Date dateOfBirth) {
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
    }*/
    
    private static int getYearOfBirth(Date dateOfBirth) {
    	Calendar dob = Calendar.getInstance();
        dob.setTime(dateOfBirth);
        return dob.get(Calendar.YEAR);
    }
}
