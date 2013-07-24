/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.social;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.bouncycastle.util.encoders.Base64;
import org.json.JSONObject;
import com.jstakun.gms.android.utils.Token;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.BCTools;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;

public class TwitterUtils extends AbstractSocialUtils {

    public void storeAccessToken(Token accessToken) {
    	try {
        String encToken = new String(Base64.encode(BCTools.encrypt(accessToken.getToken().getBytes())));
        String encSecret = new String(Base64.encode(BCTools.encrypt(accessToken.getSecret().getBytes())));
        ConfigurationManager.getInstance().putString(ConfigurationManager.TWEET_AUTH_KEY, encToken);
        ConfigurationManager.getInstance().putString(ConfigurationManager.TWEET_AUTH_SECRET_KEY, encSecret);
        this.accessToken = accessToken;
    	} catch (Exception e) {
			LoggerUtils.error("TwitterUtils.storeAccessToken error: ", e);
		}
    }

    protected Token loadAccessToken() {
        String token = null;
        String tokenSecret = null;

        try {
            String encToken = ConfigurationManager.getInstance().getString(ConfigurationManager.TWEET_AUTH_KEY);
            String encTokenSecret = ConfigurationManager.getInstance().getString(ConfigurationManager.TWEET_AUTH_SECRET_KEY);
            if (encToken != null) {
                token = new String(BCTools.decrypt(Base64.decode(encToken.getBytes())));
            }

            if (encTokenSecret != null) {
                tokenSecret = new String(BCTools.decrypt(Base64.decode(encTokenSecret.getBytes())));
            }
        } catch (Exception e) {
            LoggerUtils.error("TwitterUtils.loadAccessToken error: ", e);
            token = ConfigurationManager.getInstance().getString(ConfigurationManager.TWEET_AUTH_KEY);
            tokenSecret = ConfigurationManager.getInstance().getString(ConfigurationManager.TWEET_AUTH_SECRET_KEY);
        }

        if (null != token && null != tokenSecret) {
            return new Token(token, tokenSecret);
        } else {
            return null;
        }
    }
    
    public void logout() {
        ConfigurationManager.getInstance().removeAll(new String[]{
                    ConfigurationManager.TWEET_AUTH_KEY,
                    ConfigurationManager.TWEET_AUTH_SECRET_KEY,
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
        if (ConfigurationManager.getInstance().isDefaultUser()) {
            ConfigurationManager.getInstance().setAppUser();
        }
        
        String id = json.optString(ConfigurationManager.TWEET_USERNAME);
		if (id != null) {
			ConfigurationManager.getInstance().putString(ConfigurationManager.TWEET_USERNAME, id + "@tw");
			String name = json.optString(ConfigurationManager.TWEET_NAME);
			if (name != null) {
				ConfigurationManager.getInstance().putString(ConfigurationManager.TWEET_NAME, name);	
			} else {
				ConfigurationManager.getInstance().putString(ConfigurationManager.TWEET_NAME, id);	
			}
			ConfigurationManager.getInstance().saveConfiguration(false);
			return true;
		} else {
			logout();
			return false;
		}
    }

    public String sendPost(ExtendedLandmark landmark, int type) {
        HttpUtils utils = new HttpUtils();
		String errorMessage = null;

		try {
			String url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "twSendUpdate";			
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
