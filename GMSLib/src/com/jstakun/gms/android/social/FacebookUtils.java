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
import org.bouncycastle.util.encoders.Base64;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Message;

import com.jstakun.gms.android.utils.Token;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.BCTools;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;

/**
 * 
 * @author jstakun
 */
public class FacebookUtils extends AbstractSocialUtils {

	public static final String FB_OAUTH_ERROR = "Facebook authentication error";

	public boolean initOnTokenPresent(JSONObject json) {
		ConfigurationManager.getInstance().setOn(ConfigurationManager.FB_AUTH_STATUS);
		ConfigurationManager.getInstance().setOn(ConfigurationManager.FB_SEND_STATUS);
		if (ConfigurationManager.getInstance().isDefaultUser()) {
			ConfigurationManager.getInstance().setAppUser();
		}
		
		try {
				ConfigurationManager.getInstance().putString(ConfigurationManager.FB_USERNAME, json.getString(ConfigurationManager.FB_USERNAME) + "@fb");

				if (json.has(ConfigurationManager.FB_NAME)) {
					ConfigurationManager.getInstance().putString(
							ConfigurationManager.FB_NAME, json.getString(ConfigurationManager.FB_NAME));
				}
				if (json.has(ConfigurationManager.FB_GENDER)) {
					ConfigurationManager.getInstance().putString(
							ConfigurationManager.FB_GENDER, json.getString(ConfigurationManager.FB_GENDER));
				}
				if (json.has(ConfigurationManager.FB_BIRTHDAY)) {
					ConfigurationManager.getInstance().putString(ConfigurationManager.FB_BIRTHDAY, json.getString(ConfigurationManager.FB_BIRTHDAY));
				}
				String email = json.optString(ConfigurationManager.USER_EMAIL);
				if (StringUtils.isNotEmpty(email)) {
					email = new String(Base64.encode(BCTools.encrypt(email.getBytes())));
					ConfigurationManager.getInstance().putString(ConfigurationManager.USER_EMAIL, email);
				}

				long expires_in = json.optLong(ConfigurationManager.FB_EXPIRES_IN, -1);
				
				if (expires_in > 0) {
					ConfigurationManager.getInstance().putLong(
							ConfigurationManager.FB_EXPIRES_IN,
							System.currentTimeMillis() + (expires_in * 1000));
				}
				
				ConfigurationManager.getInstance().saveConfiguration(false);
				
			return true;
		} catch (Exception ex) {
			logout();
			LoggerUtils.error("FacebookUtils.initOnTokenPresent error:", ex);
			return false;
		}
	}

	public void storeAccessToken(Token accessToken) {
		try {
		String encToken = new String(Base64.encode(BCTools.encrypt(accessToken
				.getToken().getBytes())));
		ConfigurationManager.getInstance().putString(
				ConfigurationManager.FB_AUTH_KEY, encToken);
		this.accessToken = accessToken;
		} catch (Exception e) {
			LoggerUtils.error("FacebookUtils.storeAccessToken error: ", e);
		}
	}
	
	protected Token loadAccessToken() {
		String token = null;
		try {
			String encToken = ConfigurationManager.getInstance().getString(
					ConfigurationManager.FB_AUTH_KEY);
			if (encToken != null) {
				token = new String(BCTools.decrypt(Base64.decode(encToken
						.getBytes())));
			}
		} catch (Exception e) {
			LoggerUtils.error("FacebookUtils.loadAccessToken error: ", e);
			token = ConfigurationManager.getInstance().getString(
					ConfigurationManager.FB_AUTH_KEY);
		}
		if (null != token) {
			return new Token(token, null);
		} else {
			return null;
		}
	}

	public void logout() {
		ConfigurationManager.getInstance().removeAll(
				new String[] { ConfigurationManager.FB_TOKEN,
						ConfigurationManager.FB_AUTH_KEY,
						ConfigurationManager.FB_AUTH_SECRET_KEY,
						ConfigurationManager.FB_USERNAME,
						ConfigurationManager.FB_GENDER,
						ConfigurationManager.FB_BIRTHDAY,
						ConfigurationManager.FB_EXPIRES_IN,
						ConfigurationManager.FB_NAME });
		ConfigurationManager.getInstance().setOff(
				ConfigurationManager.FB_AUTH_STATUS);
		ConfigurationManager.getInstance().setOff(
				ConfigurationManager.FB_SEND_STATUS);

		super.logout();
	}


	public String sendPost(ExtendedLandmark landmark, int type) {
		//fbSendMessage token, key
		//check if token has expired first
		
		String errorMessage = null;
		long expires_in = ConfigurationManager.getInstance().getLong(ConfigurationManager.FB_EXPIRES_IN);
		if (expires_in > 0 && expires_in < System.currentTimeMillis()) {
			logout();
			errorMessage = Locale.getMessage(R.string.Social_token_expired, "Facebook"); 
		} else {
			HttpUtils utils = new HttpUtils();
			
			try {
				String url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "fbSendMessage";			
				List<NameValuePair> params = new ArrayList<NameValuePair>();
		    	params.add(new BasicNameValuePair("token", accessToken.getToken()));
		    	if (landmark != null) {
					String key = landmark.getServerKey();
					if (key != null) {
						params.add(new BasicNameValuePair("key", key));
					}
				}
		    	utils.sendPostRequest(url, params, true);
		    
			} catch (Exception ex) {
				LoggerUtils.error("FacebookUtils.sendMessage() exception", ex);
				errorMessage = Locale.getMessage(R.string.Http_error, ex.getMessage());
			} finally {
				try {
					if (utils != null) {
						utils.close();
					}
				} catch (Exception e) {
				}
			}
		}
		
		
       return errorMessage;
	}

	@Override
	public String checkin(String placeId, String name, Handler notifier) {
		//socialCheckin  accessToken, venueId, name, service
        HttpUtils utils = new HttpUtils();
		String message = null;

		try {
			String url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "socialCheckin";			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
		    params.add(new BasicNameValuePair("accessToken", accessToken.getToken()));
		    params.add(new BasicNameValuePair("venueId", placeId));
		    params.add(new BasicNameValuePair("name", name));
		    params.add(new BasicNameValuePair("service", Commons.FACEBOOK));
		    utils.sendPostRequest(url, params, true);
		    
		    message = utils.getResponseCodeErrorMessage();
	        if (message == null) {
	                message = Locale.getMessage(R.string.Social_checkin_success, name);
	                if (notifier != null) {
	     			   Message msg = notifier.obtainMessage();
	     			   msg.getData().putString("key", placeId);
	     			   notifier.sendMessage(msg);
	     		   }
	        } else {
	                message = Locale.getMessage(R.string.Social_checkin_failure, message);
	        }
	        
		    
		} catch (Exception ex) {
			LoggerUtils.error("FacebookUtils.checkin() exception", ex);
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

	public String sendComment(String placeId, String message, String name) {
		//socialComment  accessToken, venueId, name, message
    	HttpUtils utils = new HttpUtils();
		String errorMessage = null;

		try {
			String url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "socialComment";			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
		    params.add(new BasicNameValuePair("accessToken", accessToken.getToken()));
		    params.add(new BasicNameValuePair("venueId", placeId));
		    params.add(new BasicNameValuePair("text", message));
		    params.add(new BasicNameValuePair("name", name));
		    params.add(new BasicNameValuePair("service", Commons.FACEBOOK));
		    utils.sendPostRequest(url, params, true);
		    
		    errorMessage = utils.getResponseCodeErrorMessage();
		    if (errorMessage == null) {
	            errorMessage = Locale.getMessage(R.string.Social_comment_sent);
	        } else {
	            errorMessage = Locale.getMessage(R.string.Social_comment_failed, errorMessage);
	        }
		} catch (Exception ex) {
			LoggerUtils.error("FacebookUtils.sendComment() exception", ex);
			errorMessage = Locale.getMessage(R.string.Http_error,ex.getMessage());
		} finally {
			try {
				if (utils != null) {
					utils.close();
				}
			} catch (Exception e) {
			}
		}
		return errorMessage;
	}
	
	public String getKey(String url) {
		String venueid = url;
		String[] s = venueid.split("=");

		if (s.length > 0) {
			venueid = s[s.length - 1];
		}
		
		return venueid;
	}
}
