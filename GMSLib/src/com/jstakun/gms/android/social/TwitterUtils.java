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

public final class TwitterUtils extends AbstractSocialUtils {
	
	private static final String TWEET_AUTH_KEY = "auth_key";
    private static final String TWEET_AUTH_SECRET_KEY = "auth_secret_key";

    public void storeAccessToken(Token accessToken) {
    	ConfigurationManager.getUserManager().putStringAndEncrypt(TWEET_AUTH_KEY, accessToken.getToken());
        ConfigurationManager.getUserManager().putStringAndEncrypt(TWEET_AUTH_SECRET_KEY, accessToken.getSecret());
    }

    protected Token loadAccessToken() {
        String token = ConfigurationManager.getUserManager().getStringDecrypted(TWEET_AUTH_KEY);
        String tokenSecret = ConfigurationManager.getUserManager().getStringDecrypted(TWEET_AUTH_SECRET_KEY);
        return new Token(token, tokenSecret);
    }
    
    public void logout() {
        ConfigurationManager.getInstance().removeAll(new String[]{
                    TWEET_AUTH_KEY,
                    TWEET_AUTH_SECRET_KEY,
                    ConfigurationManager.TWEET_USERNAME,
                    ConfigurationManager.TWEET_NAME
                });
        ConfigurationManager.getInstance().setOff(ConfigurationManager.TWEET_AUTH_STATUS);
        ConfigurationManager.getInstance().setOff(ConfigurationManager.TWEET_SEND_STATUS);

        super.logout();
    }

    public boolean initOnTokenPresent(JSONObject json) {
        ConfigurationManager.getInstance().setOn(ConfigurationManager.TWEET_AUTH_STATUS);
        ConfigurationManager.getInstance().setOn(ConfigurationManager.TWEET_SEND_STATUS);
        boolean result = false;
        
        String id = json.optString(ConfigurationManager.TWEET_USERNAME);
		if (id != null) {
			ConfigurationManager.getInstance().putString(ConfigurationManager.TWEET_USERNAME, id + "@tw");
			String name = json.optString(ConfigurationManager.TWEET_NAME);
			if (name != null) {
				ConfigurationManager.getInstance().putString(ConfigurationManager.TWEET_NAME, name);	
			} else {
				ConfigurationManager.getInstance().putString(ConfigurationManager.TWEET_NAME, id);	
			}
			result = ConfigurationManager.getDatabaseManager().saveConfiguration(false);
		} 
		if (!result) {
			logout();
		}
		return result;
    }

    public String sendPost(ExtendedLandmark landmark, int type) {
        HttpUtils utils = new HttpUtils();
		String errorMessage = null;

		try {
			String url = ConfigurationManager.getInstance().getSecuredServerUrl() + "twSendUpdate";			
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
			LoggerUtils.error("TwitterUtils.sendMessage() exception", ex);
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
		return errorMessage;
    }
}
