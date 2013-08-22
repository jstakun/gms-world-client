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
public class LinkedInUtils extends AbstractSocialUtils {

	public void storeAccessToken(Token accessToken) {
		/*try {
			String encToken = new String(Base64.encode(BCTools
					.encrypt(accessToken.getToken().getBytes())));
			String encSecret = new String(Base64.encode(BCTools
					.encrypt(accessToken.getSecret().getBytes())));
			ConfigurationManager.getInstance().putString(ConfigurationManager.LN_AUTH_KEY, encToken);
			ConfigurationManager.getInstance().putString(ConfigurationManager.LN_AUTH_SECRET_KEY, encSecret);
			this.accessToken = accessToken;
		} catch (Exception e) {
			LoggerUtils.error("LinkedInUtils.storeAccessToken error: ", e);
		}*/
		ConfigurationManager.getInstance().putStringAndEncrypt(ConfigurationManager.LN_AUTH_KEY, accessToken.getToken());
		ConfigurationManager.getInstance().putStringAndEncrypt(ConfigurationManager.LN_AUTH_SECRET_KEY, accessToken.getSecret());
	}

	protected Token loadAccessToken() {
		String token = ConfigurationManager.getInstance().getStringDecrypted(ConfigurationManager.LN_AUTH_KEY);
		String tokenSecret = ConfigurationManager.getInstance().getStringDecrypted(ConfigurationManager.LN_AUTH_SECRET_KEY);

		/*try {
			String encToken = ConfigurationManager.getInstance().getString(
					ConfigurationManager.LN_AUTH_KEY);
			String encTokenSecret = ConfigurationManager.getInstance()
					.getString(ConfigurationManager.LN_AUTH_SECRET_KEY);

			if (encToken != null) {
				token = new String(BCTools.decrypt(Base64.decode(encToken
						.getBytes())));
			}

			if (encTokenSecret != null) {
				tokenSecret = new String(BCTools.decrypt(Base64
						.decode(encTokenSecret.getBytes())));
			}
		} catch (Exception e) {
			LoggerUtils.error("LinkedInUtils.loadAccessToken error: ", e);
			token = ConfigurationManager.getInstance().getString(
					ConfigurationManager.LN_AUTH_KEY);
			tokenSecret = ConfigurationManager.getInstance().getString(
					ConfigurationManager.LN_AUTH_SECRET_KEY);
		}*/

		if (null != token && null != tokenSecret) {
			return new Token(token, tokenSecret);
		} else {
			return null;
		}
	}

	public boolean initOnTokenPresent(JSONObject json) {
		ConfigurationManager.getInstance().setOn(ConfigurationManager.LN_AUTH_STATUS);
		ConfigurationManager.getInstance().setOn(ConfigurationManager.LN_SEND_STATUS);
		//if (ConfigurationManager.getInstance().isDefaultUser()) {
		//	ConfigurationManager.getInstance().setAppUser();
		//}

		String id = json.optString(ConfigurationManager.LN_USERNAME);
		if (id != null) {
			ConfigurationManager.getInstance().putString(
					ConfigurationManager.LN_USERNAME, id + "@ln");
			String name = json.optString(ConfigurationManager.LN_NAME);

			long expires_in = json.optLong(ConfigurationManager.LN_EXPIRES_IN,
					-1);

			if (expires_in > 0) {
				ConfigurationManager.getInstance().putLong(
						ConfigurationManager.LN_EXPIRES_IN,
						System.currentTimeMillis() + (expires_in * 1000));
			}
			if (name != null) {
				ConfigurationManager.getInstance().putString(
						ConfigurationManager.LN_NAME, name);
				ConfigurationManager.getInstance().saveConfiguration(false);
				return true;
			} else {
				logout();
				return false;
			}
		} else {
			logout();
			return false;
		}
	}

	public String sendPost(ExtendedLandmark landmark, int type) {
		String errorMessage = null;

		long expires_in = ConfigurationManager.getInstance().getLong(
				ConfigurationManager.LN_EXPIRES_IN);
		if (expires_in > 0 && expires_in < System.currentTimeMillis()) {
			logout();
			errorMessage = Locale.getMessage(R.string.Social_token_expired,
					"LinkedIn");
		} else {
			HttpUtils utils = new HttpUtils();

			try {
				String url = ConfigurationManager.getInstance()
						.getSecuredServicesUrl() + "lnSendUpdate";
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("token", accessToken
						.getToken()));
				params.add(new BasicNameValuePair("secret", accessToken
						.getSecret()));
				if (landmark != null) {
					String key = landmark.getServerKey();
					if (key != null) {
						params.add(new BasicNameValuePair("key", key));
					}
				}
				utils.sendPostRequest(url, params, true);
			} catch (Exception ex) {
				LoggerUtils.error("LinekInUtils.sendMessage() exception", ex);
				errorMessage = Locale.getMessage(R.string.Http_error,
						ex.getMessage());
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
				new String[] { ConfigurationManager.LN_AUTH_KEY,
						ConfigurationManager.LN_AUTH_SECRET_KEY,
						ConfigurationManager.LN_USERNAME,
						ConfigurationManager.LN_EXPIRES_IN,
						ConfigurationManager.LN_NAME });
		ConfigurationManager.getInstance().setOff(
				ConfigurationManager.LN_AUTH_STATUS);
		ConfigurationManager.getInstance().setOff(
				ConfigurationManager.LN_SEND_STATUS);

		super.logout();
	}
}
