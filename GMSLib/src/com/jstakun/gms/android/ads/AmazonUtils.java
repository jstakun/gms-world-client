package com.jstakun.gms.android.ads;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdListener;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.AdTargetingOptions;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.LoggerUtils;

public class AmazonUtils {
	
	private static final SimpleDateFormat fbFormat = new SimpleDateFormat("yyyyMMdd", java.util.Locale.US);
    private static final SimpleDateFormat ggFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
	
	protected static void loadAd(final Activity activity) {
		//change to false in production
		// For debugging purposes enable logging, but disable for production builds
        AdRegistration.enableLogging(false);
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
			public void onAdFailedToLoad(Ad arg0, AdError arg1) {
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
                	Calendar dob = Calendar.getInstance();  
                	dob.setTime(birthday);  
                	Calendar today = Calendar.getInstance();  
                	int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);  
                	if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH)) {
                	  age--;  
                	} else if (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH)
                	    && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH)) {
                	  age--;  
                	}
                    adOptions.setAge(age);
                }
            }
        
        //This Gender information is no longer used for targeting
        /*if (ConfigurationManager.getInstance().containsKey(ConfigurationManager.FB_GENDER)) {
            String gender = ConfigurationManager.getInstance().getString(ConfigurationManager.FB_GENDER);
            if (gender == null) {
               gender = ConfigurationManager.getInstance().getString(ConfigurationManager.GL_GENDER);
            }
            if (gender != null) {
               if (StringUtils.equalsIgnoreCase(gender, "male")) {
                    	adOptions.setGender(AdTargetingOptions.Gender.MALE);
                } else if (StringUtils.equalsIgnoreCase(gender, "female")) {
                        adOptions.setGender(AdTargetingOptions.Gender.FEMALE);
                }
            }
        }*/
        
        
        adView.loadAd(adOptions);
	}
	
	protected static void destroyAdView(Activity activity) {
		AdLayout adView = (AdLayout) activity.findViewById(R.id.adView);
		adView.destroy();
	}
}
