package com.jstakun.gms.android.social;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FavouritesDbDataSource;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;

public final class GMSUtils {
	
	public static final String LOCATION_CHECKIN = "locationCheckIn";
	public static final String QRCODE_CHECKIN = "qrCodeCheckIn";
	
	public static String loginAction(String login, String password) {
        HttpUtils utils = new HttpUtils();
        String errorMessage = null, //encPwd = null, 
        		email = null, user = null, token = null;

        try {
            //ConfigurationManager.getInstance().putObject(ConfigurationManager.GMS_USERNAME, login);
            //ConfigurationManager.getInstance().putObject(ConfigurationManager.GMS_PASSWORD, password);
            String url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "authenticate";
            byte[] resp = utils.loadHttpFile(url, true, "text/json");
            if (resp != null && resp.length > 0) {
                String jsonResp = new String(resp, "UTF-8");   
                if (jsonResp.startsWith("{")) {          
                	JSONObject json = new JSONObject(jsonResp);
                	//encPwd = json.getString("password");               	
                    email = json.optString(ConfigurationManager.USER_EMAIL);
                    user = json.optString(ConfigurationManager.GMS_NAME);
                    token = json.optString(ConfigurationManager.GMS_TOKEN);
                }
                //System.out.println(jsonResp);
            }    
        } catch (Exception ex) {
            LoggerUtils.error("GMSUtils.loginAction() exception", ex);
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

        if (errorMessage == null && token != null) {
        	ConfigurationManager.getInstance().putString(ConfigurationManager.GMS_USERNAME, login);
            ConfigurationManager.getInstance().putString(ConfigurationManager.GMS_TOKEN, token);
            if (StringUtils.isNotEmpty(email)) {
				ConfigurationManager.getUserManager().putStringAndEncrypt(ConfigurationManager.USER_EMAIL, email);
			}
            if (user != null) {
                ConfigurationManager.getInstance().putString(ConfigurationManager.GMS_NAME, user);    
            } else {
                ConfigurationManager.getInstance().putString(ConfigurationManager.GMS_NAME, login);
            }
            ConfigurationManager.getInstance().setOn(ConfigurationManager.GMS_AUTH_STATUS);
            ConfigurationManager.getInstance().putLong(ConfigurationManager.GMS_LOGIN_DATE, System.currentTimeMillis());
            ConfigurationManager.getDatabaseManager().saveConfiguration(false);
        }
        
        return errorMessage;
    }
	
	public static String generateToken(String scope) {
		HttpUtils utils = new HttpUtils();
        String token = null, errorMessage = null;
        
		try {
            String url = ConfigurationManager.getInstance().getSecuredServerUrl() + "token?scope=" + scope;
            byte[] resp = utils.loadHttpFile(url, false, "text/json");
            if (resp != null && resp.length > 0) {
                String jsonResp = new String(resp, "UTF-8");   
                if (jsonResp.startsWith("{")) {          
                	JSONObject json = new JSONObject(jsonResp);
                	token = json.optString(ConfigurationManager.GMS_TOKEN);
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
		
		if (errorMessage == null && token != null) {
			ConfigurationManager.getInstance().putString(ConfigurationManager.GMS_TOKEN, token);
			ConfigurationManager.getDatabaseManager().saveConfiguration(false);
		}
		
		return errorMessage;
	}
	
	public static void logout() {
		ConfigurationManager.getInstance().setOff(ConfigurationManager.GMS_AUTH_STATUS);
        ConfigurationManager.getInstance().removeAll(new String[]{
        		ConfigurationManager.GMS_NAME,		
        		ConfigurationManager.GMS_USERNAME,
        		ConfigurationManager.GMS_LOGIN_DATE});
        ConfigurationManager.getDatabaseManager().saveConfiguration(false);
        
	}
	
	public static String sendComment(String placeId, String commentText) {
        String url = ConfigurationManager.getInstance().getServicesUrl() + "addComment";
        String message = "";
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("key", placeId));
        params.add(new BasicNameValuePair("message", commentText));
        String username = ConfigurationManager.getUserManager().getLoggedInUsername();
        if (username != null) {
            params.add(new BasicNameValuePair("username", username));
        } 
        HttpUtils utils = new HttpUtils();
        try {
            utils.sendPostRequest(url, params, true);
            message = utils.getResponseCodeErrorMessage();
            if (message == null) {
                message = Locale.getMessage(R.string.Social_comment_sent);
            } else {
                message = Locale.getMessage(R.string.Social_comment_failed, message);
            }
        } catch (Exception ex) {
            LoggerUtils.error("CommentActivity.addCommentToServer exception", ex);
            message = Locale.getMessage(R.string.Social_comment_failed, ex.getMessage());
        } finally {
            try {
                if (utils != null) {
                    utils.close();
                }
            } catch (Exception ioe) {
            }
        }
        return message;
    }
	
	public static String checkin(String service, String checkinLandmarkCode, String name) {
 	   String url = ConfigurationManager.getInstance().getServicesUrl() + service; 
 	   List<NameValuePair> params = new ArrayList<NameValuePair>();
 	   params.add(new BasicNameValuePair("key", checkinLandmarkCode));
 	   String username = ConfigurationManager.getUserManager().getLoggedInUsername();
 	   if (username != null) {
 		   params.add(new BasicNameValuePair("username",username));
 	   } 

 	   HttpUtils utils = new HttpUtils();
 	   utils.sendPostRequest(url, params, true);
 	   String msg = utils.getResponseCodeErrorMessage();
 	   int responseCode = utils.getResponseCode();

 	   LoggerUtils.debug("Location check-in at " + checkinLandmarkCode + " response: " + msg);
 	   
 	   if (responseCode == HttpStatus.SC_OK) {
 		   String nameP = name;
 		   if (nameP == null) {
 			   nameP = utils.getHeader("name");
 			   if (nameP == null) {
 				   nameP = "Landmark";
 			   }
 		   }
 		   FavouritesDbDataSource fdb = (FavouritesDbDataSource) ConfigurationManager.getInstance().getObject("FAVOURITESDB", FavouritesDbDataSource.class);
 		   LoggerUtils.debug("Updating check-in with key " + checkinLandmarkCode);
 	       if (fdb != null) {
            	  fdb.updateOnCheckin(checkinLandmarkCode);
            } else {
         	   LoggerUtils.debug("AsyncTaskmanager.gmsWorldCheckin() fdb == null");
            }
 		   msg = Locale.getMessage(R.string.Social_checkin_success, nameP);
 	   } else {
 		   msg = Locale.getMessage(R.string.Social_checkin_failure, msg);
 	   }
 	   try {
 		   if (utils != null) {
 			   utils.close();
 		   }
 	   } catch (Exception ioe) {
 	   }

 	   return msg;
    }
}
