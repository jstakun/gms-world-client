/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.tapfortap.Banner;
import com.tapfortap.TapForTap;
import com.tapfortap.TapForTap.Gender;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author jstakun
 */
public class TapForTapUtils {

    private static final SimpleDateFormat fbFormat = new SimpleDateFormat("yyyyMMdd", java.util.Locale.US);
    private static final SimpleDateFormat ggFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);

    protected static void loadAd(final Activity activity) {
    	/*TapForTap.initialize(activity, activity.getResources().getString(R.string.tapForTapKey), new TapForTap.InitializationListener() {
			
			@Override
			public void onSuccess(boolean arg0) {
				System.out.println("InitOnSuccess " + arg0);
			}
			
			@Override
			public void onFail(String arg0, Throwable t) {
				System.out.println("InitOnFail " + arg0);
            	LoggerUtils.error("InitOnFail", t);
			}
		});*/
    	TapForTap.enableTapForTap();
    	//TODO comment in production
    	//TapForTap.enableTestMode();
    	
        final Banner adView = (Banner) activity.findViewById(R.id.adView);
        adView.setListener(new Banner.BannerListener() {
            @Override
            public void bannerOnReceive(Banner Banner) {
            	System.out.println("BannerOnReceive");
                //float scale = activity.getResources().getDisplayMetrics().density;
                //int width = (int)(320 * scale);
                //int height = (int)(50 * scale);
                //c.setLayoutParams(new LayoutParams(width, height));
            	//adView.setVisibility(View.VISIBLE);
            }

            @Override
            public void bannerOnFail(Banner Banner, String s, Throwable t) {
            	System.out.println("BannerOnFail");
            	LoggerUtils.error("BannerOnFail", t);
            }

            @Override
            public void bannerOnTap(Banner Banner) {
            	System.out.println("BannerOnTap");
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
