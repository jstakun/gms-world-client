package com.jstakun.gms.android.social;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
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
public final class GoogleUtils extends AbstractSocialUtils {

	private static final String GL_AUTH_KEY = "glauth_key";
    private static final String GL_REFRESH_TOKEN = "glRefreshToken";
    private static final String GL_AUTH_SECRET_KEY = "glauth_secret_key";
    
	public void storeAccessToken(Token accessToken) {
		super.storeAccessToken(accessToken);
		ConfigurationManager.getUserManager().putStringAndEncrypt(GL_AUTH_KEY, accessToken.getToken());
		ConfigurationManager.getUserManager().putStringAndEncrypt(GL_AUTH_SECRET_KEY, accessToken.getSecret());
	}

	protected Token loadAccessToken() {
		String token = ConfigurationManager.getUserManager().getStringDecrypted(GL_AUTH_KEY);
		String tokenSecret = ConfigurationManager.getUserManager().getStringDecrypted(GL_AUTH_SECRET_KEY);
		return new Token(token, tokenSecret);
	}

	public void logout() {
		ConfigurationManager.getInstance().removeAll(
				new String[] { GL_AUTH_KEY,
					    GL_AUTH_SECRET_KEY,
						GL_REFRESH_TOKEN,
						ConfigurationManager.GL_EXPIRES_IN,
						ConfigurationManager.GL_USERNAME,
						ConfigurationManager.GL_NAME,
						ConfigurationManager.GL_GENDER,
						ConfigurationManager.GL_BIRTHDAY,
						ConfigurationManager.GL_LOGIN_DATE});
		ConfigurationManager.getInstance().setOff(ConfigurationManager.GL_AUTH_STATUS);
		ConfigurationManager.getInstance().setOff(ConfigurationManager.GL_SEND_STATUS);

		super.logout();
	}
	
	public String sendPost(ExtendedLandmark landmark, int type) {
		String errorMessage = null;
		String refreshToken = ConfigurationManager.getInstance().getString(GL_REFRESH_TOKEN);
		long expires_in = ConfigurationManager.getInstance().getLong(ConfigurationManager.GL_EXPIRES_IN);
		String token = accessToken.getToken();
		
		if ((expires_in > System.currentTimeMillis() && StringUtils.isNotEmpty(token)) || StringUtils.isNotEmpty(refreshToken)) {
			HttpUtils utils = new HttpUtils();
		
			try {
				String url = ConfigurationManager.getInstance().getSecuredServerUrl() + "glSendPost";			
				Map<String, String> params = new HashMap<String, String>();
		    	if (expires_in > System.currentTimeMillis()) {
					params.put("token", token);
				}
				params.put("refresh_token", refreshToken);
				if (landmark != null) {
					String key = landmark.getServerKey();
					if (key != null) {
						params.put("key", key);
					}
				}
		    	utils.sendPostRequest(url, params, true);
		    	errorMessage = utils.getResponseErrorMessage(url);
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
		} else {
			logout();
			errorMessage = Locale.getMessage(R.string.Social_token_expired, "Google"); 
		}
		return errorMessage;
	}

	public boolean initOnTokenPresent(JSONObject json) {
		ConfigurationManager.getInstance().setOn(ConfigurationManager.GL_AUTH_STATUS);
		ConfigurationManager.getInstance().setOn(ConfigurationManager.GL_SEND_STATUS);
		boolean result = false;
		try {
			
			long expires_in = json.optLong(ConfigurationManager.GL_EXPIRES_IN, -1);
			
			if (expires_in > 0) {
				ConfigurationManager.getInstance().putLong(
						ConfigurationManager.GL_EXPIRES_IN,
						System.currentTimeMillis() + (expires_in * 1000));
			}
			
			ConfigurationManager.getInstance().putString(ConfigurationManager.GMS_TOKEN, json.getString(ConfigurationManager.GMS_TOKEN));
			
			String refreshToken = json.optString("refresh_token");
			if (StringUtils.isNotEmpty(refreshToken)) {
 				ConfigurationManager.getInstance().putString(GL_REFRESH_TOKEN, refreshToken);
 				LoggerUtils.debug("Setting gl refresh token");
			}
						
			String userid = json.getString(ConfigurationManager.GL_USERNAME);
			ConfigurationManager.getInstance().putString(ConfigurationManager.GL_USERNAME,userid + "@" + Commons.GOOGLE);
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
				ConfigurationManager.getUserManager().putStringAndEncrypt(ConfigurationManager.USER_EMAIL, email);
			}
			
			ConfigurationManager.getInstance().putLong(ConfigurationManager.GL_LOGIN_DATE, System.currentTimeMillis());
			
			result = ConfigurationManager.getDatabaseManager().saveConfiguration(false);
        } catch (Exception ex) {
			LoggerUtils.error("GoogleUtils.initOnTokenPresent() exception", ex);
		} finally {
            if (!result) {
                logout();
            }
        }
        return result;
	}

	public String checkin(String reference, String name, Double lat, Double lng) {
		HttpUtils utils = new HttpUtils();
		String message = null;

		try {
			String url = ConfigurationManager.getInstance().getSecuredServerUrl() + "socialCheckin";			
			Map<String, String> params = new HashMap<String, String>();
	    	params.put("reference", reference);
		    params.put("service", Commons.GOOGLE);
		    params.put("lat", StringUtil.formatCoordE6(lat));
		    params.put("lng", StringUtil.formatCoordE6(lng));
		    utils.sendPostRequest(url, params, true);
		    
		    message = utils.getResponseErrorMessage(url);
		    int responseCode = utils.getResponseCode(url);
	        if (responseCode == HttpURLConnection.HTTP_OK) {
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
