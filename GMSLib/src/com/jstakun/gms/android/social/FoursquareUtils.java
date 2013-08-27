/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.social;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.Token;

/**
 *
 * @author jstakun
 */
public final class FoursquareUtils extends AbstractSocialUtils {

	private static final String FS_AUTH_KEY = "fsauth_key";
    
    public FoursquareUtils() {
    	super();
        ConfigurationManager.getInstance().setDisabled(ConfigurationManager.FS_SEND_STATUS);
    }

    public void storeAccessToken(Token accessToken)  {
    	try {
    		ConfigurationManager.getUserManager().putStringAndEncrypt(FS_AUTH_KEY, accessToken.getToken());
    		this.accessToken = accessToken;
    	} catch (Exception e) {
			LoggerUtils.error("FoursquareUtils.storeAccessToken exception: ", e);
		}
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
            ConfigurationManager.getInstance().putString(ConfigurationManager.FS_USERNAME, id + "@fs");
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
			
			ConfigurationManager.getDatabaseManager().saveConfiguration(false);
			
            result = true;
        } catch (Exception ex) {
            LoggerUtils.error("FoursquareUtils.sendPost exception", ex);
            result = false;
        } finally {
            if (!result) {
                logout();
            }
        }
        return result;
    }

    public String checkin(String venueId, String name) {
        //socialCheckin  accessToken, venueId, name, service
        HttpUtils utils = new HttpUtils();
		String message = null;

		try {
			String url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "socialCheckin";			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
		    params.add(new BasicNameValuePair("accessToken", accessToken.getToken()));
		    params.add(new BasicNameValuePair("venueId", venueId));
		    params.add(new BasicNameValuePair("name", name));
		    params.add(new BasicNameValuePair("service", Commons.FOURSQUARE));
		    utils.sendPostRequest(url, params, true);
		    
		    message = utils.getResponseCodeErrorMessage();
	        if (message == null) {
	                message = Locale.getMessage(R.string.Social_checkin_success, name);
	                onCheckin(venueId);
	        } else {
	                message = Locale.getMessage(R.string.Social_checkin_failure, message);
	        }
	        
		    
		} catch (Exception ex) {
			LoggerUtils.error("FoursquareUtils.checkin() exception", ex);
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

    public String sendComment(String venueId, String tip, String name) {
    	//socialComment  accessToken, venueId, name
    	HttpUtils utils = new HttpUtils();
		String message = null;

		try {
			String url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "socialComment";			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
		    params.add(new BasicNameValuePair("accessToken", accessToken.getToken()));
		    params.add(new BasicNameValuePair("venueId", venueId));
		    params.add(new BasicNameValuePair("text", tip));
		    params.add(new BasicNameValuePair("name", name));
		    params.add(new BasicNameValuePair("service", Commons.FOURSQUARE));
		    utils.sendPostRequest(url, params, true);
		    
		    message = utils.getResponseCodeErrorMessage();
		    if (message == null) {
	            message = Locale.getMessage(R.string.Social_comment_sent);
	        } else {
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
			String url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "fsAddVenue";			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
		    params.add(new BasicNameValuePair("accessToken", accessToken.getToken()));
		    params.add(new BasicNameValuePair("ll", Double.toString(lat) + "," + Double.toString(lng)));
	        params.add(new BasicNameValuePair("desc", desc));
	        params.add(new BasicNameValuePair("name", name));
	        params.add(new BasicNameValuePair("catId", category));
		    params.add(new BasicNameValuePair("service", "fs"));
		    utils.sendPostRequest(url, params, true);
		    
		    message = utils.getResponseCodeErrorMessage();
		    if (message == null) {
	            message = Locale.getMessage(R.string.venueCreated_success);
	        } else {
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
