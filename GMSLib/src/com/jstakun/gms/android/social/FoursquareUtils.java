package com.jstakun.gms.android.social;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.StringUtil;
import com.jstakun.gms.android.utils.Token;

/**
 *
 * @author jstakun
 */
public final class FoursquareUtils extends AbstractSocialUtils {

	private static final String FS_AUTH_KEY = "fsauth_key";
    
    protected FoursquareUtils() {
    	super();
        ConfigurationManager.getInstance().setDisabled(ConfigurationManager.FS_SEND_STATUS);
    }

    public void storeAccessToken(Token accessToken)  {
    	super.storeAccessToken(accessToken);
    	ConfigurationManager.getUserManager().putStringAndEncrypt(FS_AUTH_KEY, accessToken.getToken());
   }

    protected Token loadAccessToken() {
        String token = ConfigurationManager.getUserManager().getStringDecrypted(FS_AUTH_KEY);
        return new Token(token, null);
    }

    public void logout() {
        ConfigurationManager.getInstance().removeAll(new String[]{
                    FS_AUTH_KEY,
                    ConfigurationManager.FS_USERNAME,
                    ConfigurationManager.FS_NAME,
                    ConfigurationManager.FS_LOGIN_DATE
                });
        ConfigurationManager.getInstance().setOff(ConfigurationManager.FS_AUTH_STATUS);
        ConfigurationManager.getInstance().setOff(ConfigurationManager.FS_SEND_STATUS);

        super.logout();
    }

    public boolean initOnTokenPresent(JSONObject json) {
        boolean result = false;
        ConfigurationManager.getInstance().setOn(ConfigurationManager.FS_AUTH_STATUS);

        try {
        	String id = json.getString(ConfigurationManager.FS_USERNAME);
            ConfigurationManager.getInstance().putString(ConfigurationManager.FS_USERNAME, id + "@" + Commons.FOURSQUARE);
            ConfigurationManager.getInstance().putString(ConfigurationManager.GMS_TOKEN, json.getString(ConfigurationManager.GMS_TOKEN));
            String user = json.optString(ConfigurationManager.FS_NAME); 
            if (user != null) {
               ConfigurationManager.getInstance().putString(ConfigurationManager.FS_NAME, user);    
            } else {
               ConfigurationManager.getInstance().putString(ConfigurationManager.FS_NAME, id);
            }
            String email = json.optString(ConfigurationManager.USER_EMAIL);
			if (StringUtils.isNotEmpty(email)) {
				ConfigurationManager.getUserManager().putStringAndEncrypt(ConfigurationManager.USER_EMAIL, email);
			}
			
			ConfigurationManager.getInstance().putLong(ConfigurationManager.FS_LOGIN_DATE, System.currentTimeMillis());
			
			result = ConfigurationManager.getDatabaseManager().saveConfiguration(false);		
        } catch (Exception ex) {
            LoggerUtils.error("FoursquareUtils.sendPost exception", ex);
        } finally {
            if (!result) {
                logout();
            }
        }
        return result;
    }

    public String checkin(String venueId, String name, Double lat, Double lng) {
        //socialCheckin  accessToken, venueId, name, service
        HttpUtils utils = new HttpUtils();
		String message = null;

		try {
			String token = accessToken.getToken();
			if (token == null) {
				logout();
				throw new NullPointerException("Missing FS authentication token!");
			}
			String url = ConfigurationManager.getInstance().getSecuredServerUrl() + "socialCheckin";			
			Map<String, String> params = new HashMap<String, String>();
	    	params.put("accessToken", token);
		    params.put("venueId", venueId);
		    params.put("name", name);
		    params.put("service", Commons.FOURSQUARE);
		    params.put("lat", StringUtil.formatCoordE6(lat));
		    params.put("lng", StringUtil.formatCoordE6(lng));
		    utils.sendPostRequest(url, params, true);
		    
		    message = utils.getResponseErrorMessage(url);
		    int responseCode = utils.getResponseCode(url);
	        if (responseCode == HttpURLConnection.HTTP_OK) {
	           message = Locale.getMessage(R.string.Social_checkin_success, name);
	           onCheckin(venueId);
	        } else {
	           if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
	        	   logout();
	           }
	           message = Locale.getMessage(R.string.Social_checkin_failure, message);
	        }
		} catch (Exception ex) {
			LoggerUtils.error("FoursquareUtils.checkin() exception", ex);
			message = Locale.getMessage(R.string.Http_error, ex.getMessage());
		} finally {
			try {
				if (utils != null) {
					utils.close();
				}
			} catch (Exception e) {
			}
		}
		return message;
    }

    public String sendComment(String venueId, String tip, String name) {
    	//socialComment  accessToken, venueId, name
    	HttpUtils utils = new HttpUtils();
		String message = null;

		try {
			String token = accessToken.getToken();
			if (token == null) {
				logout();
				throw new NullPointerException("Missing FS authentication token!");
			}
			String url = ConfigurationManager.getInstance().getSecuredServerUrl() + "socialComment";			
			Map<String, String> params = new HashMap<String, String>();
	    	params.put("accessToken", token);
		    params.put("venueId", venueId);
		    params.put("text", tip);
		    params.put("name", name);
		    params.put("service", Commons.FOURSQUARE);
		    utils.sendPostRequest(url, params, true);
		    
		    message = utils.getResponseErrorMessage(url);
		    int responseCode = utils.getResponseCode(url);
	        if (responseCode == HttpURLConnection.HTTP_OK) {
	            message = Locale.getMessage(R.string.Social_comment_sent);
	        } else {
	        	if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
		             logout();
		        }
		        message = Locale.getMessage(R.string.Social_comment_failed, message);
	        }
		} catch (Exception ex) {
			LoggerUtils.error("FoursqureUtils.sendComment() exception", ex);
			message = Locale.getMessage(R.string.Http_error,ex.getMessage());
		} finally {
			try {
				if (utils != null) {
					utils.close();
				}
			} catch (Exception e) {
			}
		}
		return message;
    }

    public String addPlace(String name, String desc, String category, double lat, double lng) {
        //fsAddVenue "accessToken", "name", "desc", "catId", "ll"
    	HttpUtils utils = new HttpUtils();
		String message = null;

		try {
			String token = accessToken.getToken();
			if (token == null) {
				logout();
				throw new NullPointerException("Missing FS authentication token!");
			}
			String url = ConfigurationManager.getInstance().getSecuredServerUrl() + "fsAddVenue";			
			Map<String, String> params = new HashMap<String, String>();
	    	params.put("accessToken", token);
		    params.put("ll", Double.toString(lat) + "," + Double.toString(lng));
	        params.put("desc", desc);
	        params.put("name", name);
	        if (StringUtils.isNotEmpty(category)) {
	        	params.put("catId", category);
	        }
		    params.put("service", Commons.FOURSQUARE);
		    utils.sendPostRequest(url, params, true);
		    
		    message = utils.getResponseErrorMessage(url);
		    int responseCode = utils.getResponseCode(url);
	        if (responseCode == HttpURLConnection.HTTP_OK) {
	            message = Locale.getMessage(R.string.venueCreated_success);
	        } else {
	        	if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
		             logout();
		        }
		        message = Locale.getMessage(R.string.venueCreated_failed, message);
	        }
		} catch (Exception ex) {
			LoggerUtils.error("FoursqureUtils.sendComment() exception", ex);
			message = Locale.getMessage(R.string.Http_error,ex.getMessage());
		} finally {
			try {
				if (utils != null) {
					utils.close();
				}
			} catch (Exception e) {
			}
		}
		return message;
    }
    
    public String getKey(String url) {
    	String venueid = url;
		String[] s = venueid.split("/");

		if (s.length > 0) {
			venueid = s[s.length - 1];
		}
		
		return venueid;
    }
}
