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

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.BCTools;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.Token;

/**
 * 
 * @author jstakun
 */
public class GoogleUtils extends AbstractSocialUtils {

	public void storeAccessToken(Token accessToken) {
		String encToken = null, encSecret = null;
		
		try {
			encToken = new String(Base64.encode(BCTools.encrypt(accessToken.getToken().getBytes())));
			encSecret = new String(Base64.encode(BCTools.encrypt(accessToken.getSecret().getBytes())));
		} catch (Exception e) {
			LoggerUtils.error("GoogleUtils.storeAccessToken() exception: ", e);
		}

		ConfigurationManager.getInstance().putString(ConfigurationManager.GL_AUTH_KEY, encToken);
		ConfigurationManager.getInstance().putString(ConfigurationManager.GL_AUTH_SECRET_KEY, encSecret);
		this.accessToken = accessToken;
	}

	protected Token loadAccessToken() {
		String token = null;
		String tokenSecret = null;

		try {
			String encToken = ConfigurationManager.getInstance().getString(ConfigurationManager.GL_AUTH_KEY);
			String encTokenSecret = ConfigurationManager.getInstance().getString(ConfigurationManager.GL_AUTH_SECRET_KEY);
			
			if (encToken != null) {
				token = new String(BCTools.decrypt(Base64.decode(encToken.getBytes())));
			}

			if (encTokenSecret != null) {
				tokenSecret = new String(BCTools.decrypt(Base64.decode(encTokenSecret.getBytes())));
			}
		} catch (Exception e) {
			LoggerUtils.error("GoogleUtils.loadAccessToken() exception: ", e);
			token = ConfigurationManager.getInstance().getString(ConfigurationManager.GL_AUTH_KEY);
			tokenSecret = ConfigurationManager.getInstance().getString(ConfigurationManager.GL_AUTH_SECRET_KEY);
		}
	
		if (null != token) {
			return new Token(token, tokenSecret);
		} else {
			return null;
		}
	}

	public void logout() {
		ConfigurationManager.getInstance().removeAll(
				new String[] { ConfigurationManager.GL_AUTH_KEY,
						ConfigurationManager.GL_AUTH_SECRET_KEY,
						ConfigurationManager.GL_REFRESH_TOKEN,
						ConfigurationManager.GL_EXPIRES_IN,
						ConfigurationManager.GL_USERNAME,
						ConfigurationManager.GL_GENDER,
						ConfigurationManager.GL_BIRTHDAY });
		ConfigurationManager.getInstance().setOff(ConfigurationManager.GL_AUTH_STATUS);
		ConfigurationManager.getInstance().setOff(ConfigurationManager.GL_SEND_STATUS);

		super.logout();
	}
	
	public String sendPost(ExtendedLandmark landmark, int type) {
		//send to glSendPost with parameters: key, token, refresh_token
		String errorMessage = null;
		String refreshToken = ConfigurationManager.getInstance().getString(ConfigurationManager.GL_REFRESH_TOKEN);
		long expires_in = ConfigurationManager.getInstance().getLong(ConfigurationManager.GL_EXPIRES_IN);
		
		if (expires_in > 0 && expires_in < System.currentTimeMillis() && refreshToken == null) {
			logout();
			errorMessage = Locale.getMessage(R.string.Social_token_expired, "Google"); 
		} else {
			HttpUtils utils = new HttpUtils();
		
			try {
				String url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "glSendPost";			
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				if (expires_in > System.currentTimeMillis()) {
					params.add(new BasicNameValuePair("token", accessToken.getToken()));
				}
				params.add(new BasicNameValuePair("refresh_token", refreshToken));
				if (landmark != null) {
					String key = landmark.getServerKey();
					if (key != null) {
						params.add(new BasicNameValuePair("key", key));
					}
				}
		    	utils.sendPostRequest(url, params, true);
		    
			} catch (Exception ex) {
				LoggerUtils.error("GoogleUtils.sendMessage() exception", ex);
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

	public boolean initOnTokenPresent(JSONObject json) {
		ConfigurationManager.getInstance().setOn(ConfigurationManager.GL_AUTH_STATUS);
		ConfigurationManager.getInstance().setOn(ConfigurationManager.GL_SEND_STATUS);
		if (ConfigurationManager.getInstance().isDefaultUser()) {
			ConfigurationManager.getInstance().setAppUser();
		}
		
		try {
			long expires_in = json.optLong(ConfigurationManager.GL_EXPIRES_IN, -1);
			
			if (expires_in > 0) {
				ConfigurationManager.getInstance().putLong(
						ConfigurationManager.GL_EXPIRES_IN,
						System.currentTimeMillis() + (expires_in * 1000));
			}
			
			String refreshToken = json.optString("refresh_token");
			if (refreshToken != null) {
 				ConfigurationManager.getInstance().putString(ConfigurationManager.GL_REFRESH_TOKEN, refreshToken);
			}
						
			String userid = json.getString(ConfigurationManager.GL_USERNAME);
			ConfigurationManager.getInstance().putString(
					ConfigurationManager.GL_USERNAME,
					userid + "@gg");
			
			String username = json.optString(ConfigurationManager.GL_NAME);
			if (username != null) {
				ConfigurationManager.getInstance().putString(ConfigurationManager.GL_NAME, username);
			} else {
				ConfigurationManager.getInstance().putString(ConfigurationManager.GL_NAME, userid);
			}
			
			String gender = json.optString(ConfigurationManager.GL_GENDER);
            String birthday = json.optString(ConfigurationManager.GL_BIRTHDAY);
            String displayName = json.optString(ConfigurationManager.GL_NAME);
			
			//set GL_GENDER
            if (gender != null) {
                ConfigurationManager.getInstance().putString(ConfigurationManager.GL_GENDER, gender);
            }

            //set GL_BIRTHDAY
            if (birthday != null) {
                ConfigurationManager.getInstance().putString(ConfigurationManager.GL_BIRTHDAY, birthday);
                //YYYY-MM-DD
            }

            if (displayName != null) {
                ConfigurationManager.getInstance().putString(ConfigurationManager.GL_NAME, displayName);
            }
            
            String email = json.optString(ConfigurationManager.USER_EMAIL);
			if (StringUtils.isNotEmpty(email)) {
				email = new String(Base64.encode(BCTools.encrypt(email.getBytes())));
				ConfigurationManager.getInstance().putString(ConfigurationManager.USER_EMAIL, email);
			}
			
			ConfigurationManager.getInstance().saveConfiguration(false);
            return true;
		} catch (Exception ex) {
			logout();
			LoggerUtils.error("GoogleUtils.initOnTokenPresent() exception", ex);
			return false;
		}
	}

	public String checkin(String reference, String name) {
		HttpUtils utils = new HttpUtils();
		String message = null;

		try {
			String url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "socialCheckin";			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
		    params.add(new BasicNameValuePair("reference", reference));
		    params.add(new BasicNameValuePair("service", Commons.GOOGLE));
		    utils.sendPostRequest(url, params, true);
		    
		    message = utils.getResponseCodeErrorMessage();
	        if (message == null) {
	                message = Locale.getMessage(R.string.Social_checkin_success, name);
	                onCheckin(reference);
	        } else {
	                message = Locale.getMessage(R.string.Social_checkin_failure, message);
	        }
	        
		    
		} catch (Exception ex) {
			LoggerUtils.error("GoogleUtils.checkin() exception", ex);
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
}
