/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.social;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.Token;

/**
 * 
 * @author jstakun
 */
public final class LinkedInUtils extends AbstractSocialUtils {

	private static final String LN_AUTH_KEY = "lnauth_key";
    private static final String LN_AUTH_SECRET_KEY = "lnauth_secret_key";
    
	public void storeAccessToken(Token accessToken) {
		ConfigurationManager.getUserManager().putStringAndEncrypt(LN_AUTH_KEY, accessToken.getToken());
		ConfigurationManager.getUserManager().putStringAndEncrypt(LN_AUTH_SECRET_KEY, accessToken.getSecret());
	}

	protected Token loadAccessToken() {
		String token = ConfigurationManager.getUserManager().getStringDecrypted(LN_AUTH_KEY);
		String tokenSecret = ConfigurationManager.getUserManager().getStringDecrypted(LN_AUTH_SECRET_KEY);
		return new Token(token, tokenSecret);
	}

	public boolean initOnTokenPresent(JSONObject json) {
		ConfigurationManager.getInstance().setOn(ConfigurationManager.LN_AUTH_STATUS);
		ConfigurationManager.getInstance().setOn(ConfigurationManager.LN_SEND_STATUS);
		boolean result = false;
		
		String id = json.optString(ConfigurationManager.LN_USERNAME);
		if (id != null) {
			ConfigurationManager.getInstance().putString(ConfigurationManager.LN_USERNAME, id + "@ln");
			String name = json.optString(ConfigurationManager.LN_NAME);

			long expires_in = json.optLong(ConfigurationManager.LN_EXPIRES_IN,-1);

			if (expires_in > 0) {
				ConfigurationManager.getInstance().putLong(ConfigurationManager.LN_EXPIRES_IN,
						System.currentTimeMillis() + (expires_in * 1000));
			}
			if (name != null) {
				ConfigurationManager.getInstance().putString(ConfigurationManager.LN_NAME, name);
				result = ConfigurationManager.getDatabaseManager().saveConfiguration(false);
			} 
		} 
		if (!result) {
			logout();
        }
        return result;
	}

	public String sendPost(ExtendedLandmark landmark, int type) {
		String errorMessage = null;

		long expires_in = ConfigurationManager.getInstance().getLong(ConfigurationManager.LN_EXPIRES_IN);
		if (expires_in > 0 && expires_in < System.currentTimeMillis()) {
			logout();
			errorMessage = Locale.getMessage(R.string.Social_token_expired, "LinkedIn");
		} else {
			HttpUtils utils = new HttpUtils();

			try {
				String url = ConfigurationManager.getInstance().getSecuredServerUrl() + "lnSendUpdate";
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("token", accessToken.getToken()));
				params.add(new BasicNameValuePair("secret", accessToken.getSecret()));
				if (landmark != null) {
					String key = landmark.getServerKey();
					if (key != null) {
						params.add(new BasicNameValuePair("key", key));
					}
				}
				utils.sendPostRequest(url, params, true);
			} catch (Exception ex) {
				LoggerUtils.error("LinekInUtils.sendMessage() exception", ex);
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

	public void logout() {
		ConfigurationManager.getInstance().removeAll(
				new String[] { LN_AUTH_KEY,
						LN_AUTH_SECRET_KEY,
						ConfigurationManager.LN_USERNAME,
						ConfigurationManager.LN_EXPIRES_IN,
						ConfigurationManager.LN_NAME });
		ConfigurationManager.getInstance().setOff(ConfigurationManager.LN_AUTH_STATUS);
		ConfigurationManager.getInstance().setOff(ConfigurationManager.LN_SEND_STATUS);

		super.logout();
	}
}
