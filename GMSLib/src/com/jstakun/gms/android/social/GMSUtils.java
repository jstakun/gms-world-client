package com.jstakun.gms.android.social;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.LoggerUtils;

public final class GMSUtils {
	public static String loginAction(String login, String password) {
        HttpUtils utils = new HttpUtils();
        String errorMessage = null, encPwd = null, email = null, user = null;

        try {
            ConfigurationManager.getInstance().putObject(ConfigurationManager.GMS_USERNAME, login);
            ConfigurationManager.getInstance().putObject(ConfigurationManager.GMS_PASSWORD, password);
            String url = ConfigurationManager.SSL_SERVER_SERVICES_URL + "authenticate";
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("mobile", "true"));
            byte[] resp = utils.loadHttpFile(url, true, "text/json");
            if (resp != null && resp.length > 0) {
                String jsonResp = new String(resp, "UTF-8");   
                if (jsonResp.startsWith("{")) {          
                	JSONObject json = new JSONObject(jsonResp);
                	encPwd = json.getString("password");               	
                    email = json.optString(ConfigurationManager.USER_EMAIL);
                    user =  json.optString(ConfigurationManager.GMS_NAME);
                }
                //System.out.println(jsonResp);
            }    
        } catch (Exception ex) {
            LoggerUtils.error("LoginActivity.loginAction exception", ex);
            errorMessage = ex.getMessage();
        } finally {
            try {
                if (utils != null) {
                    errorMessage = utils.getResponseCodeErrorMessage();
                    utils.close();
                }
            } catch (Exception e) {
            }
        }

        if (errorMessage == null && encPwd != null) {
        	ConfigurationManager.getInstance().putString(ConfigurationManager.GMS_USERNAME, login);
            ConfigurationManager.getInstance().putString(ConfigurationManager.GMS_PASSWORD, encPwd);
            if (StringUtils.isNotEmpty(email)) {
				ConfigurationManager.getUserManager().putStringAndEncrypt(ConfigurationManager.USER_EMAIL, email);
			}
            if (user != null) {
                ConfigurationManager.getInstance().putString(ConfigurationManager.GMS_NAME, user);    
            } else {
                ConfigurationManager.getInstance().putString(ConfigurationManager.GMS_NAME, login);
            }
            ConfigurationManager.getInstance().setOn(ConfigurationManager.GMS_AUTH_STATUS);
            ConfigurationManager.getDatabaseManager().saveConfiguration(false);
        }
        
        return errorMessage;
    }
	
	public static void logout() {
		ConfigurationManager.getInstance().setOff(ConfigurationManager.GMS_AUTH_STATUS);
        ConfigurationManager.getInstance().removeAll(new String[]{
        		ConfigurationManager.GMS_NAME,		
        		ConfigurationManager.GMS_USERNAME, 
        		ConfigurationManager.GMS_PASSWORD});
        ConfigurationManager.getDatabaseManager().saveConfiguration(false);
        
	}
}
