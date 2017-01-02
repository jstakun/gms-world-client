package com.jstakun.gms.android.social;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.OsUtil;

/**
 *
 * @author jstakun
 */
public class OAuthServiceFactory {

    public static final String CALLBACK_URL = ConfigurationManager.SERVER_URL + "m/oauth_logon_confirmation.jsp";
    public static final String CALLBACK_ERROR_URL = ConfigurationManager.SERVER_URL + "m/oauth_logon_error.jsp";
    private static GoogleUtils googleUtils = null;
    private static LinkedInUtils linkedinUtils = null;
    private static TwitterUtils twitterUtils = null;
    private static FacebookUtils facebookUtils = null;
    private static FoursquareUtils foursquareUtils = null;

    public static String getOAuthString(String service) {   	
    	if (StringUtils.isNotEmpty(service)) {
    		if (ConfigurationManager.getUserManager().isTokenPresent() && OsUtil.isFroyoOrHigher())
    			return ConfigurationManager.getInstance().getServerUrl() + service + "login";
    		else {
    			return ConfigurationManager.SERVER_URL + service + "login";
    		}
    	} else {
    		return null;
    	}
    }
    
    public static String getOAuthCallback(String service) {
    	if (service.equals(Commons.LINKEDIN)) {
    		return ConfigurationManager.getInstance().getSecuredServerUrl() + "lnauth";
    	} else if (service.equals(Commons.TWITTER)) {
    		return ConfigurationManager.getInstance().getSecuredServerUrl() + "twauth";
    	} else if (service.equals(Commons.GOOGLE)) { 
    		return ConfigurationManager.getInstance().getSecuredServerUrl() + "glauth";
    	} else if (service.equals(Commons.FOURSQUARE)) {
    		return ConfigurationManager.getInstance().getSecuredServerUrl() + "fsauth";
    	} else if (service.equals(Commons.FACEBOOK)) {
    		return ConfigurationManager.getInstance().getSecuredServerUrl() + "fbauth";
    	}
    	return null;
    }
    
    
    public static ISocialUtils getSocialUtils(String service) {
        if (service.equals(Commons.GOOGLE)) {
            if (googleUtils == null) {
                googleUtils = new GoogleUtils();
            }
            return googleUtils;
        } else if (service.equals(Commons.LINKEDIN)) {
            if (linkedinUtils == null) {
                linkedinUtils = new LinkedInUtils();
            }
            return linkedinUtils;
        } else if (service.equals(Commons.TWITTER)) {
            if (twitterUtils == null) {
                twitterUtils = new TwitterUtils();
            }
            return twitterUtils;
        } else if (service.equals(Commons.FACEBOOK) || service.equals(Commons.FACEBOOK_LAYER)) {
            if (facebookUtils == null) {
                facebookUtils = new FacebookUtils();
            }
            return facebookUtils;
        } else if (service.equals(Commons.FOURSQUARE) || service.equals(Commons.FOURSQUARE_LAYER) || service.equals(Commons.FOURSQUARE_MERCHANT_LAYER)) {
            if (foursquareUtils == null) {
                foursquareUtils = new FoursquareUtils();
            }
            return foursquareUtils;
        }

        return null;
    }

    public static String getServiceName(String service) {
        if (service.equals(Commons.GOOGLE)) {
            return "Google+";
        } else if (service.equals(Commons.LINKEDIN)) {
            return "LinkedIn";
        } else if (service.equals(Commons.TWITTER)) {
            return "Twitter";
        } else if (service.equals(Commons.FACEBOOK)) {
            return "Facebook";
        } else if (service.equals(Commons.FOURSQUARE)) {
            return "Foursquare";
        } else {
            return null;
        }
    }

    public static String getUsername(String service) {
        if (service.equals(Commons.GOOGLE)) {
            return ConfigurationManager.getInstance().getString(ConfigurationManager.GL_USERNAME);
        } else if (service.equals(Commons.LINKEDIN)) {
            return ConfigurationManager.getInstance().getString(ConfigurationManager.LN_USERNAME);
        } else if (service.equals(Commons.TWITTER)) {
            return ConfigurationManager.getInstance().getString(ConfigurationManager.TWEET_USERNAME);
        } else if (service.equals(Commons.FACEBOOK)) {
            return ConfigurationManager.getInstance().getString(ConfigurationManager.FB_USERNAME);
        } else if (service.equals(Commons.FOURSQUARE)) {
            return ConfigurationManager.getInstance().getString(ConfigurationManager.FS_USERNAME);
        } else if (service.equals(Commons.GMS_WORLD)) {
            return ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_USERNAME); 
        } else {
            return null;
        }
    }
    
    public static String getDisplayname(String service) {
        String displayName = null;
        
        if (service.equals(Commons.GOOGLE)) {
            displayName = ConfigurationManager.getInstance().getString(ConfigurationManager.GL_NAME);
        } else if (service.equals(Commons.LINKEDIN)) {
            displayName = ConfigurationManager.getInstance().getString(ConfigurationManager.LN_NAME);
        } else if (service.equals(Commons.TWITTER)) {
            displayName = ConfigurationManager.getInstance().getString(ConfigurationManager.TWEET_NAME);
        } else if (service.equals(Commons.FACEBOOK)) {
            displayName = ConfigurationManager.getInstance().getString(ConfigurationManager.FB_NAME);
        } else if (service.equals(Commons.FOURSQUARE)) {
            displayName = ConfigurationManager.getInstance().getString(ConfigurationManager.FS_NAME);
        } else if (service.equals(Commons.GMS_WORLD)) {
        	displayName = ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_NAME);
        }
        
        if (StringUtils.isEmpty(displayName)) {
            displayName = getUsername(service);
        }
        
        return displayName;
    }
    
    public static String getLoginDate(String service) {
    	long loginDate = -1;
    	if (service.equals(Commons.GOOGLE)) {
    		loginDate = ConfigurationManager.getInstance().getLong(ConfigurationManager.GL_LOGIN_DATE);
        } else if (service.equals(Commons.LINKEDIN)) {
        	loginDate = ConfigurationManager.getInstance().getLong(ConfigurationManager.LN_LOGIN_DATE);
        } else if (service.equals(Commons.TWITTER)) {
        	loginDate = ConfigurationManager.getInstance().getLong(ConfigurationManager.TWEET_LOGIN_DATE);
        } else if (service.equals(Commons.FACEBOOK)) {
        	loginDate = ConfigurationManager.getInstance().getLong(ConfigurationManager.FB_LOGIN_DATE);
        } else if (service.equals(Commons.FOURSQUARE)) {
        	loginDate = ConfigurationManager.getInstance().getLong(ConfigurationManager.FS_LOGIN_DATE);
        } else if (service.equals(Commons.GMS_WORLD)) {
        	loginDate = ConfigurationManager.getInstance().getLong(ConfigurationManager.GMS_LOGIN_DATE);
        }
    	
    	if (loginDate > 0) {
    		java.util.Locale currentLocale = ConfigurationManager.getInstance().getCurrentLocale();
    		return DateTimeUtils.getShortDateTimeString(loginDate, currentLocale); 
    	} else {
    		return null;
    	}
    }
}
